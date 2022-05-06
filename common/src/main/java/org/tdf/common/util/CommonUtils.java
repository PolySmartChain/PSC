package org.tdf.common.util;

public class CommonUtils {
    public static String sizeToStr(long size) {
        if (size < 2 * (1L << 10)) return size + "b";
        if (size < 2 * (1L << 20)) return String.format("%dKb", size / (1L << 10));
        if (size < 2 * (1L << 30)) return String.format("%dMb", size / (1L << 20));
        return String.format("%dGb", size / (1L << 30));
    }

    public static String getNodeIdShort(String nodeId) {
        return nodeId == null ? "<null>" : nodeId.substring(0, 8);
    }
}
