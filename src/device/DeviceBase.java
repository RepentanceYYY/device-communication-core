package device;

import java.io.IOException;
import java.util.function.BiConsumer;

public class DeviceBase {
    public static DeviceBase instance = new DeviceBase();

    /**
     * 设备链接
     */
    private ConnectBase connect;

    public void setConnect(ConnectBase connect) {
        this.connect = connect;
        this.connect.onActionEndEvent = this::actionEndEvent;
    }

    public void setTimeout(int timeout) {
        connect.timeOut = Math.max(timeout, connect.timeOut);
    }

    public void open() throws IOException {
        this.connect.open();
    }

    public void close() throws IOException {
        this.connect.close();
    }

    public void writeByPriorityQueue(byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.connect.write(writeBytes, priority, retryCount, dataReceived);
    }

    /**
     * 验证数据是否有效
     *
     * @param readBytes
     * @return
     */
    public boolean validate(byte[] readBytes) {
        if (readBytes == null || readBytes.length < 1) {
            return false;
        }
        return true;
    }

    /**
     * 接收到设备发过来的数据时调用此方法
     *
     * @param readBytes  读取到的设备数据
     * @param writeBytes 写入到设备的数据
     */
    public void receive(byte[] readBytes, byte[] writeBytes[]) {

    }

    /**
     * 队列执行完毕后执行
     */
    public void actionEndEvent() {

    }

}
