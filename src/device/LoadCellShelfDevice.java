package device;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
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

    public void readAddressAndModel() {
        String frameStr = "F8 96 AC 17 0A 00 00 0D 0A";
        byte[] frame = HexUtils.hexToBytes(frameStr);
        this.write(frame);
    }

    @Override
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        return super.isMatch(writeBytes, readBytes);
    }

    @Override
    public byte[] buildFullFrame(byte[] data) {
        return new byte[0];
    }
}
