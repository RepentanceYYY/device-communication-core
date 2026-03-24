package device;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public class DeviceActionModel {
    public DeviceActionModel() {

    }

    public DeviceActionModel(byte[] bytes, BiConsumer<byte[], byte[]> dataReceived, int retryCount, int priority) {
        this.bytes = bytes;
        this.dataReceived = dataReceived;
        this.retryCount = retryCount;
        this.priority = priority;
    }

    /**
     * 写入的数据
     */
    private byte[] bytes;
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
    /**
     * 序列生成器
     */
    private static final AtomicLong seqGenerator = new AtomicLong(0);
    /**
     * 序列
     */
    private final long sequence = seqGenerator.getAndIncrement();

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public BiConsumer<byte[], byte[]> getDataReceived() {
        return dataReceived;
    }

    public void setDataReceived(BiConsumer<byte[], byte[]> dataReceived) {
        this.dataReceived = dataReceived;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getSequence() {
        return sequence;
    }
}
