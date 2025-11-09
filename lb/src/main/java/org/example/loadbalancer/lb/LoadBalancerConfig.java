package org.example.loadbalancer.lb;

import org.example.loadbalancer.lb.strategy.LoadBalancerStrategyType;

import java.net.InetSocketAddress;
import java.util.List;

// Holds config needed by LB, a future addition might be to include a Lombok builder to improve readability
public class LoadBalancerConfig {

    private int port;
    private LoadBalancerStrategyType loadBalancerStrategyType;
    private List<InetSocketAddress> servers;

    // Harcoded for now, but would come from a config file and would be tuned after load testing
    private int backendPingIntervalMs = 15_000;
    private int backendPingTimeoutMs = 500;
    private int bufferSizeBytes = 128 * 1024;

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setLoadBalancerStrategyType(LoadBalancerStrategyType loadBalancerStrategyType) {
        this.loadBalancerStrategyType = loadBalancerStrategyType;
    }

    public LoadBalancerStrategyType getLoadBalancerStrategyType() {
        return loadBalancerStrategyType;
    }

    public void setServers(List<InetSocketAddress> servers) {
        this.servers = servers;
    }

    public List<InetSocketAddress> getServers() {
        return servers;
    }

    public int getBackendPingIntervalMs() {
        return backendPingIntervalMs;
    }

    public int getBackendPingTimeoutMs() {
        return backendPingTimeoutMs;
    }

    public int getBufferSizeBytes() {
        return bufferSizeBytes;
    }
}
