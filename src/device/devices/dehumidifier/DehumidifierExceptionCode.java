package device.devices.dehumidifier;

/**
 * 异常代码表 (Exception Code)
 */
public enum DehumidifierExceptionCode {

    ILLEGAL_FC(1, "ILLEGAL_FC", "无效功能码"),
    ILLEGAL_REG(2, "ILLEGAL_REG", "无效寄存器"),
    ILLEGAL_DATA(3, "ILLEGAL_DATA", "无效数据，数据范围超限"),
    DEVICE_FAILURE(4, "DEVICEFAILURE", "设备操作数据失败"),
    ACKNOWLEDGE(5, "ACKNOWLEDGE", "操作进行中，没有结束"),
    DEVICE_BUSY(6, "DEVICE BUSY", "设备忙，无法操作");

    private final int code;
    private final String name;
    private final String description;

    // 构造函数
    DehumidifierExceptionCode(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    // 提供 Getter 方法
    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 查找对应的异常枚举
     */
    public static DehumidifierExceptionCode getByCode(int code) {
        for (DehumidifierExceptionCode ec : DehumidifierExceptionCode.values()) {
            if (ec.getCode() == code) {
                return ec;
            }
        }
        return null; // 或者抛出未知错误异常
    }
}
