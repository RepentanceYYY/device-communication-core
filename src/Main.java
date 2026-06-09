import device.LoadCellShelf.ShelfDevice;
import device.SmartLocker.SmartLockerDevice;
import device.channel.ChannelFactory;
import device.channel.SerialChannel;
import device.channel.TcpClientChannel;
import device.core.SerialDispatcher;
import device.core.TcpClientDispatcher;
import device.enums.ChannelType;
import device.model.ChannelConfig;
import device.utils.CheckSumUtils;

import java.sql.SQLOutput;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder; // 引入 LongAdder

public class Main {

    public static void main(String[] args) throws Exception {

    }
}