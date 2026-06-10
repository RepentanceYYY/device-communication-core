package device.dehumidifier;

/**
 * 除湿机运行状态及告警实体类 (对应功能码 FC=0x01 读取 / FC=0x05 控制)
 */
public class DehumidifierRunStatus {

    // ==========================================
    // 1区 状态/控制寄存器 (线圈/离散输入)
    // ==========================================

    /**
     * 雾化模块状态 (GCK专用)
     * PLC地址: 10032 -> Modbus偏移量: 31 (0x001F)
     * 状态: false = 正常, true = 故障
     */
    private boolean atomizationModuleFault;

    /**
     * 雾化工作状态 (GCK专用)
     * PLC地址: 10033 -> Modbus偏移量: 32 (0x0020)
     * 状态: false = 关闭, true = 开启
     */
    private boolean atomizationWorking;

    /**
     * 故障回路状态 (仅限两路输出型号)
     * PLC地址: 10034 -> Modbus偏移量: 33 (0x0021)
     * 状态: false = 关闭, true = 开启
     */
    private boolean faultLoopOpened;

    /**
     * 控湿回路状态
     * PLC地址: 10035 -> Modbus偏移量: 34 (0x0022)
     * 状态: false = 停止, true = 开启
     */
    private boolean humidControlLoopOpened;

    /**
     * 控温回路状态
     * PLC地址: 10036 -> Modbus偏移量: 35 (0x0023)
     * 状态: false = 停止, true = 开启
     */
    private boolean tempControlLoopOpened;

    /**
     * 控温方式
     * PLC地址: 10037 -> Modbus偏移量: 36 (0x0024)
     * 状态: false = 降温, true = 升温
     * 控制(FC=0x05): false -> 0x0000 (降温), true -> 0xFF00 (升温)
     */
    private boolean tempControlModeHeating;

    /**
     * 风机模块回路状态
     * PLC地址: 10038 -> Modbus偏移量: 37 (0x0025)
     * 状态: false = 正常, true = 故障
     */
    private boolean fanModuleFault;

    /**
     * 除湿模块回路状态
     * PLC地址: 10039 -> Modbus偏移量: 38 (0x0026)
     * 状态: false = 正常, true = 故障
     */
    private boolean dehumidifyModuleFault;

    /**
     * 湿度传感器状态
     * PLC地址: 10040 -> Modbus偏移量: 39 (0x0027)
     * 状态: false = 正常, true = 故障
     */
    private boolean humiditySensorFault;

    /**
     * 外部温度传感器状态
     * PLC地址: 10041 -> Modbus偏移量: 40 (0x0028)
     * 状态: false = 正常, true = 故障
     */
    private boolean externalTempSensorFault;

    /**
     * 内部温度传感器状态
     * PLC地址: 10042 -> Modbus偏移量: 41 (0x0029)
     * 状态: false = 正常, true = 故障
     */
    private boolean internalTempSensorFault;

    /**
     * 化霜状态
     * PLC地址: 10043 -> Modbus偏移量: 42 (0x002A)
     * 状态: false = 正常, true = 正在化霜
     */
    private boolean defrosting;

    /**
     * 控湿手动开关
     * PLC地址: 10044 -> Modbus偏移量: 43 (0x002B)
     * 状态: false = 正常/关闭, true = 手动开/开启
     * 控制(FC=0x05): false -> 0x0000, true -> 0xFF00
     */
    private boolean humidManualSwitchOn;

    /**
     * 控温手动开关
     * PLC地址: 10045 -> Modbus偏移量: 44 (0x002C)
     * 状态: false = 正常/关闭, true = 手动开/开启
     * 控制(FC=0x05): false -> 0x0000, true -> 0xFF00
     */
    private boolean tempManualSwitchOn;

    /**
     * 高温告警
     * PLC地址: 10046 -> Modbus偏移量: 45 (0x002D)
     * 状态: false = 正常, true = 告警
     */
    private boolean highTempAlarm;

    /**
     * 露点温度回路工作状态 (部分型号支持)
     * PLC地址: 10047 -> Modbus偏移量: 46 (0x002E)
     * 状态: false = 关闭, true = 开启
     */
    private boolean dewPointLoopOpened;


    // ==========================================
    // 标准的 Getter 和 Setter
    // ==========================================

    public boolean isAtomizationModuleFault() {
        return atomizationModuleFault;
    }

    public void setAtomizationModuleFault(boolean atomizationModuleFault) {
        this.atomizationModuleFault = atomizationModuleFault;
    }

    public boolean isAtomizationWorking() {
        return atomizationWorking;
    }

    public void setAtomizationWorking(boolean atomizationWorking) {
        this.atomizationWorking = atomizationWorking;
    }

    public boolean isFaultLoopOpened() {
        return faultLoopOpened;
    }

    public void setFaultLoopOpened(boolean faultLoopOpened) {
        this.faultLoopOpened = faultLoopOpened;
    }

    public boolean isHumidControlLoopOpened() {
        return humidControlLoopOpened;
    }

    public void setHumidControlLoopOpened(boolean humidControlLoopOpened) {
        this.humidControlLoopOpened = humidControlLoopOpened;
    }

    public boolean isTempControlLoopOpened() {
        return tempControlLoopOpened;
    }

    public void setTempControlLoopOpened(boolean tempControlLoopOpened) {
        this.tempControlLoopOpened = tempControlLoopOpened;
    }

    public boolean isTempControlModeHeating() {
        return tempControlModeHeating;
    }

    public void setTempControlModeHeating(boolean tempControlModeHeating) {
        this.tempControlModeHeating = tempControlModeHeating;
    }

    public boolean isFanModuleFault() {
        return fanModuleFault;
    }

    public void setFanModuleFault(boolean fanModuleFault) {
        this.fanModuleFault = fanModuleFault;
    }

    public boolean isDehumidifyModuleFault() {
        return dehumidifyModuleFault;
    }

    public void setDehumidifyModuleFault(boolean dehumidifyModuleFault) {
        this.dehumidifyModuleFault = dehumidifyModuleFault;
    }

    public boolean isHumiditySensorFault() {
        return humiditySensorFault;
    }

    public void setHumiditySensorFault(boolean humiditySensorFault) {
        this.humiditySensorFault = humiditySensorFault;
    }

    public boolean isExternalTempSensorFault() {
        return externalTempSensorFault;
    }

    public void setExternalTempSensorFault(boolean externalTempSensorFault) {
        this.externalTempSensorFault = externalTempSensorFault;
    }

    public boolean isInternalTempSensorFault() {
        return internalTempSensorFault;
    }

    public void setInternalTempSensorFault(boolean internalTempSensorFault) {
        this.internalTempSensorFault = internalTempSensorFault;
    }

    public boolean isDefrosting() {
        return defrosting;
    }

    public void setDefrosting(boolean defrosting) {
        this.defrosting = defrosting;
    }

    public boolean isHumidManualSwitchOn() {
        return humidManualSwitchOn;
    }

    public void setHumidManualSwitchOn(boolean humidManualSwitchOn) {
        this.humidManualSwitchOn = humidManualSwitchOn;
    }

    public boolean isTempManualSwitchOn() {
        return tempManualSwitchOn;
    }

    public void setTempManualSwitchOn(boolean tempManualSwitchOn) {
        this.tempManualSwitchOn = tempManualSwitchOn;
    }

    public boolean isHighTempAlarm() {
        return highTempAlarm;
    }

    public void setHighTempAlarm(boolean highTempAlarm) {
        this.highTempAlarm = highTempAlarm;
    }

    public boolean isDewPointLoopOpened() {
        return dewPointLoopOpened;
    }

    public void setDewPointLoopOpened(boolean dewPointLoopOpened) {
        this.dewPointLoopOpened = dewPointLoopOpened;
    }

    @Override
    public String toString() {
        return "除湿器运行状态{" +
                "雾化模块故障=" + atomizationModuleFault +
                ", 雾化工作状态=" + (atomizationWorking ? "开启" : "关闭") +
                ", 故障回路状态=" + (faultLoopOpened ? "开启" : "关闭") +
                ", 控湿回路状态=" + (humidControlLoopOpened ? "开启" : "停止") +
                ", 控温回路状态=" + (tempControlLoopOpened ? "开启" : "停止") +
                ", 控温方式=" + (tempControlModeHeating ? "升温" : "降温") +
                ", 风机模块=" + (fanModuleFault ? "故障" : "正常") +
                ", 除湿模块=" + (dehumidifyModuleFault ? "故障" : "正常") +
                ", 湿度传感器=" + (humiditySensorFault ? "故障" : "正常") +
                ", 外部温度传感器=" + (externalTempSensorFault ? "故障" : "正常") +
                ", 内部温度传感器=" + (internalTempSensorFault ? "故障" : "正常") +
                ", 化霜状态=" + (defrosting ? "正在化霜" : "正常") +
                ", 控湿手动开关=" + (humidManualSwitchOn ? "手动开" : "正常/自动") +
                ", 控温手动开关=" + (tempManualSwitchOn ? "手动开" : "正常/自动") +
                ", 高温告警=" + highTempAlarm +
                ", 露点温度回路=" + (dewPointLoopOpened ? "开启" : "关闭") +
                '}';
    }
}