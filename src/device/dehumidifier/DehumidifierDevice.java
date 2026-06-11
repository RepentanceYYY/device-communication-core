package device.dehumidifier;

import device.core.DeviceCore;
import device.utils.ByteUtils;
import device.utils.CheckSumUtils;

/**
 * 除湿机设备
 */
public class DehumidifierDevice extends DeviceCore {
    /**
     * SM设备地址
     */
    private int address = 1;
    /**
     * 读运行状态功能码
     */
    private final byte READ_RUN_STATUS = (byte) 0x01;
    /**
     * 写运行状态功能码
     */
    private final byte WRITE_RUN_STATUS = (byte) 0x05;
    /**
     * 控温方式寄存器号
     */
    private final int REGISTER_TEMP_CONTROL_MODE = 36;
    /**
     * 控湿手动开关寄存器号
     */
    private final int REGISTER_HUMID_MANUAL_SWITCH = 43;
    /**
     * 控温手动开关寄存器号
     */
    private final int REGISTER_TEMP_MANUAL_SWITCH = 44;
    /**
     * 读运行参数功能码
     */
    private final byte READ_RUN_PARAM = (byte) 0x03;
    /**
     * 写运行参数功能码
     */
    private final byte WRITE_RUN_PARAM = (byte) 0x06;
    /**
     * 控温开始值寄存器号
     */
    private final int REGISTER_TEMP_CONTROL_START = 8;
    /**
     * 控温停止值寄存器号
     */
    private final int REGISTER_TEMP_CONTROL_STOP = 9;
    /**
     * 控湿开启值寄存器号
     */
    private final int REGISTER_HUMIDITY_CONTROL_START = 10;
    /**
     * 控湿停止值寄存器号
     */
    private final int REGISTER_HUMIDITY_CONTROL_STOP = 11;
    /**
     * 温度报警上限值寄存器号
     */
    private final int REGISTER_TEMP_ALARM_UPPER = 12;
    /**
     * 温度报警下限值寄存器号
     */
    private final int REGISTER_TEMP_ALARM_LOWER = 13;

    /**
     * 获取设备地址
     *
     * @return
     */
    public int getAddress() {
        return this.address;
    }

    /**
     * 设置设备地址
     *
     * @param address
     */
    public void setAddress(int address) {
        this.address = address;
    }

    /**
     * 查询设备运行状态及告警 (FC=0x01)
     *
     * @param startIndex 起始寄存器偏移量 (有效范围: 31 ~ 46)
     * @param length     查询的线圈连续长度 (配合startIndex, 结尾不能超过 46)
     * @return 解析后的运行状态实体类
     * @throws IllegalArgumentException 当传入的地址或长度越界时抛出
     * @throws Exception                当通信超时或设备返回异常时抛出
     */
    public DehumidifierRunStatus queryRunStatus(int startIndex, int length) throws Exception {
        // 边界校验（31-46 区间）
        if (startIndex < 0 || startIndex > 46) {
            throw new IllegalArgumentException("查询失败：起始偏移量 startIndex (" + startIndex + ") 不在有效区间 [31, 46] 内！");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("查询失败：查询长度 length 必须大于 0！");
        }
        if (startIndex + length > 47) {
            throw new IllegalArgumentException("查询失败：查询范围越界！当前请求范围 [" + startIndex + " ~ " + (startIndex + length - 1) + "] 超出了最大有效边界 46！");
        }

        // 提取起始索引的高字节和低字节
        byte startHigh = (byte) ((startIndex >> 8) & 0xFF);
        byte startLow = (byte) (startIndex & 0xFF);

        // 提取长度的高字节和低字节
        byte lenHigh = (byte) ((length >> 8) & 0xFF);
        byte lenLow = (byte) (length & 0xFF);

        // 组合成 4 字节的数据域
        byte[] dataDomain = new byte[]{startHigh, startLow, lenHigh, lenLow};

        // 发送请求帧
        byte[] bytes = this.buildFullFrame(READ_RUN_STATUS, dataDomain);

        return super.writeSync(bytes, 0, 200L, (receive, write) -> parseRunStatus(receive, write, startIndex, length));
    }

    /**
     * 设备运行状态响应解析回调 (FC=0x01 状态位解析)
     *
     * @param receive    接收到的完整响应帧
     * @param write      发送的请求帧
     * @param startIndex 起始寄存器偏移量
     * @param length     请求的线圈长度
     * @return 解析后的状态实体类
     */
    public DehumidifierRunStatus parseRunStatus(byte[] receive, byte[] write, int startIndex, int length) {

        // 功能码异常校验（第二个字节，索引为 1）
        this.handleExceptionCode(receive, write);

        // 获取返回的字节数（第三个字节，索引为 2）
        int byteLen = receive[2] & 0xFF;

        // 实例化状态实体类
        DehumidifierRunStatus statusObj = new DehumidifierRunStatus();

        // 数据从第四个字节（索引 3）开始
        int dataStartIndex = 3;

        // 循环遍历每一个请求的 Bit
        for (int i = 0; i < length; i++) {
            // 计算当前 Bit 对应的实际寄存器偏移量
            int currentRegisterOffset = startIndex + i;

            // 计算该 Bit 落在返回数据的第几个字节中
            int byteIndex = i / 8;
            // 计算该 Bit 在当前字节中的偏移位 (Modbus 协议中低位在前)
            int bitIndex = i % 8;

            // 安全校验：防止设备返回的字节数少于我们计算所需的字节
            if (byteIndex >= byteLen) {
                break;
            }

            // 提取对应的字节，并取对应的 Bit 值 (0 或 1)
            int currentByteValue = receive[dataStartIndex + byteIndex] & 0xFF;
            boolean bitStatus = ((currentByteValue >> bitIndex) & 0x01) == 1;

            // 根据寄存器偏移量，动态填充布尔值到对象中
            switch (currentRegisterOffset) {
                case 31: // 10032 雾化模块状态
                    statusObj.setAtomizationModuleFault(bitStatus);
                    break;
                case 32: // 10033 雾化工作状态
                    statusObj.setAtomizationWorking(bitStatus);
                    break;
                case 33: // 10034 故障回路状态
                    statusObj.setFaultLoopOpened(bitStatus);
                    break;
                case 34: // 10035 控湿回路状态
                    statusObj.setHumidControlLoopOpened(bitStatus);
                    break;
                case 35: // 10036 控温回路状态
                    statusObj.setTempControlLoopOpened(bitStatus);
                    break;
                case 36: // 10037 控温方式
                    statusObj.setTempControlModeHeating(bitStatus);
                    break;
                case 37: // 10038 风机模块回路状态
                    statusObj.setFanModuleFault(bitStatus);
                    break;
                case 38: // 10039 除湿模块回路状态
                    statusObj.setDehumidifyModuleFault(bitStatus);
                    break;
                case 39: // 10040 湿度传感器状态
                    statusObj.setHumiditySensorFault(bitStatus);
                    break;
                case 40: // 10041 外部温度传感器状态
                    statusObj.setExternalTempSensorFault(bitStatus);
                    break;
                case 41: // 10042 内部温度传感器状态
                    statusObj.setInternalTempSensorFault(bitStatus);
                    break;
                case 42: // 10043 化霜状态
                    statusObj.setDefrosting(bitStatus);
                    break;
                case 43: // 10044 控湿手动开关
                    statusObj.setHumidManualSwitchOn(bitStatus);
                    break;
                case 44: // 10045 控温手动开关
                    statusObj.setTempManualSwitchOn(bitStatus);
                    break;
                case 45: // 10046 高温告警
                    statusObj.setHighTempAlarm(bitStatus);
                    break;
                case 46: // 10047 露点温度回路工作状态
                    statusObj.setDewPointLoopOpened(bitStatus);
                    break;
                default:
                    break;
            }
        }

        System.out.println(statusObj);
        return statusObj;
    }

    /**
     * 设置控温方式
     *
     * @param controlModel true:升温 false:降温
     */
    public void setTempControlModeHeating(boolean controlModel) throws Exception {

        byte[] controlValueBytes = controlModel ? new byte[]{(byte) 0xFF, (byte) 0x00} : new byte[]{(byte) 0x00, (byte) 0x00};
        byte[] registerControlValueBytes = ByteUtils.intToTwoBytes(REGISTER_TEMP_CONTROL_MODE);
        byte[] dataBytes = new byte[]{registerControlValueBytes[0], registerControlValueBytes[1], controlValueBytes[0], controlValueBytes[1]};
        byte[] frame = this.buildFullFrame(WRITE_RUN_STATUS, dataBytes);
        this.writeSync(frame, 0, 500L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            return true;
        });

    }

    /**
     * 设置控湿手动开关
     *
     * @param manualOn true:手动开启控湿, false:恢复正常自动控制
     */
    public void setHumidManualSwitchOn(boolean manualOn) throws Exception {
        // 物理地址 43 (寄存器44)
        byte[] registerBytes = ByteUtils.intToTwoBytes(this.REGISTER_HUMID_MANUAL_SWITCH);

        // FF00 表示手动开启，0000 表示恢复正常关闭手动
        byte[] controlValueBytes = manualOn ? new byte[]{(byte) 0xFF, (byte) 0x00} : new byte[]{(byte) 0x00, (byte) 0x00};

        byte[] dataBytes = ByteUtils.merge(registerBytes, controlValueBytes);
        byte[] frame = this.buildFullFrame(WRITE_RUN_STATUS, dataBytes);
        super.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            return true;
        });
    }

    /**
     * 设置控温手动开关
     *
     * @param controlModel true:手动开启控温, false:恢复正常自动控制
     * @throws Exception
     */
    public void setTempManualSwitchOn(boolean controlModel) throws Exception {
        byte[] registerBytes = ByteUtils.intToTwoBytes(this.REGISTER_TEMP_MANUAL_SWITCH);
        byte[] controlValueBytes = controlModel ? new byte[]{(byte) 0xFF, (byte) 0x00} : new byte[]{(byte) 0x00, (byte) 0x00};
        byte[] dataBytes = ByteUtils.merge(registerBytes, controlValueBytes);
        byte[] frame = this.buildFullFrame(WRITE_RUN_STATUS, dataBytes);
        super.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            return true;
        });
    }

    /**
     * 查询设备运行参数
     *
     * @param startIndex 起始寄存器偏移量 (有效范围: 0 ~ 15)
     * @param length     查询的寄存器连续长度 (配合startIndex, 结尾不能超过 15)
     * @return 解析后的运行参数实体类
     * @throws IllegalArgumentException 当传入的地址或长度越界时抛出
     * @throws Exception                当通信超时或设备返回异常时抛出
     */
    public DehumidifierRunParam queryRunParam(int startIndex, int length) throws Exception {
        // 边界校验（0-15 区间）
        if (startIndex < 0 || startIndex > 15) {
            throw new IllegalArgumentException("查询失败：起始偏移量 startIndex (" + startIndex + ") 不在有效区间 [0, 15] 内！");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("查询失败：查询长度 length 必须大于 0！");
        }
        if (startIndex + length > 15) {
            throw new IllegalArgumentException("查询失败：查询范围越界！当前请求范围 [" + startIndex + " ~ " + (startIndex + length) + "] 超出了最大有效边界 15！");
        }
        // 提取起始索引的高字节和低字节
        byte startHigh = (byte) ((startIndex >> 8) & 0xFF);
        byte startLow = (byte) (startIndex & 0xFF);

        // 提取长度的高字节和低字节
        byte lenHigh = (byte) ((length >> 8) & 0xFF);
        byte lenLow = (byte) (length & 0xFF);

        // 组合成 4 字节的数据域
        byte[] dataDomain = new byte[]{startHigh, startLow, lenHigh, lenLow};

        // 发送请求帧
        byte[] bytes = this.buildFullFrame(READ_RUN_PARAM, dataDomain);

        return super.writeSync(bytes, 0, 200L, (receive, write) -> parseRunParam(receive, write, startIndex));
    }

    /**
     * 设备运行参数响应解析回调
     *
     * @param receive
     * @param write
     * @param startIndex
     * @return
     */
    public DehumidifierRunParam parseRunParam(byte[] receive, byte[] write, int startIndex) {

        // 功能码异常校验（第二个字节，索引为 1）
        this.handleExceptionCode(receive, write);

        // 获取数据长度（第三个字节，索引为 2）
        int dataLen = receive[2] & 0xFF;

        // 3. 实例化用于接收数据的实体类对象
        DehumidifierRunParam paramObj = new DehumidifierRunParam();

        // 数据从第四个字节（索引 3）开始
        int dataStartIndex = 3;

        // 4. 循环解析响应报文并根据寄存器地址进行动态填充
        for (int i = 0; i < dataLen; i += 2) {
            // 计算当前解析的数据属于哪一个寄存器偏移量（基于传入的 startIndex）
            int currentRegisterOffset = startIndex + (i / 2);

            // 安全提取 16 位原始参数值
            int high = receive[dataStartIndex + i] & 0xFF;
            int low = receive[dataStartIndex + i + 1] & 0xFF;
            int paramValue = (high << 8) | low;

            // 提前计算好有符号和无符号的真实浮点值
            double signedDoubleValue = (short) paramValue / 10.0;  // 针对温度：处理负数并除以10
            double unsignedDoubleValue = paramValue / 10.0;         // 针对湿度、控制值：直接除以10

            // 根据寄存器偏移量，填充转换后的真实 double 值到对象中
            switch (currentRegisterOffset) {
                case 0: // 30001 内部温度（有符号）
                    paramObj.setInternalTemperature(signedDoubleValue);
                    break;
                case 1: // 30002 环境温度（有符号）
                    paramObj.setAmbientTemperature(signedDoubleValue);
                    break;
                case 2: // 30003 环境湿度（无符号）
                    paramObj.setAmbientHumidity(unsignedDoubleValue);
                    break;
                case 3: // 30004 内部温度2（有符号）
                    paramObj.setInternalTemperature2(signedDoubleValue);
                    break;
                case 4: // 30005 露点温度（有符号）
                    paramObj.setDewPointTemperature(signedDoubleValue);
                    break;
                case 5:
                    paramObj.setReserved3(paramValue);
                    break;
                case 6:
                    paramObj.setReserved4(paramValue);
                    break;
                case 7:
                    paramObj.setReserved5(paramValue);
                    break;
                case 8: // 40009 控温开启值（有符号，建议作为有符号处理预防极寒天气设定）
                    paramObj.setTempControlStart(signedDoubleValue);
                    break;
                case 9: // 40010 控温停止值（有符号）
                    paramObj.setTempControlStop(signedDoubleValue);
                    break;
                case 10: // 40011 控湿开启值（无符号）
                    paramObj.setHumidityControlStart(unsignedDoubleValue);
                    break;
                case 11: // 40012 控湿停止值（无符号）
                    paramObj.setHumidityControlStop(unsignedDoubleValue);
                    break;
                case 12: // 40013 温度报警上限值（有符号）
                    paramObj.setTempAlarmUpper(signedDoubleValue);
                    break;
                case 13: // 40014 温度报警下限值（有符号）
                    paramObj.setTempAlarmLower(signedDoubleValue);
                    break;
                case 14: // 40015 露点温度启动值（有符号）
                    paramObj.setDewPointStart(signedDoubleValue);
                    break;
                case 15: // 40016 露点温度回差值（回差一般为正数，无符号）
                    paramObj.setDewPointHysteresis(unsignedDoubleValue);
                    break;
                default:
                    break;
            }
        }
        System.out.println(paramObj);
        return paramObj;
    }

    /**
     * 设置控温开始值
     *
     * @param tempControlStart 控湿开始值(1~85)
     */
    public void setTempControlStart(int tempControlStart) throws Exception {

        if (tempControlStart < 1 || tempControlStart > 85) {
            throw new RuntimeException("控温开始值只能是在1~85范围内");
        }
        tempControlStart *= 10;
        byte[] tempControlStartBytes = ByteUtils.intToTwoBytes(tempControlStart);
        byte[] tempControlStartIndexBytes = ByteUtils.intToTwoBytes(REGISTER_TEMP_CONTROL_START);
        byte[] dataBytes = new byte[]{tempControlStartIndexBytes[0], tempControlStartIndexBytes[1], tempControlStartBytes[0], tempControlStartBytes[1]};
        byte[] frame = this.buildFullFrame(WRITE_RUN_PARAM, dataBytes);
        this.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            System.out.println("控温开始值设置成功");
            return true;
        });
    }

    /**
     * 设置控温停止值
     *
     * @param tempControlStop 控温停止值(1~85)
     */
    public void setTempControlStop(int tempControlStop) throws Exception {
        if (tempControlStop < 1 || tempControlStop > 85) {
            throw new RuntimeException("控温停止值只能是在1~85范围内");
        }
        tempControlStop *= 10;
        byte[] tempControlStopBytes = ByteUtils.intToTwoBytes(tempControlStop);
        byte[] tempControlStopIndexBytes = ByteUtils.intToTwoBytes(REGISTER_TEMP_CONTROL_STOP);
        byte[] dataBytes = new byte[]{tempControlStopIndexBytes[0], tempControlStopIndexBytes[1], tempControlStopBytes[0], tempControlStopBytes[1]};
        byte[] frame = this.buildFullFrame(WRITE_RUN_PARAM, dataBytes);
        this.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            System.out.println("控温停止值设置成功");
            return true;
        });
    }

    /**
     * 设置控湿开始值
     *
     * @param humidityControlStart 控湿开始值
     */
    public void setHumidityControlStart(int humidityControlStart) throws Exception {
        if (humidityControlStart < 25 || humidityControlStart > 98) {
            throw new RuntimeException("控湿开始值只能是在25~98范围内");
        }
        humidityControlStart *= 10;
        byte[] humidityControlStartBytes = ByteUtils.intToTwoBytes(humidityControlStart);
        byte[] humidityControlStartIndexBytes = ByteUtils.intToTwoBytes(REGISTER_HUMIDITY_CONTROL_START);
        byte[] dataBytes = new byte[]{humidityControlStartIndexBytes[0], humidityControlStartIndexBytes[1], humidityControlStartBytes[0], humidityControlStartBytes[1]};
        byte[] frame = this.buildFullFrame(WRITE_RUN_PARAM, dataBytes);
        this.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            return true;
        });
    }

    /**
     * 设置控湿停止值
     *
     * @param humidityControlStop 控湿停止值
     */
    public void setHumidityControlStop(int humidityControlStop) throws Exception {
        if (humidityControlStop < 25 || humidityControlStop > 98) {
            throw new RuntimeException("控湿停止值只能是在25~98范围内");
        }
        humidityControlStop *= 10;
        byte[] humidityControlStopBytes = ByteUtils.intToTwoBytes(humidityControlStop);
        byte[] humidityControlStopIndexBytes = ByteUtils.intToTwoBytes(REGISTER_HUMIDITY_CONTROL_STOP);
        byte[] dataBytes = new byte[]{humidityControlStopIndexBytes[0], humidityControlStopIndexBytes[1], humidityControlStopBytes[0], humidityControlStopBytes[1]};
        byte[] frame = this.buildFullFrame(WRITE_RUN_PARAM, dataBytes);
        this.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            return true;
        });
    }

    /**
     * 设置温度报警上限值
     *
     * @param tempAlarmUpper 温度报警上限值
     */
    public void setTempAlarmUpper(int tempAlarmUpper) throws Exception {
        if (tempAlarmUpper < 60 || tempAlarmUpper > 95) {
            throw new RuntimeException("温度报警上限值只能是在60~95范围内");
        }
        tempAlarmUpper *= 10;
        byte[] tempAlarmUpperBytes = ByteUtils.intToTwoBytes(tempAlarmUpper);
        byte[] tempAlarmUpperIndexBytes = ByteUtils.intToTwoBytes(REGISTER_TEMP_ALARM_UPPER);
        byte[] dataBytes = new byte[]{tempAlarmUpperIndexBytes[0], tempAlarmUpperIndexBytes[1], tempAlarmUpperBytes[0], tempAlarmUpperBytes[1]};
        byte[] frame = this.buildFullFrame(WRITE_RUN_PARAM, dataBytes);
        this.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            return true;
        });
    }

    /**
     * 设置温度报警下限值
     *
     * @param tempAlarmLower 温度报警下限值
     */
    public void setTempAlarmLower(int tempAlarmLower) throws Exception {
        if (tempAlarmLower < 1 || tempAlarmLower > 50) {
            throw new RuntimeException("温度报警下限值只能是在1~50范围内");
        }
        tempAlarmLower *= 10;
        byte[] tempAlarmLowerBytes = ByteUtils.intToTwoBytes(tempAlarmLower);
        byte[] tempAlarmLowerIndexBytes = ByteUtils.intToTwoBytes(REGISTER_TEMP_ALARM_LOWER);
        byte[] dataBytes = new byte[]{tempAlarmLowerIndexBytes[0], tempAlarmLowerIndexBytes[1], tempAlarmLowerBytes[0], tempAlarmLowerBytes[1]};
        byte[] frame = this.buildFullFrame(WRITE_RUN_PARAM, dataBytes);
        this.writeSync(frame, 0, 300L, (receive, write) -> {
            this.handleExceptionCode(receive, write);
            return true;
        });
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
