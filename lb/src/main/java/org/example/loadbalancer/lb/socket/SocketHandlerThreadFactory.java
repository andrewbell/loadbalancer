package org.example.loadbalancer.lb.socket;

import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketHandlerThreadFactory extends SocketHandlerThreadAbstractFactory {

    @Override
    public SocketHandlerThread createSocketHandlerThread(Socket clientSocket, InetSocketAddress targetServer, int bufferSizeBytes) {
        return new SocketHandlerThread(clientSocket, targetServer, bufferSizeBytes);
    }
}
