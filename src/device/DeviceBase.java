package device;

import device.utils.HexUtils;

import java.io.IOException;
import java.util.HexFormat;
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

    public void write(QueueStrategy queueStrategy, byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.connect.write(queueStrategy, writeBytes, priority, retryCount, dataReceived);
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
    public void receive(byte[] readBytes, byte[] writeBytes) {
        System.out.println(HexUtils.bytesToHexString(writeBytes));
        System.out.println(HexUtils.bytesToHexString(readBytes));
    }

    /**
     * 队列执行完毕后执行
     */
    public void actionEndEvent() {
        System.out.println("当前所有队列执行完毕");
    }

}
