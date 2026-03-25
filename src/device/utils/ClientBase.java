package device.utils;

import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientBase<T> {
    public ClientBase() {
        this.charset = Charset.forName("GB2312");
    }

    /**
     * 字符编码 默认GB2312
     */
    public Charset charset;
    /**
     * 设备回发消息时触发
     */
    public BiConsumer<byte[], Integer> receiveEvent;

    /**
     * 存储所有订阅“链接打开”事件的观察者
     */
    protected final List<Consumer<T>> openEventListeners = new ArrayList<>();
    /**
     * 存储所有订阅“链接关闭”事件的观察者
     */
    protected final List<Consumer<T>> closeEventListeners = new ArrayList<>();

    /**
     * 注册一个新的观察者，当连接打开时将触发该回调
     *
     * @param listener 要添加的观察者逻辑
     */
    public void addOpenEventListener(Consumer<T> listener) {
        openEventListeners.add(listener);
    }

    /**
     * 移除已注册“打开链接”的观察者
     *
     * @param listener 要移除的观察者实例
     */
    public void removeOpenEventListener(Consumer<T> listener) {
        openEventListeners.remove(listener);
    }

    /**
     * 触发所有已注册“连接开启”的观察者
     *
     * @param resource 底层通信资源实例 (如 Socket 或 SerialPort)
     */
    protected void triggerOpen(T resource) {
        if (openEventListeners == null || openEventListeners.isEmpty()) {
            return;
        }
        for (Consumer<T> listener : openEventListeners) {
            listener.accept(resource);
        }
    }

    /**
     * 注册一个新的观察者，当链接关闭时将触发该回调
     *
     * @param listener 要添加的观察者逻辑
     */
    public void addCloseEventListener(Consumer<T> listener) {
        closeEventListeners.add(listener);
    }

    /**
     * 移除已注册“关闭链接”的观察者
     *
     * @param listener 要移除的观察者实例
     */
    public void removeCloseEventListener(Consumer<T> listener) {
        closeEventListeners.remove(listener);
    }

    /**
     * 触发所有已注册“关闭链接”的观察者
     *
     * @param resource 底层通信资源实例 (如 Socket 或 SerialPort)
     */
    protected void triggerClose(T resource) {
        for (Consumer<T> listener : closeEventListeners) {
            listener.accept(resource);
        }
    }

    /**
     * 触发设备回发消息事件
     *
     * @param readBytes
     * @param length
     */
    protected void onReceiveEvent(byte[] readBytes, int length) {
        if (receiveEvent != null) {
            receiveEvent.accept(readBytes, length);
        }
    }
}
