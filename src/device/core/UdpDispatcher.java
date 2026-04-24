package device.core;

import device.channel.UdpChannel;
import device.model.Task;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.util.Arrays;

public class UdpDispatcher extends CommDispatcher {
    public UdpDispatcher(UdpChannel udpChannel) {
        this.udpChannel = udpChannel;
        this.udpChannel.receiveEvent = this::channelReceiveEvent;
    }

    /**
     * Udp
     */
    private UdpChannel udpChannel;

    @Override
    public String getName() {
        return this.udpChannel.remoteHost + ":" + udpChannel.remotePort;
    }

    @Override
    public boolean isOpen() {
        return this.udpChannel.getIsOpen();
    }

    @Override
    public void open() throws IOException {
        this.udpChannel.open();
    }

    @Override
    public void close() throws IOException {
        this.udpChannel.close();
    }

    @Override
    public void write(Task task) throws IOException {
        this.udpChannel.send(task.getWriteBytes());
    }

    @Override
    public Charset getCharset() {
        return this.udpChannel.charset;
    }

    /**
     * 设备回调
     *
     * @param readBytes
     * @param length
     */
    private void channelReceiveEvent(DatagramPacket source, byte[] readBytes, int length) {
        byte[] data = Arrays.copyOf(readBytes, length);
        receive(data);
    }
}
