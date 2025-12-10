package link.e4mc.mixin;

import com.mojang.authlib.GameProfile;
import link.e4mc.Config;
import link.e4mc.E4mcClient;
import link.e4mc.Mirror;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.SocketAddress;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow public abstract UserBanList getBans();

    @Shadow public abstract UserWhiteList getWhiteList();

    @Shadow public abstract MinecraftServer getServer();

    @Inject(method = "/^<init>$/", at = @At("TAIL"))
    void injectListLoads(CallbackInfo ci) {
        if (Config.INSTANCE.restoreDedicatedCommands.value()) {
            Mirror.setUsingWhitelist(getServer(), (PlayerList) (Object) this, Config.INSTANCE.useWhiteList.value());
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

    @Inject(method = "/^(canPlayerLogin|method_14586|checkCanJoin|m_6418_)$/", at = @At("HEAD"), cancellable = true)
    public void allowOwnerLogin(SocketAddress socketAddress, @Coerce Object gameProfile, CallbackInfoReturnable<Component> cir) {
        if (Mirror.isSingleplayerOwnerObj(getServer(), gameProfile)) {
            cir.setReturnValue(null);
        }
    }
}
