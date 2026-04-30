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
        config.setHost("192.168.1.113");
        config.setPort(8234);

        LoadCellShelfDevice device = new LoadCellShelfDevice();

        TcpClientDispatcher dispatcher = new TcpClientDispatcher((TcpClientChannel) ChannelFactory.create(config));

        device.setCommDispatcher(dispatcher);

        dispatcher.setDeviceBase(device);

        device.setWriteIntervalTime(250L);
        device.open();
        for (int i = 0; i < 10; i++) {
            try {
                long start = System.currentTimeMillis();
                System.out.println("0x01现存数量：" + device.getQuantitySync(1));
                System.out.println("0x02现存数量：" + device.getQuantitySync(2));

                long end = System.currentTimeMillis();
                System.out.println("------------------ 总耗时：" + (end - start) + " ms");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("结束循环");
    }
}