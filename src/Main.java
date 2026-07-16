import device.core.CommDispatcher;
import device.core.CommDispatcherManager;
import device.drivers.LoadCellShelf.ShelfDevice;
import device.drivers.fingerprint.FingerprintScanner;

public class Main {

    public static void main(String[] args) throws Exception {

        CommDispatcher dispatcher = CommDispatcherManager.create("serial", "COM3@57600");
        FingerprintScanner device = new FingerprintScanner();
        device.setCommDispatcher(dispatcher);
        dispatcher.addDevice(device);

        device.ps_autoIdentify();
    }
}