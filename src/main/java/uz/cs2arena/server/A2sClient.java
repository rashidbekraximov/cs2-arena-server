package uz.cs2arena.server;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Valve A2S_INFO so'rovi (UDP): server nomi, xaritasi, o'yinchilar soni. */
public final class A2sClient {

    public record Info(boolean online, String name, String map, int players, int maxPlayers, int bots, long ping) {
        public static Info offline() {
            return new Info(false, null, null, 0, 0, 0, -1);
        }

        public int humans() {
            return Math.max(0, players - bots);
        }
    }

    private static final byte[] QUERY;

    static {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x54});
        out.writeBytes("Source Engine Query\0".getBytes(StandardCharsets.US_ASCII));
        QUERY = out.toByteArray();
    }

    private A2sClient() {}

    public static Info query(String host, int port, int timeoutMs) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMs);
            InetAddress address = InetAddress.getByName(host);
            byte[] request = QUERY;
            for (int attempt = 0; attempt < 3; attempt++) {
                long sentAt = System.nanoTime();
                socket.send(new DatagramPacket(request, request.length, address, port));
                byte[] buf = new byte[1400];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                long ping = (System.nanoTime() - sentAt) / 1_000_000;

                ByteBuffer b = ByteBuffer.wrap(buf, 0, packet.getLength()).order(ByteOrder.LITTLE_ENDIAN);
                if (b.getInt() != -1) return Info.offline();
                byte type = b.get();
                if (type == 0x41) { // challenge
                    byte[] challenge = new byte[4];
                    b.get(challenge);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    out.writeBytes(QUERY);
                    out.writeBytes(challenge);
                    request = out.toByteArray();
                    continue;
                }
                if (type == 0x49) return parse(b, ping);
                return Info.offline();
            }
        } catch (Exception ignored) {
            // timeout — server oflayn
        }
        return Info.offline();
    }

    private static Info parse(ByteBuffer b, long ping) {
        b.get();
        String name = readString(b);
        String map = readString(b);
        readString(b);
        readString(b);
        b.getShort();
        int players = b.get() & 0xFF;
        int max = b.get() & 0xFF;
        int bots = b.get() & 0xFF;
        return new Info(true, name, map, players, max, bots, ping);
    }

    private static String readString(ByteBuffer b) {
        int start = b.position();
        while (b.hasRemaining() && b.get() != 0) { /* nol baytgacha */ }
        int end = b.position() - 1;
        return new String(b.array(), b.arrayOffset() + start, Math.max(0, end - start), StandardCharsets.UTF_8);
    }
}
