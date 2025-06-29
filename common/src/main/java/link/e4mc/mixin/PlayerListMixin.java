package link.e4mc.mixin;

import link.e4mc.Config;
import link.e4mc.E4mcClient;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow public abstract void setUsingWhiteList(boolean bl);

    @Shadow public abstract UserBanList getBans();

    @Shadow public abstract UserWhiteList getWhiteList();

    @Inject(method = "/^<init>$/", at = @At("TAIL"))
    void injectListLoads(CallbackInfo ci) {
        if (Config.INSTANCE.restoreDedicatedCommands.value()) {
            setUsingWhiteList(Config.INSTANCE.useWhiteList.value());
            try {
                this.getBans().load();
            } catch (IOException e) {
                E4mcClient.LOGGER.warn("Failed to load user banlist: ", e);
            }
            try {
                this.getWhiteList().load();
            } catch (IOException e) {
                E4mcClient.LOGGER.warn("Failed to load whitelist: ", e);
            }
        }
    }

    @Inject(method = "setUsingWhiteList", at = @At("TAIL"))
    public void injectSetUsingWhiteList(boolean bl, CallbackInfo ci) {
        Config.INSTANCE.useWhiteList.setValue(bl);
    }
}
