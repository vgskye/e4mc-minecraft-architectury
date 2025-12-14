package link.e4mc.neoforge;

import link.e4mc.E4mcClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public class AgnosImpl {
    public static boolean isClient() {
        try {
            return FMLEnvironment.class.getMethod("getDist").invoke(null).equals(Dist.CLIENT);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {}
        try {
            return FMLLoader.class.getMethod("getDist").invoke(null).equals(Dist.CLIENT);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException  ignored) {}
        throw new RuntimeException("Can't determine dist!");
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path jarPath() {
        var clazz = FMLLoader.class;
        try {
            var modList = (LoadingModList) clazz.getMethod("getLoadingModList").invoke(clazz.getMethod("getCurrent").invoke(null));
            return modList.getModFileById(E4mcClient.MOD_ID).getFile().getFilePath();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {}
        try {
            var modList = (LoadingModList) clazz.getMethod("getLoadingModList").invoke(null);
            return modList.getModFileById(E4mcClient.MOD_ID).getFile().getFilePath();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {}
        throw new RuntimeException("Can't determine jar path!");
    }
}
