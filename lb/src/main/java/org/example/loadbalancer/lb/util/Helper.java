package org.example.loadbalancer.lb.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/*
 * Java boiler plate helper methods
 */
public class Helper {

    public static InetSocketAddress getInetSocketAddress(String targetServer) {
        String[] parts = targetServer.split( ":" );
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    public static void closeChannel(Closeable socketChannel) {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                /* ignore */
            }
        }
    }

    public static String getRemoteAddress(SocketChannel socketChannel) {
        try {
            return socketChannel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "";
        }
    }
}
