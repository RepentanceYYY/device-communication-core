package device.enums;

public enum CommMode {
    WAIT_RESPONSE,      // 模式3：发送并同步等待
    FIRE_AND_FORGET,    // 模式2：发送不等待
    CONTINUOUS,         // 模式4：发送后开启持续响应
    NONE                // 模式1：仅用于处理设备主动上报
}
