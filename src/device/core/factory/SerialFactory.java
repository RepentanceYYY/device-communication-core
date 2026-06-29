package device.core.factory;

import device.channel.SerialChannel;
import device.core.CommDispatcher;
import device.core.CommFactory;
import device.core.dispatcher.SerialDispatcher;

public class SerialFactory implements CommFactory {
    @Override
    public String getCommType() {
        return "serial";
    }

    @Override
    public void validate(String commAddress) {
        if (commAddress == null || commAddress.isBlank()) {
            throw new IllegalArgumentException("Serial address cannot be empty");
        }
        String[] parts = commAddress.split("@", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Serial address format must be port@baudRate");
        }
        if (parts[0].trim().isEmpty()) {
            throw new IllegalArgumentException("Serial port cannot be empty");
        }
        try {
            int baud = Integer.parseInt(parts[1].trim());
            if (baud <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid baud rate: " + parts[1]);
        }
    }

    @Override
    public CommDispatcher create(String commAddress) {
        String[] parts = commAddress.split("@");
        return new SerialDispatcher(new SerialChannel(parts[0].trim(), Integer.parseInt(parts[1].trim())));
    }
}
