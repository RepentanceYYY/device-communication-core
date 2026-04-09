package device.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 基于 TCP 协议的服务端通道
 */
public class TcpServerChannel extends CommChannel<ServerSocket, Socket> {

    private final int port;
    private boolean isOpen;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    /**
     * 固定客户端Ip
     */
    private String fixedClientIp;
    /**
     * 固定客户端端口
     */
    private int fixedClientPort;

    // 保存所有已连接的客户端
    private final CopyOnWriteArrayList<Socket> clients = new CopyOnWriteArrayList<>();

    // 客户端连接/断开事件
    private final List<Consumer<Socket>> clientOpenListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Socket>> clientCloseListeners = new CopyOnWriteArrayList<>();

    public TcpServerChannel(int port) {
        this.port = port;
    }

    public TcpServerChannel(int port, String clientIp, int clientPort) {
        this.port = port;
        this.fixedClientIp = clientIp;
        this.fixedClientPort = clientPort;
    }

    @Override
    public boolean getIsOpen() {
        return isOpen;
    }

    /**
     * 打开服务端
     */
    @Override
    public void open() throws IOException {
        if (isOpen) return;

        serverSocket = new ServerSocket(port);
        isOpen = true;
        triggerOpen(serverSocket); // 服务端打开事件

        startAcceptThread();
    }

    /**
     * 关闭服务端和所有客户端
     */
    @Override
    public void close() throws IOException {
        if (!isOpen) return;
        isOpen = false;

        // 停止 accept 线程
        if (acceptThread != null && acceptThread.isAlive()) {
            acceptThread.interrupt();
        }

        // 关闭所有客户端
        for (Socket client : clients) {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
        clients.clear();

        // 关闭服务端
        if (serverSocket != null) {
            serverSocket.close();
        }

        triggerClose(serverSocket);
    }

    /**
     * 默认发送：广播
     */
    @Override
    public void send(byte[] data) throws IOException {
        broadcast(data);
    }

    /**
     * 发送给单个客户端
     */
    public void send(Socket client, byte[] data) throws IOException {
        if (client == null || client.isClosed()) return;
        OutputStream out = client.getOutputStream();
        out.write(data);
        out.flush();
    }

    /**
     * 广播给所有客户端
     */
    public void broadcast(byte[] data) throws IOException {
        for (Socket client : clients) {
            send(client, data);
        }
    }

    /**
     * 注册客户端连接事件
     */
    public void addClientOpenListener(Consumer<Socket> listener) {
        clientOpenListeners.add(listener);
    }

    public void removeClientOpenListener(Consumer<Socket> listener) {
        clientOpenListeners.remove(listener);
    }

    private void triggerClientOpen(Socket client) {
        for (Consumer<Socket> listener : clientOpenListeners) {
            listener.accept(client);
        }
    }

    /**
     * 注册客户端断开事件
     */
    public void addClientCloseListener(Consumer<Socket> listener) {
        clientCloseListeners.add(listener);
    }

    public void removeClientCloseListener(Consumer<Socket> listener) {
        clientCloseListeners.remove(listener);
    }

    private void triggerClientClose(Socket client) {
        for (Consumer<Socket> listener : clientCloseListeners) {
            listener.accept(client);
        }
    }

    /**
     * 启动接收客户端线程
     */
    private void startAcceptThread() {
        acceptThread = new Thread(() -> {
            while (isOpen && !Thread.currentThread().isInterrupted()) {
                try {
                    Socket client = serverSocket.accept();
                    String ip = client.getInetAddress().getHostAddress();
                    // 查找旧连接
                    Socket oldClient = clients.stream()
                            .filter(x -> x.getInetAddress().getHostAddress().equals(ip))
                            .findFirst()
                            .orElse(null);

                    // 如果存在旧连接 → 关闭并移除
                    if (oldClient != null) {
                        try {
                            oldClient.close();
                        } catch (IOException ignored) {
                        }

                        clients.remove(oldClient);
                    }
                    triggerClientOpen(client); // 客户端连接事件
                    startReadThread(client);
                } catch (IOException e) {
                    if (isOpen) System.err.println("接收客户端异常: " + e.getMessage());
                }
            }
        }, "TcpServer-AcceptThread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * 每个客户端的读取线程
     */
    private void startReadThread(Socket client) {
        new Thread(() -> {
            try (InputStream input = client.getInputStream()) {
                byte[] buffer = new byte[1024];
                while (!client.isClosed()) {
                    int len = input.read(buffer);
                    if (len == -1) break;

                    byte[] data = Arrays.copyOf(buffer, len);
                    onReceiveEvent(client, data, len); // 消息回调
                }
            } catch (IOException e) {
                System.err.println("客户端读取异常: " + e.getMessage());
            } finally {
                try {
                    clients.remove(client);
                    client.close();
                    triggerClientClose(client); // 客户端断开事件
                } catch (IOException ignored) {
                }
            }
        }, "TcpServer-ReadThread-" + client.getPort()).start();
    }
}