import device.channel.SerialChannel;
import device.channel.TcpClientChannel;
import device.core.SerialDispatcher;
import device.core.TcpClientDispatcher;
import device.dehumidifier.DehumidifierDevice;
import device.dehumidifier.DehumidifierRunParam;
import device.smartLocker.SmartLockerDevice;

public class Main {

    public static void main(String[] args) throws Exception {

        TcpClientChannel channel = new TcpClientChannel("192.168.1.112", 9902);
        TcpClientDispatcher dispatcher = new TcpClientDispatcher(channel);
        DehumidifierDevice device = new DehumidifierDevice();
        device.setCommDispatcher(dispatcher);
        dispatcher.setDeviceBase(device);
        device.open();
        device.setTempControlModeHeating(false);
        device.queryRunStatus(31, 15);
        Thread.sleep(1000L);
        device.setTempManualSwitchOn(true);
        Thread.sleep(1000L);
        device.queryRunStatus(31, 15);

    }
}