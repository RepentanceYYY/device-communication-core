package device.core;

import device.enums.CommMode;
import device.enums.DispatchMode;
import device.model.Task;
import device.utils.HexUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(500),
            r -> {
                Thread t = new Thread(r);
                t.setName("commDispatcher-pool-1");
                return t;
            },
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

            // ⭐ 新增：用于记录最后一次发生的异常（无论是通信超时还是业务校验失败）
            Exception lastException = null;

            while (retries >= 0) {
                if (!isOpen()) {
                    try {
                        open();
                    } catch (IOException e) {
                        lastException = e; // 记录异常
                        retries--;
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException ignore) {
                        }
                        continue;
                    }
                }

                lock.lock();
                try {
                    this.currentTask = task;
                    this.lastReadBytes = null;

                    String hexData = HexUtils.bytesToHexString(task.getWriteBytes());
                    if (retries == initialRetryCount) {
                        System.out.println("[" + Thread.currentThread().getName() + "] [首次写入] TaskID: " + System.identityHashCode(task) + " 数据: " + hexData);
                    } else {
                        System.out.println("[CommDispatcher] [第 " + (initialRetryCount - retries) + " 次重试] 数据: " + hexData);
                    }

                    write(task);

                    if (strategy == CommMode.WAIT_RESPONSE) {
                        // 1. 等待底层物理响应
                        boolean hasResponse = responseCondition.await(task.getTimeout(), TimeUnit.MILLISECONDS);
                        if (hasResponse) {
                            responseData = this.lastReadBytes;
                            if (responseData == null || responseData.length == 0) {
                                throw new RuntimeException("设备响应了空数据");
                            }

                            // 2. ⭐【关键改动】收到数据后，立刻在锁内/循环内尝试通过业务回调进行校验
                            if (task.getDataReceived() != null) {
                                // 这里触发业务层的解析
                                task.getDataReceived().accept(responseData, task.getWriteBytes());
                            }

                            // 如果上面的 accept() 抛出了异常，代码会直接跳到下面的 catch 块，不会执行到 success = true
                            success = true;

                        } else {
                            throw new RuntimeException("设备响应超时");
                        }
                    } else {
                        success = true;
                    }

                } catch (Exception ex) {
                    // 捕获到了异常（可能是超时，也可能是业务回调里抛出的“数据非法异常”）
                    success = false;
                    lastException = ex; // 记录最后一次的异常
                    System.err.println("[CommDispatcher] 本次执行失败原因: " + ex.getMessage());

                    try {
                        close();
                    } catch (Exception ignore) {
                    }
                } finally {
                    this.currentTask = null;
                    this.lastReadBytes = null;
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }

                // 如果成功了，直接退出重试循环
                if (success) break;

                // 失败了，扣减重试次数，触发退避延时
                retries--;
                if (retries >= 0) {
                    try {
                        long backoffTime = 50 + (initialRetryCount - retries) * 30;
                        System.out.println("[CommDispatcher] 重试前等待 " + backoffTime + "ms");
                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            // ⭐ 循环完全结束后，通过一种机制把最终的结果（成功或最后的异常）通知给最上层的 Future
            // 我们可以在 Task 中专门包裹一个通知包装器，或者直接利用原有的数据流传
            // 为了配合 writeSync，我们需要把最后的异常或者成功状态传回
            if (task.getDataReceived() instanceof DeviceCore.CommCallbackWrapper) {
                ((DeviceCore.CommCallbackWrapper) task.getDataReceived()).notifyFinalResult(success, lastException);
            }
        }

        if (onAllTasksCompleted != null) {
            try {
                onAllTasksCompleted.run();
            } catch (Exception e) {
                System.err.println("ActionEndEvent 执行异常: " + e.getMessage());
            }
        }
    }

    /**
     * 核心接收逻辑：只做分发，不做任何匹配
     */
    public void receive(byte[] readBytes) {
        if (device == null || !device.validate(readBytes)) return;

        // 交给设备层去解析帧
        device.onRawDataReceived(readBytes);
    }

    /**
     * 处理完整帧
     *
     * @param completeFrame
     */
    public void handleCompleteFrame(byte[] completeFrame) {
        lock.lock();
        try {
            if (this.currentTask != null) {
                // 用完整的、干净的帧去跟当前发送的指令做 match 强校验
                if (device.isMatch(this.currentTask.getWriteBytes(), completeFrame)) {
                    this.lastReadBytes = completeFrame;
                    responseCondition.signalAll(); // 匹配成功，精准唤醒发送线程
                    return;
                }
            }
        } finally {
            lock.unlock();
        }

        // 如果没有当前任务，或者当前任务没 match 上，说明是设备主动发上来的数据
        device.onDeviceReported(completeFrame, null);
    }

    /**
     * 释放非守护线程
     */
    public void dispose() {
        executor.shutdown();
    }
}
