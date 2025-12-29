package link.e4mc;

import java.net.InetSocketAddress;

public class SmugglersInetSocketAddress extends InetSocketAddress {
    public final String ticket;

    public SmugglersInetSocketAddress(InetSocketAddress parent, String ticket) {
        super(parent.getAddress(), parent.getPort());
        this.ticket = ticket;
    }
}
