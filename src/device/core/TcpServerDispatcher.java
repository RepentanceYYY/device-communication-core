package device.core;

import device.channel.TcpServerChannel;
import device.model.Task;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

/**
 * Tcp服务端
 */
public class TcpServerDispatcher extends CommDispatcher {
    public TcpServerDispatcher(TcpServerChannel tcpServerChannel) {
        this.tcpServerChannel = tcpServerChannel;
        this.tcpServerChannel.receiveEvent = this::channelReceiveEvent;
    }

    private TcpServerChannel tcpServerChannel;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return this.tcpServerChannel.getIsOpen();
    }

    @Override
    public void open() throws IOException {
        this.tcpServerChannel.open();
    }

    @Override
    public void close() throws IOException {
        this.tcpServerChannel.close();
    }

    @Override
    public void write(Task task) throws IOException {
        this.tcpServerChannel.send(task.getWriteBytes());
    }

    /**
     * 设备回调
     *
     * @param readBytes
     * @param length
     */
    private void channelReceiveEvent(Socket source, byte[] readBytes, int length) {
        byte[] data = Arrays.copyOf(readBytes, length);
        receive(data);
    }
}
