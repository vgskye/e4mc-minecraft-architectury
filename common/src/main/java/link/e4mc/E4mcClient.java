package link.e4mc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.commands.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class E4mcClient {
    public static final String MOD_ID = "e4mc_minecraft";
    public static QuiclimeSession session;
    public static final Logger LOGGER = LoggerFactory.getLogger(E4mcClient.MOD_ID);

    public static boolean badurl = false;

    public static void init() {
        Config.INSTANCE.id(); // Touch to initialize for McQoy
        try {
            if (!PoisonPill.checkMotw()) {
                badurl = true;
                LOGGER.warn("MotW lists unknown source! Poison pill active!");
            }
        } catch (Exception e) {
            LOGGER.warn("MotW check failed!", e);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (Config.INSTANCE.restoreDedicatedCommands.value() && Agnos.isClient()) {
            BanListCommands.register(dispatcher);
            BanPlayerCommands.register(dispatcher);
            PardonCommand.register(dispatcher);
            WhitelistCommand.register(dispatcher);
        }
        dispatcher.register(
                Commands.literal("e4mc")
                        .requires(src -> {
                            if (src.getServer() == null) {
                                return false;
                            }
                            if (src.getServer().isDedicatedServer()) {
                                return src.hasPermission(4);
                            } else {
                                try {
                                    return Mirror.isSingleplayerOwner(src.getServer(), src.getPlayerOrException());
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
                        .then(Commands.literal("doctor").executes(ctx -> {
                            var thread = new Thread(() -> {
                                LOGGER.info("generating e4mc doctor report");
                                Mirror.sendSuccessToSource(ctx.getSource(), Mirror.translatable("text.e4mc_minecraft.doctor.start"));
                                var diag = Doctor.doctor();
                                LOGGER.info("e4mc doctor report:\n{}", diag);
                                Mirror.sendSuccessToSource(ctx.getSource(), Mirror.literal(diag));
                            }, "e4mc_minecraft-doctor");
                            thread.setDaemon(true);
                            thread.start();
                            return 1;
                        }))
                        .then(Commands.literal("restart").executes(ctx -> {
                            if ((session != null) && (session.state != QuiclimeSession.State.STARTED)) {
                                session.stop();
                                session = new QuiclimeSession(session.handler);
                                session.startAsync();
                            }
                            return 1;
                        }))
        );
    }
}
