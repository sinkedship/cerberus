package com.sinkedship.cerberus.commons.utils;

import com.google.common.base.Preconditions;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.Random;

/**
 * Network related utilities
 *
 * @author Derrick Guan
 */
public class HostAndPortUtils {

    private HostAndPortUtils() {
    }

    static {
        DEFAULT_HOST = findDefaultHostAddress();
    }

    private static final Random random = new Random(System.currentTimeMillis());

    private static final String DEFAULT_HOST;

    public static final int PORT_RANGE_MIN = 1024;

    public static final int PORT_RANGE_MAX = 65535;

    public static String getDefaultHost() {
        return DEFAULT_HOST;
    }

    public static int getAvailablePort() {
        return getAvailablePort(PORT_RANGE_MIN, PORT_RANGE_MAX);
    }

    public static int getAvailablePort(int minPort, int maxPort) {
        Preconditions.checkArgument(minPort > 0,
                "'minPort' must be greater than 0");
        Preconditions.checkArgument(maxPort >= minPort,
                "'maxPort' must be greater than or equals 'minPort'");
        Preconditions.checkArgument(maxPort <= PORT_RANGE_MAX,
                "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

        int portRange = maxPort - minPort;
        int candidatePort;
        int searchCounter = 0;
        do {
            if (++searchCounter > portRange) {
                throw new IllegalStateException(String.format(
                        "Could not find an available TCP port in the range [%d, %d] after %d attempts",
                        minPort, maxPort, searchCounter));
            }
            candidatePort = findRandomPort(minPort, maxPort);
        }
        while (!isPortAvailable(candidatePort));

        return candidatePort;
    }

    private static boolean isPortAvailable(int port) {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                    port, 1, InetAddress.getByName("localhost"));
            serverSocket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static int findRandomPort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        return minPort + random.nextInt(portRange + 1);
    }

    private static String findDefaultHostAddress() {
        String defaultAddress = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isLoopback()) {
                    continue;
                }
                if (!networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isLinkLocalAddress()) {
                        continue;
                    }
                    String hostAddress = inetAddress.getHostAddress();

                    // ipv4
                    if (hostAddress != null && hostAddress.split("\\.").length == 4) {
                        defaultAddress = hostAddress;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (defaultAddress == null) {
            defaultAddress = "localhost";
        }
        return defaultAddress;
    }

}
