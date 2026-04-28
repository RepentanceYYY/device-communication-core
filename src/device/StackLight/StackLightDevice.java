package device.StackLight;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;

/**
 * 塔灯设备
 */
public class StackLightDevice extends DeviceCore implements IFrameProtocol {

    @Override
    public void setCommDispatcher(CommDispatcher commDispatcher) {
        super.setCommDispatcher(commDispatcher);
        commDispatcher.responseTimeout = 1500;
    }





    @Override
    public byte[] buildFullFrame(byte[] data) {
        return new byte[0];
    }
}
