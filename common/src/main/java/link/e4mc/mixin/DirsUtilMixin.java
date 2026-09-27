package link.e4mc.mixin;

import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Shell32Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.dirs.Util")
public class DirsUtilMixin {
    @Inject(method = "getWinDirs", at = @At("HEAD"), cancellable = true)
    private static void useJna(String[] guids, CallbackInfoReturnable<String[]> cir) {
        String[] ret = new String[guids.length];
        for (int i = 0; i < guids.length; i++) {
            ret[i] = Shell32Util.getKnownFolderPath(Guid.GUID.fromString(guids[i]));
        }
        cir.setReturnValue(ret);
    }
}
