import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.strategies.RandomStrategy;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class Client {
    private CuratorFramework client;
    private final ServiceDiscovery<InstanceDetails> serviceDiscovery;
    private ServiceProvider<InstanceDetails> provider;

    public Client() throws Exception {
        client = CuratorFrameworkFactory.newClient("localhost:2181", new ExponentialBackoffRetry(1000, 3));
        client.start();
        JsonInstanceSerializer<InstanceDetails> serializer = new JsonInstanceSerializer<InstanceDetails>(InstanceDetails.class);

        serviceDiscovery = ServiceDiscoveryBuilder.builder(InstanceDetails.class)
                                                  .client(client)
                                                  .basePath("apps")
                                                  .serializer(serializer)
                                                  .build();
        serviceDiscovery.start();

        provider = serviceDiscovery.serviceProviderBuilder()
                                   .serviceName("demo")
                                   .downInstancePolicy(new DownInstancePolicy(1, TimeUnit.SECONDS, 1))
                                   .providerStrategy(new RoundRobinStrategy<>())
                                   .build();
        provider.start();
    }
    public ServiceInstance<InstanceDetails> instance() throws Exception {
        return provider.getInstance();
    }

    public Collection<ServiceInstance<InstanceDetails>> allInstances() throws Exception {
        return provider.getAllInstances();
    }
    public static void main(String[] args) throws Exception {
        Client client = new Client();

        while(true) {
            System.in.read();

            client.allInstances()
                  .forEach(instance -> System.out.println("\t" + instance.getPayload().getDescription() + ": " + instance.buildUriSpec()));
        }
    }
}
