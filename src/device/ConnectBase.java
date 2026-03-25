package device;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class ConnectBase {
    protected ConnectBase() {
        this.priorityQueue = new PriorityQueue<DeviceActionModel>();
        this.concurrentLinkedQueue = new ConcurrentLinkedQueue<DeviceActionModel>();
        this.timeOut = 50;
        setDeviceBase(DeviceBase.instance);
    }

    /**
     * 优先队列(最先执行)
     */
    protected PriorityQueue<DeviceActionModel> priorityQueue;
    /**
     * 无界线程安全队列
     */
    protected ConcurrentLinkedQueue<DeviceActionModel> concurrentLinkedQueue;

    /**
     * 所有队列的总长度
     *
     * @return
     */
    public int getAllQueueTotalLength() {
        return priorityQueue.size() + concurrentLinkedQueue.size();
    }

    /**
     * 队列执行完毕事件
     */
    public Runnable onActionEndEvent;

    /**
     * 获取链接名
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
    public int timeOut;

    /**
     * 设备回调是否触发
     */
    protected boolean isReceived;
    /**
     * 是否正在执行回调方法
     */
    protected boolean onReceived;
    /**
     * 异步发送
     */
    public CompletableFuture deviceFuture;
    /**
     * 即将回调的方法
     */
    protected DeviceActionModel receiveAction;

    /**
     * 写入数据
     *
     * @param deviceActionModel 队列中的数据
     */
    public abstract void write(DeviceActionModel deviceActionModel)  throws IOException;

    private DeviceActionModel getDeviceActionModel() {
        if (!priorityQueue.isEmpty()) {
            return priorityQueue.poll();
        }
        return concurrentLinkedQueue.poll();
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
     * @param writeBytes   写入的数据
     * @param priority     优先级
     * @param retryCount   重试次数
     * @param dataReceived 响应回调
     */
    public void write(byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.writeByPriorityQueue(this.priorityQueue,writeBytes,priority,retryCount,dataReceived);
    }

    protected void writeByPriorityQueue(PriorityQueue<DeviceActionModel> priorityQueue, byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        if (writeBytes == null || writeBytes.length < 1) {
            return;
        }
        if (!isOpen()) {
            return;
        }
        priorityQueue.offer(new DeviceActionModel(writeBytes, priority, retryCount, dataReceived));
        write();
    }

    protected void write() {
        synchronized (this) {
            if (!isReceived) {
                return;
            }
            if (onReceived) {
                return;
            }
            if (priorityQueue.isEmpty()) {
                return;
            }
            if (deviceFuture != null && !deviceFuture.isDone()) {
                return;
            }

            isReceived = false;

            if (deviceFuture != null && !deviceFuture.isDone()) {
                return;
            }
            deviceFuture = CompletableFuture.runAsync(() -> {
                try {
                    while (getAllQueueTotalLength() > 0) {
                        DeviceActionModel receiveAction = getDeviceActionModel();
                        if (receiveAction == null) {
                            break;
                        }

                        synchronized (this) {
                            this.receiveAction = receiveAction;
                        }
                        if (isOpen()) {
                            try {
                                write(this.receiveAction);
                            } catch (Exception writeEx) {
                                System.out.println("发送出现异常:" + writeEx.getMessage());
                                try {
                                    close();
                                } catch (Exception closeEx) {
                                    System.out.println("发送失败后关闭出现异常:" + closeEx.getMessage());
                                }
                                try {
                                    open();
                                    write(this.receiveAction);
                                } catch (Exception retryOpenEx) {
                                    System.out.println("重试打开出现异常:" + retryOpenEx.getMessage());
                                }
                            }
                        }
                        int time = 0;
                        while (true) {
                            time += 10;
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            if (this.receiveAction == null) {
                                while (onReceived) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    break;
                                }
                            }
                            if (time >= timeOut && receiveAction != null) {
                                synchronized (this) {
                                    if (receiveAction.getRetryCount() > 1) {
                                        break;
                                    }
                                    if (receiveAction != null) {
                                        BiConsumer<byte[], byte[]> callback = receiveAction.getDataReceived();
                                        if (callback != null) {
                                            callback.accept(null, receiveAction.getWriteBytes());
                                        }
                                    }

                                }
                            }
                        }
                        if (receiveAction.getRetryCount() > 1) {
                            receiveAction.setRetryCount(receiveAction.getRetryCount() + 1);
                        }
                    }
                } catch (Exception runEx) {
                    isReceived = true;
                    deviceFuture = null;
                    if (getAllQueueTotalLength() < 1) {
                        // 预留，所有队列任务全部执行完后的操作
                    } else {
                        write();
                    }
                }
            });
        }
    }

    /**
     * 设备
     */
    protected DeviceBase device;

    /**
     * 设置设备
     *
     * @param device
     */
    public void setDeviceBase(DeviceBase device) {
        this.device = device;
    }

    public void receive(byte[] readBytes) {
        try {
            DeviceActionModel receiveAction;
            synchronized (this) {
                receiveAction = this.receiveAction;
            }
            this.receiveAction = null;
            this.onReceived = true;

            if (receiveAction != null) {
                // 设备响应
                if (receiveAction.getRetryCount() > 1) {
                    if (device.validate(readBytes)) {
                        receiveAction.setRetryCount(0);
                        BiConsumer<byte[], byte[]> callback = receiveAction.getDataReceived();
                        if (callback != null) {
                            callback.accept(readBytes, receiveAction.getWriteBytes());
                        }
                    }
                } else {
                    BiConsumer<byte[], byte[]> callback = receiveAction.getDataReceived();
                    if (callback != null) {
                        callback.accept(readBytes, receiveAction.getWriteBytes());
                    }
                }
            } else {
                // 设备主动上报
                device.receive(readBytes, null);
            }
        } catch (Exception receiveEx) {
            System.out.println("设备回调发生异常:" + receiveEx.getMessage());
        }
        this.onReceived = false;
    }

}
