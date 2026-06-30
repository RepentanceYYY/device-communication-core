package device.core;

import device.enums.CommMode;
import device.enums.DispatchMode;
import device.model.Task;
import device.utils.HexUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
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
     * 获取当前连接链路的唯一标识（如 COM3@9600 或 192.168.1.99:1086)
     *
     * @return
     */
    public abstract String getConnectionId();

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
     * 多设备
     */
    private final Set<DeviceCore> devices = new CopyOnWriteArraySet<>();

    /**
     * 获取当前编码格式
     *
     * @return
     */
    public abstract Charset getCharset();

    /**
     * 获取当前通信链路上挂载的设备数量。
     *
     * @return 挂载的设备数量
     */
    public int getDeviceCount() {
        return this.devices.size();
    }

    /**
     * 添加设备到该通信链路上
     */
    public void addDevice(DeviceCore device) {
        if (device != null) {
            this.devices.add(device);
        }
    }

    /**
     * 从该通信链路上移除设备
     */
    public void removeDevice(DeviceCore device) {
        if (device != null) {
            this.devices.remove(device);
        }
    }

    /**
     * 移除当前通信链路上挂载的所有设备。
     */
    public void clearDevices() {
        this.devices.clear();
    }

    /**
     * 获取队列元素
     * 先取优先队列，再取普通队列
     *
     * @return
     */
    private Task pollNextTask() {
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

        while ((task = pollNextTask()) != null) {
            int initialRetryCount = task.getRetryCount();
            int retries = initialRetryCount;
            boolean success = false;
            byte[] responseData = null;
            CommMode strategy = task.getActionStrategy();

            // 用于记录最后一次发生的异常（无论是通信超时还是业务校验失败）
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

                            // 收到数据后，立刻在锁内/循环内尝试通过业务回调进行校验
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
                    System.err.println("[CommDispatcher] 本次执行失败原因: " + ex.getMessage() + ",写入的数据为：" + HexUtils.bytesToHexString(task.getWriteBytes()));

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
                        long backoffTime = 20 + (initialRetryCount - retries) * 30;
                        System.out.println("[CommDispatcher] 重试前等待 " + backoffTime + "ms");
                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
            // 为了配合 writeSync，我们需要把最后的异常或者成功状态传回
            if (task.getDataReceived() instanceof DeviceCore.CommCallbackWrapper) {
                ((DeviceCore.CommCallbackWrapper) task.getDataReceived()).notifyFinalResult(success, lastException);
            }
            // 在任务之间添加间隔，避免设备处理不过来
            long interval = this.getIntervalTimeForTask(task);
            if (interval > 0) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ignore) {
                }
            }
        }
        // 队列执行完毕，通知当前挂载的所有设备
        for (DeviceCore device : devices) {
            try {
                device.onAllTasksCompleted();
            } catch (Exception e) {
                System.err.println("[CommDispatcher] 通知设备队列完成异常: " + e.getMessage());
            }
        }
    }

    /**
     * 接收到设备发送的数据
     */
    public void receive(byte[] receiveBytes) {
        if (devices.isEmpty() || receiveBytes == null || receiveBytes.length == 0) return;
        // 交给设备层去解析帧
        DeviceCore firstDevice = devices.stream().findFirst().orElse(null);
        // 任选一个设备来做粘包拆包（因为同一通道协议是一样的，随便哪个 device 拼出的完整帧都一样）
        if (firstDevice.validate(receiveBytes)) {
            firstDevice.onRawDataReceived(receiveBytes);
        }
    }

    /**
     * 处理设备拼好的完整帧
     *
     * @param completeFrame
     */
    public void handleCompleteFrame(byte[] completeFrame) {
        lock.lock();
        try {
            if (this.currentTask != null) {
                // 遍历所有设备，看接收到的完整帧属于谁（或者只要有一个匹配上就通过）
                for (DeviceCore dev : devices) {
                    if (dev.isMatch(this.currentTask.getWriteBytes(), completeFrame)) {
                        this.lastReadBytes = completeFrame;
                        responseCondition.signalAll(); // 精准唤醒等待的主线程
                        return;
                    }
                }
            }
        } finally {
            lock.unlock();
        }

        // 如果没有当前同步任务，或者当前任务没 match 上，说明是某台设备自发主动上报的数据
        // 将数据帧广播给当前通道下的<所有>设备实例
        for (DeviceCore dev : devices) {
            dev.onDeviceReported(completeFrame);
        }
    }

    /**
     * 尝试寻找当前正在执行的任务属于哪一个设备，并获取其发送间隔
     */
    private long getIntervalTimeForTask(Task task) {
        if (task == null || task.getWriteBytes() == null || task.getWriteBytes().length == 0) return 50;
        for (DeviceCore dev : devices) {
            if (dev.isMatch(task.getWriteBytes(), task.getWriteBytes())) {
                return dev.getWriteIntervalTime();
            }
        }
        return 50;
    }

    /**
     * 释放非守护线程
     */
    public void dispose() {
        executor.shutdown();
    }
}
