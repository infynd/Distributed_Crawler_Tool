package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Component
@Getter
@Slf4j
public class LocationUtilExtrator {
    public static final String FILESEPARATOR = System.getProperty("file.separator");
    public static final String UK_ZIPCODES = "address/uk_zipcodes.csv";
    public static final String ADDRESS_ZIPCODES = "address/Zipcode.txt";
    public static final String ADDRESS_COUNTY = "address/County.txt";
    public static final String ADDRESS_CITY = "address/City.txt";
    public static final String ADDRESS_ADLINE = "address/Adline.txt";
    public static final String ADDRESS_BEGINJUNK = "address/Addr_BeginJunk.txt";
    public static final String TECH_STACK = "keywords/tech stack keys.txt";
    public static final String JOB = "keywords/job.txt";

    public static final String STOP_WORDS = "keywords/stopwords.txt";
    public Set<String> ukZipcodes = new HashSet<>();
    public Set<String> addressZipcodes = new HashSet<>();
    public Set<String> addressCounty = new HashSet<>();
    public Set<String> addressCity = new HashSet<>();
    public Set<String> addressAdLine = new HashSet<>();
    public Set<String> addressBeginJunk = new HashSet<>();
    public Set<String> techStack = new HashSet<>();
    public Set<String> job = new HashSet<>();

    @PostConstruct
    public void init() {
        ClassPathResource url = new ClassPathResource(UK_ZIPCODES, this.getClass().getClassLoader());
        try {
            loadAsSet(url, ukZipcodes);
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", UK_ZIPCODES), exception);
        }
        ClassPathResource zipUrl = new ClassPathResource(ADDRESS_ZIPCODES, this.getClass().getClassLoader());
        try {
            loadAsSet(zipUrl, addressZipcodes);

            log.info("Loaded Zipcode ");
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", ADDRESS_ZIPCODES), exception);
        }

        ClassPathResource countyUrl = new ClassPathResource(ADDRESS_COUNTY, this.getClass().getClassLoader());
        try {
            loadAsSet(countyUrl, addressCounty);

            log.info("Loaded addressCounty ");
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", ADDRESS_COUNTY), exception);
        }

        ClassPathResource cityUrl = new ClassPathResource(ADDRESS_CITY, this.getClass().getClassLoader());
        try {
            loadAsSet(cityUrl, addressCity);

            log.info("Loaded addressCity");
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", ADDRESS_CITY), exception);
        }

        ClassPathResource addressLineUrl = new ClassPathResource(ADDRESS_ADLINE, this.getClass().getClassLoader());
        try {
            loadAsSet(addressLineUrl, addressAdLine);

            log.info("Loaded addressAdLine ");
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", ADDRESS_ADLINE), exception);
        }

        ClassPathResource addressLineBeginJunkUrl = new ClassPathResource(ADDRESS_BEGINJUNK, this.getClass().getClassLoader());
        try {
            loadAsSet(addressLineBeginJunkUrl, addressBeginJunk);

            log.info("Loaded addressBeginJunk ");
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", ADDRESS_BEGINJUNK), exception);
        }

        ClassPathResource tech = new ClassPathResource(TECH_STACK, this.getClass().getClassLoader());
        try {
            loadAsSet(tech, techStack);
            log.info("Loaded tech stack");
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", TECH_STACK), exception);
        }

    /*    ClassPathResource jobwords = new ClassPathResource(JOB, this.getClass().getClassLoader());
        try {
            loadAsSet(jobwords, job);
            log.info("Loaded job stack");
        } catch (Exception exception) {
            log.error(String.format("Can't read file : %s", TECH_STACK), exception);
        }*/

    }


    private void loadAsSet(ClassPathResource file, Set<String> set) {
        if (file.exists()) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    set.add(line.trim());
                }
            } catch (Exception exception) {
                log.error(String.format("Exception while reading file %s", file), exception);
            }
        }
    }
}