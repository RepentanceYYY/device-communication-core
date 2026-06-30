package device.drivers.dehumidifier;

import device.core.DeviceCore;
import device.utils.ByteUtils;
import device.utils.CheckSumUtils;

public class DehumidifierU14Device extends DeviceCore {

    private int address;

    /**
     * 读运行参数功能码
     */
    private final byte READ_RUN_PARAM = 0x03;
    /**
     * 配置运行参数功能码
     */
    private final byte CONFIG_RUN_PARM = 0x10;

    public void setAddress(int address) {
        this.address = address;
    }

    public EnvironmentU14 queryEnvironment() throws Exception {
        byte[] dataDomain = new byte[]{
                (byte) 0x00,
                (byte) 0x0A,
                (byte) 0x00,
                (byte) 0x03
        };
        byte[] completeFrame = this.buildFullFrame(this.READ_RUN_PARAM, dataDomain);
        return super.writeSync(
                completeFrame,
                0,
                500L,
                (receive, write) -> parseEnvironment(receive, write)
        );
    }

    private EnvironmentU14 parseEnvironment(byte[] receive, byte[] write) {
        // 异常码处理
        this.handleExceptionCode(receive, write);

        // 数据长度校验，应为 6
        if ((receive[2] & 0xFF) != 6) {
            throw new IllegalArgumentException("返回数据长度异常：" + (receive[2] & 0xFF));
        }

        if (receive.length < 11) {
            throw new IllegalArgumentException("返回数据长度不足：" + receive.length);
        }

        EnvironmentU14 environment = new EnvironmentU14();

        double internalTemperature = ByteUtils.readUInt16(receive, 3) / 10.0 - 25;
        double externalTemperature = ByteUtils.readUInt16(receive, 5) / 10.0 - 25;
        double humidity = ByteUtils.readUInt16(receive, 7) / 10.0;

        environment.setInternalTemperature(internalTemperature);
        environment.setExternalTemperature(externalTemperature);
        environment.setHumidity(humidity);

        return environment;
    }

    /**
     * 构建完整协议帧
     *
     * @param functionCode 功能码
     * @param data         数据
     * @return
     */
    private byte[] buildFullFrame(byte functionCode, byte[] data) {
        // 定义总长度：地址(1) + 功能码(1) + 数据长度 + CRC(2)
        int dataLen = (data != null) ? data.length : 0;
        byte[] fullFrame = new byte[2 + dataLen + 2];

        // 组装头部
        fullFrame[0] = (byte) (address & 0xFF);
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

    /**
     * 协议帧匹配
     *
     * @param writeBytes 写入
     * @param readBytes  读取
     * @return
     */
    @Override
    protected final boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        if (readBytes[0] != (byte) (address & 0xFF)) {
            return false;
        }
        if (writeBytes[0] != readBytes[0]) {
            return false;
        }
        if ((writeBytes[1] != readBytes[1]) && (((writeBytes[1] & 0xFF) | 0x80) != (readBytes[1] & 0xFF))) {
            return false;
        }
        return super.isMatch(writeBytes, readBytes);
    }

    /**
     * 处理接收到的协议帧中的异常码
     *
     * @param receive 接收帧
     * @param write   写入帧
     * @return
     */
    private void handleExceptionCode(byte[] receive, byte[] write) {
        if (((write[1] & 0xFF) | 0x80) == (receive[1] & 0xFF)) {
            int exceptionCode = receive[2];
            throw new RuntimeException(DehumidifierExceptionCode.getByCode(exceptionCode).getDescription());
        }
    }
}
