package link.e4mc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.incubator.codec.quic.*;
import link.e4mc.dialtone.DialtoneAddress;
import link.e4mc.dialtone.DialtoneServerChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class QuiclimeSession {
    private static final Gson gson = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(E4mcClient.MOD_ID);
    final ChannelHandler handler;

    private static class ControlMessageCodec extends ByteToMessageCodec<ControlMessageCodec.ControlMessage> {
        public ControlMessageCodec() {
            super();
        }

        public interface ControlMessage {}

        public static class ProbeCapabilitiesMessageServerbound implements ControlMessage {
            String kind = "probe_capabilities";
            public ProbeCapabilitiesMessageServerbound() {}
        }

        public static class RequestDomainAssignmentMessageServerbound implements ControlMessage {
            String kind = "request_domain_assignment";
            public RequestDomainAssignmentMessageServerbound() {}
        }

        public static class DialtoneRegisterTicketMessageServerbound implements ControlMessage {
            String kind = "dialtone_register_ticket";
            String ticket;
            public DialtoneRegisterTicketMessageServerbound(String ticket) {
                this.ticket = ticket;
            }
        }

        public static class DomainAssignmentCompleteMessageClientbound implements ControlMessage {
            String kind = "domain_assignment_complete";
            String domain;
            public DomainAssignmentCompleteMessageClientbound(String domain) {
                this.domain = domain;
            }
        }

        public static class RequestMessageBroadcastMessageClientbound implements ControlMessage {
            String kind = "request_message_broadcast";
            String message;
            public RequestMessageBroadcastMessageClientbound(String message) {
                this.message = message;
            }
        }

        public static class HasCapabilitiesMessageClientbound implements ControlMessage {
            String kind = "has_capabilities";
            String[] caps;
            public HasCapabilitiesMessageClientbound(String[] caps) {
                this.caps = caps;
            }
        }

        public static class TicketRegisteredMessageClientbound implements ControlMessage {
            String kind = "ticket_registered";
            public TicketRegisteredMessageClientbound() {
            }
        }

        public static class UnknownMessageMessageClientbound implements ControlMessage {
            String kind = "ticket_registered";
            public UnknownMessageMessageClientbound() {
            }
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ControlMessage msg, ByteBuf out) {
            E4mcClient.LOGGER.info("writing {}", msg);
            try {
                byte[] json = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);
                writeVarInt(out, json.length);
                out.writeBytes(json);
            } catch (Throwable e) {
                E4mcClient.LOGGER.error("weird", e);
            }
            E4mcClient.LOGGER.info("writing {} bytes", out.readableBytes());
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            int size = in.getByte(in.readerIndex());
            if (in.readableBytes() >= size + 1) {
                in.skipBytes(1);
                var buf = new byte[size];
                in.readBytes(buf);
                var json = gson.fromJson(new String(buf, StandardCharsets.UTF_8), JsonObject.class);
                switch (json.get("kind").getAsString()) {
                    case "domain_assignment_complete":
                        out.add(gson.fromJson(json, DomainAssignmentCompleteMessageClientbound.class));
                        break;
                    case "request_message_broadcast":
                        out.add(gson.fromJson(json, RequestMessageBroadcastMessageClientbound.class));
                        break;
                    case "has_capabilities":
                        out.add(gson.fromJson(json, HasCapabilitiesMessageClientbound.class));
                        break;
                    case "ticket_registered":
                        out.add(gson.fromJson(json, TicketRegisteredMessageClientbound.class));
                        break;
                    case "unknown_message":
                        out.add(gson.fromJson(json, UnknownMessageMessageClientbound.class));
                        break;
                    default:
                        throw new RuntimeException("Invalid message type!");
                }
            }
        }
    }

    public State state = State.STARTING;
    public Throwable failureCause = null;
    public enum State {
        STARTING,
        STARTED,
        UNHEALTHY,
        STOPPING,
        STOPPED
    }

    static class BrokerResponse {
        String id;
        String host;
        int port;
    }

    final EventLoopGroup group;
    private DatagramChannel datagramChannel;
    private QuicChannel quicChannel;

    private DialtoneServerChannel dialtoneChannel;

    public QuiclimeSession(ChannelHandler handler, EventLoopGroup group) {
        this.handler = handler;
        this.group = group;
    }

    public void startAsync() {
        var thread = new Thread(this::start, "e4mc_minecraft-init");
        thread.setDaemon(true);
        thread.start();
    }

    private static BrokerResponse getRelay() throws Exception {
        if (Config.INSTANCE.useBroker.value()) {
            var httpClient = HttpClient.newHttpClient();
            var request = HttpRequest
                    .newBuilder(new URI(Config.INSTANCE.brokerUrl.value()))
                    .header("Accept", "application/json")
                    .build();
            LOGGER.info("req: {}", request);
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("resp: {}", response);
            if (response.statusCode() != 200) {
                throw new RuntimeException();
            }
            return gson.fromJson(response.body(), BrokerResponse.class);
        } else {
            var resp = new BrokerResponse();
            resp.id = "custom";
            resp.host = Config.INSTANCE.relayHost.value();
            resp.port = Config.INSTANCE.relayPort.value();
            return resp;
        }
    }

    public void start() {
        try {
            var relayInfo = getRelay();
            LOGGER.info("using relay {}", relayInfo.id);
            QuicSslContext context = QuicSslContextBuilder
                    .forClient()
                    .applicationProtocols("quiclime")
                    .build();
            var codec = new QuicClientCodecBuilder()
                    .sslContext(context)
                    .sslEngineProvider(it -> context.newEngine(it.alloc(), relayInfo.host, relayInfo.port))
                    .initialMaxStreamsBidirectional(512)
                    .maxIdleTimeout(10, TimeUnit.SECONDS)
                    .initialMaxData(4611686018427387903L)
                    .initialMaxStreamDataBidirectionalRemote(1250000)
                    .initialMaxStreamDataBidirectionalLocal(1250000)
                    .initialMaxStreamDataUnidirectional(1250000)
                    .build();
            Class<? extends DatagramChannel> channelClass = null;
            if (group instanceof EpollEventLoopGroup) {
                channelClass = EpollDatagramChannel.class;
            } else if (group instanceof NioEventLoopGroup) {
                channelClass = NioDatagramChannel.class;
            } else if (group instanceof KQueueEventLoopGroup) {
                channelClass = KQueueDatagramChannel.class;
            } else if (group instanceof MultiThreadIoEventLoopGroup mig) {
                if (mig.isIoType(EpollIoHandler.class)) {
                    channelClass = EpollDatagramChannel.class;
                } else if (mig.isIoType(NioIoHandler.class)) {
                    channelClass = NioDatagramChannel.class;
                } else if (mig.isIoType(KQueueIoHandler.class)) {
                    channelClass = KQueueDatagramChannel.class;
                }
            } else {
                throw new RuntimeException("Unknown EventLoopGroup " + group.getClass().getName());
            }
            new Bootstrap()
                    .group(group)
                    .channel(channelClass)
                    .handler(codec)
                    .bind(0)
                    .addListener(datagramChannelFuture -> {
                if (!datagramChannelFuture.isSuccess()) {
                    fail(datagramChannelFuture.cause());
                    throw new RuntimeException(datagramChannelFuture.cause());
                }
                datagramChannel = (DatagramChannel) ((ChannelFuture) datagramChannelFuture).channel();
                QuicChannel.newBootstrap(datagramChannel)
                        .streamHandler(handler)
                        .handler(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                super.exceptionCaught(ctx, cause);
                                fail(cause);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                state = State.STOPPED;
                            }
                        })
                        .remoteAddress(new InetSocketAddress(InetAddress.getByName(relayInfo.host), relayInfo.port))
                        .connect()
                        .addListener(quicChannelFuture -> {
                    if (!quicChannelFuture.isSuccess()) {
                        fail(quicChannelFuture.cause());
                        throw new RuntimeException(quicChannelFuture.cause());
                    }
                    quicChannel = (QuicChannel) quicChannelFuture.get();
                    quicChannel.createStream(QuicStreamType.BIDIRECTIONAL,
                            new ChannelInitializer<QuicStreamChannel>() {
                                @Override
                                protected void initChannel(QuicStreamChannel ch) {
                            ch.pipeline().addLast(new ControlMessageCodec(), new SimpleChannelInboundHandler<ControlMessageCodec.ControlMessage>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ControlMessageCodec.ControlMessage msg) {
                                    if (msg instanceof ControlMessageCodec.DomainAssignmentCompleteMessageClientbound) {
                                        state = State.STARTED;
                                        if (!Agnos.isClient()) {
                                            LOGGER.warn("e4mc running on Dedicated Server; This works, but isn't recommended as e4mc is designed for short-lived LAN servers");
                                        }
                                        String domain = ((ControlMessageCodec.DomainAssignmentCompleteMessageClientbound) msg).domain;
                                        LOGGER.info("Domain assigned: {}", domain);
                                        if (Agnos.isClient()) {
                                            Component message = Mirror.append(Mirror.translatable(
                                                    "text.e4mc_minecraft.domainAssigned",
                                                    Mirror.withStyle(Mirror.literal(domain), it ->
                                                    it
                                                            .withClickEvent(Mirror.copyToClipboard(domain))
                                                            .withColor(ChatFormatting.GREEN)
                                                            .withHoverEvent(Mirror.showText(Mirror.translatable("chat.copy.click"))))
                                            ),
                                                    Mirror.withStyle(Mirror.translatable("text.e4mc_minecraft.clickToStop"), it ->
                                                            it
                                                                    .withClickEvent(Mirror.runCommand("/e4mc stop"))
                                                                    .withColor(ChatFormatting.GRAY)
                                                    )
                                            );
                                            Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.getChat().addMessage(message));
                                            if (E4mcClient.badurl) {
                                                Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.poisonpill.badurl")));
                                            }
                                        }
                                    }
                                    if (msg instanceof ControlMessageCodec.RequestMessageBroadcastMessageClientbound) {
                                        if (Agnos.isClient()) {
                                            Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.getChat().addMessage(Mirror.literal(((ControlMessageCodec.RequestMessageBroadcastMessageClientbound) msg).message)));
                                        }
                                    }
                                    if (msg instanceof ControlMessageCodec.HasCapabilitiesMessageClientbound) {
                                        var streamChannel = ctx.channel();
                                        boolean hasDialtoneSidecar = false;
                                        for (String cap : ((ControlMessageCodec.HasCapabilitiesMessageClientbound) msg).caps) {
                                            if (cap.equals("dialtone_sidecar")) {
                                                hasDialtoneSidecar = true;
                                                break;
                                            }
                                        }
                                        if (hasDialtoneSidecar && Config.INSTANCE.dialtoneHostEnabled.value()) {
                                            new ServerBootstrap()
                                                    .channel(DialtoneServerChannel.class)
                                                    .handler(new ChannelInboundHandlerAdapter() {
                                                        @Override
                                                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                                            super.userEventTriggered(ctx, evt);
                                                            if (evt instanceof DialtoneAddress addr) {
                                                                streamChannel
                                                                        .writeAndFlush(new ControlMessageCodec.DialtoneRegisterTicketMessageServerbound(addr.actualAddress))
                                                                        .addListener(ignored -> LOGGER.info("notified server of our ticket"));
                                                            }
                                                        }
                                                    })
                                                    .childHandler(handler)
                                                    .group(group)
                                                    .localAddress(new DialtoneAddress(""))
                                                    .bind()
                                                    .addListener(dialtoneChannelFuture -> {
                                                        if (!dialtoneChannelFuture.isSuccess()) {
                                                            fail(dialtoneChannelFuture.cause());
                                                            throw new RuntimeException(dialtoneChannelFuture.cause());
                                                        }
                                                        dialtoneChannel = (DialtoneServerChannel) dialtoneChannelFuture.get();
                                                    });
                                        }
                                    }
                                }
                            });
                        }
                    }).addListener(it -> {
                        if (!it.isSuccess()) {
                            fail(it.cause());
                            throw new RuntimeException(it.cause());
                        }
                        QuicStreamChannel streamChannel = (QuicStreamChannel) it.getNow();
                        LOGGER.info("control channel open: {}", streamChannel);
                        streamChannel
                                .writeAndFlush(new ControlMessageCodec.ProbeCapabilitiesMessageServerbound())
                                .addListener(ignored -> LOGGER.info("probing capabilities"));
                        streamChannel
                                .writeAndFlush(new ControlMessageCodec.RequestDomainAssignmentMessageServerbound())
                                .addListener(ignored -> LOGGER.info("control channel write complete"));
                        quicChannel.closeFuture().addListener(ignored -> datagramChannel.close());
                    });
                });
            });
        } catch (Throwable e) {
            fail(e);
            throw new RuntimeException(e);
        }
    }

    private void fail(Throwable e) {
        QuiclimeSession.this.state = State.UNHEALTHY;
        failureCause = e;
        E4mcClient.LOGGER.error("error in e4mc", e);
        if (Agnos.isClient()) {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error")));
        }
    }

    private static void afterCloseIfPresent(Channel channel, Consumer<Boolean> callback) {
        if (channel == null) {
            callback.accept(false);
        } else {
            channel.close().addListener(it -> callback.accept(true));
        }
    }

    public void stop() {
        state = State.STOPPING;
        afterCloseIfPresent(dialtoneChannel, q -> afterCloseIfPresent(quicChannel, a -> afterCloseIfPresent(datagramChannel, b -> state = State.STOPPED)));
    }


    private static ByteBuf writeVarInt(ByteBuf buf, int value) {
        while ((value & 0xffffff80) != 0) {
            buf.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }

        buf.writeByte(value);
        return buf;
    }
}
