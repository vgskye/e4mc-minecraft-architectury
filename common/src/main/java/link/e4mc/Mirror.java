package link.e4mc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

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
}
