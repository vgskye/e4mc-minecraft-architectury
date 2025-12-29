package link.e4mc.mixin;

import link.e4mc.DialtoneConnectionExtensions;
import link.e4mc.dialtone.DialtoneAddress;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientHandshakePacketListenerImplMixin {
    @Shadow @Final private Connection connection;

    @Redirect(method = "handleHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/login/ClientboundHelloPacket;getPublicKey()Ljava/security/PublicKey;"))
    private PublicKey publicKey(ClientboundHelloPacket instance) throws CryptException {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return null;
        }
        return instance.getPublicKey();
    }

    @Redirect(method = "handleHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;digestData(Ljava/lang/String;Ljava/security/PublicKey;Ljavax/crypto/SecretKey;)[B"))
    private byte[] digestData(String string, PublicKey publicKey, SecretKey secretKey) throws CryptException {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return ((DialtoneConnectionExtensions) connection).e4mc$exportKeyingMaterial("EXPERIMENTAL mojang authentication".getBytes(StandardCharsets.UTF_8), new byte[0], 20);
        }
        return Crypt.digestData(string, publicKey, secretKey);
    }
}
