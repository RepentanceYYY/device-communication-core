import device.core.CommDispatcher;
import device.core.CommDispatcherManager;
import device.devices.dehumidifier.DehumidifierDevice;

public class Main {

    public static void main(String[] args) throws Exception {

        CommDispatcher dispatcher = CommDispatcherManager.create("serial", "192.168.1.1129902");

        DehumidifierDevice device = new DehumidifierDevice(1);
        device.setCommDispatcher(dispatcher);
        dispatcher.addDevice(device);
        device.open();

        device.setAddress(1);

        device.queryRunParam(0, 15);
        device.queryRunStatus(30, 15);
    }
}