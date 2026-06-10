package device.utils;

public class ByteUtils {
    /**
     * 截取 byte 数组
     *
     * @param array 原始数组
     * @param start 起始索引
     * @param end   结束索引
     * @return 新的子数组
     * @throws IllegalArgumentException 如果索引非法
     */
    public static byte[] slice(byte[] array, int start, int end) {
        if (array == null) {
            throw new IllegalArgumentException("Input array cannot be null");
        }
        if (start < 0 || end >= array.length || start > end) {
            throw new IllegalArgumentException(
                    "Invalid start or end index: start=" + start + ", end=" + end
            );
        }
        int length = end - start + 1;
        byte[] result = new byte[length];
        System.arraycopy(array, start, result, 0, length);
        return result;
    }

    /**
     * 合并byte数组
     *
     * @param arrays
     * @return
     */
    public static byte[] merge(byte[]... arrays) {
        if (arrays == null || arrays.length == 0) {
            return new byte[0];
        }

        int totalLength = 0;
        // 计算总长度
        for (byte[] arr : arrays) {
            if (arr != null) {
                totalLength += arr.length;
            }
        }

        byte[] result = new byte[totalLength];

        int offset = 0;
        // 拷贝
        for (byte[] arr : arrays) {
            if (arr != null) {
                int len = arr.length;
                System.arraycopy(arr, 0, result, offset, len);
                offset += len;
            }
        }

        return result;
    }

    /**
     * 将 int 转换为 2 字节的 byte 数组（大端字节序：高位在前，低位在后）
     * * @param value 要转换的整数，范围应在 -32768 ~ 32767 或 0 ~ 65535 之间
     *
     * @return 长度为 2 的 byte 数组
     */
    public static byte[] intToTwoBytes(int value) {
        byte[] result = new byte[2];
        result[0] = (byte) ((value >> 8) & 0xFF); // 高 8 位
        result[1] = (byte) (value & 0xFF);        // 低 8 位
        return result;
    }
}
