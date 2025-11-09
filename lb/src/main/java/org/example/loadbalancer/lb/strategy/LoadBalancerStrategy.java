package org.example.loadbalancer.lb.strategy;

import java.net.InetSocketAddress;

public interface LoadBalancerStrategy {

    InetSocketAddress selectDestinationServer();

}
