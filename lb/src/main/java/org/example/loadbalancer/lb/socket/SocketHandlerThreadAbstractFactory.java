package org.example.loadbalancer.lb.socket;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Abstract factory to create SocketHandlerThreads, makes it easier to test the load-balancer logic
 */
public abstract class SocketHandlerThreadAbstractFactory {

    public abstract SocketHandler createSocketHandlerThread(Socket clientSocket, InetSocketAddress targetServer, int bufferSizeBytes);

}
