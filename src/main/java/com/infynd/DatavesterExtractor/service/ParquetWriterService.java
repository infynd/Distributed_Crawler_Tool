package com.infynd.DatavesterExtractor.service;

import com.infynd.DatavesterExtractor.dto.CrawlResultDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class ParquetWriterService {

    @Value("${crawler.role:master}")
    private String crawlerRole;

    @Value("${parquet.output.file:crawled_output.parquet}")
    private String parquetFilePath;

    @Value("${parquet.flush.threshold:1000}")
    private int flushThreshold;

    private Connection duckDbConnection;
    private final List<CrawlResultDto> buffer = new ArrayList<>();
    private final Object bufferLock = new Object();
    private final ReentrantLock flushLock = new ReentrantLock();
    private final AtomicLong totalSaved = new AtomicLong(0);

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS crawled_data (" +
            "id BIGINT, input VARCHAR, domain VARCHAR, status VARCHAR, timeStamp VARCHAR, " +
            "title VARCHAR, responseCode VARCHAR, url VARCHAR, webServer VARCHAR, normalizedDomain VARCHAR, " +
            "hostIp VARCHAR, resolver VARCHAR, domainStatus VARCHAR, comments VARCHAR, redirectionStatus VARCHAR, " +
            "loadTimeMs BIGINT, ISO_Flag VARCHAR, Certification_Keyword VARCHAR, " +
            "tradingCompanyName VARCHAR, regCompanyName VARCHAR, metaCompanyName VARCHAR, copyRightCompanyName VARCHAR, " +
            "copyRightCompanyName2 VARCHAR, titleCompanyName VARCHAR, clearBitCompanyName VARCHAR, DescriptionCompanyName VARCHAR, " +
            "Cleaned_copyright_name1 VARCHAR, Cleaned_copyright_name2 VARCHAR, cleaned_name_from_title VARCHAR, description VARCHAR, " +
            "companyNumber VARCHAR, charityNumber VARCHAR, vatNumber VARCHAR, " +
            "emails VARCHAR, genericEmails VARCHAR, nonGenericEmails VARCHAR, phoneNumber VARCHAR, phoneNumberFormatted VARCHAR, " +
            "faxNumber VARCHAR, faxNumberFormatted VARCHAR, neaPresent VARCHAR, neaLinks VARCHAR, branchPresent VARCHAR, " +
            "aboutLinks VARCHAR, contactLInks VARCHAR, jobPresent VARCHAR, jobLinks VARCHAR, googleLinks VARCHAR, " +
            "linkedinLinks VARCHAR, facebookLinks VARCHAR, twitterLinks VARCHAR, instagramLinks VARCHAR, youtubeLinks VARCHAR, " +
            "privacyLinks VARCHAR, termsLinks VARCHAR, vimeoLinks VARCHAR, pinterestLinks VARCHAR, slideShareLinks VARCHAR, " +
            "domainMacthedLinkedinLinks VARCHAR, domainMatchedFacebookLinks VARCHAR, domainMatchedTwitterLinks VARCHAR, " +
            "domainMatchedInstagramLinks VARCHAR, domainMatchedYoutubeLinks VARCHAR, domainMatchedPrivacyLinks VARCHAR, " +
            "domainMatchedTermsLinks VARCHAR, domainMatchedVimeoLinks VARCHAR, domainMatchedPinterestLinks VARCHAR, " +
            "domainMatchedSlideShareLinks VARCHAR, googlePlusLinks VARCHAR, domainMatchedGooglePlusLinks VARCHAR, " +
            "addressPresent VARCHAR, IpCountry VARCHAR, crawledDate VARCHAR, cache VARCHAR, ContactAllLinks VARCHAR, " +
            "AboutAllLink VARCHAR, PrivacyAllLinks VARCHAR, TermsAllLinks VARCHAR, HomeAllLinks VARCHAR, " +
            "addressLine1 VARCHAR, addressLine2 VARCHAR, addressLine3 VARCHAR, city VARCHAR, county VARCHAR, " +
            "zipcode VARCHAR, isHeadQuarters VARCHAR, addressRelatedPhone VARCHAR, addressRelatedFax VARCHAR, " +
            "addressRelatedEmail VARCHAR, addressPattern VARCHAR, splittedAddress VARCHAR" +
            ");";

    private static final String INSERT_SQL =
            "INSERT INTO crawled_data VALUES (" +
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 20
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 20
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 20
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 20
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +                              // 11 -> exactly 91
            ");";

    @PostConstruct
    public void init() {
        if (!"master".equalsIgnoreCase(crawlerRole)) {
            log.info("Node role is [{}]. ParquetWriterService will stay idle (Master only).", crawlerRole);
            return;
        }

        try {
            Class.forName("org.duckdb.DuckDBDriver");
            // Persistent embedded storage ensures data safety
            this.duckDbConnection = DriverManager.getConnection("jdbc:duckdb:crawler_master.duckdb");
            try (Statement stmt = duckDbConnection.createStatement()) {
                stmt.execute(CREATE_TABLE_SQL);
                stmt.execute("PRAGMA threads=4;");
                stmt.execute("PRAGMA max_memory='4GB';");
            }
            log.info("ParquetWriterService initialized on Master. Output target: {}", parquetFilePath);
        } catch (Exception e) {
            log.error("Failed to initialize DuckDB Parquet engine on Master", e);
        }
    }

    /**
     * Non-blocking consumer: accepts incoming crawled results from crawler.results.queue.
     * Appends to buffer in microseconds without waiting for disk or database I/O.
     */
    @RabbitListener(queues = "${crawler.rabbitmq.queue.results:crawler.results.queue}")
    public void onResultReceived(CrawlResultDto result) {
        if (!"master".equalsIgnoreCase(crawlerRole) || result == null) {
            return;
        }

        boolean shouldFlush;
        synchronized (bufferLock) {
            buffer.add(result);
            shouldFlush = buffer.size() >= flushThreshold;
        }

        if (shouldFlush) {
            flushBuffer();
        }
    }

    /**
     * Periodic flush ensures buffered records are written even during low-volume crawls.
     */
    @Scheduled(fixedDelay = 15000)
    public void scheduledFlush() {
        if (!"master".equalsIgnoreCase(crawlerRole)) {
            return;
        }

        boolean hasData;
        synchronized (bufferLock) {
            hasData = !buffer.isEmpty();
        }

        if (hasData) {
            log.debug("Scheduled buffer flush triggered.");
            flushBuffer();
        }
    }

    @PreDestroy
    public void onShutdown() {
        if (!"master".equalsIgnoreCase(crawlerRole)) {
            return;
        }
        log.info("Master shutting down. Flushing all remaining buffered records...");
        flushBuffer();
        exportToParquet();
        try {
            if (duckDbConnection != null && !duckDbConnection.isClosed()) {
                duckDbConnection.close();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Thread-safe, transactional batch insertion into DuckDB.
     * Takes an atomic snapshot of buffer so listener threads are immediately released.
     */
    private void flushBuffer() {
        if (duckDbConnection == null) {
            return;
        }

        // Avoid concurrent DuckDB operations (DuckDB connection is single-threaded)
        if (!flushLock.tryLock()) {
            return;
        }

        List<CrawlResultDto> batch;
        synchronized (bufferLock) {
            if (buffer.isEmpty()) {
                flushLock.unlock();
                return;
            }
            batch = new ArrayList<>(buffer);
            buffer.clear();
        }

        try {
            long start = System.currentTimeMillis();
            int batchSize = batch.size();

            try {
                duckDbConnection.setAutoCommit(false);
                try (PreparedStatement pstmt = duckDbConnection.prepareStatement(INSERT_SQL)) {
                    for (CrawlResultDto r : batch) {
                        int i = 1;
                        pstmt.setObject(i++, r.getId());
                        pstmt.setString(i++, r.getInput());
                        pstmt.setString(i++, r.getDomain());
                        pstmt.setString(i++, r.getStatus());
                        pstmt.setString(i++, r.getTimeStamp());
                        pstmt.setString(i++, r.getTitle());
                        pstmt.setString(i++, r.getResponseCode());
                        pstmt.setString(i++, r.getUrl());
                        pstmt.setString(i++, r.getWebServer());
                        pstmt.setString(i++, r.getNormalizedDomain());
                        pstmt.setString(i++, r.getHostIp());
                        pstmt.setString(i++, r.getResolver());
                        pstmt.setString(i++, r.getDomainStatus());
                        pstmt.setString(i++, r.getComments());
                        pstmt.setString(i++, r.getRedirectionStatus());
                        pstmt.setLong(i++, r.getLoadTimeMs());
                        pstmt.setString(i++, r.getISO_Flag());
                        pstmt.setString(i++, r.getCertification_Keyword());
                        pstmt.setString(i++, r.getTradingCompanyName());
                        pstmt.setString(i++, r.getRegCompanyName());
                        pstmt.setString(i++, r.getMetaCompanyName());
                        pstmt.setString(i++, r.getCopyRightCompanyName());
                        pstmt.setString(i++, r.getCopyRightCompanyName2());
                        pstmt.setString(i++, r.getTitleCompanyName());
                        pstmt.setString(i++, r.getClearBitCompanyName());
                        pstmt.setString(i++, r.getDescriptionCompanyName());
                        pstmt.setString(i++, r.getCleaned_copyright_name1());
                        pstmt.setString(i++, r.getCleaned_copyright_name2());
                        pstmt.setString(i++, r.getCleaned_name_from_title());
                        pstmt.setString(i++, r.getDescription());
                        pstmt.setString(i++, r.getCompanyNumber());
                        pstmt.setString(i++, r.getCharityNumber());
                        pstmt.setString(i++, r.getVatNumber());
                        pstmt.setString(i++, r.getEmails());
                        pstmt.setString(i++, r.getGenericEmails());
                        pstmt.setString(i++, r.getNonGenericEmails());
                        pstmt.setString(i++, r.getPhoneNumber());
                        pstmt.setString(i++, r.getPhoneNumberFormatted());
                        pstmt.setString(i++, r.getFaxNumber());
                        pstmt.setString(i++, r.getFaxNumberFormatted());
                        pstmt.setString(i++, r.getNeaPresent());
                        pstmt.setString(i++, r.getNeaLinks());
                        pstmt.setString(i++, r.getBranchPresent());
                        pstmt.setString(i++, r.getAboutLinks());
                        pstmt.setString(i++, r.getContactLInks());
                        pstmt.setString(i++, r.getJobPresent());
                        pstmt.setString(i++, r.getJobLinks());
                        pstmt.setString(i++, r.getGoogleLinks());
                        pstmt.setString(i++, r.getLinkedinLinks());
                        pstmt.setString(i++, r.getFacebookLinks());
                        pstmt.setString(i++, r.getTwitterLinks());
                        pstmt.setString(i++, r.getInstagramLinks());
                        pstmt.setString(i++, r.getYoutubeLinks());
                        pstmt.setString(i++, r.getPrivacyLinks());
                        pstmt.setString(i++, r.getTermsLinks());
                        pstmt.setString(i++, r.getVimeoLinks());
                        pstmt.setString(i++, r.getPinterestLinks());
                        pstmt.setString(i++, r.getSlideShareLinks());
                        pstmt.setString(i++, r.getDomainMacthedLinkedinLinks());
                        pstmt.setString(i++, r.getDomainMatchedFacebookLinks());
                        pstmt.setString(i++, r.getDomainMatchedTwitterLinks());
                        pstmt.setString(i++, r.getDomainMatchedInstagramLinks());
                        pstmt.setString(i++, r.getDomainMatchedYoutubeLinks());
                        pstmt.setString(i++, r.getDomainMatchedPrivacyLinks());
                        pstmt.setString(i++, r.getDomainMatchedTermsLinks());
                        pstmt.setString(i++, r.getDomainMatchedVimeoLinks());
                        pstmt.setString(i++, r.getDomainMatchedPinterestLinks());
                        pstmt.setString(i++, r.getDomainMatchedSlideShareLinks());
                        pstmt.setString(i++, r.getGooglePlusLinks());
                        pstmt.setString(i++, r.getDomainMatchedGooglePlusLinks());
                        pstmt.setString(i++, r.getAddressPresent());
                        pstmt.setString(i++, r.getIpCountry());
                        pstmt.setString(i++, r.getCrawledDate());
                        pstmt.setString(i++, r.getCache());
                        pstmt.setString(i++, r.getContactAllLinks());
                        pstmt.setString(i++, r.getAboutAllLink());
                        pstmt.setString(i++, r.getPrivacyAllLinks());
                        pstmt.setString(i++, r.getTermsAllLinks());
                        pstmt.setString(i++, r.getHomeAllLinks());
                        pstmt.setString(i++, r.getAddressLine1());
                        pstmt.setString(i++, r.getAddressLine2());
                        pstmt.setString(i++, r.getAddressLine3());
                        pstmt.setString(i++, r.getCity());
                        pstmt.setString(i++, r.getCounty());
                        pstmt.setString(i++, r.getZipcode());
                        pstmt.setString(i++, r.getIsHeadQuarters());
                        pstmt.setString(i++, r.getAddressRelatedPhone());
                        pstmt.setString(i++, r.getAddressRelatedFax());
                        pstmt.setString(i++, r.getAddressRelatedEmail());
                        pstmt.setString(i++, r.getAddressPattern());
                        pstmt.setString(i++, r.getSplittedAddress());

                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                duckDbConnection.commit();
                duckDbConnection.setAutoCommit(true);

                totalSaved.addAndGet(batchSize);
                long elapsed = System.currentTimeMillis() - start;
                log.info("Appended {} records to DuckDB in {} ms. Total saved: {}", batchSize, elapsed, totalSaved.get());

                exportToParquet();

            } catch (Exception batchEx) {
                try {
                    duckDbConnection.rollback();
                    duckDbConnection.setAutoCommit(true);
                } catch (Exception ignored) {}
                log.error("Error writing batch to DuckDB, restoring records to buffer", batchEx);
                synchronized (bufferLock) {
                    buffer.addAll(0, batch);
                }
            }
        } finally {
            flushLock.unlock();
        }
    }

    /**
     * Exports the table into ONE single Snappy-compressed Parquet file.
     * Uses an atomic temp-file swap to avoid "file already exists" errors in DuckDB.
     */
    public synchronized void exportToParquet() {
        if (duckDbConnection == null) return;

        try (Statement stmt = duckDbConnection.createStatement()) {
            File outFile = new File(parquetFilePath);
            File parentDir = outFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            File tmpFile = new File(outFile.getAbsolutePath() + ".tmp");
            if (tmpFile.exists()) {
                tmpFile.delete();
            }

            String exportSql = String.format(
                    "COPY crawled_data TO '%s' (FORMAT PARQUET, COMPRESSION SNAPPY);",
                    tmpFile.getAbsolutePath()
            );
            stmt.execute(exportSql);

            if (tmpFile.exists()) {
                try {
                    java.nio.file.Files.move(
                            tmpFile.toPath(),
                            outFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                            java.nio.file.StandardCopyOption.ATOMIC_MOVE
                    );
                } catch (Exception moveEx) {
                    java.nio.file.Files.move(
                            tmpFile.toPath(),
                            outFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }

            log.info("Parquet export complete -> File: {} (Total records: {})", outFile.getAbsolutePath(), totalSaved.get());
        } catch (Exception e) {
            log.error("Error exporting to Parquet: " + parquetFilePath, e);
        }
    }
}
