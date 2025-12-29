package link.e4mc.dialtone;

import java.net.SocketAddress;
import java.util.Objects;

public class DialtoneAddress extends SocketAddress {
    public final String actualAddress;

    public DialtoneAddress(String ticket) {
        actualAddress = ticket;
    }

    @Override
    public String toString() {
        return actualAddress;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        DialtoneAddress that = (DialtoneAddress) object;
        return Objects.equals(actualAddress, that.actualAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(actualAddress);
    }
}
