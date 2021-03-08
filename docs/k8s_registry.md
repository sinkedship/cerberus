# Use Cerberus in Kubernetes

Cerberus supports ```K8S``` data-center from 0.2.0 version.

Like any other data-center(s) that Cerberus now supports, you can register and resolve services easily by specifying
a few configurations.

## How to provide service

* Declare what data-center that you want to use, in this case: ```k8s```

```java
public class ServiceProviderApplication {
    public static void main(String[] args) {
        DataCenter dataCenter = DataCenter.K8S;
    }
}
```

* Set up k8s related server configurations

```java
public class ServiceProviderApplication {
    public static void main(String[] args) {
        // ...

        // Cerberus server config
        CerberusServerConfig serverConfig = new CerberusServerConfig(dataCenter);
        // Set the port that your server listens to.
        serverConfig.getBootConfig().setPort(8080);
        
        // K8S related config
        K8sConfig k8sConfig = serverConfig.getConcreteDataCenterConfig(K8sConfig.class);
        // Set your namespace if require
        k8sConfig.setNamespace("Your namespace");
        // Set other k8s configs
        // ...    
    }
}
```

> :exclamation: Please pay attention that you should always set your server port explicitly if you use ```DataCenter.K8S```, 
> because ```kubernetes``` need to use this port to route traffic from ```kubernetes service```.   

* Boots up server

```java
public class ServiceProviderApplication {
    public static void main(String[] args) {
        // ...
        new CerberusServerBootstrap.Builder(dataCenter)
                .withServerConfig(serverConfig)
                .withService(AlphaService())
                //.withService(BetaService())
                //.withService(XXXXService())
                //..
                .build().boot();
    }
}
```

Once you have deployed your server to Kubernetes successfully, you will have X pod(s), which depends on your replicas
configurations, running on your cluster.

Try to expose your pods by service, like:
```shell script
# 8888 will be the port that this service is exposing.
# 8080 is the target port that the pod listens on, which normally will be the same
# as your kubernetes deployment file and the server config as above.
kubectl expose deployment xxx --name=alpha-service --port=8888 --target-port=8080
```

## How to consume service

* Declare what data-center that you want to use, in this case: ```k8s```

```java
public class ServiceConsumerApplication {
    public static void main(String[] args) {
        DataCenter dataCenter = DataCenter.K8S;
    }
}
```

* Set up k8s related client configurations

```java
public class ServiceConsumerApplication {
    public static void main(String[] args) {
        // ...

        // Cerberus client config
        CerberusClientConfig clientConfig = new CerberusClientConfig(DataCenter.K8S);
        // K8S related config
        K8sConfig k8sConfig = clientConfig.getConcreteDataCenterConfig(K8sConfig.class);
        // Set your namespace if require
        k8sConfig.setNamespace("Your namespace");
        // Set other k8s configs
        // ...    
    }
}
```

* Create RPC service

```java
public class ServiceConsumerApplication {
    public static void main(String[] args) {
        // ...

        // Create service factory
        CerberusServiceFactory serviceFactory = new CerberusServiceFactory(clientConfig);

        // Create Alpha service with K8sServiceMetaData
        AlphaService alphaService = serviceFactory.newService(AlphaService.class, new K8sServiceMetaData("alpha-service"));
        // Make RPC call
        alphaService.methodFromAlphaService(xxxx);
    }
}
```
