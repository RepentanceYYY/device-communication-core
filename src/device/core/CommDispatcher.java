package device.core;

import device.enums.CommMode;
import device.enums.DispatchMode;
import device.model.Task;
import device.utils.HexUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

/**
 * 通信调度器
 */
public abstract class CommDispatcher {
    protected CommDispatcher() {
        this.priorityQueue = new PriorityBlockingQueue<>();
        this.concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        this.responseTimeout = 500;
        setDeviceBase(DeviceCore.instance);
    }

    // 使用有界队列（500），防止指令积压撑爆内存
    // DiscardOldestPolicy: 队列满时丢弃最老的任务，确保新指令能排上队
    private final ExecutorService executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(500),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );
    // 锁机制，用于精准控制发送与响应的同步
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition responseCondition = lock.newCondition();

    // 用于暂存接收到的数据，传递给发送线程
    private byte[] lastReadBytes;

    /**
     * 优先队列(最先执行)
     */
    protected PriorityBlockingQueue<Task> priorityQueue;
    /**
     * 无界线程安全队列
     */
    protected ConcurrentLinkedQueue<Task> concurrentLinkedQueue;

    /**
     * 队列执行完毕事件
     */
    public Runnable onAllTasksCompleted;

    /**
     * 获取链接名
     *
     * @return
     */
    public abstract String getName();

    /**
     * 连接是否以及打开
     *
     * @return
     */
    public abstract boolean isOpen();

    /**
     * 打开连接
     */
    public abstract void open() throws IOException;

    /**
     * 关闭连接
     */

    public abstract void close() throws IOException;

    /**
     * 响应超时时间
     */
    public int responseTimeout;
    /**
     * 当前动作
     */
    protected volatile Task currentTask;

    /**
     * 写入数据
     *
     * @param task 队列中的数据
     */
    public abstract void write(Task task) throws IOException;

    /**
     * 设备
     */
    protected DeviceCore device;

    /**
     * 获取当前编码格式
     *
     * @return
     */
    public abstract Charset getCharset();

    /**
     * 设置设备
     *
     * @param device
     */
    public void setDeviceBase(DeviceCore device) {
        this.device = device;
    }

    /**
     * 获取队列元素
     * 先取优先队列，再取普通队列
     *
     * @return
     */
    private Task getDeviceActionModel() {
        Task task = priorityQueue.poll();
        if (task == null) {
            task = concurrentLinkedQueue.poll();
        }
        return task;
    }


    /**
     * 写入数据
     *
     * @param writeBytes 写入的数据
     * @param priority   优先级
     * @param retryCount 重试次数
     */
    public void write(byte[] writeBytes, int priority, int retryCount) {
        this.enqueueAction(DispatchMode.SEQUENTIAL, writeBytes, priority, retryCount, null);
    }

    /**
     * 写入数据
     *
     * @param writeBytes 写入的数据
     * @param priority   优先级
     * @param retryCount 重试次数
     * @param timeout    响应超时时间
     */
    public void write(byte[] writeBytes, int priority, int retryCount, long timeout) {
        this.enqueueAction(DispatchMode.SEQUENTIAL, writeBytes, priority, retryCount, timeout, null);
    }

    /**
     * 写入数据
     *
     * @param strategy     队列策略
     * @param writeBytes   写入的数据
     * @param priority     优先级
     * @param retryCount   重试次数
     * @param dataReceived 响应回调
     */
    public void write(DispatchMode strategy, byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.enqueueAction(strategy, writeBytes, priority, retryCount, dataReceived);
    }

    /**
     * 写入数据
     *
     * @param strategy     队列策略
     * @param writeBytes   写入的数据
     * @param priority     优先级
     * @param retryCount   重试次数
     * @param timeout      响应超时时间
     * @param dataReceived 响应回调
     */
    public void write(DispatchMode strategy, byte[] writeBytes, int priority, int retryCount, long timeout, BiConsumer<byte[], byte[]> dataReceived) {
        this.enqueueAction(strategy, writeBytes, priority, retryCount, timeout, dataReceived);
    }

    /**
     * 入队操作
     *
     * @param strategy
     * @param writeBytes
     * @param priority
     * @param retryCount
     * @param dataReceived
     */
    protected void enqueueAction(DispatchMode strategy, byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {

        if (writeBytes == null || writeBytes.length < 1) return;

        Task task = new Task(writeBytes, priority, retryCount, dataReceived);

        switch (strategy) {
            case PRIORITY -> this.priorityQueue.offer(task);
            case SEQUENTIAL -> this.concurrentLinkedQueue.offer(task);
        }
        executor.submit(this::processNextTask);
    }

    /**
     * 入队操作
     *
     * @param strategy
     * @param writeBytes
     * @param priority
     * @param retryCount
     * @param timeout
     * @param dataReceived
     */
    protected void enqueueAction(DispatchMode strategy, byte[] writeBytes, int priority, int retryCount, long timeout, BiConsumer<byte[], byte[]> dataReceived) {

        if (writeBytes == null || writeBytes.length < 1) return;

        Task task = new Task(writeBytes, priority, retryCount, timeout, dataReceived);

        switch (strategy) {
            case PRIORITY -> this.priorityQueue.offer(task);
            case SEQUENTIAL -> this.concurrentLinkedQueue.offer(task);
        }
        executor.submit(this::processNextTask);
    }

    /**
     * 处理下一个任务
     */
    private void processNextTask() {
        Task task;

        while ((task = getDeviceActionModel()) != null) {
            int initialRetryCount = task.getRetryCount();
            int retries = initialRetryCount;
            boolean success = false;
            byte[] responseData = null;
            CommMode strategy = task.getActionStrategy();

            while (retries >= 0) {
                if (!isOpen()) {
                    try {
                        open();
                    } catch (IOException e) {
                        System.err.println("连接打开失败: " + e.getMessage());
                        retries--;
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException ignore) {
                        }
                        continue;
                    }
                }

                if (device.getWriteIntervalTime() > 0) {
                    try {
                        Thread.sleep(device.getWriteIntervalTime());
                    } catch (InterruptedException ignore) {
                    }
                }

                lock.lock();
                try {
                    this.currentTask = task;
                    this.lastReadBytes = null;

                    String hexData = HexUtils.bytesToHexString(task.getWriteBytes());
                    if (retries == initialRetryCount) {
                        System.out.println("[CommDispatcher] [首次写入] 数据: " + hexData);
                    } else {
                        System.out.println("[CommDispatcher] [第 " + (initialRetryCount - retries) + " 次重试] 数据: " + hexData);
                    }

                    write(task);

                    if (strategy == CommMode.WAIT_RESPONSE) {

                        success = responseCondition.await(task.getTimeout(), TimeUnit.MILLISECONDS);
                        if (success) {
                            responseData = this.lastReadBytes;
                        } else {
                            System.err.println("[CommDispatcher] 等待响应超时 (Timeout: " + task.getTimeout() + "ms)");
                        }
                    } else {
                        success = true;
                    }

                    if (success) break;

                } catch (Exception ex) {
                    System.err.println("通信异常: " + ex.getMessage());
                    try {
                        close();
                    } catch (Exception ignore) {
                    }
                } finally {
                    this.currentTask = null;
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }

                retries--;
                if (!success && retries >= 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            if (task.getDataReceived() != null) {
                try {
                    task.getDataReceived().accept(responseData, task.getWriteBytes());
                } catch (Exception e) {
                    System.err.println("回调异常: " + e.getMessage());
                }
            }
        }

        // 队列清空后的通知
        if (onAllTasksCompleted != null) {
            try {
                onAllTasksCompleted.run();
            } catch (Exception e) {
                System.err.println("ActionEndEvent 执行异常: " + e.getMessage());
            }
        }
    }

    /**
     * 核心接收逻辑
     *
     * @param readBytes
     */
    public void receive(byte[] readBytes) {
        if (device == null || !device.validate(readBytes)) return;

        lock.lock();
        try {
            if (this.currentTask != null) {
                if (device.isMatch(this.currentTask.getWriteBytes(), readBytes)) {
                    this.lastReadBytes = readBytes;
                    responseCondition.signalAll();
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
        device.receive(readBytes, null);
    }

    /**
     * 释放非守护线程
     */
    public void dispose() {
        executor.shutdown();
    }
}
