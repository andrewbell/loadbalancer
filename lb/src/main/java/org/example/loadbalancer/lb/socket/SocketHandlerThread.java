package org.example.loadbalancer.lb.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import static org.example.loadbalancer.lb.util.Helper.closeChannel;
import static org.example.loadbalancer.lb.util.Helper.getRemoteAddress;

/*
 * This class represents a thread that will deal with one load-balanced socket
 * No routing is done here - its primary purpose is to stream bytes from the client port:IP to the target server port:IP
 *
 * Since we're a level 4 load balancer, we don't assume any protocols either. It could be TLS, raw HTTP, telnet or
 * anything else. We are transferring raw bytes and agnostic to what the socket holds.
 */
public class SocketHandlerThread implements SocketHandler {

    private final InetSocketAddress targetServer;
    private final ByteBuffer clientBuffer;
    private final SocketChannel clientSocketChannel;
    private SocketChannel targetSocketChannel;

    public SocketHandlerThread(Socket clientSocket, InetSocketAddress targetServer, int bufferSizeBytes) {
        this.clientSocketChannel = clientSocket.getChannel();
        this.targetServer = targetServer;
        this.clientBuffer = ByteBuffer.allocate(bufferSizeBytes);
    }

    private void handleAccept(Selector selector) throws IOException {
        clientSocketChannel.configureBlocking(false);

        targetSocketChannel = SocketChannel.open();
        targetSocketChannel.configureBlocking(false);
        targetSocketChannel.connect(targetServer);
        targetSocketChannel.register(selector, SelectionKey.OP_CONNECT, clientSocketChannel);

        clientSocketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("a client has connected: " + clientSocketChannel.getRemoteAddress());
    }

    private void handleConnect(Selector selector, SelectionKey key) throws IOException {
        final SocketChannel targetChannel = (SocketChannel) key.channel();

        if (targetChannel.isConnectionPending()) {
            targetChannel.finishConnect();
            System.out.println("connecting to backend server: " + targetChannel.getRemoteAddress());

            targetChannel.register(selector, SelectionKey.OP_READ);
        }

    }

    private boolean handleRead(Selector selector, SelectionKey key) throws IOException {
        final SocketChannel clientChannel = (SocketChannel) key.channel();

        clientBuffer.clear();

        final int bytesRead = clientChannel.read(clientBuffer);
        if (bytesRead < 0) {
            return true; // end of stream
        }

        if (bytesRead > 0) {
            clientBuffer.flip();
            final SelectionKey targetKey = targetSocketChannel.keyFor(selector);
            targetKey.interestOps(targetKey.interestOps() | SelectionKey.OP_WRITE);
            selector.wakeup();
        }

        return false; // not end of stream
    }

    private void handleWrite(Selector selector, SelectionKey key) throws IOException {
        final SocketChannel targetChannel = (SocketChannel) key.channel();

        if (clientBuffer.hasRemaining()) {
            targetChannel.write(clientBuffer);
        }

        if (!clientBuffer.hasRemaining()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            clientBuffer.clear();
            final SelectionKey sourceKey = clientSocketChannel.keyFor(selector);
            sourceKey.interestOps(sourceKey.interestOps() | SelectionKey.OP_READ);
        }

    }

    public boolean runThread() {

        // wait for source socket to go into a readable state

        System.out.println("Accepting connection...");

        try {
            final Selector selector = Selector.open();

            handleAccept(selector);

            boolean endOfStream = false;
            do {
                selector.select();

                final Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    final SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (key.isValid()) {
                        if (key.isConnectable()) {
                            handleConnect(selector, key);
                        } else if (key.isReadable()) {
                            endOfStream = handleRead(selector, key);
                        } else if (key.isWritable()) {
                            handleWrite(selector, key);
                        }
                    }

                }

            } while (!endOfStream);

            return true;
        } catch (IOException e) {
            System.out.printf("Error connecting socket %s to %s (%s)%n", getRemoteAddress(clientSocketChannel), targetServer, e.getMessage());
            return false;

        } finally {
            closeChannel(this.clientSocketChannel);
            closeChannel(targetSocketChannel);
        }
    }
}
