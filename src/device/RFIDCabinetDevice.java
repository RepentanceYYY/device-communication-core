package device;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.enums.DispatchMode;
import device.utils.HexUtils;

public class RFIDCabinetDevice extends DeviceCore implements IFrameProtocol {
    @Override
    public void setCommDispatcher(CommDispatcher comm) {
        super.setCommDispatcher(comm);
        comm.responseTimeout = 1500;
    }

    public void openLock(int cabinetNo, int lockNo) {
        byte cmdCode = (byte) 0x51; // 命令码
        byte action = (byte) 0x01;  // 动作：打开

        int sum = (cabinetNo & 0xFF) + (lockNo & 0xFF) + (action & 0xFF);
        byte checksum = (byte) (sum & 0xFF);

        byte[] data = new byte[]{
                cmdCode,
                (byte) cabinetNo,
                (byte) lockNo,
                action,
                checksum
        };

        // 最终发送的完整帧
        this.write(DispatchMode.PRIORITY, buildFullFrame(data), 1, 0, this::openLockCallback);
    }

    private void openLockCallback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("开锁回调:");
        System.out.println("send-->" + HexUtils.bytesToHexString(writeBytes));
        System.out.println("receive-->" + HexUtils.bytesToHexString(readBytes));
    }

    @Override
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        byte sentCmd = writeBytes[2];
        if (sentCmd == (byte) 0x51) {
            return readBytes[0] == 0x6A && readBytes[1] == 0x6B;
        }
        return super.isMatch(writeBytes, readBytes);
    }

    @Override
    public boolean validate(byte[] readBytes) {
        if (readBytes == null || readBytes.length < 2) {
            return false;
        }
        return true;
    }


    @Override
    public byte[] buildFullFrame(byte[] data) {
        byte[] frame = new byte[2 + data.length + 2];
        frame[0] = (byte) 0xAA;
        frame[1] = (byte) 0x7A;
        System.arraycopy(data, 0, frame, 2, data.length);
        frame[frame.length - 2] = (byte) 0x8A;
        frame[frame.length - 1] = (byte) 0x8B;
        return frame;
    }
}
