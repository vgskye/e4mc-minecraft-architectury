package link.e4mc.mixin;

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import link.e4mc.Config;
import link.e4mc.E4mcClient;
import link.e4mc.QuiclimeSession;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;

@Mixin(ServerConnectionListener.class)
public abstract class ServerConnectionListenerMixin {
    @Unique
    private ChannelHandler e4mc$childHandler;
    @Unique
    private EventLoopGroup e4mc$group;

    @ModifyArg(method = "startTcpServerListener", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;childHandler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/ServerBootstrap;", remap = false))
    private ChannelHandler interceptHandler(ChannelHandler childHandler) {
        e4mc$childHandler = childHandler;
        return childHandler;
    }

    @ModifyArg(method = "startTcpServerListener", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/ServerBootstrap;", remap = false))
    private EventLoopGroup interceptGroup(EventLoopGroup group) {
        e4mc$group = group;
        return group;
    }

    @Inject(method = "startTcpServerListener", at = @At(value = "TAIL"))
    private void interceptGroup(InetAddress inetAddress, int i, CallbackInfo ci) {
        if (Config.INSTANCE.hostEnabled.value()) {
            E4mcClient.session = new QuiclimeSession(e4mc$childHandler, e4mc$group);
            e4mc$childHandler = null;
            e4mc$group = null;
            E4mcClient.session.startAsync();
        } else {
            e4mc$childHandler = null;
            e4mc$group = null;
        }
    }

    @Inject(method = "stop", at = @At(value = "HEAD"))
    private void interceptStop(CallbackInfo ci) {
        QuiclimeSession session = E4mcClient.session;
        if ((session != null) && (session.state != QuiclimeSession.State.STOPPED)) {
            session.stop();
            E4mcClient.session = null;
        }
    }
}
