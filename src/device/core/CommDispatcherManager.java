package device.core;

import device.core.factory.SerialFactory;
import device.core.factory.TcpClientFactory;
import device.core.factory.TcpServerFactory;
import device.core.factory.UdpFactory;

import java.io.IOException;
import java.util.Iterator;
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

    /**
     * 通信工厂注册表
     * Key：通信类型（如 serial、tcp、udp、tcpserver）
     * Value：对应的通信工厂，用于创建 Dispatcher 实例。
     */
    private static final Map<String, CommFactory> factoryMap = new ConcurrentHashMap<>();

    /**
     * 全局通信调度器缓存
     * Key：通信类型:通信地址（如 serial:COM3@9600）
     * Value：对应的通信调度器实例。
     * <p>
     * 相同通信链路全局仅维护一个 Dispatcher，
     * 多个设备共享同一 Dispatcher，实现连接复用。
     */
    private static final Map<String, CommDispatcher> dispatcherMap = new ConcurrentHashMap<>();

    static {
        registerFactory(new SerialFactory());
        registerFactory(new TcpClientFactory());
        registerFactory(new UdpFactory());
        registerFactory(new TcpServerFactory());
    }

    /**
     * 构造函数私有化
     */
    private CommDispatcherManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 注册通信工厂
     *
     * @param factory
     */
    public static void registerFactory(CommFactory factory) {
        if (factory != null && factory.getCommType() != null) {
            factoryMap.put(factory.getCommType().toLowerCase(), factory);
        }
    }

    /**
     * 创建一个新的通信调度器实例，但不会注册到全局管理器。
     * <p>
     * 该方法每次调用都会创建新的 {@link CommDispatcher} 实例，
     * 不会缓存到全局 Dispatcher Map，也不会复用已有实例。
     * 如需获取全局共享的通信调度器，请使用 {@link #getOrCreate(String, String)}。
     * </p>
     *
     * @param commType    通信类型（serial、tcp、udp、tcpserver）
     * @param commAddress 通信地址
     * @return 新创建的通信调度器实例
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
     * 检查串口通信地址是否与已存在的通信链路发生冲突。
     * <p>
     * 同一个物理串口在同一时刻只能使用一种波特率，因此禁止为同一串口创建
     * 不同波特率的通信调度器。例如：
     * <ul>
     *     <li>COM3@9600 和 COM3@9600：允许（复用同一调度器）</li>
     *     <li>COM3@9600 和 COM3@19200：禁止（波特率冲突）</li>
     * </ul>
     * </p>
     *
     * @param commAddress 串口通信地址（格式：串口号@波特率）
     * @throws IllegalStateException 当检测到同一物理串口使用不同波特率时抛出
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
     * 获取指定通信链路对应的通信调度器。
     * <p>
     * 如果该通信链路对应的调度器已存在，则直接返回；
     * 如果不存在，则创建新的调度器并缓存到全局管理器中。
     * </p>
     *
     * @param commType    通信类型（serial、tcp、udp、tcpserver）
     * @param commAddress 通信地址
     * @return 全局唯一的通信调度器实例
     */
    public static CommDispatcher createAndAdd(String commType, String commAddress) {
        String key = buildKey(commType, commAddress);

        return dispatcherMap.computeIfAbsent(key, k -> create(commType, commAddress));
    }

    /**
     * 获取指定通信链路对应的通信调度器。
     *
     * @param commType    通信类型（serial、tcp、udp、tcpserver）
     * @param commAddress 通信地址
     * @return 如果存在则返回对应的通信调度器，否则返回 {@code null}
     */
    public static CommDispatcher get(String commType, String commAddress) {
        return dispatcherMap.get(buildKey(commType, commAddress));
    }

    /**
     * 获取指定通信链路对应的通信调度器。
     * <p>
     * 如果该通信链路对应的调度器已存在，则直接返回；
     * 如果不存在，则创建新的调度器并缓存到全局管理器中。
     * </p>
     *
     * @param commType    通信类型（serial、tcp、udp、tcpserver）
     * @param commAddress 通信地址
     * @return 全局唯一的通信调度器实例
     */
    public static CommDispatcher getOrCreate(String commType, String commAddress) {
        return createAndAdd(commType, commAddress);
    }

    /**
     * 判断指定通信链路对应的通信调度器是否已存在于全局管理器中。
     *
     * @param commType    通信类型（serial、tcp、udp、tcpserver）
     * @param commAddress 通信地址
     * @return {@code true} 表示已存在，否则返回 {@code false}
     */
    public static boolean isExist(String commType, String commAddress) {
        return dispatcherMap.containsKey(buildKey(commType, commAddress));
    }

    /**
     * 从全局管理器中移除指定通信链路对应的通信调度器，并释放相关资源。
     * <p>
     * 该方法会清空当前通信链路上挂载的所有设备，关闭底层通信连接，
     * 并将对应的通信调度器从全局缓存中移除。
     * </p>
     *
     * @param commType    通信类型（serial、tcp、udp、tcpserver）
     * @param commAddress 通信地址
     */
    public static void remove(String commType, String commAddress) {
        String key = buildKey(commType, commAddress);
        CommDispatcher dispatcher = dispatcherMap.remove(key);
        if (dispatcher != null) {
            try {
                dispatcher.clearDevices();
                dispatcher.close();
            } catch (IOException e) {
                System.err.println("[CommDispatcherManager] 关闭通道失败 [" + key + "]: " + e.getMessage());
            }
        }
    }

    /**
     * 从全局管理器中移除指定通信调度器，并释放相关资源。
     * <p>
     * 该方法会查找与指定 Dispatcher 对应的缓存项，
     * 清空其挂载的所有设备、关闭底层通信连接，并将其从全局缓存中移除。
     * </p>
     *
     * @param dispatcher 待移除的通信调度器
     */
    public static void remove(CommDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        //remove(dispatcher) 有“弱一致删除 + IO副作用”问题
        for (Iterator<Map.Entry<String, CommDispatcher>> it = dispatcherMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, CommDispatcher> entry = it.next();

            if (entry.getValue() == dispatcher) {
                try {
                    dispatcher.clearDevices();
                    dispatcher.close();
                } catch (IOException e) {
                }
                it.remove();
                break;
            }
        }
    }


    /**
     * 关闭并释放所有通信调度器。
     * <p>
     * 该方法会关闭所有底层通信连接，清空各通信链路上挂载的设备，
     * 并移除全局管理器中缓存的所有通信调度器。
     * 通常在应用关闭或框架销毁时调用。
     * </p>
     */
    public static void shutdownAll() {
        for (Map.Entry<String, CommDispatcher> entry : dispatcherMap.entrySet()) {
            try {
                CommDispatcher value = entry.getValue();
                value.clearDevices();
                value.close();
            } catch (IOException e) {
                System.err.println("[CommDispatcherManager] 关闭通道失败 [" + entry.getKey() + "]: " + e.getMessage());
            }
        }
        dispatcherMap.clear();
    }

    /**
     * 构建通信调度器在全局管理器中的唯一标识。
     * <p>
     * Key 格式为：{@code 通信类型:通信地址}，
     * 例如：{@code serial:COM3@9600}、{@code tcp:192.168.1.100:8080}。
     * 为保证唯一性，通信类型和通信地址都会进行去除首尾空格及统一小写处理。
     * </p>
     *
     * @param commType    通信类型
     * @param commAddress 通信地址
     * @return 通信调度器唯一标识
     * @throws IllegalArgumentException 当通信类型或通信地址为空时抛出
     */
    private static String buildKey(String commType, String commAddress) {
        if (commType == null || commAddress == null) {
            throw new IllegalArgumentException("commType and commAddress cannot be null");
        }
        return commType.toLowerCase().trim() + ":" + commAddress.trim().toLowerCase();
    }
}