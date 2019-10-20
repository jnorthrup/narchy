package nars.experiment.mario;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;


public class Replayer {
    private final DataInputStream dis;

    private byte tick;
    private int tickCount = -99999999;

    public Replayer(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        dis = new DataInputStream(bais);
    }

    public long nextLong() {
        try {
            return dis.readLong();
        } catch (IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public byte nextTick() {
        if (tickCount == -99999999) {
            try {
                tickCount = dis.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (tickCount == 0) {
            try {
                tick = (byte) dis.read();
                tickCount = dis.readInt();
            } catch (IOException e) {
            }
        }

        if (tickCount > 0) {
            tickCount--;
        }

        return tick;
    }
}