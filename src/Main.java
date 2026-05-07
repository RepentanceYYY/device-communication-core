import device.LoadCellShelf.LoadCellShelfDevice;
import device.channel.ChannelFactory;
import device.channel.TcpClientChannel;
import device.core.TcpClientDispatcher;
import device.enums.ChannelType;
import device.model.ChannelConfig;

public class Main {
    public static void main(String[] args) throws Exception {

        ChannelConfig config = new ChannelConfig();
        config.setType(ChannelType.TCP_CLIENT);
        config.setHost("116.25.92.54");
        config.setPort(8234);

        LoadCellShelfDevice device = new LoadCellShelfDevice();

        TcpClientDispatcher dispatcher = new TcpClientDispatcher((TcpClientChannel) ChannelFactory.create(config));

        device.setCommDispatcher(dispatcher);

        dispatcher.setDeviceBase(device);

        device.setWriteIntervalTime(130L);
        device.open();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000L);
            long start = System.currentTimeMillis();
            for (int j = 1; j <= 7; j++) {
                System.out.println(j+": "+device.getQuantitySync(j));
            }
            long end = System.currentTimeMillis();
            System.out.println("------------------ 总耗时：" + (end - start) + " ms");
        }
    }
}