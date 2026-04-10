package device.enums;

public enum CommMode {
    /**
     * 发送并同步等待
     */
    WAIT_RESPONSE,
    /**
     * 发送不等待
     */
    FIRE_AND_FORGET,
    /**
     * 发送后开启持续响应
     */
    CONTINUOUS,
    /**
     * 仅用于处理设备主动上报
     */
    NONE
}
