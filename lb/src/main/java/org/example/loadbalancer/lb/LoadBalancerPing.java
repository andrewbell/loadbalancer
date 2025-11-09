package org.example.loadbalancer.lb;

import java.net.InetSocketAddress;

public interface LoadBalancerPing {

    boolean pingServer(InetSocketAddress server, int timeoutMs);

}
