package device.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * 基于 TCP 协议的具体通道
 */
public class TcpClientChannel extends CommChannel<Socket> {
    /**
     * @param host 主机名
     * @param port 端口号
     */
    public TcpClientChannel(String host, int port) {
        this.address = new InetSocketAddress(host, port);
    }

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
     * 读取数据的线程
     */
    private Thread readThread;

    /**
     * 打开tcp websocket
     */
    @Override
    public void open() throws IOException {
        this.start(-1);
    }

    /**
     * 启动tcp websocket
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
        startReadThread();
    }

    /**
     * 关闭链接
     *
     * @throws IOException
     */
    @Override
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
    public void send(String message) throws IOException {
        byte[] sendBytes = message.getBytes(this.charset);
        this.send(sendBytes);
    }

    /**
     * 发送消息
     *
     * @param bytes
     * @return
     * @throws IOException
     */
    @Override
    public synchronized void send(byte[] bytes) throws IOException {
        if (!this.isOpen || clientSocket == null || !clientSocket.isConnected()) {
            return;
        }
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
        out.flush();
    }

    /**
     * 启动读取线程
     */
    private void startReadThread() {
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
