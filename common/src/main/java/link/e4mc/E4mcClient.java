package link.e4mc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class E4mcClient {
    public static final String MOD_ID = "e4mc_minecraft";
    public static QuiclimeSession session;
    private static final Logger LOGGER = LoggerFactory.getLogger(E4mcClient.MOD_ID);
    public static void init() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            var path = Agnos.jarPath();
            var motwPath = path + ":Zone.Identifier";
            try(FileInputStream inputStream = new FileInputStream(motwPath)) {
                String hidden = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                LOGGER.warn(hidden);
            } catch (IOException ignored) {}
        }
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("e4mc")
                        .requires(src -> {
                            if (src.getServer().isDedicatedServer()) {
                                return src.hasPermission(4);
                            } else {
                                try {
                                    return src.getServer().isSingleplayerOwner(src.getPlayerOrException().getGameProfile());
                                } catch (CommandSyntaxException e) {
                                    return false;
                                }
                            }
                        })
                        .then(Commands.literal("stop").executes(ctx -> {
                            if ((session != null) && (session.state != QuiclimeSession.State.STOPPED)) {
                                session.stop();
                                Mirror.sendSuccessToSource(ctx.getSource(), Mirror.translatable("text.e4mc_minecraft.closeServer"));
                            } else {
                                Mirror.sendFailureToSource(ctx.getSource(), Mirror.translatable("text.e4mc_minecraft.serverAlreadyClosed"));
                            }
                            return 1;
                        }))
                        .then(Commands.literal("restart").executes(ctx -> {
                            if ((session != null) && (session.state != QuiclimeSession.State.STARTED)) {
                                session.stop();
                                session = new QuiclimeSession();
                                session.startAsync();
                            }
                            return 1;
                        }))
        );
    }
}
