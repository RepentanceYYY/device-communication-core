package device.channel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通讯通道基类
 * <p>
 * 泛型说明：
 * T - 底层资源类型，如 Socket / SerialPort / DatagramSocket
 * S - 消息来源类型，例如 TCP 服务端的客户端 Socket，或串口自身
 * <p>
 * 该类提供：
 * 1. 通用的打开/关闭接口
 * 2. 发送消息接口（支持 byte[] 和 String）
 * 3. 消息回调（接收事件）
 * 4. 打开/关闭事件监听机制
 */
public abstract class CommChannel<T, S> {

    /**
     * 构造函数，默认使用 GB2312 编码
     */
    public CommChannel() {
        this.charset = Charset.forName("GB2312");
    }

    /**
     * 当前通道是否已打开
     *
     * @return true 表示已打开
     */
    public abstract boolean getIsOpen();

    /**
     * 打开通道
     *
     * @throws IOException 打开失败时抛出
     */
    public abstract void open() throws IOException;

    /**
     * 关闭通道
     *
     * @throws IOException 关闭失败时抛出
     */
    public abstract void close() throws IOException;

    /**
     * 发送字节数据
     *
     * @param data 待发送的字节数组
     * @throws IOException 发送失败时抛出
     */
    public abstract void send(byte[] data) throws IOException;

    /**
     * 发送字符串消息，会根据通道的 charset 转为字节
     *
     * @param message 待发送的字符串
     * @throws IOException 发送失败时抛出
     */
    public void send(String message) throws IOException {
        byte[] sendBytes = message.getBytes(this.charset);
        this.send(sendBytes);
    }

    /**
     * 通道使用的编码方式（发送字符串时使用）
     */
    public Charset charset;

    /**
     * 消息回调接口
     *
     * @param source 消息来源，如 TCP 服务端的某个客户端 Socket
     * @param data 接收到的字节数组
     * @param length 数据长度
     */
    public BiReceiver<S> receiveEvent;

    /**
     * 消息回调函数接口
     *
     * @param <S> 消息来源类型
     */
    @FunctionalInterface
    public interface BiReceiver<S> {
        void accept(S source, byte[] data, int length);
    }

    /**
     * 内部触发消息回调
     *
     * @param source 消息来源
     * @param data   接收到的字节数组
     * @param length 数据长度
     */
    protected void onReceiveEvent(S source, byte[] data, int length) {
        if (receiveEvent != null) {
            receiveEvent.accept(source, data, length);
        }
    }

    // -------------------- 通道打开/关闭事件监听 --------------------

    /**
     * 打开事件监听器列表
     */
    protected final List<Consumer<T>> openEventListeners = new ArrayList<>();

    /**
     * 关闭事件监听器列表
     */
    protected final List<Consumer<T>> closeEventListeners = new ArrayList<>();

    /**
     * 注册通道打开事件监听器
     */
    public void addOpenEventListener(Consumer<T> listener) {
        openEventListeners.add(listener);
    }

    /**
     * 移除通道打开事件监听器
     */
    public void removeOpenEventListener(Consumer<T> listener) {
        openEventListeners.remove(listener);
    }

    /**
     * 内部触发通道打开事件
     */
    protected void triggerOpen(T resource) {
        for (Consumer<T> listener : openEventListeners) {
            listener.accept(resource);
        }
    }

    /**
     * 注册通道关闭事件监听器
     */
    public void addCloseEventListener(Consumer<T> listener) {
        closeEventListeners.add(listener);
    }

    /**
     * 移除通道关闭事件监听器
     */
    public void removeCloseEventListener(Consumer<T> listener) {
        closeEventListeners.remove(listener);
    }

    /**
     * 内部触发通道关闭事件
     */
    protected void triggerClose(T resource) {
        for (Consumer<T> listener : closeEventListeners) {
            listener.accept(resource);
        }
    }
}