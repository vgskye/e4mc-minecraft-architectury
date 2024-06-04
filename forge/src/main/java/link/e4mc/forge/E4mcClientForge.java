package link.e4mc.forge;

import link.e4mc.E4mcClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(E4mcClient.MOD_ID)
public class E4mcClientForge {
    public E4mcClientForge() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommandEvent(RegisterCommandsEvent event) {
        E4mcClient.registerCommands(event.getDispatcher());
    }
}
