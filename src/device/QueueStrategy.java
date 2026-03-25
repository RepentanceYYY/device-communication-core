package device;

/**
 * 队列策略
 */
public enum QueueStrategy {
    /**
     * 高优先级策略：插队执行
     * 内部对应 PriorityQueue
     */
    PRIORITY,

    /**
     * 普通顺序策略：先进先出
     * 内部对应 ConcurrentLinkedQueue
     */
    SEQUENTIAL
}
