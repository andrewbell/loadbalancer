package org.example.loadbalancer.lb;

import org.example.loadbalancer.lb.socket.SocketHandlerThreadFactory;
import org.example.loadbalancer.lb.strategy.LoadBalancerStrategy;
import org.example.loadbalancer.lb.strategy.LoadBalancerStrategyFactory;
import org.example.loadbalancer.lb.strategy.LoadBalancerStrategyType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.example.loadbalancer.lb.util.Helper.getInetSocketAddress;

// main() entry point for load balancer with argument parsing
public class Main {
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: lb <LISTEN_PORT> <COMMA_SEPARATED_HOST_AND_PORTS>");
            return;
        }

        final int port = parseIntArgument(args[0], 8080);
        final List<InetSocketAddress> servers = parseServers(args[1]);

        // For now, we just create the config directly, in a production environment this
        // would be pulled from a config file or server
        final LoadBalancerConfig config = new LoadBalancerConfig();
        config.setPort(port);
        config.setLoadBalancerStrategyType(LoadBalancerStrategyType.ROUND_ROBIN);
        config.setServers(servers);

        // Get the class that will do the server routing
        final LoadBalancerStrategy lbStrategy = LoadBalancerStrategyFactory.createLoadBalancerStrategy(config.getLoadBalancerStrategyType(), config.getServers());
        final SocketHandlerThreadFactory factory = new SocketHandlerThreadFactory();

        System.out.printf("Starting load balancer on port %d%n", config.getPort());
        config.getServers().forEach(server -> System.out.println("BE server: " + server));

        new LoadBalancer(config, lbStrategy, factory).run();
    }

    private static int parseIntArgument(String portArg, int defaultPort) {
        try {
            return Integer.parseInt(portArg);
        } catch (NumberFormatException e) {
            System.out.printf("Invalid port: %s%n", portArg);
            return defaultPort;
        }
    }

    private static List<InetSocketAddress> parseServers(String serverArgs) {
        final List<InetSocketAddress> servers = new ArrayList<>();
        for (String nextServer: serverArgs.split(",")) {
            servers.add(getInetSocketAddress(nextServer));
        }
        return servers;
    }

}
