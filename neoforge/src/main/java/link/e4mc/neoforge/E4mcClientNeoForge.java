package link.e4mc.neoforge;

import link.e4mc.E4mcClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(E4mcClient.MOD_ID)
public class E4mcClientNeoForge {
    public E4mcClientNeoForge() {
        E4mcClient.init();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommandEvent(RegisterCommandsEvent event) {
        E4mcClient.registerCommands(event.getDispatcher());
    }
}
