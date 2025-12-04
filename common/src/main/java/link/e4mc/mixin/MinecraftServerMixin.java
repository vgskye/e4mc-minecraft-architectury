package link.e4mc.mixin;

import link.e4mc.Config;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow public abstract void setUsingWhitelist(boolean bl);

    @Inject(method = "setUsingWhitelist", at = @At("TAIL"))
    public void injectSetUsingWhitelist(boolean bl, CallbackInfo ci) {
        Config.INSTANCE.useWhiteList.setValue(bl);
    }
}
