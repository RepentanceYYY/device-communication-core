package device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TcpClient {
    public TcpClient(String host, int port) {
        this.charset = Charset.forName("GB2312");
        this.address = new InetSocketAddress(host, port);
    }

    /**
     * 服务器回发消息时触发
     */
    public BiConsumer<byte[], Integer> receiveEvent;
    /**
     * 存储所有订阅“链接打开”事件的观察者
     */
    private final List<Consumer<Socket>> openEventListeners = new ArrayList<>();
    /**
     * 存储所有订阅“链接关闭”事件的观察者
     */
    private final List<Consumer<Socket>> closeEventListeners = new ArrayList<>();
    /**
     * 字符编码 默认GB2312
     */
    public Charset charset;
    /**
     * 是否打开了链接
     */
    private boolean isOpen;

    public boolean getIsOpen() {
        return isOpen;
    }

    /**
     * 通信地址
     */
    public InetSocketAddress address;
    /**
     * socket客户端
     */
    public Socket clientSocket;


    /**
     *  读取数据的线程
     */
    private Thread readThread;

    /**
     * 注册一个新的观察者，当连接打开时将触发该回调
     *
     * @param listener 要添加的观察者逻辑
     */
    public void addOpenEventListener(Consumer<Socket> listener) {
        openEventListeners.add(listener);
    }

    /**
     * 移除已注册“打开链接”的观察者
     *
     * @param listener 要移除的观察者实例
     */
    public void removeOpenEventListener(Consumer<Socket> listener) {
        openEventListeners.remove(listener);
    }

    /**
     * 触发所有已注册“打开链接”的观察者
     *
     * @param socket 刚刚开启的 Socket 实例
     */
    protected void triggerOpen(Socket socket) {
        for (Consumer<Socket> listener : openEventListeners) {
            listener.accept(socket);
        }
    }

    /**
     * 注册一个新的观察者，当链接关闭时将触发该回调
     *
     * @param listener 要添加的观察者逻辑
     */
    public void addCloseEventListener(Consumer<Socket> listener) {
        closeEventListeners.add(listener);
    }

    /**
     * 移除已注册“关闭链接”的观察者
     *
     * @param listener 要移除的观察者实例
     */
    public void removeCloseEventListener(Consumer<Socket> listener) {
        closeEventListeners.remove(listener);
    }

    /**
     * 触发所有已注册“关闭链接”的观察者
     *
     * @param socket 刚刚关闭的 Socket 实例
     */
    protected void triggerClose(Socket socket) {
        for (Consumer<Socket> listener : closeEventListeners) {
            listener.accept(socket);
        }
    }


    /**
     * 根据DeviceActionModel触发onReceiveEvent事件
     *
     * @param readBytes
     * @param length
     */
    protected void onReceiveEvent(byte[] readBytes, int length) {
        if (receiveEvent != null) {
            receiveEvent.accept(readBytes, length);
        }
    }

    /**
     * 启动websocket
     */
    public void start() throws IOException {
        this.start(-1);
    }

    /**
     * 启动websocket
     *
     * @param maxTryCount 最大尝试次数
     */
    public void start(int maxTryCount) throws IOException {
        if (this.isOpen) {
            return;
        }
        int tryCount = 1;
        while (true) {
            try {
                clientSocket = new Socket();
                clientSocket.connect(address, 2000);
                break;
            } catch (IOException connectEx) {
                if (maxTryCount != -1 && maxTryCount >= tryCount++) {
                    throw connectEx;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        this.isOpen = true;
        // 通知所有观察者
        triggerOpen(this.clientSocket);
        receiveMessage();
    }

    /**
     * 关闭链接
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (!this.isOpen) {
            return;
        }
        this.isOpen = false;

        // 停止读取线程
        if (readThread != null && readThread.isAlive()) {
            readThread.interrupt();
        }

        try {
            // 2. 优雅关闭 TCP 四次挥手
            if (clientSocket != null && !clientSocket.isClosed()) {
                if (clientSocket.isConnected()) {
                    // 停止接收和发送，但保持连接直到 close
                    clientSocket.shutdownInput();
                    clientSocket.shutdownOutput();
                }
                // 3. 彻底释放底层资源
                clientSocket.close();
            }
        } finally {
            // 通知外部：连接已关闭
            triggerClose(clientSocket);
            //  清理观察者，彻底断开引用
            if (closeEventListeners != null) {
                closeEventListeners.clear();
            }
            clientSocket = null;
        }
    }

    /**
     * 发送消息
     *
     * @param message
     * @return
     * @throws IOException
     */
    public int sendMessage(String message) throws IOException {
        byte[] sendBytes = message.getBytes(this.charset);
        return sendMessage(sendBytes);
    }

    /**
     * 发送消息
     *
     * @param bytes
     * @return
     * @throws IOException
     */
    public synchronized int  sendMessage(byte[] bytes) throws IOException {
        if (!this.isOpen || clientSocket == null || !clientSocket.isConnected()) {
            return -1;
        }
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
        out.flush();
        return bytes.length;
    }

    /**
     * 接收消息
     */
    private void receiveMessage() {
        readThread = new Thread(() -> {
            try {
                InputStream input = clientSocket.getInputStream();
                byte[] buffer = new byte[1024];

                // 只要连接是开着的且没被中断，就一直读
                while (isOpen && !Thread.currentThread().isInterrupted()) {
                    // 阻塞等待数据
                    int len = input.read(buffer);
                    // 对端关闭了连接
                    if (len == -1) break;
                    // 复制并处理数据
                    byte[] data = Arrays.copyOf(buffer, len);
                    onReceiveEvent(data, len);
                }
            } catch (IOException e) {
                if (isOpen) {
                    System.err.println("读取异常: " + e.getMessage());
                }
            } finally {
                try {
                    close();
                } catch (IOException ignored) {
                }
            }
        }, "Socket-Read-Thread");

        readThread.setDaemon(true);
        readThread.start();
    }


}
