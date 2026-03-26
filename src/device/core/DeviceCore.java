package device.core;

import device.enums.DispatchMode;
import device.utils.HexUtils;

import java.io.IOException;
import java.util.function.BiConsumer;

public class DeviceCore {
    public static DeviceCore instance = new DeviceCore();

    /**
     * 通信调度器
     */
    private CommDispatcher commDispatcher;

    public void setCommDispatcher(CommDispatcher commDispatcher) {
        this.commDispatcher = commDispatcher;
        if (this.commDispatcher != null) {
            this.commDispatcher.onAllTasksCompleted = this::onAllTasksCompleted;
        }
    }

    public void setTimeout(int timeout) {
        commDispatcher.responseTimeout = Math.max(timeout, commDispatcher.responseTimeout);
    }

    /**
     * 打开链接
     *
     * @throws IOException
     */
    public void open() throws IOException {
        this.commDispatcher.open();
    }

    /**
     * 关闭链接
     *
     * @throws IOException
     */
    public void close() throws IOException {
        this.commDispatcher.close();
    }

    /**
     * 写入数据
     *
     * @param dispatchMode
     * @param writeBytes
     * @param priority
     * @param retryCount
     * @param dataReceived
     */
    public void write(DispatchMode dispatchMode, byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(dispatchMode, writeBytes, priority, retryCount, dataReceived);
    }

    /**
     * 基础校验
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
     * 强校验
     * @param writeBytes 写入
     * @param readBytes 读取
     * @return
     */
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        if (readBytes == null || readBytes.length < 1) return false;
        return validate(readBytes);
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
    public void onAllTasksCompleted() {
        System.out.println("当前所有队列执行完毕");
    }
}
