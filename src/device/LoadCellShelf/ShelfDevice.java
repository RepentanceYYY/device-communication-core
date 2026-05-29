package device.LoadCellShelf;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.model.PositionData;
import device.utils.ByteUtils;
import device.utils.HexUtils;
import device.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 称重货架
 */
public class ShelfDevice extends DeviceCore implements IFrameProtocol {

    @Override
    public void setCommDispatcher(CommDispatcher comm) {
        super.setCommDispatcher(comm);
        comm.responseTimeout = 1500;
    }


    /**
     * 批量读取数量
     */
    public void batchReadCount() {
        this.write(this.buildRelayFrame((byte) 0x01, null), this::batchReadCountCallback);
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

            int count = (high << 8) | low;

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
    public void setSlaveDeviceAddress(byte startAddress, byte endAddress) {
        byte[] dataPackage = new byte[]{startAddress, startAddress};
        this.write(this.buildRelayFrame((byte) 0x02, dataPackage));
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
        byte[] dataPackage = new byte[]{model == 0 ? (byte) 0x00 : (byte) 0x01};
        this.write(this.buildRelayFrame((byte) 0x09, dataPackage), this::switchModelCallback);
    }

    public void switchModelCallback(byte[] readBytes, byte[] writeBytes) {
        String model = writeBytes[7] == (byte) 0x00 ? "透传" : "中继";
        System.out.println("已切换到" + model);
    }

    /**
     * 读取范围和模式
     */
    public void readAddressAndModel() {
        this.write(this.buildRelayFrame((byte) 0x0A, null), this::readAddressAndModelCallback);
    }

    /**
     * 读取地址范围和模式回调
     *
     * @param readBytes
     * @param writeBytes
     */
    private void readAddressAndModelCallback(byte[] readBytes, byte[] writeBytes) {
        super.callback(readBytes, writeBytes);
        byte[] dataPackage = ByteUtils.slice(readBytes, 7, readBytes.length - 3);
        int startAddress = dataPackage[0];
        int endAddress = dataPackage[1];
        int model = dataPackage[2];
        System.out.println("10进制开始地址: " + startAddress + ", 结束地址: " + endAddress);
        System.out.println("当前模式:" + (model == 0 ? "透传模式" : "中继模式"));
    }

    /**
     * 停用启用货位
     *
     * @param address 设备地址
     * @param enabled true=启用；false=停用
     * @return
     */
    public Boolean setShelfSlotStateSync(byte address, Boolean enabled) throws Exception {
        String status = enabled ? "01" : "00";
        String frameASCII = String.format("%02d", address) + "work " + status;
        try {
            return writeSync(frameASCII, 2, 8000L, (readBytes, writeBytes) -> {
                if (readBytes == null) {
                    throw new RuntimeException("设备响应超时");
                }
                if (readBytes.length < 2) {
                    return false;
                }
                return readBytes[1] == (byte) 0x30;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    private void setEnabledCallback(byte[] readBytes, byte[] writeBytes) {
        if (readBytes == null || readBytes.length < 2) {
            System.out.println("超时");
        }
        if (readBytes[1] == (byte) 0x30) {
            System.out.println("启用停用成功");
        } else {
            System.out.println("启用停用失败");
        }
    }

    /**
     * 停用启用所有货位
     *
     * @param enabled true=启用；false=停用
     */
    public void setAllEnabled(Boolean enabled) {
        String enabledHex = enabled ? "01" : "00";
        String ascii = "work " + enabledHex;
        byte[] dataPackage = ascii.getBytes(this.getCharset());
        byte[] frame = this.buildRelayFrame((byte) 0x0B, dataPackage);

        this.write(frame, 5000L);
    }

    /**
     * 读取现存数量
     *
     * @param addressHex
     */
    public void readCurrentNumber(String addressHex) {
        String ascii = addressHex + "hdnum";
        this.write(ascii, this::readCurrentNumberCallback);
    }

    private void readCurrentNumberCallback(byte[] readBytes, byte[] writeBytes) {
        super.callback(readBytes, writeBytes);
        int start = 3;
        int end = readBytes.length - 2;
        int result = 0;

        for (int i = start; i < end; i++) {
            byte b = readBytes[i];
            if (b >= 0x30 && b <= 0x39) {
                result = result * 10 + (b - 0x30);
            }
        }
        System.out.println("数量为: " + result);
    }

    /**
     * 获取重量(已去皮)
     *
     * @param address
     * @return
     * @throws Exception
     */
    public Double getNetWeightSync(int address) throws Exception {
        String ascii = String.format("%02d", address) + "weight";
        try {
            return this.writeSync(ascii, 2, 1000L, (readBytes, writeBytes) -> {
                if (readBytes == null) {
                    throw new RuntimeException("设备响应超时");
                }
                if (readBytes.length <= 2) {
                    return null;
                }
                byte[] dataPackage = ByteUtils.slice(readBytes, 3, readBytes.length - 3);
                String asciiString = new String(dataPackage, StandardCharsets.US_ASCII);
                double value = Double.parseDouble(asciiString);
                return value;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 获取现存数量
     *
     * @param address
     * @return
     * @throws Exception
     */
    public Integer getQuantitySync(int address) throws Exception {
        String ascii = String.format("%02d", address) + "hdnum";
        try {
            return this.writeSync(ascii, 2, 500L, (readBytes, writeBytes) -> {
                // 至少需要：地址(1) + 状态(1) + 数据(1+) + 帧尾(2) = 5字节
                // 帧尾通常是 0D 0A，但也可能是 8D 0A 等异常情况
                if (readBytes.length < 5) {
                    throw new RuntimeException("响应数据太短: " + readBytes.length + " bytes, 数据: " + HexUtils.bytesToHexString(readBytes));
                }
                // 提取数据部分：从索引3开始，到倒数第3个字节结束（去掉最后2字节的帧尾）
                // 例如：01 30 30 33 0D 0A -> 提取索引3的 33
                int endIndex = readBytes.length - 3;  // 倒数第三个字节的索引
                // 确保 startIndex <= endIndex
                if (endIndex < 3) {
                    throw new RuntimeException("获取数据包结束索引失败: " + readBytes.length + " bytes, 数据: " + HexUtils.bytesToHexString(readBytes));
                }
                byte[] dataPackage = ByteUtils.slice(readBytes, 3, endIndex);
                if (dataPackage == null || dataPackage.length == 0) {
                    throw new RuntimeException("获取数据包部分失败: " + readBytes.length + " bytes, 数据: " + HexUtils.bytesToHexString(readBytes));
                }
                String asciiString = new String(dataPackage, StandardCharsets.US_ASCII).trim();
                if (asciiString.isEmpty()) {
                    throw new RuntimeException("数据字节数组转成字符串为空: " + readBytes.length + " bytes, 数据: " + HexUtils.bytesToHexString(readBytes));
                }
                // 校验是否全为数字字符
                for (char c : asciiString.toCharArray()) {
                    if (!Character.isDigit(c)) {
                        throw new RuntimeException("数据有误: " + readBytes.length + " bytes, 数据: " + HexUtils.bytesToHexString(readBytes));
                    }
                }
                Integer value = Integer.parseInt(asciiString);
                return value;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 清空领用数量(同步)
     *
     * @param address
     * @return
     * @throws Exception
     */
    public Boolean clearQuantityIssuedSync(int address) throws Exception {
        String ascii = String.format("%02d", address) + "clrget";
        try {
            return this.writeSync(ascii, 2, 500L, (readBytes, writeBytes) -> {
                if (readBytes.length != 2) {
                    throw new RuntimeException("清空领用数量设备响应的数据非法，数据为：" + HexUtils.bytesToHexString(readBytes));
                }
                if (readBytes[1] == (byte) 0x30) {
                    return true;
                } else {
                    throw new RuntimeException("清空领用数量设备响应的数据非法，数据为：" + HexUtils.bytesToHexString(readBytes));
                }
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 设置货位库存下限
     *
     * @param address
     * @param lowerLimit
     * @return
     * @throws Exception
     */
    public Boolean setShelfSlotInventoryLowerLimitSync(int address, int lowerLimit) throws Exception {

        String ascii = String.format("%02d", address) + "limdn 0," + lowerLimit;

        return writeSync(ascii, 0, 1000L, (readBytes, writeBytes) -> {

            if (readBytes == null) {
                throw new RuntimeException("设置库存下限超时");
            }
            if (readBytes.length != 2) {
                throw new RuntimeException(
                        "返回数据长度异常: " + readBytes.length
                );
            }
            if (readBytes[1] != (byte) 0x30) {
                throw new RuntimeException("设置库存下限失败: " + HexUtils.bytesToHexString(readBytes));
            }
            return true;
        });
    }

    /**
     * 设置货位上限
     *
     * @param address
     * @param upperLimit
     * @return
     * @throws Exception
     */
    public Boolean setShelfSlotInventoryUpperLimitSync(int address, int upperLimit) throws Exception {

        String ascii = String.format("%02d", address) + "limup 0," + upperLimit;

        return writeSync(ascii, 0, 1000L, (readBytes, writeBytes) -> {

            if (readBytes == null) {
                throw new RuntimeException("设置库存上限超时");
            }
            if (readBytes.length != 2) {
                throw new RuntimeException("返回数据长度异常: " + readBytes.length);
            }
            if (readBytes[1] != (byte) 0x30) {
                throw new RuntimeException("设置库存上限失败: " + HexUtils.bytesToHexString(readBytes));
            }
            return true;
        });
    }

    /**
     * 统一处理库存上下限
     *
     * @param addressHex 地址
     * @param WR         0:写；1:读
     * @param value      值
     * @param type       MAX 或 MIN
     */
    public void setInventoryLimit(String addressHex, String WR, String value, InventoryLimitType type) {

        String ascii = addressHex + type.cmd + " " + WR + "," + value;
        this.write(ascii, (readBytes, writeBytes) -> inventoryLimitCallback(readBytes, writeBytes, type));
    }

    private void inventoryLimitCallback(byte[] readBytes, byte[] writeBytes, InventoryLimitType type) {
        super.callback(readBytes, writeBytes);

        boolean isWrite = (writeBytes[8] == (byte) 0x30);
        boolean isRead = (writeBytes[8] == (byte) 0x31);

        if (isWrite) {
            if (readBytes != null && readBytes.length > 1 && readBytes[1] == (byte) 0x30) {
                System.out.println("库存" + type.desc + "写入成功");
            } else {
                System.out.println("库存" + type.desc + "写入失败");
            }
        } else if (isRead) {
            byte[] dataPackage = ByteUtils.slice(readBytes, 3, readBytes.length - 2);
            String asciiString = new String(dataPackage, StandardCharsets.US_ASCII);
            System.out.println("库存" + type.desc + "为:" + asciiString);
        }
    }

    /**
     * 打开关闭背光
     *
     * @param addressHex 地址
     * @param enabled    true打开false关闭
     */
    public void enabledBacklight(String addressHex, boolean enabled) {
        String ascii = StringUtils.join(addressHex, "ctrbl " + (enabled ? "1" : "0"));
        this.write(ascii);
    }

    /**
     * 写计划数量
     *
     * @param addressHex 地址
     * @param planTag    0:结束,1:开始
     * @param number     数量
     */
    public void writePlanNumber(String addressHex, String planTag, int number) {
        String ascii = StringUtils.join(addressHex, "excnum ", planTag, ",", number);
        this.write(ascii);
    }

    public void getReceiveNumber(String addressHex) {
        String ascii = StringUtils.join(addressHex, "status");
        this.write(ascii, this::getReceiveNumberCallback);
    }

    private void getReceiveNumberCallback(byte[] readBytes, byte[] writeBytes) {
        super.callback(readBytes, writeBytes);

        // 1. 基础校验：仅进行最小必要的检查
        if (readBytes == null || readBytes.length <= 3) return;

        // 2. 转换数据：直接取字节值，避免多余的变量转换
        // 如果 readBytes[3] 是 ASCII 码字符 (如 '5')，则保留 - '0'
        // 如果 readBytes[3] 本身就是原始字节状态位，则直接使用 readBytes[3] & 0xFF
        int data = (readBytes[3] - '0') & 0xFF;

        // 3. 位运算解析 (极致性能：零对象创建)
        // 直接通过位掩码 & 运算，非 0 即为 true
        boolean isLowerLimitAlarm = (data & 0x01) != 0; // Bit 0
        boolean isOverweightAlarm = (data & 0x02) != 0; // Bit 1
        boolean isPickStatus = (data & 0x04) != 0; // Bit 2
        boolean isPlanGreaterThanActual = (data & 0x08) != 0; // Bit 3
        boolean isPlanLessThanActual = (data & 0x10) != 0; // Bit 4
        boolean noWeightSensor = (data & 0x20) != 0; // Bit 5
        boolean noPlanOperation = (data & 0x40) != 0; // Bit 6
        boolean isUpperLimitAlarm = isLowerLimitAlarm; // 原逻辑中这两个变量指向同一位

        // 4. 仅在需要打印调试信息时才创建 StringBuilder
        // 生产环境中如果不打印，这部分开销也可以省去
        StringBuilder sb = new StringBuilder(256); // 预设容量减少扩容开销
        sb.append("--- 设备状态报告 (Raw: 0x").append(Integer.toHexString(data)).append(") ---\n");
        sb.append("1. 上限/下限警告: ").append(isLowerLimitAlarm ? "【异常】触发" : "正常").append("\n");
        sb.append("3. 超重警告: ").append(isOverweightAlarm ? "【异常】触发" : "正常").append("\n");
        sb.append("4. 取货状态: ").append(isPickStatus ? "正在取货" : "空闲/放货").append("\n");
        sb.append("5. 计划 > 实际: ").append(isPlanGreaterThanActual ? "是" : "否").append("\n");
        sb.append("6. 计划 < 实际: ").append(isPlanLessThanActual ? "是" : "否").append("\n");
        sb.append("7. 称重传感器: ").append(noWeightSensor ? "【错误】未连接" : "已连接").append("\n");
        sb.append("8. 计划取放货: ").append(noPlanOperation ? "无计划" : "有计划").append("\n");

        System.out.println(sb);
    }

    /**
     * 设置取货结束时间
     *
     * @param address
     * @param time
     * @return
     * @throws Exception
     */
    public Boolean setPickupEndTimeSync(int address, int time) throws Exception {

        String ascii = StringUtils.join(String.format("%02d", address), "ovtime ", time);
        try {
            return this.writeSync(ascii, 2, 1000L, (readBytes, writeBytes) -> {
                if (readBytes == null) {
                    throw new RuntimeException("设备响应超时");
                }
                if (readBytes.length < 2) throw new RuntimeException("设备回复字节数不够");
                ;
                if (readBytes[1] != (byte) 0x30) {
                    throw new RuntimeException("设备回复错误码");
                }
                return true;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 设置称重上限
     *
     * @param address
     * @param weight
     * @return
     * @throws Exception
     */
    public Boolean setMaxWeightSync(int address, BigDecimal weight) throws Exception {
        String ascii = String.format("%02d", address) + "setmax 0," + weight.toPlainString();
        try {
            return this.writeSync(ascii, 2, 1000L, (readBytes, writeBytes) -> {
                if (readBytes == null) {
                    throw new RuntimeException("设备响应超时");
                }
                if (readBytes.length < 2) return false;
                return readBytes[1] == (byte) 0x30;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 去皮(校准)
     *
     * @param address
     */
    public Boolean tareSync(int address) throws Exception {
        String ascii = String.format("%02d", address) + "fixed 0,0";
        try {
            return this.writeSync(ascii, 2, 1000L, (readBytes, writeBytes) -> {
                if (readBytes == null) {
                    throw new RuntimeException("设备响应超时");
                }
                if (readBytes.length < 2) return false;
                return readBytes[1] == (byte) 0x30;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 设置单重(校准)
     *
     * @param address
     * @param count
     * @return
     * @throws Exception
     */
    public Boolean setUnitWeightSync(int address, int count) throws Exception {
        String ascii = String.format("%02d", address) + "fixed 1," + count;
        try {
            return this.writeSync(ascii, 2, 1000L, (readBytes, writeBytes) -> {
                if (readBytes == null) {
                    throw new RuntimeException("设备响应超时");
                }
                if (readBytes.length < 2) return false;
                return readBytes[1] == (byte) 0x30;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 获取单重(校准)
     *
     * @param address
     * @return
     * @throws Exception
     */
    public Double getUniWeightSync(int address) throws Exception {
        String ascii = String.format("%02d", address) + "fixed 4,1";
        try {
            return this.writeSync(ascii, 2, 1000L, (readBytes, writeBytes) -> {
                if (readBytes == null) {
                    throw new RuntimeException("设备响应超时");
                }
                if (readBytes.length <= 2) return null;
                System.out.println(HexUtils.bytesToHexString(readBytes));
                byte[] dataPackage = ByteUtils.slice(readBytes, 3, readBytes.length - 3);
                System.out.println(HexUtils.bytesToHexString(dataPackage));
                String asciiString = new String(dataPackage, StandardCharsets.US_ASCII);
                double value = Double.parseDouble(asciiString);
                return value;
            });
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * 校准
     *
     * @param addressHex       地址
     * @param calibrationModel 校准模式
     *                         0:0,1:数量,4:0(读单重)
     * @param asciiN           参数
     */
    public void calibration(String addressHex, String calibrationModel, String asciiN) {
        String ascii = addressHex + "fixed " + calibrationModel + "," + asciiN;
        this.write(ascii, this::calibrationCallback);
    }


    private void calibrationCallback(byte[] readBytes, byte[] writeBytes) {
        super.callback(readBytes, writeBytes);
        // 读重量值
        if (writeBytes[8] == (byte) 0x34) {
            byte[] dataPackage = ByteUtils.slice(readBytes, 3, readBytes.length - 3);
            String asciiString = new String(dataPackage, StandardCharsets.US_ASCII);
            System.out.println("重量为:" + asciiString + "g");
        }
    }

    public void closeLamp(String addressHex) {
        String ascii = addressHex + "close";
        this.write(ascii, this::callback);
    }

    /**
     * @param addressHex 地址
     * @param sw         0:超时结束后返回系统控制,1:用户控制，结束后不返回系统控制。
     * @param flick      0:常量;1:闪烁
     * @param lampColor  灯光颜色
     * @param interval   闪烁间隔(1s~255s)
     */
    public void lampController(String addressHex, String sw, String flick, LampColor lampColor, String interval) {
        String ascii = StringUtils.join(addressHex, "ctrlled ", sw, ",", flick, ",", lampColor.getHex() + ",", interval);
        this.write(ascii, this::callback);

    }

    /**
     * 设置屏幕显示文字
     *
     * @param address    屏幕地址
     * @param titleIndex 标题索引:
     *                   0:货位,1:名称,2:规格,3:单位(小数),4:单位(整数)
     * @param context    内容
     */
    public Boolean setScreenDisplayTestSync(int address, int titleIndex, String context) throws Exception {

        String ascii = String.format("%02d", address)
                + "pdstr " + titleIndex + "," + context.trim();

        return writeSync(ascii, 2, 1000L, (readBytes, writeBytes) -> {

            if (readBytes == null) {
                throw new RuntimeException("设备响应超时");
            }
            if (readBytes.length != 2) {
                throw new RuntimeException("屏幕返回数据长度异常: " + readBytes.length);
            }

            if (readBytes[1] != (byte) 0x30) {
                throw new RuntimeException("屏幕返回失败码: " + HexUtils.bytesToHexString(readBytes));
            }
            return true;
        });
    }

    /**
     * 剪切成完整帧
     *
     * @param buffer
     * @return
     */
    @Override
    protected List<byte[]> splitFrames(ByteArrayOutputStream buffer) {
        List<byte[]> frames = new ArrayList<>();

        // 防止内存被乱码撑爆
        if (buffer.size() > 5 * 1024) {
            System.err.println("[DeviceSecurity]警告：接收缓冲区积压已达 " + buffer.size()
                    + " 字节，超过5KB上限！疑似协议错乱或遭遇持续乱码干扰，执行强行清空。");
            buffer.reset(); // 直接放弃所有缓存，丢弃垃圾数据
            return frames;  // 返回空列表，终止本次处理
        }

        // 只要缓冲区有数据，就持续尝试拆帧
        while (buffer.size() > 0) {
            byte[] currentData = buffer.toByteArray();
            int bufLen = currentData.length;

            boolean isCompleteFrame = false;
            int frameLength = 0;

            // 规则1：从前往后找第一个 0x0A
            int indexOf0A = -1;
            for (int i = 0; i < bufLen; i++) {
                if (currentData[i] == (byte) 0x0A) {
                    indexOf0A = i;
                    break;
                }
            }

            // 如果找到了 0x0A 且它前面至少还有一个字节（满足长度 >= 2 的安全限制）
            if (indexOf0A >= 1) {
                isCompleteFrame = true;
                frameLength = indexOf0A + 1; // 帧长度包含 0x0A 本身
            }
            // 规则2: 检测设置命令响应 (2字节: 地址 + 0x30/0x31)
            else if (bufLen >= 2 && (currentData[1] == (byte) 0x30 || currentData[1] == (byte) 0x31)) {
                isCompleteFrame = true;
                frameLength = 2;
            }

            // 如果判定成功，切分缓冲区
            if (isCompleteFrame) {
                // 提取完整帧
                byte[] frame = new byte[frameLength];
                System.arraycopy(currentData, 0, frame, 0, frameLength);
                frames.add(frame);

                // 移除已处理的数据，更新缓冲区
                buffer.reset();
                int remainLen = bufLen - frameLength;
                if (remainLen > 0) {
                    buffer.write(currentData, frameLength, remainLen);
                }
            } else {
                // 既没有找到 0x0A，也不符合两字节的设置响应，数据不全，退出循环等待更多碎包
                break;
            }
        }

        return frames;
    }


    @Override
    public boolean validate(byte[] readBytes) {
        if (readBytes == null || readBytes.length < 2) {
            return false;
        }
        return true;
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
        if (writeBytes == null || readBytes == null ||
                writeBytes.length < 2 || readBytes.length < 1) {
            return false;
        }

        String writeAddr = new String(writeBytes, 0, 2);

        String readAddr = String.format("%02X", readBytes[0] & 0xFF);

        return writeAddr.equalsIgnoreCase(readAddr);
    }

    /**
     * 构建完整帧
     *
     * @param cmdCodeHex
     * @param asciiData
     * @return
     */
    private byte[] buildIntactFrame(String cmdCodeHex, String asciiData) {
        byte[] dataPackage = asciiData.getBytes(super.getCharset());

        return null;
    }

    /**
     * 构建中继模式下完整帧
     *
     * @param cmdCodeByte 命令码
     * @param dataPackage 数据包
     * @return
     */
    private byte[] buildRelayFrame(byte cmdCodeByte, byte[] dataPackage) {

        int dataLen = (dataPackage == null) ? 0 : dataPackage.length;
        // 总长度
        byte[] frame = new byte[4 + 1 + 1 + 1 + dataLen + 2];

        int i = 0;
        // header
        frame[i++] = (byte) 0xF8;
        frame[i++] = (byte) 0x96;
        frame[i++] = (byte) 0xAC;
        frame[i++] = (byte) 0x17;
        // cmd
        frame[i++] = cmdCodeByte;
        // status
        frame[i++] = 0x00;
        // length
        frame[i++] = (byte) dataLen;
        // data
        if (dataLen > 0) {
            System.arraycopy(dataPackage, 0, frame, i, dataLen);
            i += dataLen;
        }
        // tail
        frame[i++] = 0x0D;
        frame[i] = 0x0A;

        return frame;
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
