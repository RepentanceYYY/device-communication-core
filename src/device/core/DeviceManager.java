package device.core;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DeviceManager<T extends DeviceCore> {

    /**
     * 设备缓存
     */
    protected final Map<String, T> deviceMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建设备
     *
     * @return 设备实例
     */
    public abstract T getOrCreate();

    /**
     * 根据 Key 移除设备
     *
     * @param key 设备唯一标识
     */
    public void remove(String key) {
        remove(deviceMap.get(key));
    }

    /**
     * 移除指定设备
     *
     * @param device 设备实例
     */
    public void remove(T device) {
        if (device == null) {
            return;
        }

        // 从设备管理器中移除
        deviceMap.values().remove(device);

        // 从通信调度器中移除
        CommDispatcher commDispatcher = device.getCommDispatcher();
        if (commDispatcher != null) {
            commDispatcher.removeDevice(device);

            // 当前通信调度器已无设备，释放资源
            if (commDispatcher.isEmpty()) {
                CommDispatcherManager.remove(commDispatcher);
            }
        }
    }

    /**
     * 移除所有设备
     */
    public void removeAll() {
        for (T device : new ArrayList<>(deviceMap.values())) {
            remove(device);
        }
    }
}