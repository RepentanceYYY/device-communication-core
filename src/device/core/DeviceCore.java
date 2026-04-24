package device.core;

import device.enums.DispatchMode;
import device.utils.HexUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;

public class DeviceCore {
    public static DeviceCore instance = new DeviceCore();

    /**
     * 通信调度器
     */
    private CommDispatcher commDispatcher;
    /**
     * 写入间隔时间
     */
    private volatile long writeIntervalTime = 0L;

    public long getWriteIntervalTime() {
        return writeIntervalTime;
    }

    public void setWriteIntervalTime(long writeIntervalTime) {
        this.writeIntervalTime = writeIntervalTime;
    }

    public void setCommDispatcher(CommDispatcher commDispatcher) {
        this.commDispatcher = commDispatcher;
        if (this.commDispatcher != null) {
            this.commDispatcher.onAllTasksCompleted = this::onAllTasksCompleted;
        }
    }

    public Charset getCharset() {
        return this.commDispatcher.getCharset();
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
     * @param dispatchMode 调度策略
     * @param writeBytes   写入数据
     * @param priority     优先级
     * @param retryCount   重试次数
     * @param dataReceived 接收到响应的回调
     */
    public void write(DispatchMode dispatchMode, byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(dispatchMode, writeBytes, priority, retryCount, dataReceived);
    }

    /**
     * 写入数据
     * 优先级为10，重试0次
     *
     * @param writeBytes
     * @param dataReceived 接收到响应的回调
     */
    public void write(byte[] writeBytes, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, dataReceived);
    }

    /**
     * 写入数据
     * 优先级为10，重试0次，无回调
     *
     * @param writeBytes
     */
    public void write(byte[] writeBytes) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, this::callback);
    }

    /**
     * 写入数据
     * 优先级为10，重试0次，无回调
     *
     * @param writeBytes
     * @param timeout
     */
    public void write(byte[] writeBytes, long timeout) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, timeout, this::callback);
    }

    /**
     * 写入数据
     * 优先级为10，重试0次，无回调
     *
     * @param writeBytes
     * @param timeout
     * @param dataReceived 回调
     */
    public void write(byte[] writeBytes, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, timeout, dataReceived);
    }

    /**
     * 写入数据
     *
     * @param writeASCII
     */
    public void write(String writeASCII) {
        System.out.println("写入ASCII:" + writeASCII);
        byte[] bytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.write(bytes);
    }

    public void write(String writeASCII, long timeout) {
        byte[] bytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.write(bytes, timeout);
    }

    public void write(String writeASCII, int retryCount, long timeout) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, retryCount, timeout, this::callback);
    }

    public void write(String writeASCII, long timeout,BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, timeout, dataReceived);
    }

    public void write(String writeASCII, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, dataReceived);
    }

    public void write(String writeASCII, int retryCount, long timeout,BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, retryCount, timeout, dataReceived);
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
     *
     * @param writeBytes 写入
     * @param readBytes  读取
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
        System.out.println("被标记为主动上报的帧:" + HexUtils.bytesToHexString(readBytes));
    }

    /**
     * 可作为默认回调
     *
     * @param readBytes
     * @param writeBytes
     */
    protected void callback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("进入默认回调:");
        System.out.println("发送:" + HexUtils.bytesToHexString(writeBytes));
        System.out.println("接收:" + HexUtils.bytesToHexString(readBytes));
    }

    /**
     * 队列执行完毕后执行
     */
    public void onAllTasksCompleted() {
        System.out.println("当前所有队列执行完毕");
    }
}
