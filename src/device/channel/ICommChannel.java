package device.channel;

public interface ICommChannel {
    void start() throws Exception;
    void close() throws Exception;
    int send(byte[] bytes) throws Exception;
    boolean getIsOpen();
}
