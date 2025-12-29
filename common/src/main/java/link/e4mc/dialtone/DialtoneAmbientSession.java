package link.e4mc.dialtone;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import link.e4mc.E4mcClient;
import link.e4mc.QuiclimeSession;
import link.e4mc.iroh.Endpoint;
import link.e4mc.iroh.NativeException;

import java.nio.charset.StandardCharsets;

public class DialtoneAmbientSession {
    public static final DialtoneAmbientSession INSTANCE = new DialtoneAmbientSession();

    public EventLoopGroup group = new DefaultEventLoopGroup();
    Endpoint endpoint;
    Thread dispatcher;

    private DialtoneAmbientSession() {}

    public void start() throws Exception {
        E4mcClient.LOGGER.info("Starting DialtoneAmbientSession!");
        this.endpoint = new Endpoint(new byte[][]{"e4mc-dialtone".getBytes(StandardCharsets.UTF_8)}, QuiclimeSession.getRelayMap());
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
        }, "Dialtone Session Dispatcher");
        this.dispatcher.setDaemon(true);
        this.dispatcher.start();
    }

    public void stop() {
        endpoint.closeAsync().join();
        endpoint.close();
        dispatcher.interrupt();
        endpoint = null;
        dispatcher = null;
    }
}
