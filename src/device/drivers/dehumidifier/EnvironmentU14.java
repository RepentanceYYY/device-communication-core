package device.drivers.dehumidifier;

public class EnvironmentU14 {
    /**
     * 内部温度
     */
    private double internalTemperature;
    /**
     * 外部温度
     */
    private double externalTemperature;
    /**
     * 湿度
     */
    private double humidity;

    public double getInternalTemperature() {
        return internalTemperature;
    }

    public void setInternalTemperature(double internalTemperature) {
        this.internalTemperature = internalTemperature;
    }

    public double getExternalTemperature() {
        return externalTemperature;
    }

    public void setExternalTemperature(double externalTemperature) {
        this.externalTemperature = externalTemperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    @Override
    public String toString() {
        return "EnvironmentU14{" +
                "内部温度=" + internalTemperature + "℃" +
                ", 外部温度=" + externalTemperature + "℃" +
                ", 湿度=" + humidity + "%" +
                '}';
    }
}
