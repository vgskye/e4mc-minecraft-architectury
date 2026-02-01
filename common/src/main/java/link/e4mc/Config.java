package link.e4mc;

import folk.sisby.kaleido.api.ReflectiveConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;

public class Config extends ReflectiveConfig {
    public static final Config INSTANCE = Config.createToml(Agnos.configDir(), "e4mc", "e4mc", Config.class);

    @Comment("Whether to use the broker to get the best relay based on location or use a hard-coded relay.")
    public final TrackedValue<Boolean> useBroker = this.value(true);
    public final TrackedValue<String> brokerUrl = this.value("https://broker.e4mc.link/getBestRelay");

    public final TrackedValue<String> relayHost = this.value("test.e4mc.link");
    public final TrackedValue<Integer> relayPort = this.value(25575);

    @Comment("Allows use of certain dedicated server commands such as /ban and /whitelist")
    public final TrackedValue<Boolean> restoreDedicatedCommands = this.value(true);
    @Comment("Whether to use whitelists on LAN worlds")
    public final TrackedValue<Boolean> useWhiteList = this.value(false);

    @Comment("Whether to enable sharing LAN worlds with e4mc")
    public final TrackedValue<Boolean> hostEnabled = this.value(true);
    @Comment("Whether to enable Dialtone peer-to-peer connections as the host")
    public final TrackedValue<Boolean> dialtoneHostEnabled = this.value(true);
    @Comment("Whether to enable Dialtone peer-to-peer connections as the player")
    public final TrackedValue<Boolean> dialtonePlayerEnabled = this.value(true);
    @Comment("The URL to get the list of Iroh relays to use")
    public final TrackedValue<String> dialtoneRelayMap = this.value("https://natives.e4mc.link/relaymap.json");
    @Comment("Whether to hide direct IP addresses from the relay")
    public final TrackedValue<Boolean> dialtoneSanitizeTicket = this.value(true);
}