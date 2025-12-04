package link.e4mc.fabric;

import link.e4mc.E4mcClient;
import net.fabricmc.api.ModInitializer;

public class E4mcClientFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        E4mcClient.init();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, ignored, ignored2) -> E4mcClient.registerCommands(dispatcher));
    }
}
