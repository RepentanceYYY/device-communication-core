package device.core;

import device.channel.TcpChannel;
import device.model.Task;

import java.io.IOException;
import java.util.Arrays;

public class TcpDispatcher extends CommDispatcher {
    public TcpDispatcher(TcpChannel tcpChannel) {
        this.tcpChannel = tcpChannel;
        this.tcpChannel.receiveEvent = this::channelReceiveEvent;
    }

    /**
     * Tcp客户端
     */
    private TcpChannel tcpChannel;

    @Override
    public String getName() {
        return this.tcpChannel.address.toString();
    }

    @Override
    public boolean isOpen() {
        return tcpChannel.getIsOpen();
    }

    @Override
    public void open() throws IOException {
        tcpChannel.start(3);
    }

    @Override
    public void close() throws IOException {
        tcpChannel.close();
    }

    @Override
    public void write(Task task) throws IOException {
        tcpChannel.send(task.getWriteBytes());
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
