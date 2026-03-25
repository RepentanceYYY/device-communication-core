import com.fazecast.jSerialComm.SerialPort;
public class Main {
    public static void main(String[] args) {
        // 1. 获取所有串口
        SerialPort[] ports = SerialPort.getCommPorts();
        System.out.println("检测到串口数量: " + ports.length);

        if (ports.length == 0) {
            System.out.println("未发现串口，请检查硬件连接！");
            return;
        }

        // 2. 打印串口列表
        for (int i = 0; i < ports.length; i++) {
            System.out.println(i + ": " + ports[i].getSystemPortName() + " (" + ports[i].getDescriptivePortName() + ")");
        }

        // 3. 尝试打开第一个串口（请确保它没被其他程序占用）
        SerialPort myPort = ports[0];
        myPort.setBaudRate(9600);

        if (myPort.openPort()) {
            System.out.println("成功打开: " + myPort.getSystemPortName());
            myPort.closePort();
            System.out.println("串口已关闭");
        } else {
            System.err.println("打开失败！可能是权限不足或串口已被占用。");
        }
    }
}