package device.core;

import device.core.factory.SerialFactory;
import device.core.factory.TcpClientFactory;
import device.core.factory.TcpServerFactory;
import device.core.factory.UdpFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通信调度器管理（全局静态工具类）
 *
 * <p>负责全局通信通道（Dispatcher）的生命周期管理、复用及线程安全调度。</p>
 *
 * <h3>支持的通信类型及地址格式规范：</h3>
 * <table border="1">
 * <tr>
 * <th>通信类型 (commType)</th>
 * <th>地址格式 (commAddress)</th>
 * <th>示例</th>
 * <th>说明</th>
 * </tr>
 * <tr>
 * <td>{@code "serial"}</td>
 * <td>{@code 串口号@波特率}</td>
 * <td>{@code "COM1@9600"}</td>
 * <td>波特率必须为正整数，相同串口号波特率必须相同</td>
 * </tr>
 * <tr>
 * <td>{@code "tcp"}</td>
 * <td>{@code IP地址:端口号}</td>
 * <td>{@code "192.168.1.100:8080"}</td>
 * <td>TCP 客户端模式，支持域名</td>
 * </tr>
 * <tr>
 * <td>{@code "udp"}</td>
 * <td>{@code IP地址:端口号}</td>
 * <td>{@code "192.168.1.100:8080"}</td>
 * <td>UDP 通信</td>
 * </tr>
 * <tr>
 * <td>{@code "tcpserver"}</td>
 * <td>{@code 监听IP:监听端口}</td>
 * <td>{@code "0.0.0.0:8888"}</td>
 * <td>TCP 服务端模式</td>
 * </tr>
 * </table>
 *
 * <p>注：{@code commType} 内部会自动转换为小写处理，大小写不敏感。</p>
 *
 * @author YourName
 * @since 2026/06/29
 */
public class CommDispatcherManager {

    private static final Map<String, CommFactory> factoryMap = new ConcurrentHashMap<>();
    private static final Map<String, CommDispatcher> dispatcherMap = new ConcurrentHashMap<>();

    static {
        registerFactory(new SerialFactory());
        registerFactory(new TcpClientFactory());
        registerFactory(new UdpFactory());
        registerFactory(new TcpServerFactory());
    }

    private CommDispatcherManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void registerFactory(CommFactory factory) {
        if (factory != null && factory.getCommType() != null) {
            factoryMap.put(factory.getCommType().toLowerCase(), factory);
        }
    }

    /**
     * 仅创建，不保存到 Map
     */
    public static CommDispatcher create(String commType, String commAddress) {
        if (commType == null) {
            throw new IllegalArgumentException("Communication type cannot be null");
        }

        String typeLower = commType.toLowerCase().trim();
        CommFactory factory = factoryMap.get(typeLower);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported communication type: " + commType);
        }

        // 防止同一个串口被赋予不同的波特率重复创建
        if ("serial".equals(typeLower)) {
            checkSerialPortConflict(commAddress);
        }

        // 执行该类型特有的格式校验
        factory.validate(commAddress);
        // 执行创建
        return factory.create(commAddress);
    }

    /**
     * 串口独有的冲突检测：提取串口号，检测是否有其他波特率已经占用了该物理端口
     */
    private static void checkSerialPortConflict(String commAddress) {
        if (commAddress == null || !commAddress.contains("@")) {
            return; // 格式不合法交由工厂的 validate 处理
        }

        // 提取当前的物理端口号，例如 "COM3@19200" -> "COM3"
        String currentPort = commAddress.split("@")[0].trim().toUpperCase();

        // 遍历当前运行中的调度器，查找潜在冲突
        for (String activeKey : dispatcherMap.keySet()) {
            // 确保是串口类型的 Key
            if (activeKey.startsWith("serial:")) {
                // 提取已存在的物理端口号
                String activeAddress = activeKey.substring("serial:".length());
                if (activeAddress.contains("@")) {
                    String activePort = activeAddress.split("@")[0].trim().toUpperCase();

                    // 如果物理端口相同，但整个地址字符串不同（说明波特率不同）
                    if (activePort.equals(currentPort) && !activeAddress.equalsIgnoreCase(commAddress.trim())) {
                        throw new IllegalStateException(String.format(
                                "物理端口冲突！串口 [%s] 已被 [%s] 占用，无法以 [%s] 重复创建通道！",
                                currentPort, activeAddress, commAddress.trim()
                        ));
                    }
                }
            }
        }
    }

    /**
     * 创建并添加
     */
    public static CommDispatcher createAndAdd(String commType, String commAddress) {
        String key = buildKey(commType, commAddress);

        return dispatcherMap.computeIfAbsent(key, k -> create(commType, commAddress));
    }

    public static CommDispatcher get(String commType, String commAddress) {
        return dispatcherMap.get(buildKey(commType, commAddress));
    }

    public static CommDispatcher getOrCreate(String commType, String commAddress) {
        return createAndAdd(commType, commAddress);
    }

    public static boolean isExist(String commType, String commAddress) {
        return dispatcherMap.containsKey(buildKey(commType, commAddress));
    }

    public static void remove(String commType, String commAddress) {
        String key = buildKey(commType, commAddress);
        CommDispatcher dispatcher = dispatcherMap.remove(key);
        if (dispatcher != null) {
            try {
                dispatcher.close();
            } catch (IOException e) {
                System.err.println("[CommDispatcherManager] 关闭通道失败 [" + key + "]: " + e.getMessage());
            }
        }
    }

    public static void shutdownAll() {
        for (Map.Entry<String, CommDispatcher> entry : dispatcherMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                System.err.println("[CommDispatcherManager] 关闭通道失败 [" + entry.getKey() + "]: " + e.getMessage());
            }
        }
        dispatcherMap.clear();
    }

    private static String buildKey(String commType, String commAddress) {
        if (commType == null || commAddress == null) {
            throw new IllegalArgumentException("commType and commAddress cannot be null");
        }
        return commType.toLowerCase().trim() + ":" + commAddress.trim().toLowerCase();
    }
}