import device.FingerprintDevice;
import device.LoadCellShelfDevice;
import device.RFIDCabinetDevice;
import device.channel.ChannelFactory;
import device.channel.SerialChannel;
import device.channel.TcpClientChannel;
import device.core.SerialDispatcher;
import device.core.TcpClientDispatcher;
import device.enums.ChannelType;
import device.model.ChannelConfig;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
//        FingerprintDevice fingerprintDevice = new FingerprintDevice();
//        ChannelConfig config = new ChannelConfig();
//        config.setType(ChannelType.SERIAL);
//        config.setPortName("COM4");
//        config.setBaudRate(57600);
//
//        SerialDispatcher serialDispatcher = new SerialDispatcher((SerialChannel) ChannelFactory.create(config));
//        fingerprintDevice.setCommDispatcher(serialDispatcher);
//        serialDispatcher.setDeviceBase(fingerprintDevice);
//        fingerprintDevice.readIndexTable();
//        System.out.println("主线程执行完毕");

        ChannelConfig config = new ChannelConfig();
        config.setType(ChannelType.TCP_CLIENT);
        config.setHost("192.168.1.113");
        config.setPort(8234);
        LoadCellShelfDevice loadCellShelfDevice = new LoadCellShelfDevice();
        TcpClientDispatcher tcpClientDispatcher = new TcpClientDispatcher((TcpClientChannel) ChannelFactory.create(config));
        loadCellShelfDevice.setCommDispatcher(tcpClientDispatcher);
        tcpClientDispatcher.setDeviceBase(loadCellShelfDevice);
        loadCellShelfDevice.readAddressAndModel();
    }
}