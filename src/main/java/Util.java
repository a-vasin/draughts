import java.nio.ByteBuffer;

public class Util {
    public static byte[] expand(byte[] bytes, int size) {
        if (bytes.length > size)
            System.out.println("Can't fully match");
        byte[] result = new byte[size];
        for (int i = 0; i < Math.min(size, bytes.length); ++i)
            result[i] = bytes[i];
        return result;
    }

    public static byte[] intToByte(int i) {
        return expand(ByteBuffer.allocate(4).putInt(i).array(), 4);
    }
}
