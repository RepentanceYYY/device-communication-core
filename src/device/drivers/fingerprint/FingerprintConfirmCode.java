package device.drivers.fingerprint;

/**
 * 指纹模块确认码
 */
public enum FingerprintConfirmCode {


    OK(0x00, "指令执行完毕或OK"),

    PACKET_ERROR(0x01, "数据包接收错误"),

    NO_FINGER(0x02, "传感器上没有手指"),

    IMAGE_FAIL(0x03, "录入指纹图像失败"),

    IMAGE_TOO_DRY(0x04, "指纹图像太干、太淡而不能生成特征"),

    IMAGE_TOO_WET(0x05, "指纹图像太湿、太糊而不能生成特征"),

    IMAGE_TOO_MESSY(0x06, "指纹图像太乱而不能生成特征"),

    FEATURE_TOO_FEW(0x07, "指纹正常，但特征点太少或面积太小"),

    NOT_MATCH(0x08, "指纹不匹配"),

    NOT_FOUND(0x09, "没搜索到指纹"),

    MERGE_FAIL(0x0A, "特征合并失败"),

    DB_INDEX_OUT(0x0B, "访问指纹库时地址序号超出范围"),

    READ_TEMPLATE_FAIL(0x0C, "读取模板错误或无效"),

    UPLOAD_FEATURE_FAIL(0x0D, "上传特征失败"),

    RECEIVE_NEXT_PACKET_FAIL(0x0E, "模块不能接收后续数据包"),

    UPLOAD_IMAGE_FAIL(0x0F, "上传图像失败"),

    DELETE_TEMPLATE_FAIL(0x10, "删除模板失败"),

    CLEAR_DB_FAIL(0x11, "清空指纹库失败"),

    LOW_POWER_FAIL(0x12, "不能进入低功耗状态"),

    PASSWORD_ERROR(0x13, "口令不正确"),

    RESET_FAIL(0x14, "系统复位失败"),

    NO_VALID_IMAGE(0x15, "缓冲区内没有有效原始图"),

    OTA_FAIL(0x16, "在线升级失败"),

    FINGER_NOT_MOVED(0x17, "残留指纹或两次采集之间手指没有移动"),

    FLASH_ERROR(0x18, "读写FLASH出错"),

    RANDOM_FAIL(0x19, "随机数生成失败"),

    INVALID_REGISTER(0x1A, "无效寄存器号"),

    REGISTER_VALUE_ERROR(0x1B, "寄存器设定内容错误"),

    NOTEBOOK_PAGE_ERROR(0x1C, "记事本页码指定错误"),

    PORT_OPERATION_FAIL(0x1D, "端口操作失败"),

    AUTO_ENROLL_FAIL(0x1E, "自动注册失败"),

    DATABASE_FULL(0x1F, "指纹库满"),

    DEVICE_ADDRESS_ERROR(0x20, "设备地址错误"),

    PASSWORD_WRONG(0x21, "密码有误"),

    TEMPLATE_NOT_EMPTY(0x22, "指纹模板非空"),

    TEMPLATE_EMPTY(0x23, "指纹模板为空"),

    DATABASE_EMPTY(0x24, "指纹库为空"),

    ENROLL_TIMES_ERROR(0x25, "录入次数设置错误"),

    TIMEOUT(0x26, "超时"),

    FINGER_ALREADY_EXISTS(0x27, "指纹已存在"),

    TEMPLATE_ASSOCIATED(0x28, "指纹模板有关联"),

    SENSOR_INIT_FAIL(0x29, "传感器初始化失败"),


    /**
     * 特殊状态
     */
    HAVE_NEXT_PACKET(0xF0, "有后续数据包，正确接收"),

    COMMAND_NEXT_PACKET(0xF1, "有后续数据包的命令包应答"),

    FLASH_CHECKSUM_ERROR(0xF2, "烧写FLASH校验和错误"),

    FLASH_PACKET_ID_ERROR(0xF3, "烧写FLASH包标识错误"),

    FLASH_LENGTH_ERROR(0xF4, "烧写FLASH包长度错误"),

    FLASH_TOO_LONG(0xF5, "烧写FLASH代码长度太长"),

    FLASH_WRITE_FAIL(0xF6, "烧写FLASH失败"),


    RESERVED(-1, "保留");


    private final int code;

    private final String message;


    FingerprintConfirmCode(
            int code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }


    public int getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }


    /**
     * 根据确认码获取枚举
     */
    public static FingerprintConfirmCode fromCode(int code) {

        for (FingerprintConfirmCode item : values()) {

            if (item.code == code) {
                return item;
            }
        }

        return RESERVED;
    }
}
