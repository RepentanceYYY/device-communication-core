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
                // 1. 自动断线重连检测
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

                // 2. 状态预初始化（放在锁外面，此时尚未产生线程挂起竞争）
                this.currentTask = task;
                this.lastReadBytes = null;
                success = false;

                // 3. 执行物理层数据写入（无锁状态下进行，防止耗时 I/O 拖垮接收线程）
                try {
                    write(task);
                } catch (Exception ex) {
                    System.err.println("通信写入异常: " + ex.getMessage());
                    this.currentTask = null; // 发生异常及时清空

                    // 此时线程未持有锁，调用 close 触发重连逻辑绝对安全，绝无死锁可能
                    try {
                        close();
                    } catch (Exception ignore) {
                    }

                    retries--;
                    if (retries >= 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException ignore) {
                        }
                    }
                    continue; // 写入失败，直接跳转到下一次重试循环
                }

                // 4. 根据通信模式控制同步挂起
                if (strategy == CommMode.WAIT_RESPONSE) {
                    lock.lock(); // 【精准上锁】：只保护线程通知与条件流转
                    try {
                        // 【双重检查】：防止 write(task) 刚完成的间隙，硬件秒回导致 receive 线程已经把数据收完了
                        if (this.lastReadBytes == null) {
                            // 挂起当前发送线程，等待提取完完整帧后发出信号唤醒
                            success = responseCondition.await(task.getTimeout(), TimeUnit.MILLISECONDS);
                        } else {
                            success = true;
                        }

                        if (success) {
                            responseData = this.lastReadBytes;
                            if (responseData == null || responseData.length == 0) {
                                success = false;
                                System.err.println("[CommDispatcher] 收到空响应");
                            }
                        } else {
                            System.err.println("[CommDispatcher] 等待响应超时 (Timeout: " + task.getTimeout() + "ms)");
                        }
                    } catch (InterruptedException e) {
                        success = false;
                        Thread.currentThread().interrupt(); // 保持中断状态
                        System.err.println("[CommDispatcher] 等待响应被线程中断");
                    } finally {
                        // 在锁区间的最后，干净利落地清理状态并释放锁
                        this.currentTask = null;
                        this.lastReadBytes = null;
                        lock.unlock();
                    }
                } else {
                    // 如果是无响应模式（仅发送），写入成功即代表任务成功
                    success = true;
                    this.currentTask = null;
                }

                // 5. 任务执行成功，直接跳出重试循环
                if (success) {
                    break;
                }

                // 6. 任务失败且还有重试机会，执行指数退避等待
                retries--;
                if (retries >= 0) {
                    try {
                        long backoffTime = 50 + (initialRetryCount - retries) * 30;
                        System.out.println("[CommDispatcher] 重试前退避等待 " + backoffTime + "ms");
                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            // 7. 触发业务响应回调
            if (task.getDataReceived() != null) {
                try {
                    task.getDataReceived().accept(responseData, task.getWriteBytes());
                } catch (Exception e) {
                    System.err.println("回调异常: " + e.getMessage());
                }
            }

            // 8. 指令间隙控制，防止频繁刷包导致硬件缓冲区爆满
            if (device != null && device.getWriteIntervalTime() > 0) {
                try {
                    Thread.sleep(device.getWriteIntervalTime());
                } catch (InterruptedException ignore) {
                }
            }
        }

        // 9. 全局任务队列清空事件通知
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
     * 无论收到的是断包还是粘包，一律先交由设备层缓冲区进行完整帧提取
     *
     * @param readBytes 驱动或物理层直接上报的裸数据碎包
     */
    public void receive(byte[] readBytes) {
        if (device == null || readBytes == null || readBytes.length == 0) return;

        // 1. 底层不做任何强校验和匹配，直接喂给 device 拼接并提取完整帧
        // 2. 传入 Lambda 回调：一旦 device 拼出了一帧完整的 frame，立刻回调 handleCompleteFrame
        device.onRawBytesReceived(readBytes, this::handleCompleteFrame);
    }

    private void handleCompleteFrame(byte[] frame, Void ignored) {
        lock.lock();
        try {
            // 判定是否为当前等待的响应
            if (this.currentTask != null) {
                if (device.isMatch(this.currentTask.getWriteBytes(), frame)) {
                    this.lastReadBytes = frame;
                    responseCondition.signalAll(); // 唤醒同步等待
                    return; // 匹配成功，生命周期结束
                }
            }
        } finally {
            lock.unlock();
        }

        // 2. 匹配失败，百分之百是设备自己上报的数据,调用主动上报接口
        try {
            device.onAutoReport(frame);
        } catch (Exception e) {
            System.err.println("[Dispatcher] 业务层处理主动上报异常: " + e.getMessage());
        }
    }

    /**
     * 释放非守护线程
     */
    public void dispose() {
        executor.shutdown();
    }
}
