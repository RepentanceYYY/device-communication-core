package device.core;

import com.fazecast.jSerialComm.SerialPort;
import device.channel.SerialChannel;
import device.model.Task;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class SerialDispatcher extends CommDispatcher {
    public SerialDispatcher(SerialChannel serialChannel) {
        this.serialChannel = serialChannel;
        this.serialChannel.receiveEvent = this::channelReceiveEvent;
    }

    private SerialChannel serialChannel;

    @Override
    public String getName() {
        return this.serialChannel.getPortName();
    }

    @Override
    public boolean isOpen() {
        return this.serialChannel.getIsOpen();
    }

    @Override
    public void open() throws IOException {
        this.serialChannel.open();
    }

    @Override
    public void close() throws IOException {
        this.serialChannel.close();
    }

    @Override
    public void write(Task task) throws IOException {
        this.serialChannel.send(task.getWriteBytes());
    }

    @Override
    public Charset getCharset() {
        return this.serialChannel.charset;
    }

    /**
     * 设备回调
     *
     * @param readBytes
     * @param length
     */
    private void channelReceiveEvent(SerialPort source, byte[] readBytes, int length) {
        byte[] data = Arrays.copyOf(readBytes, length);
        receive(data);
    }
}
