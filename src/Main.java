import device.LoadCellShelf.LoadCellShelfDevice;
import device.channel.ChannelFactory;
import device.channel.TcpClientChannel;
import device.core.TcpClientDispatcher;
import device.enums.ChannelType;
import device.model.ChannelConfig;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {

        ChannelConfig config = new ChannelConfig();
        config.setType(ChannelType.TCP_CLIENT);
        config.setHost("192.168.1.113");
        config.setPort(8234);

        LoadCellShelfDevice device = new LoadCellShelfDevice();

        TcpClientDispatcher dispatcher = new TcpClientDispatcher((TcpClientChannel) ChannelFactory.create(config));

        device.setCommDispatcher(dispatcher);

        dispatcher.setDeviceBase(device);

        device.setWriteIntervalTime(50L);
        device.open();
        System.out.println(device.setScreenDisplayTestSync("01","0","无题编号"));
    }
}