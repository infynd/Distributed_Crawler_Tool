package com.infynd.DatavesterExtractor.service;

import com.infynd.DatavesterExtractor.dto.TaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Enterprise Master Task Ingestion Producer.
 * Reads domain inputs from text, CSV, or TSV files with resilient header detection,
 * domain canonicalization, URL cleaning, and high-throughput streaming into RabbitMQ.
 */
@Service
@Slf4j
public class TaskProducerService implements CommandLineRunner {

    private static final int BUFFER_SIZE_BYTES = 64 * 1024; // 64 KB streaming buffer
    private static final List<String> HEADER_KEYWORDS = Arrays.asList(
            "domain", "url", "website", "host", "site", "company_domain", "company_url", "web"
    );

    private static final Pattern PROTOCOL_PREFIX_PATTERN = Pattern.compile("^(?i)[a-z]+://");
    private static final Pattern PORT_PATTERN = Pattern.compile(":\\d+$");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${crawler.rabbitmq.exchange:crawler.direct.exchange}")
    private String exchangeName;

    @Value("${crawler.rabbitmq.routing.tasks:crawler.tasks.key}")
    private String taskRoutingKey;

    @Value("${crawler.role:master}")
    private String crawlerRole;

    @Value("${crawler.input.file:}")
    private String inputFile;

    @Override
    public void run(String... args) {
        if (!"master".equalsIgnoreCase(crawlerRole)) {
            log.info("Current node role is [{}]. TaskProducerService will stay idle (Master only).", crawlerRole);
            return;
        }

        if (inputFile != null && !inputFile.trim().isEmpty()) {
            File file = new File(inputFile.trim());
            if (file.exists() && file.isFile()) {
                log.info("Master detected input file [{}]. Starting ingestion...", file.getAbsolutePath());
                long totalIngested = ingestFromFile(file.getAbsolutePath());
                log.info("Ingestion completed! Pushed total of {} valid tasks to RabbitMQ.", totalIngested);
            } else {
                log.warn("Configured input file [{}] was not found on Master.", inputFile);
            }
        } else {
            log.info("Master started without automatic file ingestion. Configure 'crawler.input.file' or invoke ingestFromFile().");
        }
    }

    /**
     * Streams domains from CSV/TSV/TXT file into RabbitMQ with header detection and canonicalization.
     *
     * @param filePath Absolute or relative path to file.
     * @return Total number of tasks pushed to RabbitMQ.
     */
    public long ingestFromFile(String filePath) {
        AtomicLong taskCounter = new AtomicLong(0);
        long lineCounter = 0;
        long skippedCounter = 0;
        long startTime = System.currentTimeMillis();
        long lastLogTime = startTime;
        long lastTaskCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8), BUFFER_SIZE_BYTES)) {

            String firstLine = reader.readLine();
            if (firstLine == null) {
                log.warn("Input file [{}] is completely empty.", filePath);
                return 0;
            }

            // Remove UTF-8 BOM if present
            if (firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }

            char delimiter = detectDelimiter(firstLine);
            int domainColumnIndex = detectDomainColumnIndex(firstLine, delimiter);
            boolean isHeader = (domainColumnIndex != -1);

            if (isHeader) {
                log.info("Detected CSV header in [{}]. Delimiter: '{}', Domain column index: {}",
                        filePath, delimiter, domainColumnIndex);
            } else {
                domainColumnIndex = 0; // Default to first column for raw domain lists
                lineCounter++;
                String domain = extractDomainFromLine(firstLine, delimiter, domainColumnIndex);
                if (isValidDomain(domain)) {
                    long taskId = taskCounter.incrementAndGet();
                    sendTask(taskId, domain);
                } else {
                    skippedCounter++;
                }
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lineCounter++;
                String raw = line.trim();
                if (raw.isEmpty() || raw.startsWith("#") || raw.startsWith("//")) {
                    skippedCounter++;
                    continue;
                }

                String domain = extractDomainFromLine(raw, delimiter, domainColumnIndex);
                if (isValidDomain(domain)) {
                    long taskId = taskCounter.incrementAndGet();
                    sendTask(taskId, domain);

                    if (taskId % 10_000 == 0) {
                        long now = System.currentTimeMillis();
                        double rate = (taskId - lastTaskCount) / Math.max(0.001, (now - lastLogTime) / 1000.0);
                        log.info("Progress: {} domains pushed | Speed: {} domains/sec | Active queue: {}",
                                taskId, Math.round(rate), taskRoutingKey);
                        lastLogTime = now;
                        lastTaskCount = taskId;
                    }
                } else {
                    skippedCounter++;
                }
            }

        } catch (Exception e) {
            log.error("Fatal error during file ingestion from [{}]: {}", filePath, e.getMessage(), e);
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        double overallRate = taskCounter.get() / Math.max(0.001, totalElapsed / 1000.0);
        log.info("====================================================================");
        log.info("INGESTION FINISHED | File: {}", filePath);
        log.info("Total Lines: {} | Tasks Queued: {} | Skipped: {} | Time: {} ms (avg {} domains/sec)",
                lineCounter, taskCounter.get(), skippedCounter, totalElapsed, Math.round(overallRate));
        log.info("====================================================================");

        return taskCounter.get();
    }

    /**
     * Pushes a single domain task into RabbitMQ.
     */
    public void sendTask(Long id, String domain) {
        TaskMessage task = new TaskMessage(id, domain);
        rabbitTemplate.convertAndSend(exchangeName, taskRoutingKey, task);
    }

    /**
     * Extracts and canonicalizes domain from a line using the detected delimiter and column index.
     */
    private String extractDomainFromLine(String line, char delimiter, int columnIndex) {
        if (line == null) return null;

        String rawToken;
        if (delimiter == '\0') {
            rawToken = line;
        } else {
            String[] tokens = line.split(Pattern.quote(String.valueOf(delimiter)), -1);
            if (columnIndex < tokens.length) {
                rawToken = tokens[columnIndex];
            } else {
                rawToken = tokens[0];
            }
        }

        return canonicalizeDomain(rawToken);
    }

    /**
     * Canonicalizes raw input into clean domain format:
     * - Strips quotes ("example.com" -> example.com)
     * - Strips protocol (https://example.com/foo -> example.com)
     * - Strips path, query params, hash fragments, and ports
     */
    public static String canonicalizeDomain(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim();

        // Strip surrounding quotes
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
                (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            if (cleaned.length() >= 2) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }

        // Strip protocols (http://, https://, ftp://)
        cleaned = PROTOCOL_PREFIX_PATTERN.matcher(cleaned).replaceFirst("");

        // Strip path, query params, hash fragments
        int slashIdx = cleaned.indexOf('/');
        if (slashIdx != -1) {
            cleaned = cleaned.substring(0, slashIdx);
        }
        int queryIdx = cleaned.indexOf('?');
        if (queryIdx != -1) {
            cleaned = cleaned.substring(0, queryIdx);
        }
        int hashIdx = cleaned.indexOf('#');
        if (hashIdx != -1) {
            cleaned = cleaned.substring(0, hashIdx);
        }

        // Strip port if present (e.g. example.com:8080)
        cleaned = PORT_PATTERN.matcher(cleaned).replaceFirst("");

        return cleaned.toLowerCase().trim();
    }

    /**
     * Validates if a string looks like a legitimate domain candidate.
     */
    private boolean isValidDomain(String domain) {
        if (domain == null || domain.isEmpty() || domain.length() < 3 || domain.length() > 255) {
            return false;
        }
        // Must contain at least one dot and no spaces
        return domain.contains(".") && !domain.contains(" ") && !domain.startsWith(".") && !domain.endsWith(".");
    }

    /**
     * Automatically detects whether a CSV/TSV header is present and returns the domain column index.
     * Returns -1 if no header keyword matches.
     */
    private int detectDomainColumnIndex(String headerLine, char delimiter) {
        if (headerLine == null) return -1;

        String[] cols = delimiter == '\0' ? new String[]{headerLine} : headerLine.split(Pattern.quote(String.valueOf(delimiter)));
        for (int i = 0; i < cols.length; i++) {
            String colName = cols[i].replaceAll("[\"']", "").trim().toLowerCase();
            for (String kw : HEADER_KEYWORDS) {
                if (colName.equals(kw) || colName.contains(kw)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Detects delimiter used in the line: comma, tab, semicolon, pipe.
     * Returns '\0' if none found (single column / raw text).
     */
    private char detectDelimiter(String line) {
        if (line == null) return '\0';
        int commas = countOccurrences(line, ',');
        int tabs = countOccurrences(line, '\t');
        int semis = countOccurrences(line, ';');
        int pipes = countOccurrences(line, '|');

        if (commas > 0 && commas >= tabs && commas >= semis && commas >= pipes) return ',';
        if (tabs > 0 && tabs >= semis && tabs >= pipes) return '\t';
        if (semis > 0 && semis >= pipes) return ';';
        if (pipes > 0) return '|';
        return '\0';
    }

    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) count++;
        }
        return count;
    }
}
