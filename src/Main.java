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
        config.setHost("192.168.1.113");
        config.setPort(8234);

        ShelfDevice device = new ShelfDevice();

        TcpClientDispatcher dispatcher = new TcpClientDispatcher((TcpClientChannel) ChannelFactory.create(config));

        device.setCommDispatcher(dispatcher);

        dispatcher.setDeviceBase(device);

        device.setWriteIntervalTime(130L);
        try{
            device.getQuantitySync(1);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(2);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(3);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(8);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(9);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(4);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(6);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(5);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
        try{
            device.getQuantitySync(7);
        } catch (Exception e) {
            System.out.println("捕获到异常："+e.getMessage());
        }
    }
}