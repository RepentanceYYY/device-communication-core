package device.dehumidifier;

import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.utils.CheckSumUtils;

/**
 * 除湿机设备
 */
public class DehumidifierDevice extends DeviceCore {
    // SM设备地址
    private final byte ADDRESS = (byte) 0x01;
    // 读运行参数功能码
    private final byte READ_RUN_PARAM = (byte) 0x03;


    public void query() {
        byte[] bytes = this.buildFullFrame(ADDRESS, READ_RUN_PARAM, new byte[]{(byte) 0x00, (byte) 0x03, (byte) 0x05, (byte) 0x05});
        this.write(bytes);
    }

    private byte[] buildFullFrame(byte address, byte functionCode, byte[] data) {
        // 定义总长度：地址(1) + 功能码(1) + 数据长度 + CRC(2)
        int dataLen = (data != null) ? data.length : 0;
        byte[] fullFrame = new byte[2 + dataLen + 2];

        // 组装头部
        fullFrame[0] = address;
        fullFrame[1] = functionCode;

        // 组装数据区
        if (dataLen > 0) {
            System.arraycopy(data, 0, fullFrame, 2, dataLen);
        }

        // 计算 CRC 校验码
        byte[] tmpFrame = new byte[2 + dataLen];
        System.arraycopy(fullFrame, 0, tmpFrame, 0, tmpFrame.length);
        byte[] modbusCRC16 = CheckSumUtils.getModbusCRC16(tmpFrame);

        // 组装 CRC
        System.arraycopy(modbusCRC16, 0, fullFrame, 2 + dataLen, 2);

        return fullFrame;
    }

}
