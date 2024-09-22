package link.e4mc.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class AgnosImpl {
    public static boolean isClient() {
        return FMLLoader.getDist().equals(Dist.CLIENT);
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path jarPath() {
        return FMLLoader.getLoadingModList().getMods().stream().filter(modInfo -> modInfo.getModId().equals("e4mc_minecraft")).findAny().get().getOwningFile().getFile().getFilePath();
    }
}
