package com.sinkedship.cerberus.commons.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

/**
 * @author Derrick Guan
 */
public final class CerberusStringUtils {

    private static final String DEFAULT_ASYNC_SUFFIX = ".Async";

    public static final Supplier<String> DEFAULT_ASYNC_SUFFIX_SUPPLIER = () -> DEFAULT_ASYNC_SUFFIX;

    private CerberusStringUtils() {
    }

    public static String stripAsyncSuffix(String s) {
        return stripAsyncSuffix(s, () -> DEFAULT_ASYNC_SUFFIX);
    }

    public static String stripAsyncSuffix(String s, Supplier<String> supplier) {
        if (supplier == null) {
            supplier = DEFAULT_ASYNC_SUFFIX_SUPPLIER;
        }
        if (StringUtils.endsWith(s, supplier.get())) {
            return StringUtils.stripEnd(s, supplier.get());
        }
        return s;
    }
}
