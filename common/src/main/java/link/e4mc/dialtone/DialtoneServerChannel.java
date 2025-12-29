package link.e4mc.dialtone;

import io.netty.channel.AbstractServerChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import link.e4mc.E4mcClient;
import link.e4mc.iroh.Endpoint;
import link.e4mc.iroh.NativeException;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class DialtoneServerChannel extends AbstractServerChannel {
    private final ChannelConfig config = new DefaultChannelConfig(this);
    Endpoint endpoint;
    Thread dispatcher;
    boolean closed = false;

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    protected SocketAddress localAddress0() {
        return new DialtoneAddress(endpoint.address());
    }

    @Override
    protected void doBind(SocketAddress localAddress) {
        this.endpoint = new Endpoint(new byte[][]{"e4mc-dialtone".getBytes(StandardCharsets.UTF_8)});
        this.dispatcher = new Thread(() -> {
                while (true) {
                    try {
                        Runnable polled = endpoint.pollCallbackLoop();
                        polled.run();
                    } catch (NativeException e) {
                        E4mcClient.LOGGER.error("poll exc, stopping", e);
                        throw e;
                    } catch (Throwable e) {
                        E4mcClient.LOGGER.error("poll exc, continuing", e);
                    }
                }
        }, "Dialtone Server Dispatcher");
        this.dispatcher.setDaemon(true);
        this.dispatcher.start();
        endpoint.waitOnline().thenAccept(addr -> {
            E4mcClient.LOGGER.warn("session ticket is {}", addr);
            this.pipeline().fireUserEventTriggered(new DialtoneAddress(addr));
        });
    }

    @Override
    protected void doClose() {
        endpoint.closeAsync().join();
        endpoint.close();
        dispatcher.interrupt();
        endpoint = null;
        dispatcher = null;
        closed = true;
    }

    @Override
    protected void doBeginRead() throws Exception {
        endpoint.accept().thenAccept(preconn -> {
            E4mcClient.LOGGER.info("preconn accepted, dialtone child registered");
            var channel = new DialtoneChannel(this);
            pipeline().fireChannelRead(channel);
            pipeline().fireChannelReadComplete();
            preconn.thenAccept(conn -> {
                E4mcClient.LOGGER.info("conn accepted, dialtone child pre-active");
                channel.connection = conn;
                conn.acceptBi().thenAccept(bidi -> {
                    E4mcClient.LOGGER.info("bidi accepted, dialtone child active");
                    channel.stream = bidi;
                    channel.pipeline().fireChannelActive();
                });
            }).exceptionally(e -> {
                channel.pipeline().fireChannelInactive();
                pipeline().fireExceptionCaught(e);
                return null;
            });
        }).exceptionally(e -> {
            pipeline().fireExceptionCaught(e);
            return null;
        });
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public boolean isActive() {
        return endpoint != null;
    }
}
