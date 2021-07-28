# Cerberus

## What is Cerberus

### A brief introduction

An easy-to-use service(s) registration, discovery framework for common RPC solution, Apache Thrift.

### A slightly detailed information

Generally but sadly speaking, Cerberus are not doing anything creatively:

* Simplifying every Thrift RPC calls.

* Registering and/or discovering any available service instances

These are the **two** and the **only two** main purposes of Cerberus is going to provide for you.

#### Simplifying the usage of Apache Thrift

Cerberus integrates [Drift](https://github.com/airlift/drift), an annotation-based Java library for creating Thrift serializable types and services, internally.

All Thrift related functionality is basically relied on it while Cerberus makes a little twist of it as well as encapsulates service boots-up and invocation proxy for both server side and client side, respectively.

#### Auto service registration and discovery

Personally, I am sure that I don't need to explain the necessary of service auto registration and discovery in a distributed SOA environments, as I assure you have already borne in mind.

Cerberus provides the same functionality, with integration of a few well-known data center(s), **Zookeeper**, **Consul**, **Etcd**, a special one called **Local** which is good for local RPC testing currently.

## How to use Cerberus

> Cerberus integrates Drift internally which provides all the awesome necessary functionality about Thrift.

I am gonna to present you with a pretty simple example, a calculator service which provides only **ADDITION**.

Showing you how to:

* Define a calculator service.

* Boot up a server:
  * With calculator service, serving as a Thrift server.
  * Registers calculator service to a specific data center.

* Create service client:
  * Finds a calculator service instance from a specific data center.
  * Makes a RPC invocation in both **synchronous** and **asynchronous** ways.

Here we go.

### Define calculator service

First of all, you do not need to share the contracts (like IDL in Apache Thrift) between a service provider(aka: server) and a service consumer(aka: client) as before. However, of course, you can still share them as a old-fashion way.

And what's more important is that you can just use Java Annotation to define Thrift types and/or services instead of writing IDL files.

#### Define an interface that ***WILL BE SHARED*** between server and client.

```java
@ThriftService
public interface CalculatorService {

    @ThriftMethod
    int add(int a, int b);

    @ThriftService
    interface Async {
        @ThriftMethod
        ListenableFuture<Integer> add(int a, int b);
    }
}
```

Done!

### Boot a server and register it to a data center

#### Add dependency to use cerberus service bootstrap

```maven
<dependency>
    <groupId>com.sinkedship.cerberus</groupId>
    <artifactId>cerberus-service-bootstrap</artifactId>
    <version>0.2.2-SNAPSHOT</version>
</dependency>
```

#### Implement calculator service

```java
public class CalculatorServiceImpl implements CalculatorService {

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
```

#### Serve calculator service

Currently, Cerberus support three kinds of data center: **Zookeeper**, **Consul**, **Etcd** and a special one called **Local** which is good for local RPC services testing without depending on any other middleware.

Here we use **LOCAL** for demonstration.

```java
public class CalculatorServer {
    public static void main(String[] args) {

        // Use local data center which doesn't depend on any others middleware and friend for testing.
        DataCenter dataCenter = DataCenter.LOCAL;
        // Boot it!
        new CerberusServerBootstrap.Builder(dataCenter)
            .withService(new CalculatorServiceImpl())
            .builder()
            .boot();
    }
}
```

This example above will serve calculator service to an available inet IPv4 address and a arbitrary valid port number.

If you like to specify to host and port explicitly, configure it like this way:

```java
public class CalculatorServer {
    public static void main(String[] args) {

        // Use local data center which doesn't depend on any others middleware and friend for testing.
        DataCenter dataCenter = DataCenter.LOCAL;

        // Configure it
        CerberusServerConfig serverConfig = new CerberusServerConfig(dataCenter);
        serverConfig.getBootConfig().setHost("192.168.1.31").setPort(11111);

        // Boot it!
        new CerberusServerBootstrap.Builder(serverConfig)
            .withService(new CalculatorServiceImpl())
            .builder()
            .boot();
    }
}
```

These are all you need to create a Thrift server and register it to a data center, easy and simple!

### Create client service and make an actual RPC invocation

From a client's aspect, Cerberus needs to know which data center your service provider is using and the fundamental information, like data center's connection host and port.

In our example, we use **LOCAL** as a data center and the connection address will be **192.168.1.31:11111**.

One more word, as we share the service interface between server and client, we do not need to create another annotated Thrift service for our client.

#### Add dependency to use cerberus service client

```maven
<dependency>
    <groupId>com.sinkedship.cerberus</groupId>
    <artifactId>cerberus-service-client</artifactId>
    <version>0.2.2-SNAPSHOT</version>
</dependency>
```

#### Create a client and make a call

* Synchronous

```java
public class CalculatorClient {
    public static void main(String[] args) {

        // Configure the client
        // Use local data center which doesn't depend on any others middleware and it's friend for testing.
        CerberusClientConfig config = new CerberusClientConfig(DataCenter.LOCAL);
        config.getConcreteDataCenterConfig(LocalConfig.class)
            .setConnectHost("192.168.1.31")
            .setConnectPort(11111);
        // Create proxy thrift client
        CerberusServiceFactory serviceFactory = new CerberusServiceFactory(config);
        // Create a synchronous calculator service
        Calculator calculatorService = serviceFactory.newService(Calculator.class);
        // Call it
        int result = calculatorService.add(3, 4);
        assert result == 7;
    }
}
```

* Asynchronous

```java
public class CalculatorClient {
    public static void main(String[] args) {
        // Same like above example
        // ...
        // ...
        // ...
        // Create a asynchronous calculator service
        Calculator.Async asyncService = serviceFactory.newService(Calculator.Async.class);
        // Call it and fetch result
        ListenableFuture<Integer> result = asyncService.add(3, 4);
        result.addListener(() -> {
            try {
                int r = result.get();
                assert r == 7;
            } catch (Throwable t) {
                // Log exception
                LOGGER.error(t);
            }
        }, Executors.newFixedThreadPool(10));
    }
}
```

This is how you can easily create your service from client with Cerberus who can finds your service automatically and proxies all the RPC calls for you.

## Why use Cerberus

Everything starts with a reason, the same as the initialization of Cerberus.

### How do we use Apache Thrift before

It was a difficult time that I could not bear to remind myself, however I am gonna show you in case of you have forgotten:

----------

#### Defines an IDL

```idl
namespace java aa.bb.cc

// some structures

struct One {
    1: ...
    2: ...
    3: ...
    X: ...
}

struct Two {
    // same as above
    // bla bla bla
}

service SomeService {
    // you define some methods here
    void methodA(1:One one) throws (1:TExeption e)

    // more methods
    ...
}
```

> Frankly I do not think it's a bad idea to define an IDL file, actually I admire it because of the universal contract for both server and client side,
> especially when you use different programming languages in individual environments.
>
> However, it seems like a little cumbersome if you use only one language, like Java, all the development time.

What makes me feel struggling is what happens next.

#### Generates codes using Thrift

```bash
thrift --gen java /path/to/idl/some.thrift
```

This creates all the source code defined by IDL file above, however they contains **THOUSANDS OF** unreadable codes that you will depend on later, for both two sides.

#### Implement service with generated codes

From a server's aspect, you will fill in your own business logic by implementing a specific **Iface**:

```java
public class SomeServiceImpl implements SomeService.Iface {

    // Bunch of Override methods
    @Override
    public void methodA(One one) throws Exception {
        // bla bla bla
    }

    // And more
}
```

#### Serve your service

After you have implemented all of your stuff, you are about to reaching the final step, make everything run:

```java
public class MyServer {
    public static void main(String[] args) throws Exception {
        // Define  processor
        TProcessor processor = new SomeService.Processor<>(new SomeServiceImpl());
        // Define transport
        TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(11111);
        TNonblockingServer.Args tArgs = new TNonblockingServer.Args(serverTransport);
        tArgs.processor(tprocessor);
        tArgs.transportFactory(new TFramedTransport.Factory());
        // Define protocol
        tArgs.protocolFactory(new TCompactProtocol.Factory());
        TServer server = new TNonblockingServer(tArgs);
        server.serve();
    }
}
```

#### Consume your service from client

```java
public class MyClient {
    private static final String HOST = "some where";
    private static final int PORT = 11111;
    private static final int TIME_OUT = 3 * 1000;

    public static void main(String[] args) throws Exception {
        // Define transport
        TTransport transport = new TFramedTransport(new TSocket(HOST, PORT, TIME_OUT));
        // Define protocol
        TProtocol protocol = new TCompactProtocol(transport);
        // Create client
        SomeService.Client client = new SomeService.Client(protocol);
        // Make connection
        transport.open();
        // make an actually meaningful RPC call
        client.methodA(new One());
        // Close your connection after you are done
        transport.close();
    }
}
```

----------

#### So, what is annoying

Image we are working at a SOA environment and breaking services into small but strong enough pieces is our first concern. Thrift gives us a reliable foudation as a development framework, but somehow introduces some pitfalls with it:

* Cumbersome generated codes that must be implemented and/or used by server and client side.

* Verbose of steps that need to serve a services or create a client.
  * When serving a service, you got to choose:
    * A processor with your service implementation
    * A specific transport
    * A specific communication protocol
    * A server argument that contains all of above
    * A server that takes the argument
  * When creating a client, you got to:
    * Do the pretty much same thing as serving a service
    * Explicitly open a connection before any RPC invocations
    * Explicitly close a connection after all RPC invocations

> I appreciate Thrift provides different kinds of transport, protocol and server for us to combine with, depends on our own needs and situations.
>
> But I think a configurable way is better than verbose steps.
>
> Always forgetting how to boot a service or create a client does make me feel frustrating.

* Bunches of boilerplate codes as services increase with your business scale.

* Extracts a part of your attention from developing business logic to the verbose usages of Thrift, which should not and do not have to happen, at least in my opinion.

And of course, there are other factors may or may not be a pitfall, but still possibly be a **BURDEN**:

* Pre-installation of Thrift, or cannot generate any template codes that you rely on.

* IDL files

> **Again**: I do think IDL benefits the cross-platform environments, but I did see some colleagues bothered by it.
>
> Well, old words: every coin has two sides.

### How do we deal with service registration and discovery before

There are plenty of trusted open source solutions (such as: Zookeeper, Consul, Etcd, Eureka, etc) for service registration and discovery, however none of them can be plugged into your system naturally and without any efforts.

Each of these middleware has its own features and APIs that manipulate it. That means you got to read through the documentations and be familiar with the APIs when start using it.

I feel sorry for a business developer has to deal with these details who should not have to. If I am, as a user of services registration of discovery center, going to use one this solutions, all I want is:

* Get my services registered to the registration center correctly.

* Find my services from the registration center correctly.

* And neither two of these will bother me with any manipulation details.

Sadly, you can not achieve these goals with directly depending on the middleware. Because somehow, you got to programme with it, more or less:

For instance, if Zookeeper is your best bet as a registration center. Then messing with the original Zookeeper client library or Curator Framework will be unavoidable.

One more step, what if one day you find out Consul is more suitable for the environment you are facing. Then refactoring and replacing all the codes with the corresponding APIs for Consul will be your great fun.

### How does Cerberus solve these problems

#### About Thrift RPC

##### Contracts between server and client

* Original Apache Thrift: uses **IDL** to define contracts between server and client.

* Cerberus: integrates **Drift**, which provides an annotation way to define all essential concepts in Apache Thrift, that means you does not need to define IDL files and generates bunches of unreadable codes any more. Further more, the annotated services doesn't need to be shared between server and client, the annotated service can either be an interface or a concrete class.

##### Serve services

* Original Apache Thrift: verbose steps to combine components or concepts to boot a server.

* Cerberus: can almost boot a server with fall-through in **ONE LINE** of code without any configurations. And of course, cerberus also provides a lots of understandable configurations as you needed.

##### Consume services

* Original Apache Thrift: verbose steps to create a thrift client and has to explicitly manage all the resources that should not be your jobs.

* Cerberus: uses service factory, which can be created easily with fall-through in **ONE LINE** configurations, to create any services you need. Besides, nothing to be managed anymore and you can grasp all your attentions on business.

----------

#### About service registration and discovery

* Common open source solutions: must be integrated in your application with considerable development. Registers services when serving your server and discovers services when consuming from client manually. If you got to change another registration center later, you need to re-integrate again.

* Cerberus: just only need to declares a data center(as known as: registration center) you would like to use when booting server or create service factory. Cerberus Keeps all the dirty work from you and frees your hands. What's more, you can just change another data center as you declare before, no developments anymore.