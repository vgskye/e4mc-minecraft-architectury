package link.e4mc.neoforge;

import link.e4mc.E4mcClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
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
        try {
            return Path.of(new File(E4mcClient.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
