package device;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.utils.ByteUtils;
import device.utils.HexUtils;

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
        String frameStr = "F8 96 AC 17 01 00 0D 0A";
        byte[] frame = HexUtils.hexToBytes(frameStr);
        this.write(frame, this::callback);
    }

    private void batchReadCountCallback(byte[] readBytes, byte[] writeBytes) {

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
        String frameStr = "F8 96 AC 17 09 01 " + modelHexStr + " 0D 0A";
        byte[] frame = HexUtils.hexToBytes(frameStr);
        this.write(frame, this::callback);
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

    @Override
    public boolean validate(byte[] readBytes) {
        if (readBytes == null || readBytes.length < 9) {
            return false;
        }
        // 校验识别码(0xF8 0x96 0xAC 0x17)
        if (readBytes[0] != (byte) 0xF8 || readBytes[1] != (byte) 0x96
                || readBytes[2] != (byte) 0xAC || readBytes[3] != (byte) 0x17) return false;
        return true;
    }

    @Override
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        try {
            // 校验命令码是否相同以及状态码是否正确
            if (writeBytes[4] != readBytes[4] || readBytes[5] != (byte) 0x00) return false;
            return super.isMatch(writeBytes, readBytes);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public byte[] buildFullFrame(byte[] data) {
        return new byte[0];
    }
}
