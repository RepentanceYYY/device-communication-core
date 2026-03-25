package device;

import java.io.IOException;
import java.util.Arrays;

public class ConnectTcpClient extends ConnectBase {
    public ConnectTcpClient(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
        this.tcpClient.receiveEvent = this::clientReceiveEvent;
    }

    /**
     * Tcp客户端
     */
    private TcpClient tcpClient;

    @Override
    public String getName() {
        return this.tcpClient.address.toString();
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void open() throws IOException {
        tcpClient.start(3);
    }

    @Override
    public void close() throws IOException {
        tcpClient.close();
    }

    @Override
    public void write(DeviceActionModel deviceActionModel) throws IOException {
        tcpClient.sendMessage(deviceActionModel.getWriteBytes());
    }

    /**
     * 设备回调
     *
     * @param readBytes
     * @param length
     */
    private void clientReceiveEvent(byte[] readBytes, int length) {
        byte[] data = Arrays.copyOf(readBytes, length);
        receive(data);
    }
}
