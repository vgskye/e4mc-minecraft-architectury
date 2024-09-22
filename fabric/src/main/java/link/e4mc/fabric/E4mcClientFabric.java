package link.e4mc.fabric;

import link.e4mc.E4mcClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

public class E4mcClientFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        E4mcClient.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, ignored) -> E4mcClient.registerCommands(dispatcher));
    }
}
