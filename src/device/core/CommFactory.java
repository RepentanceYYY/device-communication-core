package device.core;

public interface CommFactory {
    /**
     * 获取当前工厂支持的通信类型名称，目前仅支持:"serial", "tcp","udp","tcpserver"
     */
    String getCommType();

    /**
     * 校验通信地址是否合法
     *
     * @param commAddress 通信地址
     * @throws IllegalArgumentException 如果校验失败抛出此异常
     */
    void validate(String commAddress);

    /**
     * 创建对应的调度器
     *
     * @param commAddress 通信地址
     * @return 调度器实例
     */
    CommDispatcher create(String commAddress);
}
