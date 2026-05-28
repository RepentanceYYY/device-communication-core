package device.channel;

import device.model.ChannelConfig;

/**
 * 通道工厂类
 */
public class ChannelFactory {

    /**
     * 创建通道
     *
     * @param config 配置
     * @return 通道实例
     */
    public static CommChannel<?, ?> create(ChannelConfig config) {
        switch (config.getType()) {
            case SERIAL:
                return new SerialChannel(
                        config.getPortName(),
                        config.getBaudRate()
                );

            case TCP_CLIENT:
                return new TcpClientChannel(
                        config.getHost(),
                        config.getPort()
                );

            case TCP_SERVER:
                return new TcpServerChannel(
                        config.getPort()
                );

            default:
                throw new IllegalArgumentException("不支持的通道类型: " + config.getType());
        }
    }
}
