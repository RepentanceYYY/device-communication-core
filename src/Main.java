import device.core.CommDispatcher;
import device.core.CommDispatcherManager;
import device.drivers.LoadCellShelf.ShelfDevice;

public class Main {

    public static void main(String[] args) throws Exception {

        CommDispatcher dispatcher = CommDispatcherManager.create("tcp", "192.168.1.114:8234");
        ShelfDevice device = new ShelfDevice();
        device.setCommDispatcher(dispatcher);
        dispatcher.addDevice(device);
        long start = System.currentTimeMillis();
        for (int i = 0; i <= 100; i++) {
            Integer quantitySync = device.getQuantitySync(1);
            System.out.println("数量为:" + quantitySync);
        }
        System.out.println("结束:" + (System.currentTimeMillis() - start));
    }
}