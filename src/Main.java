import device.RFIDCabinetDevice;
import device.channel.SerialChannel;
import device.core.SerialDispatcher;
import device.core.TcpDispatcher;
import device.HLDevice;
import device.channel.TcpChannel;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        RFIDCabinetDevice rfidCabinetDevice = new RFIDCabinetDevice();
        SerialDispatcher serialDispatcher = new SerialDispatcher(new SerialChannel("COM7", 57600));
        rfidCabinetDevice.setCommDispatcher(serialDispatcher);
        serialDispatcher.setDeviceBase(rfidCabinetDevice);
        rfidCabinetDevice.open();
        List<String> epcListSync = rfidCabinetDevice.getEpcListSync(5);
        System.out.println("主线程执行完毕");
    }
}