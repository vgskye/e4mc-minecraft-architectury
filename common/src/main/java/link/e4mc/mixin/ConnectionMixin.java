package link.e4mc.mixin;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import link.e4mc.DialtoneConnectionExtensions;
import link.e4mc.E4mcClient;
import link.e4mc.SmugglersInetSocketAddress;
import link.e4mc.dialtone.DialtoneAddress;
import link.e4mc.dialtone.DialtoneAmbientSession;
import link.e4mc.dialtone.DialtoneChannel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.crypto.Cipher;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(Connection.class)
public abstract class ConnectionMixin implements DialtoneConnectionExtensions {

    @Shadow private Channel channel;
    @Shadow private boolean encrypted;
    @Unique
    private static DialtoneAddress e4mc$smuggledDialtoneAddress = null;

    @Override
    public byte[] e4mc$exportKeyingMaterial(byte[] label, byte[] context, int length) {
        if (channel instanceof DialtoneChannel dialtoneChannel) {
            return dialtoneChannel.exportKeyingMaterial(label, context, length);
        }
        return null;
    }

    @Inject(method = "/^(connect|method_52271|m_290025_|method_10753|m_178300_)$/", at = @At("HEAD"))
    private static void hijackStart(InetSocketAddress inetSocketAddress, @Coerce Object obj, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        if (inetSocketAddress instanceof SmugglersInetSocketAddress smuggledAddress) {
            e4mc$smuggledDialtoneAddress = new DialtoneAddress(smuggledAddress.ticket);
        }
    }

    @Surrogate
    private static void hijackStart(InetSocketAddress inetSocketAddress, boolean bl, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        if (inetSocketAddress instanceof SmugglersInetSocketAddress smuggledAddress) {
            e4mc$smuggledDialtoneAddress = new DialtoneAddress(smuggledAddress.ticket);
        }
    }

    @Surrogate
    private static void hijackStart(InetSocketAddress inetSocketAddress, boolean bl, CallbackInfoReturnable<Connection> cir) {
        if (inetSocketAddress instanceof SmugglersInetSocketAddress smuggledAddress) {
            e4mc$smuggledDialtoneAddress = new DialtoneAddress(smuggledAddress.ticket);
        }
    }

    @ModifyArg(method = "/^(connect|method_52271|m_290025_|method_10753|m_178300_)$/", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"))
    private static Class hijackChannel(Class clazz) {
        if (e4mc$smuggledDialtoneAddress != null) {
            return DialtoneChannel.class;
        } else {
            return clazz;
        }
    }

    @ModifyArg(method = "/^(connect|method_52271|m_290025_|method_10753|m_178300_)$/", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"))
    private static EventLoopGroup hijackGroup(EventLoopGroup group) {
        if (e4mc$smuggledDialtoneAddress != null) {
            return DialtoneAmbientSession.INSTANCE.group;
        } else {
            return group;
        }
    }

    @Redirect(method = "/^(connect|method_52271|m_290025_|method_10753|m_178300_)$/", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;connect(Ljava/net/InetAddress;I)Lio/netty/channel/ChannelFuture;"))
    private static ChannelFuture hijackConnect(Bootstrap instance, InetAddress inetHost, int inetPort) {
        if (e4mc$smuggledDialtoneAddress != null) {
            var ret = instance.connect(e4mc$smuggledDialtoneAddress);
            e4mc$smuggledDialtoneAddress = null;
            return ret;
        } else {
            return instance.connect(inetHost, inetPort);
        }
    }

    @Inject(method = "setEncryptionKey", at = @At("HEAD"), cancellable = true)
    private void killDoubleEncryption(Cipher cipher, Cipher cipher2, CallbackInfo ci) {
        if (channel instanceof DialtoneChannel) {
            encrypted = true;
            ci.cancel();
        }
    }
}
