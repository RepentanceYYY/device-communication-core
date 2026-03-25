package device.utils;

import java.util.HexFormat;

/**
 * 工业通信校验工具类
 */
public final class CheckSumUtils {

    private static final HexFormat HEX_FORMAT = HexFormat.ofDelimiter(" ").withUpperCase();

    /**
     * 计算 Modbus CRC16
     * 遵循低位在前 (Little-endian) 的标准输出
     *
     * @param data 输入字节数组
     * @return 16进制字符串，例如 "4B 37"
     */
    public static String getModbusCRC16(byte[] data) {
        int crc = 0xFFFF; // 初始值

        for (byte b : data) {
            crc ^= (b & 0xFF); // 与当前字节异或
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    // 如果最低位为1，右移并与多项式0xA001异或
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    // 如果最低位为0，直接右移
                    crc >>= 1;
                }
            }
        }

        // Modbus 标准通常要求低字节在前，高字节在后
        byte low = (byte) (crc & 0xFF);
        byte high = (byte) ((crc >> 8) & 0xFF);

        return HEX_FORMAT.formatHex(new byte[]{low, high});
    }

    /**
     * LRC 校验 (用于 Modbus ASCII)
     * 原理：所有字节累加求和，取反加一
     */
    public static String getLRC(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += (b & 0xFF);
        }
        // 取反加一 (补码)
        byte lrc = (byte) (((sum & 0xFF) ^ 0xFF) + 1);
        return HEX_FORMAT.formatHex(new byte[]{lrc});
    }
}
