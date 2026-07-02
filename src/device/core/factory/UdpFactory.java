package device.core.factory;

import device.channel.UdpChannel;
import device.core.CommDispatcher;
import device.core.CommFactory;
import device.core.dispatcher.UdpDispatcher;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class UdpFactory implements CommFactory {

    @Override
    public String getCommType() {
        return "udp";
    }

    @Override
    public void validate(String commAddress) {
        if (commAddress == null || commAddress.isBlank()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }

        String[] parts = commAddress.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Address format must be localPort:ip:remotePort");
        }

        // 本地端口
        try {
            int localPort = Integer.parseInt(parts[0]);
            if (localPort < 0 || localPort > 65535) {
                throw new IllegalArgumentException("Local port must be between 0 and 65535");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid local port: " + parts[0]);
        }

        // 远程IP/域名
        try {
            InetAddress.getByName(parts[1]);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid host/IP address: " + parts[1]);
        }

        // 远程端口
        try {
            int remotePort = Integer.parseInt(parts[2]);
            if (remotePort < 1 || remotePort > 65535) {
                throw new IllegalArgumentException("Remote port must be between 1 and 65535");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid remote port: " + parts[2]);
        }
    }

    @Override
    public CommDispatcher create(String commAddress) {
        String[] parts = commAddress.split(":");

        int localPort = Integer.parseInt(parts[0]);
        String remoteHost = parts[1];
        int remotePort = Integer.parseInt(parts[2]);

        return new UdpDispatcher(
                new UdpChannel(remoteHost, remotePort, localPort)
        );
    }
}
