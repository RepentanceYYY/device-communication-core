package device.utils;

public class StringUtils {
    public static String join(Object... args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(64);
        for (Object arg : args) {
            if (arg != null) {
                sb.append(arg);
            }
        }
        return sb.toString();
    }
}
