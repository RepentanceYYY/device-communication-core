package device.core;

import device.enums.CommMode;
import device.enums.DispatchMode;
import device.model.Task;
import device.utils.HexUtils;

import java.io.IOException;
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
     * 处理下一个任务
     */
    private void processNextTask() {
        Task task;

        while ((task = getDeviceActionModel()) != null) {
            int retries = task.getRetryCount();
            boolean success = true;
            byte[] responseData = null;
            // 获取该任务的策略
            CommMode strategy = task.getActionStrategy();
            while (retries >= 0) {
                success = false;
                lock.lock();
                try {
                    this.currentTask = task;
                    this.lastReadBytes = null;

                    if (!isOpen()) {
                        try {
                            open();
                        } catch (IOException e) {
                            System.err.println("连接打开失败: " + e.getMessage());
                            lock.unlock();
                            TimeUnit.MILLISECONDS.sleep(200);
                            lock.lock();
                            retries--;
                            continue;
                        }
                    }
                    System.out.println("即将写入:"+ HexUtils.bytesToHexString(this.currentTask.getWriteBytes()));
                    write(task);

                    if (strategy == CommMode.WAIT_RESPONSE) {
                        // 等待响应
                        success = responseCondition.await(responseTimeout, TimeUnit.MILLISECONDS);
                        if (success) {
                            responseData = this.lastReadBytes;
                            break; // 成功则跳出重试循环
                        }
                    } else {
                        // 非同步模式，直接视为发送成功
                        success = true;
                        break;
                    }


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
        // 基础校验
        if (device == null || !device.validate(readBytes)) return;

        // 认领逻辑
        if (lock.tryLock()) {
            try {
                if (this.currentTask != null) {
                    // 只要匹配成功，就截获该包
                    if (device.isMatch(this.currentTask.getWriteBytes(), readBytes)) {
                        this.lastReadBytes = readBytes;
                        responseCondition.signalAll(); // 唤醒发送线程
                        return; // 匹配成功，拦截此包
                    }
                }
            } finally {
                lock.unlock();
            }
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
