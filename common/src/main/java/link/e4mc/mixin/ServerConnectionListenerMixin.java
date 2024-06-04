package link.e4mc.mixin;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import link.e4mc.E4mcClient;
import link.e4mc.QuiclimeSession;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.InetAddress;

@Mixin(ServerConnectionListener.class)
public abstract class ServerConnectionListenerMixin {
    @Shadow public abstract void startTcpServerListener(InetAddress inetAddress, int i) throws IOException;

    private static final ThreadLocal<Boolean> initializingE4mc = ThreadLocal.withInitial(() -> false);

    @Inject(method = "startTcpServerListener", at = @At("HEAD"))
    private void startTcpServerListenerInject(InetAddress address, int port, CallbackInfo ci) {
        if (!initializingE4mc.get()) {
            initializingE4mc.set(true);
            try {
                startTcpServerListener(address, port);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                initializingE4mc.set(false);
            }
        } else {
            E4mcClient.session = new QuiclimeSession();
            E4mcClient.session.startAsync();
        }
    }

    @ModifyArg(method = "startTcpServerListener", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private Class<? extends ServerChannel> redirectChannel(Class<? extends ServerChannel> aClass) {
        return initializingE4mc.get() ? LocalServerChannel.class : aClass;
    }

    @Redirect(method = "startTcpServerListener", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;localAddress(Ljava/net/InetAddress;I)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private AbstractBootstrap<ServerBootstrap, ServerChannel> redirectAddress(ServerBootstrap instance, InetAddress address, int port) {
        return initializingE4mc.get() ? instance.localAddress(new LocalAddress("e4mc-relay")) : instance.localAddress(address, port);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void stop(CallbackInfo ci) {
        if (E4mcClient.session != null) {
            E4mcClient.session.stop();
        }
    }
}
