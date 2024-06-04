package link.e4mc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.incubator.codec.quic.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
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
    private static class ControlMessageCodec extends ByteToMessageCodec<ControlMessageCodec.ControlMessage> {
        public ControlMessageCodec() {
            super();
        }

        public interface ControlMessage {}

        public static class RequestDomainAssignmentMessageServerbound implements ControlMessage {
            String kind = "request_domain_assignment";
            public RequestDomainAssignmentMessageServerbound() {}
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

        @Override
        protected void encode(ChannelHandlerContext ctx, ControlMessage msg, ByteBuf out) {
            byte[] json = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);
            out.writeByte(json.length);
            out.writeBytes(json);
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
                    default:
                        throw new RuntimeException("Invalid message type!");
                }
            }
        }
    }

    private class ToMinecraftHandler extends ChannelInboundHandlerAdapter {
        QuicStreamChannel toQuiclime;
        public ToMinecraftHandler(QuicStreamChannel channel) {
            super();
            toQuiclime = channel;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.read();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            toQuiclime.writeAndFlush(msg).addListener(it -> {
                if (it.isSuccess()) {
                    ctx.channel().read();
                } else {
                    QuiclimeSession.this.state = State.UNHEALTHY;
                    if (Agnos.isClient()) {
                        Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
                    }
                    toQuiclime.close();
                }
            });
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            LOGGER.info("channel inactive(from MC): {} (MC: {})", toQuiclime, ctx.channel());
            if (toQuiclime.isActive()) {
                toQuiclime.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            QuiclimeSession.this.state = State.UNHEALTHY;
            if (Agnos.isClient()) {
                Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
            }
            this.channelInactive(ctx);
        }
    }

    private class ToQuiclimeHandler extends ChannelInboundHandlerAdapter {
        LocalChannel toMinecraft;

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            LOGGER.info("channel active: {}", ctx.channel());
            var fut = new Bootstrap()
                    .group(ctx.channel().eventLoop())
                    .channel(LocalChannel.class)
                    .handler(new ToMinecraftHandler((QuicStreamChannel) ctx.channel()))
                    .option(ChannelOption.AUTO_READ, false)
                    .connect(new LocalAddress("e4mc-relay"));
            toMinecraft = (LocalChannel) fut.channel();
            fut.addListener(it -> {
                if (it.isSuccess()) {
                    ctx.channel().read();
                } else {
                    QuiclimeSession.this.state = State.UNHEALTHY;
                    if (Agnos.isClient()) {
                        Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
                    }
                    ctx.channel().close();
                }
            });
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (toMinecraft.isActive()) {
                toMinecraft.writeAndFlush(msg).addListener(it -> {
                    if (it.isSuccess()) {
                        ctx.channel().read();
                    } else {
                        QuiclimeSession.this.state = State.UNHEALTHY;
                        if (Agnos.isClient()) {
                            Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
                        }
                        ((ChannelFuture) it).channel().close();
                    }
                });
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            LOGGER.info("channel inactive(from Quiclime): {} (MC: {})", ctx.channel(), toMinecraft);
            if (toMinecraft.isActive()) {
                toMinecraft.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt.equals(ChannelInputShutdownReadComplete.INSTANCE)) {
                this.channelInactive(ctx);
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            QuiclimeSession.this.state = State.UNHEALTHY;
            if (Agnos.isClient()) {
                Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
            }
            this.channelInactive(ctx);
        }
    }

    public State state = State.STARTING;
    public enum State {
        STARTING,
        STARTED,
        UNHEALTHY,
        STOPPING,
        STOPPED
    }

    private static class BrokerResponse {
        String id;
        String host;
        int port;
    }

    private final NioEventLoopGroup group = new NioEventLoopGroup();
    private NioDatagramChannel datagramChannel;
    private QuicChannel quicChannel;

    public QuiclimeSession() {
    }

    public void startAsync() {
        var thread = new Thread(this::start, "e4mc_minecraft-init");
        thread.setDaemon(true);
        thread.start();
    }

    public void start() {
        try {
            var httpClient = HttpClient.newHttpClient();
            var request = HttpRequest
                    .newBuilder(new URI("https://broker.e4mc.link/getBestRelay"))
                    .header("Accept", "application/json")
                    .build();
            LOGGER.info("req: {}", request);
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("resp: {}", response);
            if (response.statusCode() != 200) {
                throw new RuntimeException();
            }
            var relayInfo = gson.fromJson(response.body(), BrokerResponse.class);
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
            new Bootstrap()
                    .group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(0)
                    .addListener(datagramChannelFuture -> {
                if (!datagramChannelFuture.isSuccess()) {
                    QuiclimeSession.this.state = State.UNHEALTHY;
                    if (Agnos.isClient()) {
                        Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
                    }
                    throw new RuntimeException(datagramChannelFuture.cause());
                }
                datagramChannel = (NioDatagramChannel) ((ChannelFuture) datagramChannelFuture).channel();
                QuicChannel.newBootstrap(datagramChannel)
                        .streamHandler(
                                new ChannelInitializer<QuicStreamChannel>() {
                                    @Override
                                    protected void initChannel(QuicStreamChannel channel) {
                                        channel.pipeline().addLast(new ToQuiclimeHandler());
                                    }
                                }
                        )
                        .handler(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                super.exceptionCaught(ctx, cause);
                                QuiclimeSession.this.state = State.UNHEALTHY;
                                if (Agnos.isClient()) {
                                    Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
                                }
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                state = State.STOPPED;
                            }
                        })
                        .streamOption(ChannelOption.AUTO_READ, false)
                        .remoteAddress(new InetSocketAddress(InetAddress.getByName(relayInfo.host), relayInfo.port))
                        .connect()
                        .addListener(quicChannelFuture -> {
                    if (!quicChannelFuture.isSuccess()) {
                        QuiclimeSession.this.state = State.UNHEALTHY;
                        if (Agnos.isClient()) {
                            Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
                        }
                        throw new RuntimeException(datagramChannelFuture.cause());
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
                                                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, domain))
                                                            .withColor(ChatFormatting.GREEN)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mirror.translatable("chat.copy.click"))))
                                            ),
                                                    Mirror.withStyle(Mirror.translatable("text.e4mc_minecraft.clickToStop"), it ->
                                                            it
                                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/e4mc stop"))
                                                                    .withColor(ChatFormatting.GRAY)
                                                    )
                                            );
                                            Minecraft.getInstance().gui.getChat().addMessage(message);
                                        }
                                    }
                                    if (msg instanceof ControlMessageCodec.RequestMessageBroadcastMessageClientbound) {

                                        if (Agnos.isClient()) {
                                            Minecraft.getInstance().gui.getChat().addMessage(Mirror.literal(((ControlMessageCodec.RequestMessageBroadcastMessageClientbound) msg).message));
                                        }
                                    }
                                }
                            });
                        }
                    }).addListener(it -> {
                        if (!it.isSuccess()) {
                            QuiclimeSession.this.state = State.UNHEALTHY;
                            if (Agnos.isClient()) {
                                Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
                            }
                            throw new RuntimeException(datagramChannelFuture.cause());
                        }
                        QuicStreamChannel streamChannel = (QuicStreamChannel) it.getNow();
                        LOGGER.info("control channel open: {}", streamChannel);
                        streamChannel
                                .writeAndFlush(new ControlMessageCodec.RequestDomainAssignmentMessageServerbound())
                                .addListener(ignored -> LOGGER.info("control channel write complete"));


                        quicChannel.closeFuture().addListener(ignored -> datagramChannel.close());
                    });
                });
            });
        } catch (Throwable e) {
            QuiclimeSession.this.state = State.UNHEALTHY;
            if (Agnos.isClient()) {
                Minecraft.getInstance().gui.getChat().addMessage(Mirror.translatable("text.e4mc_minecraft.error"));
            }
            throw new RuntimeException(e);
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
        afterCloseIfPresent(quicChannel, a -> afterCloseIfPresent(datagramChannel, b -> group.shutdownGracefully().addListener(c -> state = State.STOPPED)));
    }
}
