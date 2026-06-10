package device.dehumidifier;

/**
 * 控温方式枚举 (对应寄存器 10037)
 */
public enum TempControlMode {

    /**
     * 降温模式: 状态值为 false，控制值为 0x0000
     */
    COOLING(false, 0x0000, "降温"),

    /**
     * 升温模式: 状态值为 true，控制值为 0xFF00
     */
    HEATING(true, 0xFF00, "升温");

    private final boolean statusValue; // 读取回来的布尔状态
    private final int controlValue;    // 控制时需要写入的 16 位整型值
    private final String description;   // 中文描述

    TempControlMode(boolean statusValue, int controlValue, String description) {
        this.statusValue = statusValue;
        this.controlValue = controlValue;
        this.description = description;
    }

    public boolean isStatusValue() {
        return statusValue;
    }

    public int getControlValue() {
        return controlValue;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据读取到的布尔值，安全匹配对应的枚举
     */
    public static TempControlMode fromStatus(boolean status) {
        for (TempControlMode mode : values()) {
            if (mode.statusValue == status) {
                return mode;
            }
        }
        return COOLING; // 默认防空
    }
}