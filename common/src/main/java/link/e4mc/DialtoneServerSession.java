package link.e4mc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import link.e4mc.dialtone.DialtoneAddress;
import link.e4mc.dialtone.DialtoneServerChannel;

public class DialtoneServerSession {
    private final ChannelHandler handler;
    private final EventLoopGroup group;
    private DialtoneServerChannel handlerChannel;

    public DialtoneServerSession(ChannelHandler handler, EventLoopGroup group) {
        this.handler = handler;
        this.group = group;
    }

    public void startAsync() {
        var thread = new Thread(this::start, "e4mc-dialtone-init");
        thread.setDaemon(true);
        thread.start();
    }

    public void start() {
        handlerChannel = (DialtoneServerChannel) new ServerBootstrap()
                .channel(DialtoneServerChannel.class)
                .childHandler(handler)
                .group(group)
                .localAddress(new DialtoneAddress(""))
                .bind()
                .syncUninterruptibly()
                .channel();
    }

    public void stop() {
        handlerChannel.close().syncUninterruptibly();
    }
}
