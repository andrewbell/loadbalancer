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
        int port = 8080;
        if (args.length >= 1) {
            port = parseArgument(args[0], port);
        }

        // For now, we just create the config directly, in a production environment this
        // would be pulled from a config file or server
        final LoadBalancerConfig config = new LoadBalancerConfig();
        config.setPort(port);
        config.setLoadBalancerStrategyType(LoadBalancerStrategyType.ROUND_ROBIN);
        final List<InetSocketAddress> servers = new ArrayList<>();
        servers.add(getInetSocketAddress("127.0.0.1:8050"));
        servers.add(getInetSocketAddress("127.0.0.1:8051"));
        config.setServers(servers);

        // Get the class that will do the server routing
        final LoadBalancerStrategy lbStrategy = LoadBalancerStrategyFactory.createLoadBalancerStrategy(config.getLoadBalancerStrategyType(), config.getServers());
        final SocketHandlerThreadFactory factory = new SocketHandlerThreadFactory();

        new LoadBalancer(config, lbStrategy, factory).run();
    }

    private static int parseArgument(String portArg, int defaultPort) {
        try {
            return Integer.parseInt(portArg);
        } catch (NumberFormatException e) {
            System.out.printf("Invalid port: %s%n", portArg);
            return defaultPort;
        }
    }
}
