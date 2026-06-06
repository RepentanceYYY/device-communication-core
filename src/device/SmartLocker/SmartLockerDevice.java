package device.SmartLocker;

import device.core.DeviceCore;
import device.utils.HexUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 千鸣锁控板设备控制类
 * 协议：RS485，帧格式：0x5A + func + data + XOR校验
 * 功能：开箱、查询箱门开关状态、查询存物状态、设置箱号范围
 * <p>
 * 模拟模式：通过 setSimulationMode(true) 开启，所有操作返回模拟数据，不依赖真实硬件。
 * 真实模式：需要正确配置 CommDispatcher 并调用 open() 打开硬件连接。
 */
public class SmartLockerDevice extends DeviceCore {

    private static final byte HEAD = (byte) 0x5A;
    private static final byte FUNC_OPEN_BOX = (byte) 0x21;
    private static final byte FUNC_QUERY_STATUS = (byte) 0x22;
    private static final byte FUNC_SET_BOX_RANGE = (byte) 0x23;
    private static final byte FUNC_QUERY_GOODS = (byte) 0x25;
    private static final byte RESP_OPEN_BOX = (byte) 0xA1;
    private static final byte RESP_QUERY_STATUS = (byte) 0xA2;
    private static final byte RESP_SET_BOX_RANGE = (byte) 0xA3;
    private static final byte RESP_QUERY_GOODS = (byte) 0xA5;

    private boolean simulationMode;          // 默认 false，由外部通过 setter 注入（配置文件 lock.simulation.mode）
    private final Random random = new Random();

    /**
     * 设置模拟模式，true=使用模拟数据，false=真实硬件通信
     */
    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
        System.out.println("[设备] 模拟模式已" + (simulationMode ? "开启" : "关闭"));
    }

    public boolean isSimulationMode() {
        return simulationMode;
    }

    /**
     * 重写 open 方法：模拟模式下直接返回，不打开硬件连接
     */
    @Override
    public void open() throws IOException {
        if (simulationMode) {
            System.out.println("[模拟模式] 跳过打开硬件");
            return;
        }
        super.open();
    }

    /**
     * 重写 close 方法：模拟模式下直接返回，不关闭硬件连接
     */
    @Override
    public void close() throws IOException {
        if (simulationMode) {
            System.out.println("[模拟模式] 跳过关闭硬件");
            return;
        }
        super.close();
    }

    // ======================== 异步 API ========================

    public void openBox(int boxNo, BiConsumer<Boolean, String> callback) {
        if (simulationMode) {
            boolean success = simulateOpenBox(boxNo);
            String msg = success ? "开箱成功(模拟)" : "开箱失败(模拟)";
            if (callback != null) callback.accept(success, msg);
            return;
        }
        byte[] frame = buildFrame(FUNC_OPEN_BOX, intToTwoBytes(boxNo));
        write(frame, (readBytes, writeBytes) -> {
            boolean success = parseOpenBoxResponse(readBytes);
            String msg = success ? "开箱成功" : "开箱失败";
            if (callback != null) callback.accept(success, msg);
        });
    }

    public void queryBoxStatus(Consumer<BoxStatusData> callback) {
        if (simulationMode) {
            BoxStatusData mockData = simulateBoxStatus();
            if (callback != null) callback.accept(mockData);
            return;
        }
        byte[] frame = buildFrame(FUNC_QUERY_STATUS, new byte[0]);
        write(frame, (readBytes, writeBytes) -> {
            BoxStatusData data = parseBoxStatusResponse(readBytes);
            if (callback != null) callback.accept(data);
        });
    }

    public void queryGoodsStatus(Consumer<BoxGoodsData> callback) {
        if (simulationMode) {
            BoxGoodsData mockData = simulateGoodsStatus();
            if (callback != null) callback.accept(mockData);
            return;
        }
        byte[] frame = buildFrame(FUNC_QUERY_GOODS, new byte[0]);
        write(frame, (readBytes, writeBytes) -> {
            BoxGoodsData data = parseGoodsStatusResponse(readBytes);
            if (callback != null) callback.accept(data);
        });
    }

    public void setBoxRange(int startBox, int endBox, Consumer<Boolean> callback) {
        if (simulationMode) {
            boolean success = simulateSetBoxRange(startBox, endBox);
            if (callback != null) callback.accept(success);
            return;
        }
        byte[] data = new byte[4];
        System.arraycopy(intToTwoBytes(startBox), 0, data, 0, 2);
        System.arraycopy(intToTwoBytes(endBox), 0, data, 2, 2);
        byte[] frame = buildFrame(FUNC_SET_BOX_RANGE, data);
        write(frame, (readBytes, writeBytes) -> {
            boolean success = parseSetRangeResponse(readBytes);
            if (callback != null) callback.accept(success);
        });
    }

    // ======================== 同步 API ========================

    public boolean openBoxSync(int boxNo, long timeout) throws Exception {
        if (simulationMode) {
            return simulateOpenBox(boxNo);
        }
        byte[] frame = buildFrame(FUNC_OPEN_BOX, intToTwoBytes(boxNo));
        Boolean result = writeSync(frame, 3, timeout, (readBytes, writeBytes) -> parseOpenBoxResponse(readBytes));
        return result != null && result;
    }

    public BoxStatusData queryBoxStatusSync(long timeout) throws Exception {
        if (simulationMode) {
            return simulateBoxStatus();
        }
        byte[] frame = buildFrame(FUNC_QUERY_STATUS, new byte[0]);
        return writeSync(frame, 3, timeout, (readBytes, writeBytes) -> parseBoxStatusResponse(readBytes));
    }

    public BoxGoodsData queryGoodsStatusSync(long timeout) throws Exception {
        if (simulationMode) {
            return simulateGoodsStatus();
        }
        byte[] frame = buildFrame(FUNC_QUERY_GOODS, new byte[0]);
        return writeSync(frame, 3, timeout, (readBytes, writeBytes) -> parseGoodsStatusResponse(readBytes));
    }

    public boolean setBoxRangeSync(int startBox, int endBox, long timeout) throws Exception {
        if (simulationMode) {
            return simulateSetBoxRange(startBox, endBox);
        }
        byte[] data = new byte[4];
        System.arraycopy(intToTwoBytes(startBox), 0, data, 0, 2);
        System.arraycopy(intToTwoBytes(endBox), 0, data, 2, 2);
        byte[] frame = buildFrame(FUNC_SET_BOX_RANGE, data);
        Boolean result = writeSync(frame, 3, timeout, (readBytes, writeBytes) -> parseSetRangeResponse(readBytes));
        return result != null && result;
    }

    // ======================== 模拟数据生成 ========================

    private boolean simulateOpenBox(int boxNo) {
        System.out.println("[模拟] 开启箱门 " + boxNo);
        if (boxNo >= 1 && boxNo <= 100) return true;
        return random.nextBoolean();
    }

    private BoxStatusData simulateBoxStatus() {
        int startBox = 1;
        int endBox = 16;
        int byteCount = (endBox - startBox + 7) / 8;
        byte[] stateBits = new byte[byteCount];
        for (int i = startBox; i <= endBox; i++) {
            if (i % 2 == 1) {
                int offset = i - startBox;
                int byteIdx = offset / 8;
                int bitIdx = offset % 8;
                stateBits[byteIdx] |= (1 << bitIdx);
            }
        }
        System.out.println("[模拟] 生成箱门开关状态数据");
        return new BoxStatusData(startBox, endBox, stateBits);
    }

    private BoxGoodsData simulateGoodsStatus() {
        int startBox = 1;
        int endBox = 24;
        int byteCount = (endBox - startBox + 7) / 8;
        byte[] goodsBits = new byte[byteCount];
        for (int i = startBox; i <= endBox; i++) {
            if (i <= 12) {
                int offset = i - startBox;
                int byteIdx = offset / 8;
                int bitIdx = offset % 8;
                goodsBits[byteIdx] |= (1 << bitIdx);
            }
        }
        System.out.println("[模拟] 生成存物状态数据");
        return new BoxGoodsData(startBox, endBox, goodsBits);
    }

    private boolean simulateSetBoxRange(int startBox, int endBox) {
        System.out.println("[模拟] 设置箱号范围: " + startBox + " ~ " + endBox);
        return true;
    }

    // ======================== 协议辅助方法 ========================

    private byte[] buildFrame(byte func, byte[] data) {
        int len = 1 + 1 + data.length + 1;
        byte[] frame = new byte[len];
        frame[0] = HEAD;
        frame[1] = func;
        System.arraycopy(data, 0, frame, 2, data.length);
        byte xor = 0;
        for (int i = 0; i < len - 1; i++) xor ^= frame[i];
        frame[len - 1] = xor;
        return frame;
    }

    private byte[] intToTwoBytes(int value) {
        return new byte[]{(byte) ((value >> 8) & 0xFF), (byte) (value & 0xFF)};
    }

    private int twoBytesToInt(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    private boolean parseOpenBoxResponse(byte[] response) {
        if (response == null || response.length < 4) return false;
        if (response[0] != HEAD || response[1] != RESP_OPEN_BOX) return false;
        return response[2] == 0;
    }

    private BoxStatusData parseBoxStatusResponse(byte[] response) {
        if (response == null || response.length < 6) return null;
        if (response[0] != HEAD || response[1] != RESP_QUERY_STATUS) return null;
        int startBox = twoBytesToInt(response[2], response[3]);
        int endBox = twoBytesToInt(response[4], response[5]);
        int stateLen = response.length - 6 - 1;
        if (stateLen < 0) return null;
        byte[] stateBits = new byte[stateLen];
        System.arraycopy(response, 6, stateBits, 0, stateLen);
        return new BoxStatusData(startBox, endBox, stateBits);
    }

    private BoxGoodsData parseGoodsStatusResponse(byte[] response) {
        if (response == null || response.length < 6) return null;
        if (response[0] != HEAD || response[1] != RESP_QUERY_GOODS) return null;
        int startBox = twoBytesToInt(response[2], response[3]);
        int endBox = twoBytesToInt(response[4], response[5]);
        int stateLen = response.length - 6 - 1;
        byte[] goodsBits = new byte[stateLen];
        System.arraycopy(response, 6, goodsBits, 0, stateLen);
        return new BoxGoodsData(startBox, endBox, goodsBits);
    }

    private boolean parseSetRangeResponse(byte[] response) {
        if (response == null || response.length < 4) return false;
        if (response[0] != HEAD || response[1] != RESP_SET_BOX_RANGE) return false;
        return response[2] == 0;
    }

    // ======================== 帧拆解与匹配（真实模式使用） ========================

    @Override
    protected List<byte[]> splitFrames(ByteArrayOutputStream buffer) {
        List<byte[]> frames = new ArrayList<>();
        byte[] all = buffer.toByteArray();
        int start = 0;
        while (start < all.length) {
            if (all[start] != HEAD) {
                start++;
                continue;
            }
            int frameEnd = -1;
            for (int len = 3; len <= 64 && start + len <= all.length; len++) {
                byte xor = 0;
                for (int i = start; i < start + len - 1; i++) xor ^= all[i];
                if (xor == all[start + len - 1]) {
                    frameEnd = start + len;
                    break;
                }
            }
            if (frameEnd != -1) {
                byte[] frame = new byte[frameEnd - start];
                System.arraycopy(all, start, frame, 0, frame.length);
                frames.add(frame);
                start = frameEnd;
            } else break;
        }
        if (start < all.length) {
            byte[] remaining = new byte[all.length - start];
            System.arraycopy(all, start, remaining, 0, remaining.length);
            buffer.reset();
            try {
                buffer.write(remaining);
            } catch (Exception ignored) {
            }
        } else buffer.reset();
        return frames;
    }

    @Override
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        if (readBytes == null || readBytes.length < 2) return false;
        if (readBytes[0] != HEAD) return false;
        byte sendFunc = writeBytes[1];
        byte expectedRespFunc = (byte) (sendFunc | 0x80);
        return readBytes[1] == expectedRespFunc;
    }

    @Override
    public boolean validate(byte[] readBytes) {
        if (readBytes == null || readBytes.length < 3) return false;
        if (readBytes[0] != HEAD) return false;
        byte xor = 0;
        for (int i = 0; i < readBytes.length - 1; i++) xor ^= readBytes[i];
        return xor == readBytes[readBytes.length - 1];
    }

    // ======================== 内部数据类 ========================

    public static class BoxStatusData {
        public final int startBox;
        public final int endBox;
        public final byte[] stateBits;

        public BoxStatusData(int startBox, int endBox, byte[] stateBits) {
            this.startBox = startBox;
            this.endBox = endBox;
            this.stateBits = stateBits;
        }

        public boolean isOpen(int boxNo) {
            if (boxNo < startBox || boxNo > endBox) return false;
            int offset = boxNo - startBox;
            int byteIdx = offset / 8;
            int bitIdx = offset % 8;
            if (byteIdx >= stateBits.length) return false;
            return ((stateBits[byteIdx] >> bitIdx) & 0x01) == 1;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = startBox; i <= endBox; i++)
                sb.append(i).append(":").append(isOpen(i) ? "开" : "关").append(" ");
            return sb.toString();
        }
    }

    public static class BoxGoodsData {
        public final int startBox;
        public final int endBox;
        public final byte[] goodsBits;

        public BoxGoodsData(int startBox, int endBox, byte[] goodsBits) {
            this.startBox = startBox;
            this.endBox = endBox;
            this.goodsBits = goodsBits;
        }

        public boolean hasGoods(int boxNo) {
            if (boxNo < startBox || boxNo > endBox) return false;
            int offset = boxNo - startBox;
            int byteIdx = offset / 8;
            int bitIdx = offset % 8;
            if (byteIdx >= goodsBits.length) return false;
            return ((goodsBits[byteIdx] >> bitIdx) & 0x01) == 1;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = startBox; i <= endBox; i++)
                sb.append(i).append(":").append(hasGoods(i) ? "有物" : "无物").append(" ");
            return sb.toString();
        }
    }
}
