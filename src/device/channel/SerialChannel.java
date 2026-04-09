package device.channel;

import com.fazecast.jSerialComm.SerialPort;
import device.channel.CommChannel;

import java.io.IOException;

/**
 * 基于串口的具体通道
 */
public class SerialChannel extends CommChannel<SerialPort,SerialPort> {
    public SerialChannel(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    /**
     * 串口名
     */
    private String portName;

    public String getPortName() {
        return portName;
    }

    /**
     * 波特率
     */
    private int baudRate;
    /**
     * 读取数据的线程
     */
    private Thread readThread;
    /**
     * 静默检测时间，单位毫秒
     */
    private int idleTimeout = 20;

    /**
     * 是否打开了链接
     */
    private boolean isOpen;
    /**
     * 串口
     */
    private SerialPort serialPort;
    @Override
    public boolean getIsOpen() {
        return isOpen && serialPort != null && serialPort.isOpen();
    }

    public void open() throws IOException {
        if (isOpen) return;

        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        // 设置超时模式：非阻塞
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

        if (serialPort.openPort()) {
            this.isOpen = true;
            startReadThread();
        } else {
            throw new IOException("无法打开串口: " + portName);
        }
    }

    private void startReadThread() {
        readThread = new Thread(() -> {
            while (isOpen && !Thread.currentThread().isInterrupted()) {
                try {
                    int available = serialPort.bytesAvailable();
                    if (available > 0) {
                        int lastSize;
                        do {
                            lastSize = available;
                            Thread.sleep(idleTimeout);
                            available = serialPort.bytesAvailable();
                        } while (available != lastSize); // 如果还在增加，继续等

                        // 数据稳定了，一次性读取
                        byte[] readBuffer = new byte[available];
                        int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                        onReceiveEvent(serialPort,readBuffer, numRead);
                    } else {
                        Thread.sleep(50); // 没数据时降低 CPU 占用
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Serial-Read-Thread");
        readThread.setDaemon(true);
        readThread.start();
    }

    public void send(byte[] data) {
        if (getIsOpen()) {
            serialPort.writeBytes(data, data.length);
        }
    }

    public void close() {
        this.isOpen = false;
        if (readThread != null) readThread.interrupt();
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        triggerClose(this.serialPort);
        closeEventListeners.clear();
        serialPort = null;
    }
}
