import device.RFIDCabinetDevice;
import device.channel.SerialChannel;
import device.core.SerialDispatcher;
import device.core.TcpDispatcher;
import device.HLDevice;
import device.channel.TcpChannel;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        RFIDCabinetDevice rfidCabinetDevice = new RFIDCabinetDevice();
        SerialDispatcher serialDispatcher = new SerialDispatcher(new SerialChannel("COM6",57600));
        rfidCabinetDevice.setCommDispatcher(serialDispatcher);
        serialDispatcher.setDeviceBase(rfidCabinetDevice);
        rfidCabinetDevice.open();
        rfidCabinetDevice.openLock(5,1);
        System.out.println("主线程执行完毕");
    }
}