import device.LoadCellShelf.ShelfDevice;
import device.channel.ChannelFactory;
import device.channel.TcpClientChannel;
import device.core.TcpClientDispatcher;
import device.enums.ChannelType;
import device.model.ChannelConfig;

public class Main {
    public static void main(String[] args) throws Exception {

        ChannelConfig config = new ChannelConfig();
        config.setType(ChannelType.TCP_CLIENT);
        config.setHost("113.90.135.198");
        config.setPort(8235);

        ShelfDevice device = new ShelfDevice();

        TcpClientDispatcher dispatcher = new TcpClientDispatcher((TcpClientChannel) ChannelFactory.create(config));

        device.setCommDispatcher(dispatcher);

        dispatcher.setDeviceBase(device);

        device.setWriteIntervalTime(130L);
        try {
            Boolean res = device.clearQuantityIssuedSync(10);
        } catch (Exception e) {
            System.out.println("main捕获到错误消息:" + e.getMessage());
        }

        try {
            Boolean res = device.clearQuantityIssuedSync(3);
        } catch (Exception e) {
            System.out.println("main捕获到错误消息:" + e.getMessage());
        }
    }
}