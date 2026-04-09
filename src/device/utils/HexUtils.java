package device.utils;

import java.util.HexFormat;

public class HexUtils {
    private static final HexFormat FORMATTER = HexFormat.ofDelimiter(" ").withUpperCase();

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * 将 16 进制字符串（空格分隔）转回 byte 数组
     *
     * @param hexString 例如 "AE B0 C0 7A"
     * @return byte[]
     */
    public static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return new byte[0];
        }
        try {
            return FORMATTER.parseHex(hexString);
        } catch (IllegalArgumentException e) {
            System.err.println("非法 16 进制字符串: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * byte数组转成16进制字符串
     *
     * @param bytes 输入字节数组
     * @return 16进制字符串，若输入为null则返回空字符串
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return FORMATTER.formatHex(bytes);
    }

    /**
     * 将10进制数转成字符串格式的16进制数
     * @param value
     * @return
     */
    public static String toHexByteFast(int value) {
        int v = value & 0xFF;
        char[] chars = new char[2];
        chars[0] = HEX_ARRAY[v >>> 4];
        chars[1] = HEX_ARRAY[v & 0x0F];
        return new String(chars);
    }
}
