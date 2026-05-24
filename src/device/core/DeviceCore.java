package device.core;

import device.enums.DispatchMode;
import device.utils.HexUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class DeviceCore {
    public static DeviceCore instance = new DeviceCore();
    /**
     * 接收缓冲区
     */
    protected final java.io.ByteArrayOutputStream receiveBuffer = new java.io.ByteArrayOutputStream();

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

    public String getName() {
        return this.commDispatcher.getName();
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

    public void write(byte[] writeBytes, int retryCount, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, retryCount, timeout, dataReceived);
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

    public void write(String writeASCII, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, timeout, dataReceived);
    }

    public void write(String writeASCII, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, dataReceived);
    }

    public void write(String writeASCII, int retryCount, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, retryCount, timeout, dataReceived);
    }

    /**
     * 写入数据同步获取结果
     *
     * @param frameBytes 待发送的byte数组
     * @param retryCount 重试此时
     * @param timeout    单次响应超时时间
     * @param parser     结果解析器
     * @param <T>        返回的结果类型
     * @return
     * @throws Exception
     */
    public <T> T writeSync(byte[] frameBytes, int retryCount, long timeout, BiFunction<byte[], byte[], T> parser) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        this.write(frameBytes, retryCount, timeout, (readBytes, writeBytes) -> {
            if (readBytes == null) {
                future.completeExceptionally(new java.io.IOException("设备响应超时，重试耗尽"));
            } else {
                try {
                    T result = parser.apply(readBytes, writeBytes);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future.get();
    }

    /**
     * 写入 ASCII 数据同步获取结果
     *
     * @param frameASCII 待发送的 ASCII 字符串
     * @param retryCount 重试次数
     * @param timeout    单次响应超时时间
     * @param parser     结果解析器
     * @param <T>        返回的结果类型
     * @return 解析后的业务对象
     * @throws Exception 通信超时或解析异常
     */
    public <T> T writeSync(String frameASCII, int retryCount, long timeout, BiFunction<byte[], byte[], T> parser) throws Exception {

        CompletableFuture<T> future = new CompletableFuture<>();

        this.write(frameASCII, retryCount, timeout, (readBytes, writeBytes) -> {
            if (readBytes == null) {
                future.completeExceptionally(new java.io.IOException("设备响应超时，重试耗尽"));
            } else {
                try {
                    T result = parser.apply(readBytes, writeBytes);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future.get();
    }


    /**
     * 基础校验
     *
     * @param readBytes
     * @return
     */
    public boolean validate(byte[] readBytes) {
        return readBytes != null && readBytes.length > 0;
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
     * 通信调度器收到裸数据时，首先调用此方法
     *
     * @param readBytes    裸数据碎包
     * @param onFrameReady 拼好完整帧后的回调执行器
     */
    public void onRawBytesReceived(byte[] readBytes, BiConsumer<byte[], Void> onFrameReady) {
        if (readBytes == null || readBytes.length == 0) return;

        synchronized (receiveBuffer) {
            // 无条件将零碎的字节流灌入缓冲区
            try {
                receiveBuffer.write(readBytes);
            } catch (IOException ignored) {
            }

            // 调用帧驱动解析引擎
            parseFrame(onFrameReady);
        }
    }

    /**
     * 帧解析核心引擎 - 基类默认实现
     * 如果子类不重写，默认不处理
     * 具体的设备子类根据协议重写逻辑
     */
    protected void parseFrame(BiConsumer<byte[], Void> onFrameReady) {
        if (receiveBuffer.size() > 0) {
            byte[] frame = receiveBuffer.toByteArray();
            receiveBuffer.reset();
            if (validate(frame)) {
                onFrameReady.accept(frame, null);
            }
        }
    }

    /**
     * 主动上报完整帧
     * 当一帧完整的数据被调度器判定为非响应数据时，由调度器最终触发
     *
     * @param frame 绝对完整的一帧数据
     */
    public void onAutoReport(byte[] frame) {
        // 基类做默认的日志输出
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String now = LocalDateTime.now().format(formatter);
        System.out.println("[" + now + "]收到全局未拦截的主动上报帧: " + HexUtils.bytesToHexString(frame));
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
        // System.out.println("当前所有队列执行完毕");
    }
}
