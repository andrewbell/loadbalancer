package org.example.loadbalancer.lb.strategy.algorithms;

import org.example.loadbalancer.lb.strategy.LoadBalancerStrategy;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.example.loadbalancer.lb.util.Helper.getInetSocketAddress;

class RoundRobinLoadBalancerStrategyTest {

    @Test
    void shouldReturnCorrectRoundRobinEntries() {

        final List<InetSocketAddress> servers = new ArrayList<>();
        servers.add(getInetSocketAddress("localhost:8050"));
        servers.add(getInetSocketAddress("localhost:8051"));

        final LoadBalancerStrategy strategyToTest = new RoundRobinLoadBalancerStrategy(servers);

        InetSocketAddress actual = strategyToTest.selectDestinationServer();
        assertEquals("localhost", actual.getHostName());
        assertEquals(8050, actual.getPort());

        actual = strategyToTest.selectDestinationServer();
        assertEquals("localhost", actual.getHostName());
        assertEquals(8051, actual.getPort());

        actual = strategyToTest.selectDestinationServer();
        assertEquals("localhost", actual.getHostName());
        assertEquals(8050, actual.getPort());
    }
}