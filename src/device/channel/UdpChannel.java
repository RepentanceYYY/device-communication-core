package device.channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * 基于UDP的具体通道
 */
public class UdpChannel extends CommChannel<DatagramSocket, DatagramPacket> {

    private DatagramSocket socket;
    private boolean isOpen;

    private Thread receiveThread;

    public int localPort;

    public int remotePort;

    public String remoteHost;

    public UdpChannel(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public UdpChannel(int localPort) {
        this.localPort = localPort;
    }


    @Override
    public boolean getIsOpen() {
        return isOpen;
    }

    @Override
    public synchronized void open() throws IOException {
        if (isOpen) {
            close();
        }

        socket = new DatagramSocket(localPort);

        isOpen = true;

        triggerOpen(socket);
        startReceiveThread();
        this.localPort = socket.getLocalPort();
    }

    @Override
    public synchronized void close() throws IOException {
        if (!isOpen) return;
        isOpen = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        triggerClose(socket);
    }

    @Override
    public void send(byte[] data) throws IOException {
        if (!isOpen) {
            open();
        }

        InetAddress address = InetAddress.getByName(remoteHost);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, remotePort);

        socket.send(packet);
    }

    private void startReceiveThread() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isOpen && !Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet); // 阻塞

                    byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

                    onReceiveEvent(packet, data, data.length);

                } catch (IOException e) {
                    if (isOpen) {
                        System.err.println("UDP接收异常: " + e.getMessage());
                    }
                }
            }
        }, "Udp-Receive-Thread");

        receiveThread.setDaemon(true);
        receiveThread.start();
    }

}
