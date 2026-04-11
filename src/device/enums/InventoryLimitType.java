package device.enums;

/**
 * 库存上下限
 */
public enum InventoryLimitType {
    MAX("limup", "上限"),
    MIN("limdn", "下限");

    public final String cmd;    // 指令关键字
    public final String desc;   // 描述文字

    InventoryLimitType(String cmd, String desc) {
        this.cmd = cmd;
        this.desc = desc;
    }
}
