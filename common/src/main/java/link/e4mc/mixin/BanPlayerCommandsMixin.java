package link.e4mc.mixin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import link.e4mc.Mirror;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.BanPlayerCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(BanPlayerCommands.class)
public class BanPlayerCommandsMixin {
    @Redirect(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"))
    private static ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> allowOwner(LiteralArgumentBuilder<CommandSourceStack> instance, Predicate<CommandSourceStack> predicate) {
        return instance.requires(src -> {
            try {
                if (Mirror.isSingleplayerOwner(src.getServer(), src.getPlayerOrException()))
                    return true;
            } catch (CommandSyntaxException ignored) {
            }
            return predicate.test(src);
        });
    }
}
