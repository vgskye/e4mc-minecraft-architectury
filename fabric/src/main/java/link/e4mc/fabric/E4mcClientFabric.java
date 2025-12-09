package link.e4mc.fabric;

import link.e4mc.E4mcClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class E4mcClientFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        E4mcClient.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> E4mcClient.registerCommands(dispatcher));
    }
}
