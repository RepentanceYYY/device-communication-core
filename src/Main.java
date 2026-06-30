import device.core.CommDispatcher;
import device.core.CommDispatcherManager;
import device.drivers.dehumidifier.DehumidifierDevice;
import device.drivers.dehumidifier.DehumidifierRunParam;
import device.drivers.dehumidifier.DehumidifierU14Device;
import device.drivers.dehumidifier.EnvironmentU14;

public class Main {

    public static void main(String[] args) throws Exception {

        CommDispatcher dispatcher = CommDispatcherManager.create("tcp", "192.168.1.112:9902");
        // U14
        DehumidifierU14Device deviceU14 = new DehumidifierU14Device();
        deviceU14.setAddress(2);
        deviceU14.setCommDispatcher(dispatcher);
        dispatcher.addDevice(deviceU14);
        // U15
        DehumidifierDevice deviceU15 = new DehumidifierDevice(1);
        deviceU15.setCommDispatcher(dispatcher);
        dispatcher.addDevice(deviceU15);
        // 查询
        dispatcher.open();
        EnvironmentU14 environmentU14 = deviceU14.queryEnvironment();
        System.out.println(environmentU14.toString());

        DehumidifierRunParam dehumidifierRunParam = deviceU15.queryRunParam(0, 15);

    }
}