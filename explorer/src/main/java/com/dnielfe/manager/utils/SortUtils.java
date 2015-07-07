package com.dnielfe.manager.utils;

import com.dnielfe.manager.settings.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class SortUtils {

    private static final int SORT_ALPHA = 0;
    private static final int SORT_TYPE = 1;
    private static final int SORT_SIZE = 2;
    private static final int SORT_DATE = 3;

    public static void sortList(ArrayList<String> content,
                                String current) {
        int len = content != null ? content.size() : 0;

        if (len == 0)
            return;

        int index = 0;
        String[] items = new String[len];
        content.toArray(items);

        switch (Settings.getSortType()) {
            case SORT_ALPHA:
                Arrays.sort(items, Comparator_ALPH);
                content.clear();

                Collections.addAll(content, items);
                break;
            case SORT_SIZE:
                Arrays.sort(items, Comparator_SIZE);
                content.clear();

                for (String a : items) {
                    if (new File(current + "/" + a).isDirectory())
                        content.add(index++, a);
                    else
                        content.add(a);
                }
                break;
            case SORT_TYPE:
                Arrays.sort(items, Comparator_TYPE);
                content.clear();

                for (String a : items) {
                    if (new File(current + "/" + a).isDirectory())
                        content.add(index++, a);
                    else
                        content.add(a);
                }
                break;

            case SORT_DATE:
                Arrays.sort(items, Comparator_DATE);
                content.clear();

                for (String a : items) {
                    if (new File(current + "/" + a).isDirectory())
                        content.add(index++, a);
                    else
                        content.add(a);
                }
                break;
        }

        if (Settings.reverseListView()) {
            Collections.reverse(content);
        }
    }

    private static final Comparator<? super String> Comparator_ALPH = new Comparator<String>() {

        @Override
        public int compare(String arg0, String arg1) {
            return arg0.toLowerCase().compareTo(arg1.toLowerCase());
        }
    };

    private final static Comparator<? super String> Comparator_SIZE = new Comparator<String>() {

        @Override
        public int compare(String arg0, String arg1) {
            File a = new File(arg0);
            File b = new File(arg1);

            if (a.isDirectory() && b.isDirectory()) {
                return arg0.toLowerCase().compareTo(arg1.toLowerCase());
            }

            if (a.isDirectory()) {
                return -1;
            }

            if (b.isDirectory()) {
                return 1;
            }

            final long len_a = a.length();
            final long len_b = b.length();

            if (len_a == len_b) {
                return arg0.toLowerCase().compareTo(arg1.toLowerCase());
            }

            if (len_a < len_b) {
                return -1;
            }

            return 1;
        }
    };

    private final static Comparator<? super String> Comparator_TYPE = new Comparator<String>() {

        @Override
        public int compare(String arg0, String arg1) {
            File a = new File(arg0);
            File b = new File(arg1);

            if (a.isDirectory() && b.isDirectory()) {
                return arg0.toLowerCase().compareTo(arg1.toLowerCase());
            }

            if (a.isDirectory()) {
                return -1;
            }

            if (b.isDirectory()) {
                return 1;
            }

            final String ext_a = SimpleUtils.getExtension(a.getName());
            final String ext_b = SimpleUtils.getExtension(b.getName());

            if (ext_a.isEmpty() && ext_b.isEmpty()) {
                return arg0.toLowerCase().compareTo(arg1.toLowerCase());
            }

            if (ext_a.isEmpty()) {
                return -1;
            }

            if (ext_b.isEmpty()) {
                return 1;
            }

            final int res = ext_a.compareTo(ext_b);
            if (res == 0) {
                return arg0.toLowerCase().compareTo(arg1.toLowerCase());
            }
            return res;
        }
    };

    private final static Comparator<? super String> Comparator_DATE = new Comparator<String>() {

        @Override
        public int compare(String arg0, String arg1) {
            Long first = new File(arg0).lastModified();
            Long second = new File(arg1).lastModified();

            return first.compareTo(second);
        }
    };
}