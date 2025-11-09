package org.example.loadbalancer.lb.util;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import static org.example.loadbalancer.lb.util.Helper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HelperTest {

    @Test
    void shouldParseServerHostPort() {
        final InetSocketAddress address = getInetSocketAddress("localhost:1234");
        assertEquals("localhost", address.getHostName());
        assertEquals(1234, address.getPort());
    }

    @Test
    void shouldParseServerIpPort() {
        final InetSocketAddress address = getInetSocketAddress("1.2.3.4:1234");
        assertEquals("1.2.3.4", address.getHostName());
        assertEquals(1234, address.getPort());
    }

    @Test
    void shouldCloseChannelAndNotThrowOnNull() throws IOException {
        assertDoesNotThrow(() -> closeChannel(null));
    }

    @Test
    void shouldSuppressException() throws IOException {
        final Closeable throwingCloseable = () -> { throw new IOException("dummy"); };
        assertDoesNotThrow(() -> closeChannel(throwingCloseable));
    }

    @Test
    void shouldCloseChannel() throws IOException {
        final Closeable closeable = mock(Closeable.class);
        doAnswer((Answer<Void>) invocation -> null).when(closeable).close();
        closeChannel(closeable);
        verify(closeable).close();
    }

    @Test
    void shouldGetRemoteAddress() throws IOException {

        final SocketChannel channel = mock(SocketChannel.class);
        when(channel.getRemoteAddress()).thenReturn(getInetSocketAddress("1.2.3.4:1234"));
        final String actual = getRemoteAddress(channel);
        assertEquals("/1.2.3.4:1234", actual);
    }

    @Test
    void shouldGetEmptyRemoteAddressOnError() throws IOException {

        final SocketChannel channel = mock(SocketChannel.class);
        when(channel.getRemoteAddress()).thenThrow(new IOException("dummy"));
        final String actual = getRemoteAddress(channel);
        assertEquals("", actual);
    }
}