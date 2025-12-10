package link.e4mc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class Mirror {
    private static final String[] LITERAL_CLASS_NAMES = {
            "net.minecraft.text.LiteralText", // yarn
            "net.minecraft.network.chat.TextComponent",
            "net.minecraft.class_2585",
            "net.minecraft.src.C_5025_"
    };
    private static final String[] LITERAL_METHOD_NAMES = {
            "literal",
            "method_43470",
            "m_237113_"
    };
    private static final String[] TRANSLATABLE_CLASS_NAMES = {
            "net.minecraft.text.TranslatableText", // yarn
            "net.minecraft.network.chat.TranslatableComponent",
            "net.minecraft.class_2588",
            "net.minecraft.src.C_5026_"
    };
    private static final String[] TRANSLATABLE_METHOD_NAMES = {
            "translatable",
            "method_43469",
            "m_237110_"
    };
    private static final String[] SUCCESS_METHOD_NAMES = {
            "sendFeedback", // yarn
            "sendSuccess",
            "method_9226",
            "m_288197_"
    };
    private static final String[] FAILURE_METHOD_NAMES = {
            "sendError", // yarn
            "sendFailure",
            "method_9213",
            "m_81352_"
    };
    private static final String[] WITH_STYLE_METHOD_NAMES = {
            "styled", // yarn
            "withStyle",
            "method_27694",
            "m_130938_"
    };
    private static final String[] APPEND_METHOD_NAMES = {
            "append",
            "method_10852",
            "m_7220_"
    };
    private static final String[] RUNCOMMAND_CLASS_NAMES = {
            "net.minecraft.text.ClickEvent$RunCommand", // yarn
            "net.minecraft.network.chat.ClickEvent$RunCommand",
            "net.minecraft.class_2558$class_10609"
    };
    private static final String[] COPYTOCLIPBOARD_CLASS_NAMES = {
            "net.minecraft.text.ClickEvent$CopyToClipboard", // yarn
            "net.minecraft.network.chat.ClickEvent$CopyToClipboard",
            "net.minecraft.class_2558$class_10606"
    };
    private static final String[] SHOWTEXT_CLASS_NAMES = {
            "net.minecraft.text.HoverEvent$ShowText", // yarn
            "net.minecraft.network.chat.HoverEvent$ShowText",
            "net.minecraft.class_2568$class_10613"
    };
    private static final String[] NAME_AND_ID_METHOD_NAMES = {
            "nameAndId",
            "method_72498",
            "getPlayerConfigEntry"
    };
    private static final String[] IS_SINGLEPLAYER_OWNER_METHOD_NAMES = {
            "isSingleplayerOwner",
            "method_19466",
            "m_7779_"
    };
    private static final String[] SET_USING_WHITELIST_METHOD_NAMES = {
            "setUsingWhiteList",
            "m_6628_",
            "method_14557",
            "setWhitelistEnabled",
            "setUsingWhitelist",
            "method_73589",
    };

    public static ClickEvent runCommand(String command) {
        if (ClickEvent.class.isInterface()) {
            for (String className : RUNCOMMAND_CLASS_NAMES) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor(String.class);
                    return (ClickEvent) constructor.newInstance(command);
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException | ClassCastException ignored) {}
            }
        } else {
            return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        }
        throw new RuntimeException("Could not locate any way to make a ClickEvent!");
    }

    public static ClickEvent copyToClipboard(String text) {
        if (ClickEvent.class.isInterface()) {
            for (String className : COPYTOCLIPBOARD_CLASS_NAMES) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor(String.class);
                    return (ClickEvent) constructor.newInstance(text);
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException | ClassCastException ignored) {}
            }
        } else {
            return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
        }
        throw new RuntimeException("Could not locate any way to make a ClickEvent!");
    }

    public static HoverEvent showText(Component text) {
        if (HoverEvent.class.isInterface()) {
            for (String className : SHOWTEXT_CLASS_NAMES) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor(Component.class);
                    return (HoverEvent) constructor.newInstance(text);
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException | ClassCastException ignored) {}
            }
        } else {
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
        }
        throw new RuntimeException("Could not locate any way to make a ClickEvent!");
    }

    public static Component withStyle(Component component, UnaryOperator<Style> operator) {
        Class<? extends Component> clazz = component.getClass();
        for (String methodName : WITH_STYLE_METHOD_NAMES) {
            try {
                Method method = clazz.getMethod(methodName, UnaryOperator.class);
                return (Component) method.invoke(component, operator);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {}
        }
        throw new RuntimeException("Could not locate any way to style this Component!");
    }

    public static Component append(Component component, Component other) {
        Class<? extends Component> clazz = component.getClass();
        for (String methodName : APPEND_METHOD_NAMES) {
            try {
                Method method = clazz.getMethod(methodName, Component.class);
                return (Component) method.invoke(component, other);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {}
        }
        throw new RuntimeException("Could not locate any way to append a Component to this Component!");
    }

    public static Component literal(String text) {
        // Try 1.18-and-older-style TextComponent initialization first
        for (String className : LITERAL_CLASS_NAMES) {
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> constructor = clazz.getConstructor(String.class);
                return (Component) constructor.newInstance(text);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException | ClassCastException ignored) {}
        }
        Class<Component> clazz = Component.class;
        for (String methodName : LITERAL_METHOD_NAMES) {
            try {
                Method method = clazz.getMethod(methodName, String.class);
                return (Component) method.invoke(null, text);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {}
        }
        throw new RuntimeException("Could not locate any way to make a literal Component!");
    }


    public static Component translatable(String text, Object... args) {
        // Try 1.18-and-older-style TranslatableComponent initialization first
        for (String className : TRANSLATABLE_CLASS_NAMES) {
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> constructor = clazz.getConstructor(String.class, Object[].class);
                return (Component) constructor.newInstance(text, args);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException | ClassCastException ignored) {}
        }
        Class<Component> clazz = Component.class;
        for (String methodName : TRANSLATABLE_METHOD_NAMES) {
            try {
                Method method = clazz.getMethod(methodName, String.class, Object[].class);
                return (Component) method.invoke(null, text, args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {}
        }
        throw new RuntimeException("Could not locate any way to make a literal Component!");
    }

    public static void sendSuccessToSource(CommandSourceStack source, Component message) {
        sendGenericMessageToSource(source, message, SUCCESS_METHOD_NAMES);
    }

    public static void sendFailureToSource(CommandSourceStack source, Component message) {
        sendGenericMessageToSource(source, message, FAILURE_METHOD_NAMES);
    }

    private static void sendGenericMessageToSource(CommandSourceStack source, Component message, String[] methodNames) {
        Class<CommandSourceStack> clazz = CommandSourceStack.class;
        for (String methodName : methodNames) {
            try {
                Method method = clazz.getMethod(methodName, Component.class, boolean.class);
                method.invoke(source, message, true);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            try {
                Method method = clazz.getMethod(methodName, Supplier.class, boolean.class);
                method.invoke(source, (Supplier<Component>) () -> message, true);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
    }

    public static boolean isSingleplayerOwner(MinecraftServer server, ServerPlayer player) {
        Class<ServerPlayer> clazz = ServerPlayer.class;
        Object profile = player.getGameProfile();
        for (String methodName : NAME_AND_ID_METHOD_NAMES) {
            try {
                Method method = clazz.getMethod(methodName);
                profile = method.invoke(player);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
        Class<MinecraftServer> clazz2 = MinecraftServer.class;
        for (String methodName : IS_SINGLEPLAYER_OWNER_METHOD_NAMES) {
            try {
                Method method = clazz2.getMethod(methodName, profile.getClass());
                return (boolean) method.invoke(server, profile);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
        throw new RuntimeException("Could not locate any way to call isSingleplayerOwner!");
    }

    public static boolean isSingleplayerOwnerObj(MinecraftServer server, Object maybeProfile) {
        Class<MinecraftServer> clazz2 = MinecraftServer.class;
        for (String methodName : IS_SINGLEPLAYER_OWNER_METHOD_NAMES) {
            try {
                Method method = clazz2.getMethod(methodName, maybeProfile.getClass());
                return (boolean) method.invoke(server, maybeProfile);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
        throw new RuntimeException("Could not locate any way to call isSingleplayerOwner!");
    }

    public static void setUsingWhitelist(MinecraftServer server, PlayerList playerList, boolean enabled) {
        Class<MinecraftServer> clazz = MinecraftServer.class;
        Class<PlayerList> clazz2 = PlayerList.class;
        for (String methodName : SET_USING_WHITELIST_METHOD_NAMES) {
            try {
                Method method = clazz.getMethod(methodName, boolean.class);
                method.invoke(server, enabled);
                return;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            try {
                Method method = clazz2.getMethod(methodName, boolean.class);
                method.invoke(playerList, enabled);
                return;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
        throw new RuntimeException("Could not locate any way to call setUsingWhitelist!");
    }
}
