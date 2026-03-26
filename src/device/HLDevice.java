package device;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.enums.DispatchMode;
import device.utils.CheckSumUtils;
import device.utils.HexUtils;

public class HLDevice extends DeviceCore implements IFrameProtocol {
    @Override
    public void setCommDispatcher(CommDispatcher commDispatcher) {
        super.setCommDispatcher(commDispatcher);
        commDispatcher.responseTimeout = 1500;
    }

    @Override
    public boolean validate(byte[] readBytes) {
        return super.validate(readBytes);
    }

    public void openLockOnlyOne(int address, int index) {
        this.write(DispatchMode.PRIORITY, this.getOpenLockOnlyOne(address, index), 1, 0, this::openLockOnlyOneCallback);
    }

    public void openLockOnlyOneCallback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("开锁回调:");
        System.out.println("send-->" + HexUtils.bytesToHexString(readBytes));
        System.out.println("receive-->" + HexUtils.bytesToHexString(writeBytes));
    }

    /**
     * 获取打开单个格子锁byte数组
     *
     * @param address 锁板地址
     * @param index   格口号
     * @return
     */
    public byte[] getOpenLockOnlyOne(int address, int index) {
        byte[] data = {0x00, 0x08, 0x11, 0x11, (byte) address, 0x00, 0x00, 0x00, (byte) index};
        return buildFullFrame(data);
    }
    @Override
    public byte[] buildFullFrame(byte[] data) {
        byte bcc = CheckSumUtils.getBCC(data);
        byte[] frame = new byte[1 + data.length + 1];
        frame[0] = (byte) 0xF3;
        System.arraycopy(data, 0, frame, 1, data.length);
        frame[frame.length - 1] = bcc;
        return frame;
    }
}
