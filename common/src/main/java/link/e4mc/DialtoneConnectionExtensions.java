package link.e4mc;

public interface DialtoneConnectionExtensions {
    byte[] e4mc$exportKeyingMaterial(byte[] label, byte[] context, int length);
}
