package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

@Service
public class IntegerValueComparatorExtractor implements Comparator<String> {

    private final Map<String, Integer> map;

    public IntegerValueComparatorExtractor(Map<String, Integer> map) {
        this.map = map;
    }


    @Override

    public int compare(String a, String b) {

        Integer value1 = map.get(a);

        Integer value2 = map.get(b);


        return value1.compareTo(value2);

    }


}
