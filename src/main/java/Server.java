import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

public class Server {
    private CuratorFramework client;
    private final ServiceDiscovery<InstanceDetails> serviceDiscovery;
    private final ServiceInstance<InstanceDetails> thisInstance;

    public Server(String path, String serviceName, String description) throws Exception {
        client = CuratorFrameworkFactory.newClient("localhost:2181",
                new ExponentialBackoffRetry(1000, 3));
        client.start();
        // in a real application, you'd have a convention of some kind for the URI layout
        UriSpec uriSpec = new UriSpec("{scheme}://{address}:{port}");

        thisInstance = ServiceInstance.<InstanceDetails>builder()
                .name(serviceName)
                .payload(new InstanceDetails(description))
                .port((int) (65535 * Math.random())) // in a real application, you'd use a common port
                .uriSpec(uriSpec)
                .build();

        // if you mark your payload class with @JsonRootName the provided JsonInstanceSerializer will work
        JsonInstanceSerializer<InstanceDetails> serializer = new JsonInstanceSerializer<InstanceDetails>(InstanceDetails.class);

        serviceDiscovery = ServiceDiscoveryBuilder.builder(InstanceDetails.class)
                                                  .client(client)
                                                  .basePath(path)
                                                  .serializer(serializer)
                                                  .thisInstance(thisInstance)
                                                  .build();
//
    }

    public ServiceInstance<InstanceDetails> getThisInstance() {
        return thisInstance;
    }

    public void start() throws Exception {
        serviceDiscovery.start();
    }

    //    @Override
    public void close() {
        CloseableUtils.closeQuietly(serviceDiscovery);
        CloseableUtils.closeQuietly(client);
    }

    public static void main(String[] args) throws Exception {
        Server srv = new Server("apps", "demo", "desc");
        srv.start();

        Runtime.getRuntime()
               .addShutdownHook(new Thread(srv::close));

        System.in.read();


    }
}
