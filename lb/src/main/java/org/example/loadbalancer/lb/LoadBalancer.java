package org.example.loadbalancer.lb;

import org.example.loadbalancer.lb.socket.SocketHandlerThreadAbstractFactory;
import org.example.loadbalancer.lb.strategy.LoadBalancerStrategy;
import org.example.loadbalancer.lb.util.Ping;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.util.*;
import java.util.concurrent.*;

public class LoadBalancer {

    // Each socket will be handled by a separate lightweight virtual thread, rather than a regular thread pool
    // This will allow more long term sockets without exhausting a regular thread pool.
    private final ExecutorService threadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Used to run regular task that will ping servers for health
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());

    // List of back-end server that are unreachable or have errors + timestamp of when error was seen
    private final Map<InetSocketAddress, Long> unhealthyServers = new ConcurrentHashMap<>();

    // Config for this LB
    private final LoadBalancerConfig config;

    // LB algorithm we are using
    private final LoadBalancerStrategy lbStrategy;

    // Used to create the socket threads
    private final SocketHandlerThreadAbstractFactory socketHandlerThreadFactory;

    // Ping handler used to detect back-end servers, this can be replaced by unit tests
    private LoadBalancerPing pinger = new Ping();

    private boolean abort = false;

    public LoadBalancer(LoadBalancerConfig config, LoadBalancerStrategy lbStrategy, SocketHandlerThreadAbstractFactory factory) {
        this.config = config;
        this.lbStrategy = lbStrategy;
        this.socketHandlerThreadFactory = factory;
    }

    void run() {

        System.out.printf("Starting load balancer on port %d%n", config.getPort());

        // Check if any servers are offline before starting the LB
        pingAllServers(config.getServers());

        try (final ServerSocketChannel socketChannel = ServerSocketChannel.open()) {
            final ServerSocket serverSocket = socketChannel.socket();
            serverSocket.bind(new InetSocketAddress("localhost", config.getPort()));

            // Start accepting connections from clients
            processClientConnections(lbStrategy, serverSocket);

        } catch (IOException e) {
            System.out.printf("Error starting load balancer on port %d (%s)%n", config.getPort(), e.getMessage());
        }
    }

    /**
     * Accept incoming client connections, load balancer routing is delegated to the provided LoadBalancerStrategy instance.
     * Each socket is offloaded to a virtual thread for processing, problematic servers are taken offline and a periodic task
     * is run to ping and check if they're back online.
     *
     * @param lbStrategy A LoadBalancerStrategy, e.g. RoundRobin
     * @param serverSocket The bound server socket/port clients will connect on
     * @throws IOException Thrown if there is a problem accepting connections
     */
    private void processClientConnections(LoadBalancerStrategy lbStrategy, ServerSocket serverSocket) throws IOException {

        // Check unhealthy servers every few seconds
        scheduler.scheduleAtFixedRate(this::pingUnhealthyServers,
                config.getBackendPingIntervalMs(),
                config.getBackendPingIntervalMs(),
                TimeUnit.MILLISECONDS);

        // Accept TCP connections
        do {

            if (getBackendServerAvailability() >= 1) {
                // Find the next healthy backend server to route the request to
                final InetSocketAddress targetServer = lbStrategy.selectDestinationServer();

                if (!isServerHealthy(targetServer)) {
                    // not online, ask LoadBalancerStrategy to give us a different backend server
                    continue;
                }

                final Socket socket = serverSocket.accept();

                System.out.println("Routing connection to server: " + targetServer);

                // Start a thread to process the socket
                final CompletableFuture<Boolean> future = CompletableFuture
                        .supplyAsync(() -> socketHandlerThreadFactory.createSocketHandlerThread(socket, targetServer, config.getBufferSizeBytes()).runThread(), threadExecutor);

                // When the thread aborts prematurely or returned false, assume there is something wrong with the server and mark it unhealthy
                future.whenCompleteAsync((result, throwable) -> markServerUnhealthy(result, throwable, targetServer));

            } else {
                // Until at least 1 backend comes up we can only close accepted sockets
                // In this case we'd want to trigger a critical alert against our monitoring systems

                final Socket socket = serverSocket.accept();
                System.out.println("rejected connection: " + socket.getRemoteSocketAddress());
                socket.close();
            }

        } while (!abort && !Thread.currentThread().isInterrupted());

        System.out.println("Exit LoadBalancer thread");

    }

    /**
     * Check if the given server was previsouly marked as unhealthy
     *
     * @param targetServer server to check
     * @return true for healthy
     */
    private boolean isServerHealthy(InetSocketAddress targetServer) {
        return !unhealthyServers.containsKey(targetServer);
    }

    /**
     * Check how many back-end servers are online, if none then ping them
     * @return number of active BE servers
     */
    private int getBackendServerAvailability() {
        int available = config.getServers().size() - unhealthyServers.size();

        if (available <= 0) {
            // all backend servers are down, there's not much we can do except warn a human and ping all server in case any are back up again
            System.out.printf("WARNING: All %d backend servers are unhealthy, attempting to ping them...%n", config.getServers().size());
            pingAllServers(config.getServers());
        }

        return available;
    }

    /**
     * Socket thread indicated that something went wrong with the connection, e.g. connection refused. This callback will mark the server as unhealthy
     *
     * @param result Result from the CompletableFuture, false for error, true for end of stream
     * @param throwable Exception if there is one (maybe null)
     * @param targetServer Server being marked unhealthy
     */
    private void markServerUnhealthy(Boolean result, Throwable throwable, InetSocketAddress targetServer) {
        if (result == false || throwable != null) {
            final String message = throwable != null ? throwable.getMessage() : "Socket thread was aborted";
            System.out.printf("Taking backend server offline %s (%s)%n", targetServer, message);
            unhealthyServers.put(targetServer, System.currentTimeMillis());
        }
    }

    /**
     * Check the list of unhealthy servers to see if any have came online
     */
    private void pingUnhealthyServers() {

        System.out.printf("Checking for unhealthy servers%n");

        for (Map.Entry<InetSocketAddress, Long> entry : unhealthyServers.entrySet()) {
            final InetSocketAddress address = entry.getKey();
            markServerOnlineOrOffline(address);
        }
    }

    /**
     * Check all the given servers and mark them online or unhealthy.
     * @param servers List of servers provided by config
     */
    private void pingAllServers(List<InetSocketAddress> servers) {
        for (InetSocketAddress address : servers) {
            markServerOnlineOrOffline(address);
        }
    }

    /**
     * Mark a single server online or unhealthy by pinging it
     *
     * @param address Backend server address
     */
    private void markServerOnlineOrOffline(InetSocketAddress address) {
        if (pinger.pingServer(address, config.getBackendPingTimeoutMs())) {
            // Server is reachable again
            unhealthyServers.remove(address);
            System.out.printf("Server marked online: %s %n", address);
        } else {
            unhealthyServers.put(address, System.currentTimeMillis());
            System.out.printf("Server marked offline: %s %n", address);
        }
    }

    /**
     * Set the ping routine, can be replaced to easier unit testing
     * @param pinger A LoadBalancerPing instance
     */
    void setLoadBalancerPing(LoadBalancerPing pinger) {
        this.pinger = pinger;
    }

    void setAbort(boolean abort) {
        this.abort = abort;
    }
}
