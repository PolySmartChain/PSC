package org.tdf.sunflower.net;

import com.google.common.primitives.Bytes;
import lombok.SneakyThrows;
import org.tdf.common.util.BigEndian;
import org.tdf.sunflower.proto.Message;
import org.tdf.sunflower.types.Transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Util {
    public static byte[] getRawForSign(Message msg) {
        return Bytes.concat(
            BigEndian.encodeInt32(msg.getCode().getNumber()),
            BigEndian.encodeInt64(msg.getCreatedAt().getSeconds()),
            msg.getRemotePeer().getBytes(StandardCharsets.UTF_8),
            BigEndian.encodeInt64(msg.getTtl()),
            BigEndian.encodeInt64(msg.getNonce()),
            msg.getBody().toByteArray()
        );
    }

    @SneakyThrows
    public static boolean ping(String host, int port) {
        try (Socket s = new Socket()) {
            SocketAddress add = new InetSocketAddress(host, port);
            s.connect(add, 1000);// 1 second timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This can be a blocking call with long timeout (thus no ValidateMe)
     */
    public static String bindIp() throws Exception {
        Socket s = new Socket("www.baidu.com", 80);
        return s.getLocalAddress().getHostAddress();
    }

    /**
     * This can be a blocking call with long timeout (thus no ValidateMe)
     */
    static String externalIp() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(
            new URL("http://checkip.amazonaws.com").openStream()));
        String externalIp = in.readLine();
        if (externalIp == null || externalIp.trim().isEmpty()) {
            throw new IOException("Invalid address: '" + externalIp + "'");
        }
        InetAddress.getByName(externalIp);
        return externalIp;
    }

    public static int distance(byte[] left, byte[] right) {
        int res = 0;
        byte[] bits = new byte[Transaction.ADDRESS_LENGTH];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = (byte) (left[i] ^ right[i]);
        }
        for (int i = 0; i < bits.length; i++) {
            for (int j = 0; j < 7; j++) {
                res += ((1 << j) & bits[i]) >>> j;
            }
        }
        return res;
    }

    public static int subTree(byte[] left, byte[] right) {
        byte[] bits = new byte[Transaction.ADDRESS_LENGTH];
        byte mask = (byte) (1 << 7);
        for (int i = 0; i < bits.length; i++) {
            bits[i] = (byte) (left[i] ^ right[i]);
        }
        for (int i = 0; i < Transaction.ADDRESS_LENGTH * 8; i++) {
            if ((bits[i / 8] & (mask >>> (i % 8))) != 0) {
                return i;
            }
        }
        return 0;
    }
}
