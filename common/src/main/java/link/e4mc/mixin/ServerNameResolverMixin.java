package link.e4mc.mixin;

import link.e4mc.SmugglersInetSocketAddress;
import link.e4mc.TicketSmuggler;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(ServerNameResolver.class)
public class ServerNameResolverMixin {
    @Redirect(method = "resolveAddress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/resolver/ServerAddressResolver;resolve(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;"))
    private Optional<ResolvedServerAddress> resolveBogus(ServerAddressResolver instance, ServerAddress serverAddress) {
        var smuggledTicket = ((TicketSmuggler) (Object) serverAddress).e4mc$getSmuggledTicket();
        if (smuggledTicket != null) {
            return instance.resolve(serverAddress).map(addr -> ResolvedServerAddress.from(new SmugglersInetSocketAddress(addr.asInetSocketAddress(), smuggledTicket)));
        }
        return instance.resolve(serverAddress);
    }
}
