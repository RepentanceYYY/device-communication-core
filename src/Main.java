import device.LoadCellShelf.ShelfDevice;
import device.SmartLocker.SmartLockerDevice;
import device.channel.ChannelFactory;
import device.channel.SerialChannel;
import device.channel.TcpClientChannel;
import device.core.SerialDispatcher;
import device.core.TcpClientDispatcher;
import device.enums.ChannelType;
import device.model.ChannelConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder; // 引入 LongAdder

public class Main {
    public static void main(String[] args) throws Exception {

        ChannelConfig config = new ChannelConfig();
        config.setType(ChannelType.SERIAL);
        config.setPortName("COM4");
        config.setBaudRate(19200);

        SmartLockerDevice device = new SmartLockerDevice();
        SerialDispatcher dispatcher = new SerialDispatcher((SerialChannel) ChannelFactory.create(config));
        device.setCommDispatcher(dispatcher);
        dispatcher.setDeviceBase(device);
        device.setWriteIntervalTime(100L);
        device.open();
    }
}