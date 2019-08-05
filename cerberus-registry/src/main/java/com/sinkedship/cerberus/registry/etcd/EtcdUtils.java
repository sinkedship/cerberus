package com.sinkedship.cerberus.registry.etcd;

/**
 * @author Derrick Guan
 */
final class EtcdUtils {

    private static final String PATTERN_SERVICE = "%s/%s";
    private static final String PATTERN_SERVICE_INSTANCE = "%s/%s/%s";

    private EtcdUtils() {
    }

    static String getServiceKey(String prefix, String serviceIdentifier) {
        return String.format(PATTERN_SERVICE, prefix, serviceIdentifier);
    }

    static String getInstanceKey(String prefix, String serviceIdentifier, String id) {
        return String.format(PATTERN_SERVICE_INSTANCE, prefix, serviceIdentifier, id);
    }
}
