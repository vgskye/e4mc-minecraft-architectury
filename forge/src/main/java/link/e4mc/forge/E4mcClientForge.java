package link.e4mc.forge;

import link.e4mc.E4mcClient;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod(E4mcClient.MOD_ID)
public class E4mcClientForge {
    public E4mcClientForge() {
        E4mcClient.init();
        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommandEvent);
    }

    @SubscribeEvent
    public void onRegisterCommandEvent(RegisterCommandsEvent event) {
        E4mcClient.registerCommands(event.getDispatcher());
    }
}