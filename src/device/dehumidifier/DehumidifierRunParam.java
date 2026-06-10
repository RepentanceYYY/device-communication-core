package device.dehumidifier;

/**
 * 运行参数及状态实体类 (对应功能码 FC=0x03 / 0x04)
 */
public class DehumidifierRunParam {

    // ==========================================
    // 3区 只读寄存器 (Input Registers)
    // ==========================================
    private double internalTemperature;     // 内部温度值 (℃)
    private double ambientTemperature;      // 环境温度值 (℃)
    private double ambientHumidity;         // 环境湿度值 (%RH)
    private double internalTemperature2;    // 内部温度值 2 (℃)
    private double dewPointTemperature;     // 露点温度 (℃)

    private int reserved3;
    private int reserved4;
    private int reserved5;

    // ==========================================
    // 4区/3区 可读写寄存器 (Holding Registers)
    // ==========================================
    private double tempControlStart;        // 控温开启值 (℃)
    private double tempControlStop;         // 控温停止值 (℃)
    private double humidityControlStart;    // 控湿开启值 (%RH)
    private double humidityControlStop;     // 控湿停止值 (%RH)
    private double tempAlarmUpper;          // 温度报警上限值 (℃)
    private double tempAlarmLower;          // 温度报警下限值 (℃)
    private double dewPointStart;           // 露点温度启动值 (℃)
    private double dewPointHysteresis;      // 露点温度回差值 (℃)

    public double getInternalTemperature() {
        return internalTemperature;
    }

    public void setInternalTemperature(double internalTemperature) {
        this.internalTemperature = internalTemperature;
    }

    public double getAmbientTemperature() {
        return ambientTemperature;
    }

    public void setAmbientTemperature(double ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }

    public double getAmbientHumidity() {
        return ambientHumidity;
    }

    public void setAmbientHumidity(double ambientHumidity) {
        this.ambientHumidity = ambientHumidity;
    }

    public double getInternalTemperature2() {
        return internalTemperature2;
    }

    public void setInternalTemperature2(double internalTemperature2) {
        this.internalTemperature2 = internalTemperature2;
    }

    public double getDewPointTemperature() {
        return dewPointTemperature;
    }

    public void setDewPointTemperature(double dewPointTemperature) {
        this.dewPointTemperature = dewPointTemperature;
    }

    public int getReserved3() {
        return reserved3;
    }

    public void setReserved3(int reserved3) {
        this.reserved3 = reserved3;
    }

    public int getReserved4() {
        return reserved4;
    }

    public void setReserved4(int reserved4) {
        this.reserved4 = reserved4;
    }

    public int getReserved5() {
        return reserved5;
    }

    public void setReserved5(int reserved5) {
        this.reserved5 = reserved5;
    }

    public double getTempControlStart() {
        return tempControlStart;
    }

    public void setTempControlStart(double tempControlStart) {
        this.tempControlStart = tempControlStart;
    }

    public double getTempControlStop() {
        return tempControlStop;
    }

    public void setTempControlStop(double tempControlStop) {
        this.tempControlStop = tempControlStop;
    }

    public double getHumidityControlStart() {
        return humidityControlStart;
    }

    public void setHumidityControlStart(double humidityControlStart) {
        this.humidityControlStart = humidityControlStart;
    }

    public double getHumidityControlStop() {
        return humidityControlStop;
    }

    public void setHumidityControlStop(double humidityControlStop) {
        this.humidityControlStop = humidityControlStop;
    }

    public double getTempAlarmUpper() {
        return tempAlarmUpper;
    }

    public void setTempAlarmUpper(double tempAlarmUpper) {
        this.tempAlarmUpper = tempAlarmUpper;
    }

    public double getTempAlarmLower() {
        return tempAlarmLower;
    }

    public void setTempAlarmLower(double tempAlarmLower) {
        this.tempAlarmLower = tempAlarmLower;
    }

    public double getDewPointStart() {
        return dewPointStart;
    }

    public void setDewPointStart(double dewPointStart) {
        this.dewPointStart = dewPointStart;
    }

    public double getDewPointHysteresis() {
        return dewPointHysteresis;
    }

    public void setDewPointHysteresis(double dewPointHysteresis) {
        this.dewPointHysteresis = dewPointHysteresis;
    }

    @Override
    public String toString() {
        return "除湿器运行参数:{" +
                "内部温度=" + internalTemperature + " ℃" +
                ", 环境温度=" + ambientTemperature + " ℃" +
                ", 环境湿度=" + ambientHumidity + " %RH" +
                ", 内部温度2=" + internalTemperature2 + " ℃" +
                ", 露点温度=" + dewPointTemperature + " ℃" +
                ", 控温开启值=" + tempControlStart + " ℃" +
                ", 控温停止值=" + tempControlStop + " ℃" +
                ", 控湿开启值=" + humidityControlStart + " %RH" +
                ", 控湿停止值=" + humidityControlStop + " %RH" +
                ", 温度报警上限值=" + tempAlarmUpper + " ℃" +
                ", 温度报警下限值=" + tempAlarmLower + " ℃" +
                ", 露点温度启动值=" + dewPointStart + " ℃" +
                ", 露点温度回差值=" + dewPointHysteresis + " ℃" +
                '}';
    }
}