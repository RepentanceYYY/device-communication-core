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
}
