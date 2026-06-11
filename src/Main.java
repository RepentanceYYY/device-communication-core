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
        dispatcher.addDevice(device);
        device.open();
        device.setAddress(1);
        for (int i = 1; i <= 85; i++) {
            try {
                device.setTempControlStop(i);
                System.out.println("值 " + i + " 成功");
                break;
            } catch (Exception e) {
                System.out.println("值 " + i + " 失败");
                Thread.sleep(500L);
            }
        }
       device.queryRunParam(0, 15);
       device.queryRunStatus(30,15);
        device.setTempControlModeHeating(true);
        Thread.sleep(500L);
        device.queryRunStatus(30,15);
    }
}