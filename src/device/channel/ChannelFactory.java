package device.channel;

import device.model.ChannelConfig;

/**
 * 通道工厂类
 */
public class ChannelFactory {
    public static CommChannel<?> create(ChannelConfig config) {
        switch (config.getType()) {
            case SERIAL:
                return new SerialChannel(
                        config.getPortName(),
                        config.getBaudRate()
                );

            case TCP:
                return new TcpClientChannel(
                        config.getHost(),
                        config.getPort()
                );
            default:
                throw new IllegalArgumentException("不支持的通道类型");
        }
    }
}
