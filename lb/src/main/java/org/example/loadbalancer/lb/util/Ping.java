package org.example.loadbalancer.lb.util;

import org.example.loadbalancer.lb.LoadBalancerPing;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Ping implements LoadBalancerPing {

    @Override
    public boolean pingServer(InetSocketAddress server, int timeoutMs) {
        try {
            final InetAddress address = InetAddress.getByName(server.getHostName());
            if (address == null) {
                return false;
            }

            if (address.isReachable(timeoutMs)) {
                // IP reachable, now check port
                return checkPort(server, address);
            }
        } catch (IOException e) {
            return false;
        }

        return false;
    }

    private static boolean checkPort(InetSocketAddress server, InetAddress address) {
        try (final Socket socket = new Socket(address, server.getPort())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}