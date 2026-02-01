package link.e4mc.mixin;

import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.security.Key;
import java.security.PrivateKey;

@Mixin(ServerboundKeyPacket.class)
public class ServerboundKeyPacketMixin {
    @Redirect(method = "<init>(Ljavax/crypto/SecretKey;Ljava/security/PublicKey;[B)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;encryptUsingKey(Ljava/security/Key;[B)[B"))
    private byte[] encryptUsingKey(Key key, byte[] bs) throws CryptException {
        if (key == null) {
            return new byte[0];
        }
        return Crypt.encryptUsingKey(key, bs);
    }

    @Inject(method = "*(Ljava/security/PrivateKey;)[B", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void patchGetNonce(PrivateKey privateKey, CallbackInfoReturnable<byte[]> callbackInfoReturnable) {
        if (privateKey == null) {
            callbackInfoReturnable.setReturnValue(new byte[0]);
        }
    }
}
