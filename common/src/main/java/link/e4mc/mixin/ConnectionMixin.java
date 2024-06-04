package link.e4mc.mixin;

import io.netty.channel.Channel;
import io.netty.channel.local.LocalAddress;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Shadow
    private Channel channel;

    @Inject(method = "isMemoryConnection", at = @At("RETURN"), cancellable = true)
    private void isMemoryConnectionInject(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            if (this.channel.localAddress().equals(new LocalAddress("e4mc-relay"))) {
                cir.setReturnValue(false);
            }
        }
    }
}