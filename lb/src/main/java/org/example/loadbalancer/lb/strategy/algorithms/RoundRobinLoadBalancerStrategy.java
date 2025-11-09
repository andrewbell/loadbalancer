package org.example.loadbalancer.lb.strategy.algorithms;

import org.example.loadbalancer.lb.strategy.LoadBalancerStrategy;

import java.net.InetSocketAddress;
import java.util.List;

/*
 * Simplest and most common load balancer type
 * Keeps track of a list of servers to connect to and the index of the last used server
 * When we reach past the last server entry, we move back to the first
 */
public class RoundRobinLoadBalancerStrategy implements LoadBalancerStrategy {

    private final List<InetSocketAddress> servers;
    private int currentIndex = 0;

    /**
     * @param servers a set of <HOST>:<PORT> of target servers we can connect to
     */
    public RoundRobinLoadBalancerStrategy(List<InetSocketAddress> servers) {
        this.servers = servers;
    }

    @Override
    public InetSocketAddress selectDestinationServer() {
        if (currentIndex >= servers.size()) {
            currentIndex = 0;
        }

        return servers.get(currentIndex++);
    }
}
