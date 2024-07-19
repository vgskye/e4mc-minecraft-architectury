package link.e4mc;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class Agnos {
    @ExpectPlatform
    public static boolean isClient() {
        return false;
    }

    @ExpectPlatform
    public static Path configDir() {
        return null;
    }
}
