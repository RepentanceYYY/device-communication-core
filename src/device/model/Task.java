package device.model;

import device.enums.CommMode;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Task implements Comparable<Task> {
    /**
     *
     * @param writeBytes 写入的数据
     * @param priority 优先级
     * @param retryCount 重试次数
     * @param dataReceived 回调函数
     */
    public Task(byte[] writeBytes, int priority, int retryCount, BiConsumer<byte[], byte[]> dataReceived) {
        this.writeBytes = writeBytes;
        this.dataReceived = dataReceived;
        this.retryCount = retryCount;
        this.priority = priority;
    }

    /**
     * 写入的数据
     */
    private byte[] writeBytes;
    /**
     * 回调函数:
     * 参数1：读取到的数据
     * 参数2：写入的数据
     */
    private BiConsumer<byte[], byte[]> dataReceived;
    /**
     * 重试次数
     */
    private int retryCount;
    /**
     * 优先级
     */
    private int priority;
    private CommMode commMode = CommMode.WAIT_RESPONSE;
    /**
     * 序列生成器
     */
    private static final AtomicLong seqGenerator = new AtomicLong(0);
    /**
     * 序列
     */
    private final long sequence = seqGenerator.getAndIncrement();

    @Override
    public int compareTo(Task other) {
        // 1. 首先比较优先级 (数值越小越优先)
        int res = Integer.compare(this.priority, other.priority);

        // 2. 如果优先级一样，则比较序列号 (先入队的先发)
        if (res == 0) {
            res = Long.compare(this.sequence, other.getSequence());
        }
        return res;
    }

    public byte[] getWriteBytes() {
        return writeBytes;
    }

    public BiConsumer<byte[], byte[]> getDataReceived() {
        return dataReceived;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getPriority() {
        return priority;
    }

    public long getSequence() {
        return sequence;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public CommMode getActionStrategy() {
        return commMode;
    }

    public void setActionStrategy(CommMode commMode) {
        this.commMode = commMode;
    }
}
