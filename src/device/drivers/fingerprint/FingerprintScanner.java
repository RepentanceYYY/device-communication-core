package device.drivers.fingerprint;

import device.core.DeviceCore;
import device.utils.ByteUtils;
import device.utils.HexUtils;

public class FingerprintScanner extends DeviceCore {

    private byte[] frameHead = new byte[]{(byte) 0xEF, (byte) 0x01};

    private byte[] address = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    private byte command_packetIdentifier = (byte) 0x01; // 命令包
    private byte data_packetIdentifier = (byte) 0x02; // 数据包
    private byte end_packetIdentifier = (byte) 0x08; // 结束包

    // 记录最后一次发送的指令码
    private volatile byte latestCommand = 0x00;

    /**
     * 设备主动上报
     *
     * @param readBytes 读取到的设备完整协议帧数据
     */
    @Override
    public void onDeviceReported(byte[] readBytes) {
        super.onDeviceReported(readBytes);
        int checkCode = readBytes[9] & 0xFF;
        System.out.println(FingerprintConfirmCode.fromCode(checkCode).getMessage());

        if (latestCommand == (byte) 0x32) {

        }
    }

    public void ps_autoEnroll() {
        byte[] domain = {(byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x43};
        byte[] bytes = this.buildCompleteCommandFrame((byte) 0x31, domain);
        this.write(bytes);
    }

    public void ps_autoIdentify() {
        byte[] domain = {(byte) 0x03, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x03};
        byte[] bytes = this.buildCompleteCommandFrame((byte) 0x32, domain);
        this.latestCommand = (byte) 0x32;
        this.write(bytes);

    }


    public void ps_deletChar(int pageIdInt, int countInt) {
        byte[] pageId = ByteUtils.intToTwoBytes(pageIdInt);
        byte[] count = ByteUtils.intToTwoBytes(countInt);


    }

    public void ps_getImages() {
        byte[] completeFrame = this.buildCompleteCommandFrame((byte) 0x01, null);
        this.write(completeFrame);
    }

    /**
     * 清空指纹库
     */
    public void ps_empty() {
        byte[] completeFrame = this.buildCompleteCommandFrame((byte) 0x0D, null);
        this.write(completeFrame);
    }

    /**
     * 取消命令
     */
    public void ps_cancel() {
        byte[] completeFrame = this.buildCompleteCommandFrame((byte) 0x30, null);
        this.write(completeFrame);
    }

    public void ps_sleep() {
        byte[] completeFrame = this.buildCompleteCommandFrame((byte) 0x33, null);
        this.write(completeFrame);
    }

    @Override
    protected boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        if (readBytes.length < 12) {
            return false;
        }
        return true;
    }

    @Override
    protected void callback(byte[] readBytes, byte[] writeBytes) {
        int checkCode = readBytes[9] & 0xFF;
        System.out.println(FingerprintConfirmCode.fromCode(checkCode).getMessage());
        System.out.println("发送:" + HexUtils.bytesToHexString(writeBytes));
        System.out.println("接收:" + HexUtils.bytesToHexString(readBytes));
    }

    /**
     * 构建完整指令包
     *
     * @param command 指令码
     * @param domain  数据域
     * @return
     */
    private byte[] buildCompleteCommandFrame(byte command, byte[] domain) {

        int lenInt = this.calculateCommandPacketLength(domain);
        int checkSumInt = this.calculateCommandChecksum(lenInt, command, domain);
        byte[] completeCommandFrame = ByteUtils.merge(frameHead, address, new byte[]{command_packetIdentifier}, ByteUtils.intToTwoBytes(lenInt), new byte[]{command}, domain, ByteUtils.intToTwoBytes(checkSumInt));

        return completeCommandFrame;
    }


    /**
     * 命令包长度
     * <p>
     * 长度 = 指令(1) + 参数(N) + 校验和(2)
     */
    public int calculateCommandPacketLength(byte[] params) {

        int paramLength = params == null ? 0 : params.length;

        return 1 + paramLength + 2;
    }


    /**
     * 命令包校验和
     * <p>
     * 从包标识开始累加
     */
    public int calculateCommandChecksum(int packetLength, byte command, byte[] params) {

        int sum = 0;
        // 包标识
        sum += 0x01;
        // 包长度
        sum += (packetLength >> 8) & 0xFF;
        sum += packetLength & 0xFF;
        // 指令
        sum += command & 0xFF;
        // 参数
        if (params != null) {
            for (byte b : params) {
                sum += b & 0xFF;
            }
        }
        // 保留低16位
        return sum & 0xFFFF;
    }


    /**
     * 数据包长度
     */
    public int calculateDataPacketLength(byte[] data) {

        int dataLength = data == null ? 0 : data.length;

        return dataLength + 2;
    }

    /**
     * 数据包校验和
     */
    public int calculateDataPacketChecksum(int packetLength, byte[] data) {

        int sum = 0;


        // 包标识
        sum += 0x02;


        // 长度
        sum += (packetLength >> 8) & 0xFF;
        sum += packetLength & 0xFF;


        // 数据
        if (data != null) {

            for (byte b : data) {

                sum += b & 0xFF;

            }
        }


        return sum & 0xFFFF;
    }

    /**
     * 结束包长度
     */
    public int calculateEndPacketLength(byte[] data) {

        int dataLength = data == null ? 0 : data.length;

        return dataLength + 2;
    }

    /**
     * 结束包校验和
     */
    public int calculateEndPacketChecksum(int packetLength, byte[] data) {

        int sum = 0;


        // 包标识
        sum += 0x08;


        // 长度
        sum += (packetLength >> 8) & 0xFF;
        sum += packetLength & 0xFF;


        // 数据
        if (data != null) {

            for (byte b : data) {

                sum += b & 0xFF;

            }
        }


        return sum & 0xFFFF;
    }


}
