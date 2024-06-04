package link.e4mc;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class Agnos {
    @ExpectPlatform
    public static boolean isClient() {
        return false;
    }
}
