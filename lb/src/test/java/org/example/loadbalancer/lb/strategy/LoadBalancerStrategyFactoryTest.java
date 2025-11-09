package org.example.loadbalancer.lb.strategy;

import org.example.loadbalancer.lb.strategy.algorithms.RandomLoadBalancerStrategy;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerStrategyFactoryTest {

    @Test
    void shouldCreateCorrectTypes() {
        LoadBalancerStrategy strategy = LoadBalancerStrategyFactory.createLoadBalancerStrategy(LoadBalancerStrategyType.ROUND_ROBIN, List.of());
        assertInstanceOf(LoadBalancerStrategy.class, strategy);

        strategy = LoadBalancerStrategyFactory.createLoadBalancerStrategy(LoadBalancerStrategyType.RANDOM, List.of());
        assertInstanceOf(RandomLoadBalancerStrategy.class, strategy);
    }

}