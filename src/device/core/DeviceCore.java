package device.core;

import device.enums.DispatchMode;
import device.utils.HexUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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
    /**
     * 接收缓冲区
     */
    private final ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();
    /**
     * 并发锁
     */
    private final Object bufferLock = new Object();

    /**
     * 获取写入时间间隔
     *
     * @return
     */
    public final long getWriteIntervalTime() {
        return writeIntervalTime;
    }

    /**
     * 设置写入时间间隔(ms)
     *
     * @param writeIntervalTime
     */
    public void setWriteIntervalTime(long writeIntervalTime) {
        this.writeIntervalTime = writeIntervalTime;
    }

    /**
     * 设置通信调度器
     *
     * @param commDispatcher
     */
    public void setCommDispatcher(CommDispatcher commDispatcher) {
        this.commDispatcher = commDispatcher;
        if (this.commDispatcher != null) {
            this.commDispatcher.onAllTasksCompleted = this::onAllTasksCompleted;
        }
    }

    public final Charset getCharset() {
        return this.commDispatcher.getCharset();
    }

    public final void setTimeout(int timeout) {
        commDispatcher.responseTimeout = Math.max(timeout, commDispatcher.responseTimeout);
    }

    @FunctionalInterface
    public interface TaskParser<T> {
        /**
         * @param readBytes  接收到的字节
         * @param writeBytes 发送的字节
         * @return 解析后的业务结果
         * @throws Exception 业务校验不通过时抛出异常
         */
        T parse(byte[] readBytes, byte[] writeBytes) throws Exception;
    }

    interface CommCallbackWrapper extends BiConsumer<byte[], byte[]> {
        void notifyFinalResult(boolean success, Exception lastException);
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
    public final void write(DispatchMode dispatchMode, byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(dispatchMode, writeBytes, priority, retryCount, dataReceived);
    }

    /**
     * 写入数据
     * 优先级为10，重试0次
     *
     * @param writeBytes
     * @param dataReceived 接收到响应的回调
     */
    public final void write(byte[] writeBytes, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, dataReceived);
    }

    /**
     * 写入数据
     * 优先级为10，重试0次，无回调
     *
     * @param writeBytes
     */
    public final void write(byte[] writeBytes) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, this::callback);
    }

    /**
     * 写入数据
     * 优先级为10，重试0次，无回调
     *
     * @param writeBytes
     * @param timeout
     */
    public final void write(byte[] writeBytes, long timeout) {
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
    public final void write(byte[] writeBytes, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, timeout, dataReceived);
    }

    /**
     * 写入数据
     *
     * @param writeBytes   写入的byte数组
     * @param retryCount   重试次数
     * @param timeout      单条命令超时时间
     * @param dataReceived 业务处理回调函数
     */
    public final void write(byte[] writeBytes, int retryCount, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, retryCount, timeout, dataReceived);
    }

    /**
     * 写入ASCII码数据
     *
     * @param writeASCII
     */
    public final void write(String writeASCII) {
        byte[] bytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.write(bytes);
    }

    public final void write(String writeASCII, long timeout) {
        byte[] bytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.write(bytes, timeout);
    }

    public final void write(String writeASCII, int retryCount, long timeout) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, retryCount, timeout, this::callback);
    }

    public final void write(String writeASCII, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, timeout, dataReceived);
    }

    public final void write(String writeASCII, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, 0, dataReceived);
    }

    public final void write(String writeASCII, int retryCount, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(DispatchMode.SEQUENTIAL, writeBytes, 10, retryCount, timeout, dataReceived);
    }

    /**
     * 写入ascii码字符串
     *
     * @param writeASCII
     * @param dispatchMode 队列策略
     * @param priority     优先级
     * @param retryCount   重试次数
     * @param timeout      超时时间
     * @param dataReceived 回调业务解析函数
     */
    public final void write(String writeASCII, DispatchMode dispatchMode, int priority, int retryCount, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        byte[] writeBytes = writeASCII.getBytes(this.commDispatcher.getCharset());
        this.commDispatcher.write(dispatchMode, writeBytes, priority, retryCount, timeout, dataReceived);
    }

    /**
     * 同步写入byte数组
     *
     * @param frame      写入的byte数组
     * @param retryCount 重试次数
     * @param timeout    超时时间
     * @param parser     业务处理回调函数
     * @param <T>
     * @return
     * @throws Exception
     */
    public final <T> T writeSync(byte[] frame, int retryCount, long timeout, BiFunction<byte[], byte[], T> parser) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();

        // 创建一个复合包装器
        CommCallbackWrapper callbackWrapper = new CommCallbackWrapper() {
            private byte[] lastRead;
            private byte[] lastWrite;

            @Override
            public void accept(byte[] readBytes, byte[] writeBytes) {
                this.lastRead = readBytes;
                this.lastWrite = writeBytes;

                // 执行业务解析
                T result = parser.apply(readBytes, writeBytes);

                // 如果业务层返回 null（表示数据非法需要重试），主动抛出异常驱动重试
                if (result == null) {
                    throw new RuntimeException("业务处理校验结果返回 null");
                }

                // 如果解析成功且无异常，立刻使 Future 完结！让主线程无需等待，直接返回
                future.complete(result);
            }

            @Override
            public void notifyFinalResult(boolean success, Exception lastException) {
                // 当所有的重试轮次全部结束（或者彻底失败）时，CommDispatcher 会调用这个方法
                if (!success) {
                    // 如果彻底失败了，把最后一次记录的异常注入到 future 中，唤醒主线程
                    future.completeExceptionally(lastException != null ? lastException : new RuntimeException("未知通信错误"));
                } else {
                    // 安全兜底：确保 future 判定成功
                    if (!future.isDone()) {
                        try {
                            T result = parser.apply(lastRead, lastWrite);
                            future.complete(result);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }
                }
            }
        };

        // 依然调用底层的 write 压入队列
        this.write(frame, retryCount, timeout, callbackWrapper);

        try {
            // 主线程等待时间：(重试次数 + 1) * 单次超时 + 退避累加时间 + 缓冲
            long maxWaitTime = (retryCount + 1) * timeout + (retryCount * 100) + 500;
            return future.get(maxWaitTime, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                throw (Exception) e.getCause(); // 抛出最后一次的真实异常给调用者
            }
            throw e;
        }
    }

    /**
     * 同步写入ascii码字符串
     *
     * @param frameASCII 写入的ascii码字符串
     * @param retryCount 重试次数
     * @param timeout    超时时间
     * @param parser     业务处理回调函数
     * @param <T>
     * @return
     * @throws Exception
     */
    public final <T> T writeSync(String frameASCII, int retryCount, long timeout, BiFunction<byte[], byte[], T> parser) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();

        // 创建一个复合包装器
        CommCallbackWrapper callbackWrapper = new CommCallbackWrapper() {
            private byte[] lastRead;
            private byte[] lastWrite;

            @Override
            public void accept(byte[] readBytes, byte[] writeBytes) {
                this.lastRead = readBytes;
                this.lastWrite = writeBytes;

                // 执行业务解析
                T result = parser.apply(readBytes, writeBytes);

                // 如果业务层返回 null（表示数据非法需要重试），主动抛出异常驱动重试
                if (result == null) {
                    throw new RuntimeException("业务校验未通过（数据包不完整或格式错误），触发退避重试");
                }

                // 如果解析成功且无异常，立刻使 Future 完结！让主线程无需等待，直接返回
                future.complete(result);
            }

            @Override
            public void notifyFinalResult(boolean success, Exception lastException) {
                // 当所有的重试轮次全部结束（或者彻底失败）时，CommDispatcher 会调用这个方法
                if (!success) {
                    // 如果彻底失败了，把最后一次记录的异常注入到 future 中，唤醒主线程
                    future.completeExceptionally(lastException != null ? lastException : new RuntimeException("未知通信错误"));
                } else {
                    // 安全兜底：确保 future 判定成功
                    if (!future.isDone()) {
                        try {
                            T result = parser.apply(lastRead, lastWrite);
                            future.complete(result);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }
                }
            }
        };

        // 依然调用底层的 write 压入队列
        this.write(frameASCII, retryCount, timeout, callbackWrapper);

        try {
            // 主线程等待时间：(重试次数 + 1) * 单次超时 + 退避累加时间 + 缓冲
            long maxWaitTime = (retryCount + 1) * timeout + (retryCount * 100) + 500;
            return future.get(maxWaitTime, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                throw (Exception) e.getCause(); // 抛出最后一次的真实异常给调用者
            }
            throw e;
        }
    }

    /**
     * 同步写入
     *
     * @param frameASCII   写入的ascii字符串
     * @param dispatchMode 队列策略
     * @param priority     优先级
     * @param retryCount   重试次数
     * @param timeout      超时时间
     * @param parser       业务处理回调函数
     * @param <T>
     * @return
     * @throws Exception
     */
    public final <T> T writeSync(String frameASCII, DispatchMode dispatchMode, int priority, int retryCount, long timeout, BiFunction<byte[], byte[], T> parser) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();

        // 创建一个复合包装器
        CommCallbackWrapper callbackWrapper = new CommCallbackWrapper() {
            private byte[] lastRead;
            private byte[] lastWrite;

            @Override
            public void accept(byte[] readBytes, byte[] writeBytes) {
                this.lastRead = readBytes;
                this.lastWrite = writeBytes;

                // 执行业务解析
                T result = parser.apply(readBytes, writeBytes);

                // 如果业务层返回 null（表示数据非法需要重试），主动抛出异常驱动重试
                if (result == null) {
                    throw new RuntimeException("业务校验未通过（数据包不完整或格式错误），触发退避重试");
                }

                // 如果解析成功且无异常，立刻使 Future 完结！让主线程无需等待，直接返回
                future.complete(result);
            }

            @Override
            public void notifyFinalResult(boolean success, Exception lastException) {
                // 当所有的重试轮次全部结束（或者彻底失败）时，CommDispatcher 会调用这个方法
                if (!success) {
                    // 如果彻底失败了，把最后一次记录的异常注入到 future 中，唤醒主线程
                    future.completeExceptionally(lastException != null ? lastException : new RuntimeException("未知通信错误"));
                } else {
                    // 安全兜底：确保 future 判定成功
                    if (!future.isDone()) {
                        try {
                            T result = parser.apply(lastRead, lastWrite);
                            future.complete(result);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }
                }
            }
        };

        // 依然调用底层的 write 压入队列
        this.write(frameASCII, dispatchMode, priority, retryCount, timeout, callbackWrapper);

        try {
            // 主线程等待时间：(重试次数 + 1) * 单次超时 + 退避累加时间 + 缓冲
            long maxWaitTime = (retryCount + 1) * timeout + (retryCount * 100) + 500;
            return future.get(maxWaitTime, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                throw (Exception) e.getCause(); // 抛出最后一次的真实异常给调用者
            }
            throw e;
        }
    }

    /**
     * 收到原始数据时
     *
     * @param rawBytes
     */
    public final void onRawDataReceived(byte[] rawBytes) {
        List<byte[]> completeFrames = new ArrayList<>();

        synchronized (bufferLock) {
            // 灌入缓冲区
            try {
                receiveBuffer.write(rawBytes);
                byte[] tmpData = receiveBuffer.toByteArray();
                System.out.println("[收到设备原始数据 | 当前缓冲区全量数据]：" + HexUtils.bytesToHexString(tmpData));
            } catch (IOException ignored) {
            }

            // 调用可复写的解析方法，提取出所有的完整帧
            completeFrames = splitFrames(receiveBuffer);
        }

        // 跨出锁区，把解析出来的完整帧逐个抛给调度层认领
        if (this.commDispatcher != null && !completeFrames.isEmpty()) {
            for (byte[] frame : completeFrames) {
                this.commDispatcher.handleCompleteFrame(frame);
            }
        }
    }

    /**
     * 从缓冲区中拆分出完整的协议帧。
     * <p>
     * 默认实现为简单处理：将当前缓冲区内的所有数据整体作为一个数据帧提取，并清空缓冲区。
     * 子类可重写此方法以实现具体的粘包、断包和拆包逻辑（如基于固定长度、特殊帧尾或长度域）。
     * </p>
     *
     * @param buffer 待拆分的字节缓冲区
     * @return 拆分出的数据帧列表，若缓冲区无数据则返回空列表
     */
    protected List<byte[]> splitFrames(ByteArrayOutputStream buffer) {
        List<byte[]> frames = new ArrayList<>();
        if (buffer.size() == 0) return frames;

        // 默认行为：将当前缓存区全部字节抠出来当作一帧
        byte[] frame = buffer.toByteArray();
        frames.add(frame);

        // 清空缓冲区
        buffer.reset();
        return frames;
    }

    public final void clearReceiveBuffer() {
        synchronized (bufferLock) {
            receiveBuffer.reset();
        }
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
     * 接收到设备主动上报数据时
     *
     * @param readBytes 读取到的设备完整协议帧数据
     */
    public void onDeviceReported(byte[] readBytes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String now = LocalDateTime.now().format(formatter);

        System.out.println(now + " 被标记为主动上报的帧:" + HexUtils.bytesToHexString(readBytes));
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
