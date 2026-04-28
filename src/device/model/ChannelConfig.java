package device.model;

import device.enums.ChannelType;

/**
 * 通道配置类
 */
public class ChannelConfig {
    private ChannelType type;

    /**
     * 串口号
     */
    private String portName;
    /**
     * 波特率
     */
    private int baudRate;

    // TCP参数
    private String host;
    /**
     * 端口号
     */
    private int port;

    // UDP参数
    private int localPort;

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }
}
