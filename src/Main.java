import device.FingerprintDevice;
import device.RFIDCabinetDevice;
import device.channel.ChannelFactory;
import device.channel.SerialChannel;
import device.core.SerialDispatcher;
import device.enums.ChannelType;
import device.model.ChannelConfig;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        FingerprintDevice fingerprintDevice = new FingerprintDevice();
        ChannelConfig config = new ChannelConfig();
        config.setType(ChannelType.SERIAL);
        config.setPortName("COM4");
        config.setBaudRate(57600);

        SerialDispatcher serialDispatcher = new SerialDispatcher((SerialChannel) ChannelFactory.create(config));
        fingerprintDevice.setCommDispatcher(serialDispatcher);
        serialDispatcher.setDeviceBase(fingerprintDevice);
        fingerprintDevice.readIndexTable();
        System.out.println("主线程执行完毕");
    }
}