package org.example.loadbalancer.lb;

import org.example.loadbalancer.lb.socket.SocketHandler;
import org.example.loadbalancer.lb.socket.SocketHandlerThreadAbstractFactory;
import org.example.loadbalancer.lb.strategy.LoadBalancerStrategy;
import org.example.loadbalancer.lb.strategy.LoadBalancerStrategyType;
import org.example.loadbalancer.lb.strategy.algorithms.RoundRobinLoadBalancerStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.example.loadbalancer.lb.util.Helper.getInetSocketAddress;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class LoadBalancerTest {

    @Test
    void testLoadBalancerWithRoundRobin() throws Exception {
        final LoadBalancerConfig config = getLoadBalancerConfig();
        final LoadBalancerStrategy rr = new RoundRobinLoadBalancerStrategy(config.getServers());

        // create several mocks
        final SocketHandlerThreadAbstractFactory mockFactory = mock(SocketHandlerThreadAbstractFactory.class);
        final SocketHandler mockThread = mock(SocketHandler.class);
        final LoadBalancerPing mockPing = mock(LoadBalancerPing.class);

        // set their expectations
        when(mockThread.runThread()).thenReturn(true);
        when(mockFactory.createSocketHandlerThread(any(Socket.class), any(InetSocketAddress.class), anyInt())).thenReturn(mockThread);
        when(mockPing.pingServer(any(InetSocketAddress.class), anyInt())).thenReturn(true);

        // run the test, inject the mock dependencies
        final LoadBalancer lb = new LoadBalancer(config, rr, mockFactory);
        lb.setLoadBalancerPing(mockPing);

        final CompletableFuture<Void> future = CompletableFuture.runAsync(lb::run);

        writeToPort(config.getPort());
        Thread.sleep(100);
        lb.setAbort(true);
        writeToPort(config.getPort());

        future.join();

        final ArgumentCaptor<InetSocketAddress> captor = ArgumentCaptor.forClass(InetSocketAddress.class);
        verify(mockFactory, times(2)).createSocketHandlerThread(any(Socket.class), captor.capture(), anyInt());

        // assert that both write went to different addresses
        assertEquals("/127.0.0.1:8050", captor.getAllValues().getFirst().toString());
        assertEquals("/127.0.0.1:8051", captor.getAllValues().get(1).toString());
    }

    private static void writeToPort(int port) {
        try (final Socket socket = new Socket("localhost", port)) {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("PING");
            writer.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private static LoadBalancerConfig getLoadBalancerConfig() {
        final LoadBalancerConfig config = new LoadBalancerConfig();
        config.setPort(8080);
        config.setLoadBalancerStrategyType(LoadBalancerStrategyType.ROUND_ROBIN);
        final List<InetSocketAddress> servers = new ArrayList<>();
        servers.add(getInetSocketAddress("127.0.0.1:8050"));
        servers.add(getInetSocketAddress("127.0.0.1:8051"));
        config.setServers(servers);
        return config;
    }
}