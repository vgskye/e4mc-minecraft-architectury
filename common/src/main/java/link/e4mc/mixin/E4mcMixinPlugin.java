package link.e4mc.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class E4mcMixinPlugin implements IMixinConfigPlugin {
    private static final boolean KRYPTON_LOADED = isKryptonLoaded();

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Disable ServerLoginPacketListenerImplMixin if Krypton is loaded
        // Krypton's cipher optimization takes priority
        if (mixinClassName.contains("ServerLoginPacketListenerImplMixin") && KRYPTON_LOADED) {
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    private static boolean isKryptonLoaded() {
        try {
            Class.forName("me.steinborn.krypton.mod.shared.KryptonMixinPlugin");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
