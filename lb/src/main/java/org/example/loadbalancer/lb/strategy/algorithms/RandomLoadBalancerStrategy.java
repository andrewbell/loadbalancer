package org.example.loadbalancer.lb.strategy.algorithms;

import org.example.loadbalancer.lb.strategy.LoadBalancerStrategy;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Returns a random server from the list of servers given on construction
 */
public class RandomLoadBalancerStrategy implements LoadBalancerStrategy {

    private final List<InetSocketAddress> servers;

    public RandomLoadBalancerStrategy(List<InetSocketAddress> servers) {
        this.servers = servers;
    }

    @Override
    public InetSocketAddress selectDestinationServer() {
        int index = ThreadLocalRandom.current().nextInt(0, servers.size());
        return servers.get(index);
    }

}
