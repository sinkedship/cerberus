package com.sinkedship.cerberus.sample.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.sinkedship.cerberus.client.CerberusServiceFactory;
import com.sinkedship.cerberus.client.config.CerberusClientConfig;
import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.config.data_center.LocalConfig;
import com.sinkedship.cerberus.sample.api.service.CalculateService;
import com.sinkedship.cerberus.sample.api.service.HelloService;

import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Derrick Guan
 */
public class CerberusClientProxySample {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("need ${host} ${port}");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        // Configure the client
        // Use local data center which doesn't depend on any others middleware and it's friend for testing.
        CerberusClientConfig config = new CerberusClientConfig(DataCenter.LOCAL);
        config.getConcreteDataCenterConfig(LocalConfig.class).setConnectHost(host).setConnectPort(port);

        // Create proxy thrift client
        CerberusServiceFactory serviceFactory = new CerberusServiceFactory(config);

        // Create a non-async hello service
        HelloService helloService = serviceFactory.newService(HelloService.class);
        // Create an async hello service
        HelloService.Async asyncHelloService = serviceFactory.newService(HelloService.Async.class);

        // Create a non-async calculate service
        // Because the service provider and consume do not share a same service api
        // we use service meta data the find the specific RPC service in the data center
        ServiceMetaData metaData = new ServiceMetaData("com.sinkedship", "test", "calculate");
        CalculateService calService = serviceFactory.newService(CalculateService.class, metaData);
        // Create a async calculate service
        CalculateService.Async asyncCalService = serviceFactory.newService(CalculateService.Async.class, metaData);

        Executor executor = Executors.newFixedThreadPool(10);

        Scanner scanner = new Scanner(System.in);
        printHelp();
        while (true) {
            while (scanner.hasNext()) {
                try {
                    String line = scanner.nextLine().trim();
                    String[] arr = line.split("\\s+", 3);
                    String type = arr[0];
                    String func = arr[1];
                    String others = arr[2];
                    switch (type) {
                        case "sync":
                            switch (func) {
                                case "hello":
                                    sync_hello(helloService, others);
                                    break;
                                case "add": {
                                    String[] _args = others.split("\\s+");
                                    sync_add(calService, Integer.parseInt(_args[0]), Integer.parseInt(_args[1]));
                                    break;
                                }

                                case "very_slow_gcd": {
                                    String[] _args = others.split("\\s+");
                                    sync_very_slow_gcd(calService, Integer.parseInt(_args[0]), Integer.parseInt(_args[1]));
                                    break;
                                }
                                default:
                                    printHelp();
                            }
                            break;
                        case "async":
                            switch (func) {
                                case "hello":
                                    async_hello(asyncHelloService, others, executor);
                                    break;
                                case "very_slow_gcd":
                                    String[] _args = others.split("\\s+");
                                    async_very_slow_gcd(asyncCalService,
                                            Integer.parseInt(_args[0]), Integer.parseInt(_args[1]),
                                            executor);
                                    break;
                                default:
                                    printHelp();
                            }
                            break;
                        default:
                            printHelp();
                    }
                } catch (Exception ignore) {
                }
            }
        }
    }


    // sync hello ${your_greeting}
    // async hello ${your_greeting}
    // sync add ${number_1} ${number_2}
    // sync very_slow_gcd ${number_1} ${number_2}
    // async very_slow_gcd ${number_1} ${number_2}
    private static void printHelp() {
        System.out.println("Available testing RPC calls:\n");
        System.out.println("1> sync hello ${your_greeting}");
        System.out.println("2> async hello ${your_greeting}");
        System.out.println("3> sync add ${number_1} ${number_2}");
        System.out.println("4> sync very_slow_gcd ${number_1} ${number_2}");
        System.out.println("5> async very_slow_gcd ${number_1} ${number_2}");
        System.out.println("\nSample usage:\nIf you gonna make a synchronous RPC `hello` invocation, please input:");
        System.out.println("sync hello derrick");
        System.out.println("\nSample usage:\nIf you gonna make a asynchronous RPC `very_slow_gcd` invocation, please input:");
        System.out.println("async very_slow_gcd 24 6\n");
    }

    private static void sync_hello(HelloService helloService, String s) {
        // Non async calls
        String result = helloService.hello(s, false);
        System.out.println(String.format("> get RPC sync hello result: %s", result));
    }

    private static void async_hello(HelloService.Async helloService, String s, Executor executor) {
        ListenableFuture<String> result = helloService.hello(s, true);
        result.addListener(() -> {
            try {
                System.out.println(String.format("> get RPC async hello result: %s", result.get()));
            } catch (Exception e) {
                System.out.println("> RPC async call with error" + e.toString());
            }
        }, executor);
    }

    private static void sync_add(CalculateService calculateService, int a, int b) {
        int result = calculateService.add(a, b);
        System.out.println(String.format("> get RPC sync add result: %d", result));
    }

    private static void sync_very_slow_gcd(CalculateService calculateService, int a, int b) {
        int result = calculateService.verySlowGcd(a, b);
        System.out.println(String.format("> get RPC sync gcd result: %d", result));
    }

    private static void async_very_slow_gcd(CalculateService.Async calculateService, int a, int b, Executor executor) {
        ListenableFuture<Integer> result = calculateService.verySlowGcd(a, b);
        result.addListener(() -> {
            try {
                System.out.println(String.format("> get RPC async gcd result: %d", result.get()));
            } catch (Exception e) {
                System.out.println("> RPC async call with error" + e.toString());
            }
        }, executor);
    }
}