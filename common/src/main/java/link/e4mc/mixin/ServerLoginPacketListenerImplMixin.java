package link.e4mc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import link.e4mc.DialtoneConnectionExtensions;
import link.e4mc.dialtone.DialtoneAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Shadow @Final
    Connection connection;

    @WrapOperation(method = "handleHello", at = @At(value = "INVOKE", target = "Ljava/security/PublicKey;getEncoded()[B"))
    private byte[] killDoubleEncryption(PublicKey instance, Operation<byte[]> operation) {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return new byte[0];
        }
        return operation.call(instance);
    }

    @WrapOperation(method = "handleKey", at = @At(value = "INVOKE", target = "*([BLjava/security/PrivateKey;)Z", remap = false), require = 0)
    private boolean isChallengeValid(ServerboundKeyPacket instance, byte[] bs, PrivateKey privateKey, Operation<Boolean> operation) {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return true;
        }
        return operation.call(instance, bs, privateKey);
    }

    @WrapOperation(method = "handleKey", at = @At(value = "INVOKE", target = "Ljava/util/Arrays;equals([B[B)Z"), require = 0)
    private boolean isNonceEqual(byte[] lhs, byte[] rhs, Operation<Boolean> operation) {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return true;
        }
        return operation.call(lhs, rhs);
    }

    @ModifyArg(method = "handleKey", at = @At(value = "INVOKE", target = "*(Ljava/security/PrivateKey;)[B", remap = false), index = 0, require = 0)
    private PrivateKey patchGetNonce(PrivateKey privateKey) {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return null;
        }
        return privateKey;
    }

    @WrapOperation(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/login/ServerboundKeyPacket;getSecretKey(Ljava/security/PrivateKey;)Ljavax/crypto/SecretKey;"))
    private SecretKey getSecretKey(ServerboundKeyPacket instance, PrivateKey privateKey, Operation<SecretKey> operation) {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return null;
        }
        return operation.call(instance, privateKey);
    }

    @WrapOperation(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;getCipher(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher getCipher(int i, Key key, Operation<Cipher> operation) {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return null;
        }
        return operation.call(i, key);
    }

    @WrapOperation(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;digestData(Ljava/lang/String;Ljava/security/PublicKey;Ljavax/crypto/SecretKey;)[B"))
    private byte[] digestData(String string, PublicKey publicKey, SecretKey secretKey, Operation<byte[]> operation) {
        if (connection.getRemoteAddress() instanceof DialtoneAddress) {
            return ((DialtoneConnectionExtensions) connection).e4mc$exportKeyingMaterial("EXPERIMENTAL mojang authentication".getBytes(StandardCharsets.UTF_8), new byte[0], 20);
        }
        return operation.call(string, publicKey, secretKey);
    }
}
