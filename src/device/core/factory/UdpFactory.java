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
        if (parts.length != 2) {
            throw new IllegalArgumentException("Address format must be ip:port");
        }

        // 校验 IP (支持纯IP或域名)
        try {
            InetAddress.getByName(parts[0]);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid host/IP address: " + parts[0]);
        }

        // 校验端口
        try {
            int port = Integer.parseInt(parts[1]);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + parts[1]);
        }
    }

    @Override
    public CommDispatcher create(String commAddress) {
        String[] parts = commAddress.split(":");
        return new UdpDispatcher(new UdpChannel(parts[0], Integer.parseInt(parts[1])));
    }
}
