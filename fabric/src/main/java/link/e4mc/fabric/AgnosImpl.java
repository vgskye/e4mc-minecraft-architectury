package link.e4mc.fabric;

import link.e4mc.E4mcClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class AgnosImpl {
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT);
    }

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static Path jarPath() {
        return FabricLoader.getInstance().getModContainer(E4mcClient.MOD_ID).get().getOrigin().getPaths().get(0);
    }
}
