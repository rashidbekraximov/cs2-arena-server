package uz.cs2arena.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Source RCON (TCP) — serverga buyruq yuborish uchun. */
public final class RconClient {

    private static final int AUTH = 3;
    private static final int EXEC = 2;

    private RconClient() {}

    public static String execute(String host, int port, String password, String command) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            DataInputStream in = new DataInputStream(socket.getInputStream());

            send(out, 1, AUTH, password);
            // Server avval bo'sh RESPONSE_VALUE (type 0), keyin AUTH_RESPONSE (type 2) yuborishi mumkin
            Packet p = read(in);
            if (p.type() == 0) p = read(in);
            if (p.id() == -1) throw new IOException("RCON parol noto'g'ri");

            send(out, 2, EXEC, command);
            return read(in).body;
        }
    }

    private record Packet(int id, int type, String body) {}

    private static void send(OutputStream out, int id, int type, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        ByteBuffer b = ByteBuffer.allocate(4 + 4 + 4 + payload.length + 2).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(4 + 4 + payload.length + 2);
        b.putInt(id);
        b.putInt(type);
        b.put(payload);
        b.put((byte) 0).put((byte) 0);
        out.write(b.array());
        out.flush();
    }

    private static Packet read(DataInputStream in) throws IOException {
        byte[] sizeBytes = new byte[4];
        in.readFully(sizeBytes);
        int size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (size < 10 || size > 1 << 20) throw new IOException("RCON javobi noto'g'ri");
        byte[] data = new byte[size];
        in.readFully(data);
        ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int id = b.getInt();
        int type = b.getInt();
        String body = new String(data, 8, Math.max(0, size - 10), StandardCharsets.UTF_8);
        return new Packet(id, type, body);
    }
}
