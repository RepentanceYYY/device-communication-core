package device.utils;

public class ByteUtils {
    /**
     * 截取 byte 数组
     *
     * @param array 原始数组
     * @param start 起始索引（包含）
     * @param end   结束索引（不包含）
     * @return 新的子数组
     * @throws IllegalArgumentException 如果索引非法
     */
    public static byte[] slice(byte[] array, int start, int end) {
        if (array == null) {
            throw new IllegalArgumentException("Input array cannot be null");
        }
        if (start < 0 || end > array.length || start > end) {
            throw new IllegalArgumentException(
                    "Invalid start or end index: start=" + start + ", end=" + end
            );
        }
        int length = end - start;
        byte[] result = new byte[length];
        System.arraycopy(array, start, result, 0, length);
        return result;
    }
}
