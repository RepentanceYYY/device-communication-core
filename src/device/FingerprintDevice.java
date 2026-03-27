package device;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.utils.HexUtils;

import java.util.ArrayList;
import java.util.List;

public class FingerprintDevice extends DeviceCore implements IFrameProtocol {
    @Override
    public void setCommDispatcher(CommDispatcher commDispatcher) {
        super.setCommDispatcher(commDispatcher);
    }

    @Override
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        if (writeBytes.length < 12) return false;
        if (readBytes[9] != (byte) 0x00) return false;
        return super.isMatch(writeBytes, readBytes);
    }

    @Override
    public byte[] buildFullFrame(byte[] data) {
        return new byte[0];
    }

    public void readValidTemplateNumber() {
        String frameStr = "EF 01 FF FF FF FF 01 00 03 1D 00 21";
        byte[] frame = new byte[]{(byte) 0xEF, (byte) 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x1D, (byte) 0x00, (byte) 0x21};
        this.write(frame, this::readValidTemplateNumberCallback);
    }

    public void setLamplight() {
        String frameStr = "EF 01 FF FF FF FF 01 00 07 3C 03 00 07 00 00 4E";
        this.write(HexUtils.hexToBytes(frameStr), this::callback);
    }

    private void readValidTemplateNumberCallback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("读取有效指纹模板:");
        System.out.println("send-->" + HexUtils.bytesToHexString(writeBytes));
        System.out.println("receive-->" + HexUtils.bytesToHexString(readBytes));
    }

    public void empty() {
        String frameStr = "EF 01 FF FF FF FF 01 00 03 0D 00 11";
        byte[] frame = HexUtils.hexToBytes(frameStr);
        this.write(frame, this::callback);
    }

    public void readIndexTable() {
        String frameStr = "EF 01 FF FF FF FF 01 00 04 1F 00";
        int checkSumInt = calculateSum(HexUtils.hexToBytes("01 00 04 1F 00"));
        byte[] frame = HexUtils.hexToBytes(frameStr);
        byte[] fullFrame = new byte[13];
        for (int i = 0; i < frame.length; i++) {
            fullFrame[i] = frame[i];
        }
        fullFrame[11] = (byte) ((checkSumInt >> 8) & 0xFF);
        fullFrame[12] = (byte) (checkSumInt & 0xFF);
        this.write(fullFrame, this::readIndexTableCallback);
    }
    public void readIndexTableCallback(byte[] readBytes, byte[] writeBytes) {

        if (readBytes == null || readBytes.length < 44) {
            System.out.println("返回数据长度异常");
            return;
        }

        // 1️⃣ 校验包头
        if ((readBytes[0] & 0xFF) != 0xEF || (readBytes[1] & 0xFF) != 0x01) {
            System.out.println("包头错误");
            return;
        }

        // 2️⃣ 确认码
        int confirmCode = readBytes[9] & 0xFF;
        if (confirmCode != 0x00) {
            System.out.println("设备返回错误码: " + confirmCode);
            return;
        }

        // 3️⃣ 取索引信息（32字节）
        byte[] indexBytes = new byte[32];
        System.arraycopy(readBytes, 10, indexBytes, 0, 32);

        // 4️⃣ 解析已占用的指纹索引
        List<Integer> usedIndexList = new ArrayList<>();

        for (int byteIndex = 0; byteIndex < indexBytes.length; byteIndex++) {
            int value = indexBytes[byteIndex] & 0xFF;

            for (int bit = 0; bit < 8; bit++) {
                if ((value & (1 << bit)) != 0) {
                    int index = byteIndex * 8 + bit;
                    usedIndexList.add(index);
                }
            }
        }

        // 5️⃣ 打印结果
        System.out.println("已录入指纹索引: " + usedIndexList);

        // 6️⃣ （可选）找第一个空位
        int firstEmpty = -1;
        for (int i = 0; i < 256; i++) {
            if (!usedIndexList.contains(i)) {
                firstEmpty = i;
                break;
            }
        }

        System.out.println("第一个空闲索引: " + firstEmpty);
    }

    public static List<Integer> parseIndexTable(byte[] data) {
        List<Integer> result = new ArrayList<>();

        for (int byteIndex = 0; byteIndex < data.length; byteIndex++) {
            int value = data[byteIndex] & 0xFF;

            for (int bit = 0; bit < 8; bit++) {
                if ((value & (1 << bit)) != 0) {
                    int index = byteIndex * 8 + bit;
                    result.add(index);
                }
            }
        }

        return result;
    }

    /**
     * 计算指纹模块校验和
     *
     * @param data 完整的包字节数组
     * @return 计算出的 2 字节校验和
     */
    public int calculateSum(byte[] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += (data[i] & 0xFF);
        }
        return sum & 0xFFFF;
    }
}
