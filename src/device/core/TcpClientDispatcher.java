package device.core;

import device.channel.TcpClientChannel;
import device.model.Task;

import java.io.IOException;
import java.util.Arrays;

public class TcpClientDispatcher extends CommDispatcher {
    public TcpClientDispatcher(TcpClientChannel tcpClientChannel) {
        this.tcpClientChannel = tcpClientChannel;
        this.tcpClientChannel.receiveEvent = this::channelReceiveEvent;
    }

    /**
     * Tcp客户端
     */
    private TcpClientChannel tcpClientChannel;

    @Override
    public String getName() {
        return this.tcpClientChannel.address.toString();
    }

    @Override
    public boolean isOpen() {
        return tcpClientChannel.getIsOpen();
    }

    @Override
    public void open() throws IOException {
        tcpClientChannel.start(3);
    }

    @Override
    public void close() throws IOException {
        tcpClientChannel.close();
    }

    @Override
    public void write(Task task) throws IOException {
        tcpClientChannel.send(task.getWriteBytes());
    }

    /**
     * 设备回调
     *
     * @param readBytes
     * @param length
     */
    private void channelReceiveEvent(byte[] readBytes, int length) {
        byte[] data = Arrays.copyOf(readBytes, length);
        receive(data);
    }
}
