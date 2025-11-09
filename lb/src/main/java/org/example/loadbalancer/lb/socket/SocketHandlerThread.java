package org.example.loadbalancer.lb.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.example.loadbalancer.lb.util.Helper.closeChannel;
import static org.example.loadbalancer.lb.util.Helper.getRemoteAddress;

/*
 * This class represents a thread that will deal with one load-balanced socket
 * No routing is done here - its primary purpose is to stream bytes from the client port:IP to the target server port:IP
 *
 * Since we're a level 4 load balancer, we don't assume any protocols either. It could be TLS, raw HTTP, telnet or
 * anything else. We are transferring raw bytes and are agnostic to what the socket holds.
 */
public class SocketHandlerThread implements SocketHandler {

    private final InetSocketAddress targetServer;
    private final Socket clientSocket;
    private final int bufferSizeBytes;

    public SocketHandlerThread(Socket clientSocket, InetSocketAddress targetServer, int bufferSizeBytes) {
        this.clientSocket = clientSocket;
        this.targetServer = targetServer;
        this.bufferSizeBytes = bufferSizeBytes;
    }

    public boolean runThread() {
        Socket serverSocket = null;
        InputStream clientReader = null;
        OutputStream clientWriter = null;
        InputStream serverReader = null;
        OutputStream serverWriter = null;

        try {
            String clientIp = clientSocket.getRemoteSocketAddress().toString();
            System.out.println("A client has connected: " + clientIp);

            clientReader = clientSocket.getInputStream();
            clientWriter = clientSocket.getOutputStream();

            final byte[] clientData = readBytes(clientReader);
            if (clientData == null) {
                System.out.println("ERROR: end of client stream");
                return false;
            }

            System.out.println(clientIp + " connecting to backend server: " + targetServer);

            serverSocket = new Socket(targetServer.getHostName(), targetServer.getPort());

            System.out.println("Connected to " + targetServer);

            serverReader = serverSocket.getInputStream();
            serverWriter = serverSocket.getOutputStream();
            serverWriter.write(clientData, 0, clientData.length);
            serverWriter.flush();

            byte[] serverRespData = readBytes(serverReader);
            if (serverRespData == null) {
                System.out.println("ERROR: end of server stream");
                return false;
            }

            clientWriter.write(serverRespData, 0, serverRespData.length);
            clientWriter.flush();

            return true;
        } catch (IOException e) {
            System.out.printf("Error connecting socket %s to %s (%s)%n", getRemoteAddress(clientSocket.getChannel()), targetServer, e.getMessage());
            return false;
        } finally {
            closeChannel(serverSocket);
            closeChannel(clientSocket);

            closeChannel(clientReader);
            closeChannel(clientWriter);
            closeChannel(serverReader);
            closeChannel(serverWriter);
        }
    }

    private byte[] readBytes(InputStream incomingStream) throws IOException {

        byte[] buf = new byte[bufferSizeBytes];

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            int len = incomingStream.read(buf);
            if (len < 0) {
                // end of stream
                return null;
            }

            if (len > 0) {
                stream.write(buf, 0, len);
                stream.flush();
            }
        }
        return buf;
    }
}
