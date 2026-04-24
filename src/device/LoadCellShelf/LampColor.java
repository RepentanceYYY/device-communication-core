package device.LoadCellShelf;

/**
 * 灯光颜色枚举
 * 基于 3-Bit RGB 控制模式: Bit0-红, Bit1-绿, Bit2-蓝
 */
public enum LampColor {
    /**
     * 熄灭 (无颜色)
     */
    BLACK("00", 0),

    /**
     * 红色 (Bit0 为 1)
     */
    RED("01", 1),

    /**
     * 绿色 (Bit1 为 1)
     */
    GREEN("02", 2),

    /**
     * 黄色 (红色 + 绿色: Bit0+Bit1)
     */
    YELLOW("03", 3),

    /**
     * 蓝色 (Bit2 为 1)
     */
    BLUE("04", 4),

    /**
     * 品红色/紫色 (红色 + 蓝色: Bit0+Bit2)
     */
    MAGENTA("05", 5),

    /**
     * 青色/蓝绿色 (绿色 + 蓝色: Bit1+Bit2)
     */
    CYAN("06", 6),

    /**
     * 白色 (红 + 绿 + 蓝: Bit0+Bit1+Bit2)
     */
    WHITE("07", 7);

    private final String hex;
    private final int value;

    /**
     * 构造函数
     *
     * @param hex   十六进制字符串表示
     * @param value 对应的十进制数值
     */
    LampColor(String hex, int value) {
        this.hex = hex;
        this.value = value;
    }

    /**
     * 获取十六进制字符串表示，如 "03"
     */
    public String getHex() {
        return hex;
    }

    /**
     * 获取字节值，用于协议指令发送
     */
    public byte getByte() {
        return (byte) value;
    }
}
