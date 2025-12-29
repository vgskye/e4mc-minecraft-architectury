package link.e4mc;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PoisonPill {
    public static boolean checkMotw() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            var path = Agnos.jarPath();
            var motwPath = path + ":Zone.Identifier";
            try(FileInputStream inputStream = new FileInputStream(motwPath)) {
                String hidden = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                for (var line: hidden.split("\n")) {
                    if (line.startsWith("HostUrl=")) {
                        if (!(line.startsWith("HostUrl=https://beta.e4mc.link/"))) {
                            return false;
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
        return true;
    }
}
