package org.example.loadbalancer.lb.strategy;

import org.example.loadbalancer.lb.strategy.algorithms.RandomLoadBalancerStrategy;
import org.example.loadbalancer.lb.strategy.algorithms.RoundRobinLoadBalancerStrategy;

import java.net.InetSocketAddress;
import java.util.List;

/*
 * Factory class allows us to decouple the code used to choose a destination server from the LB implementation
 * We can implement new strategies with no (or minimal changes) to the main code.
 *
 * Some ideas:
 *  - Weighted round-robin
 *  - IP hash
 *  - Least connections (*)
 *  - Least response time (*)
 *
 * (*) interface would need some methods to get additional information from LB
 */

public class LoadBalancerStrategyFactory {

    private LoadBalancerStrategyFactory() {}

    public static LoadBalancerStrategy createLoadBalancerStrategy(LoadBalancerStrategyType type, List<InetSocketAddress> servers) {
        switch (type) {
            case ROUND_ROBIN:
                return new RoundRobinLoadBalancerStrategy(servers);
            case RANDOM:
                return new RandomLoadBalancerStrategy(servers);

            default:
                throw new IllegalArgumentException("Unknown strategy type: " + type);
        }
    }
}
