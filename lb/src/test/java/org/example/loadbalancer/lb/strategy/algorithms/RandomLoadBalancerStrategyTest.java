package org.example.loadbalancer.lb.strategy.algorithms;

import org.example.loadbalancer.lb.strategy.LoadBalancerStrategy;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.example.loadbalancer.lb.util.Helper.getInetSocketAddress;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomLoadBalancerStrategyTest {

    @Test
    void shouldReturnCorrectRandomEntries() {
        final List<InetSocketAddress> servers = new ArrayList<>();
        servers.add(getInetSocketAddress("localhost:8050"));
        servers.add(getInetSocketAddress("localhost:8051"));

        final LoadBalancerStrategy strategyToTest = new RandomLoadBalancerStrategy(servers);

        for (int i = 0; i < 3; i++) {
            final InetSocketAddress actual = strategyToTest.selectDestinationServer();

            assertEquals("localhost", actual.getHostName());
            assertTrue(actual.getPort() == 8050 || actual.getPort() == 8051);
        }

    }

}