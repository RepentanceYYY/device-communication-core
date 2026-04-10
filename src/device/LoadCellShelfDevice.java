package device;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.model.PositionData;
import device.utils.ByteUtils;
import device.utils.HexUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 称重货架
 */
public class LoadCellShelfDevice extends DeviceCore implements IFrameProtocol {
    @Override
    public void setCommDispatcher(CommDispatcher comm) {
        super.setCommDispatcher(comm);
        comm.responseTimeout = 1500;
    }

    /**
     * 批量读取数量
     */
    public void batchReadCount() {
        String frameStr = "F8 96 AC 17 01 00 00 0D 0A";
        byte[] frame = HexUtils.hexToBytes(frameStr);
        this.write(frame, this::batchReadCountCallback);
    }

    private void batchReadCountCallback(byte[] readBytes, byte[] writeBytes) {
        int dataPackageLength = readBytes[6] & 0xFF;
        byte[] data = ByteUtils.slice(readBytes, 7, 7 + dataPackageLength);

        System.out.println("send-->" + HexUtils.bytesToHexString(writeBytes));
        System.out.println("receive-->" + HexUtils.bytesToHexString(readBytes));
        System.out.println("数据包：" + HexUtils.bytesToHexString(data));

        List<PositionData> list = new ArrayList<>();

        for (int i = 0; i < data.length; i += 3) {

            int low = data[i] & 0xFF;
            int high = data[i + 1] & 0xFF;

            int count = (high << 8) | low;   // 小端合并

            int status = data[i + 2] & 0xFF;

            PositionData p = new PositionData();
            p.count = count;
            p.status = status;

            list.add(p);
        }

        // debug输出
        for (int i = 0; i < list.size(); i++) {
            PositionData p = list.get(i);
            System.out.println("位置" + i +
                    " 数量=" + p.count +
                    " 状态=" + formatStatus(p.status));
        }

    }

    /**
     * 设置下位机地址
     *
     * @param startAddress
     * @param endAddress
     */
    public void setSlaveDeviceAddress(int startAddress, int endAddress) {
        String startAddressHexStr = HexUtils.toHexByteFast(startAddress);
        String endAddressHexStr = HexUtils.toHexByteFast(endAddress);
        String frameStr = "F8 96 AC 17 02 00 02 " + startAddressHexStr + " " + endAddressHexStr + " 0D 0A";
        this.write(HexUtils.hexToBytes(frameStr));
    }

    /**
     * 模式
     * 0：透传
     * 1：中继
     *
     * @param model
     */
    public void switchModel(int model) {
        if (model != 0 && model != 1) {
            model = 1;
        }
        String modelHexStr = HexUtils.toHexByteFast(model);
        String frameStr = "F8 96 AC 17 09 00 01 " + modelHexStr + " 0D 0A";
        byte[] frame = HexUtils.hexToBytes(frameStr);
        this.write(frame, this::switchModelCallback);
    }

    public void switchModelCallback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("模式切换成功");
    }

    /**
     * 读取范围和模式
     */
    public void readAddressAndModel() {
        String frameStr = "F8 96 AC 17 0A 00 00 0D 0A";
        byte[] frame = HexUtils.hexToBytes(frameStr);
        this.write(frame, this::readAddressAndModelCallback);
    }

    /**
     * 读取地址范围和模式回调
     *
     * @param readBytes
     * @param writeBytes
     */
    private void readAddressAndModelCallback(byte[] readBytes, byte[] writeBytes) {
        byte[] dataPackage = ByteUtils.slice(readBytes, 7, readBytes.length - 2);
        int startAddress = dataPackage[0];
        int endAddress = dataPackage[1];
        int model = dataPackage[2];
        String message = "读开始结束地址,模式结果: 10进制开始地址: " + startAddress
                + ", 结束地址: " + endAddress
                + ", 模式: " + (model == 0 ? "透传模式" : "中继模式");

        System.out.println(message);
    }

    /**
     * 停用启用
     *
     * @param addressHex 字符串格式Hex地址
     * @param enabled    true=启用；false=停用
     */
    public void setEnabled(String addressHex, Boolean enabled) {
        String status = enabled ? "01" : "00";
        String frameASCII = addressHex + "work " + status;
        this.switchModel(0);
        this.write(frameASCII, 8000000L);
        this.switchModel(1);
    }


    @Override
    public boolean validate(byte[] readBytes) {
        if (readBytes == null || readBytes.length < 2) {
            return false;
        }
        return true;

//        return true;
    }

    @Override
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        if (this.transparentModeIsMatch(writeBytes, readBytes)) return true;
        if (this.relayModelIsMatch(writeBytes, readBytes)) return true;
        return false;

    }

    /**
     * 中继模式的校验
     *
     * @param writeBytes
     * @param readBytes
     * @return
     */
    private boolean relayModelIsMatch(byte[] writeBytes, byte[] readBytes) {
        try {
            if (readBytes == null || readBytes.length < 9) {
                return false;
            }
            // 校验识别码是否正确(0xF8 0x96 0xAC 0x17)
            if (readBytes[0] != (byte) 0xF8 || readBytes[1] != (byte) 0x96
                    || readBytes[2] != (byte) 0xAC || readBytes[3] != (byte) 0x17) return false;
            // 校验命令码是否相同以及状态码是否正确
            if (writeBytes[4] != readBytes[4]) return false;

            if (readBytes[5] != (byte) 0x00) {
                System.out.println("响应:" + HexUtils.bytesToHexString(writeBytes) + " 错误码为:" + HexUtils.toHexByteFast(readBytes[5]));
                return false;
            }
            return super.isMatch(writeBytes, readBytes);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 透传模式的校验
     *
     * @param writeBytes
     * @param readBytes
     * @return
     */
    private boolean transparentModeIsMatch(byte[] writeBytes, byte[] readBytes) {

        try {
            byte[] address = new byte[]{writeBytes[0], writeBytes[1]};
            String addressHex = new String(address, Charset.forName("GB2312"));
            if (addressHex.equals(HexUtils.byteToHex(readBytes[0]))) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public byte[] buildFullFrame(byte[] data) {
        return new byte[0];
    }

    private String formatStatus(int status) {
        return switch (status) {
            case 0x00 -> "正常";
            case 0x01 -> "停用";
            case 0x02 -> "超重";
            case 0xFF -> "未读取";
            default -> "未知";
        };
    }
}
