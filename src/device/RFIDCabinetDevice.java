package device;

import device.core.CommDispatcher;
import device.core.DeviceCore;
import device.core.IFrameProtocol;
import device.enums.DispatchMode;
import device.utils.HexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RFIDCabinetDevice extends DeviceCore implements IFrameProtocol {
    @Override
    public void setCommDispatcher(CommDispatcher comm) {
        super.setCommDispatcher(comm);
        comm.responseTimeout = 1500;
    }

    // 只需要回复确认的命令类型
    List<Byte> replyCheckType = Arrays.asList((byte) 0x51, (byte) 0x56, (byte) 0x57, (byte) 0x58, (byte) 0x59, (byte) 0x60, (byte) 0x66);

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

    public void startScan(int cabinetNo) {
        byte cmdCode = (byte) 0x54;
        byte checksum = (byte) (cabinetNo & 0xFF);
        byte[] data = new byte[]{cmdCode, (byte) cabinetNo, checksum};
        this.write(DispatchMode.PRIORITY, buildFullFrame(data), 1, 0, this::startScanCallback);
    }


    private void startScanCallback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("启动盘点回调:");
        System.out.println("send-->" + HexUtils.bytesToHexString(writeBytes));
        System.out.println("receive-->" + HexUtils.bytesToHexString(readBytes));
    }

    private void openLockCallback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("开锁回调:");
        System.out.println("send-->" + HexUtils.bytesToHexString(writeBytes));
        System.out.println("receive-->" + HexUtils.bytesToHexString(readBytes));
    }

    public CompletableFuture<List<String>> readEPCAsync(int cabinetNo) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        byte cmdCode = (byte) 0x55;
        byte checksum = (byte) (cabinetNo & 0xFF);
        byte[] data = new byte[]{cmdCode, (byte) cabinetNo, checksum};

        // 在回调中完成 future
        this.write(DispatchMode.PRIORITY, buildFullFrame(data), 1, 0, (readBytes, writeBytes) -> {
            try {
                List<String> tags = new ArrayList<>();
                int count = readBytes[4] & 0xFF;
                int startIndex = 5;
                int epcLength = 12;

                for (int i = 0; i < count; i++) {
                    int off = startIndex + i * epcLength;
                    if (off + epcLength <= readBytes.length - 3) {
                        byte[] epcData = new byte[epcLength];
                        System.arraycopy(readBytes, off, epcData, 0, epcLength);
                        String fullHex = HexUtils.bytesToHexString(epcData).toUpperCase().replace(" ", "");
                        String cleanedHex = fullHex.replaceFirst("^0+", "");
                        tags.add(cleanedHex.isEmpty() ? "0" : cleanedHex);
                    }
                }
                // 成功：告诉等待者数据拿到了
                future.complete(tags);
            } catch (Exception e) {
                // 异常：告诉等待者出错了
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public List<String> getEpcListSync(int cabinetNo) {
        try {
            // 调用异步方法，并阻塞等待结果，设置 5 秒超时防止死锁
            return readEPCAsync(cabinetNo).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("读取EPC超时或失败: " + e.getMessage());
            return new ArrayList<>(); // 或者抛出你的自定义异常
        }
    }

    public void readEPC(int cabinetNo) {
        byte cmdCode = (byte) 0x55;
        byte checksum = (byte) (cabinetNo & 0xFF);
        byte[] data = new byte[]{cmdCode, (byte) cabinetNo, checksum};
        this.write(DispatchMode.PRIORITY, buildFullFrame(data), 1, 0, this::readEPCCallback);
    }

    private void readEPCCallback(byte[] readBytes, byte[] writeBytes) {
        System.out.println("读取EPC回调:");

        // 1. 获取标签数量
        int count = readBytes[4] & 0xFF;

        int startIndex = 5;
        int epcLength = 12;

        for (int i = 0; i < count; i++) {
            int off = startIndex + i * epcLength;

            // 对应 JS 的: if (off + 12 <= frameRaw.length - 3)
            // 这里的 -3 是减去结尾的 AA 8A 8B
            if (off + epcLength <= readBytes.length - 3) {

                // 2. 截取 12 字节并转为十六进制大写字符串
                byte[] epcData = new byte[epcLength];
                System.arraycopy(readBytes, off, epcData, 0, epcLength);
                String fullHex = HexUtils.bytesToHexString(epcData).toUpperCase();

                // 3. 去掉开头的 0 (对应 JS 的 .replace(/^0+/, '') || '0')
                String cleanedHex = fullHex.replaceFirst("^0+", "");
                if (cleanedHex.isEmpty()) {
                    cleanedHex = "0";
                }

                System.out.println(String.format("标签 [%d]: %s", i + 1, cleanedHex));
            }
        }
    }


    @Override
    public boolean isMatch(byte[] writeBytes, byte[] readBytes) {
        byte sentCmd = writeBytes[2];
        if (replyCheckType.contains(sentCmd)) {
            return readBytes[0] == 0x6A && readBytes[1] == 0x6B;
        }
        // 所有的回复都是AA 9A开头，index=2的是指令类型
        if (!(readBytes[0] == (byte) 0xAA && readBytes[1] == (byte) 0x9A && readBytes[2] == writeBytes[2]) && readBytes[3] == writeBytes[3]) {
            return false;
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
