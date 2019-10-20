package nars.experiment.invaders;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class SpaceInvaders {

    public static void main(String[] args) throws Exception {

        run();

    }

    public static void run() throws Exception {
        var mem = new Memory();

        load(mem, "invaders.h", 0);
        load(mem, "invaders.g", 2048);
        load(mem, "invaders.f", 4096);
        load(mem, "invaders.e", 6144);


        var cpu = new CPU(mem);
        cpu.run();

    }

    public static void showCodes() throws Exception {
        var arr = new ArrayList<String>();
        InputStream is = new FileInputStream("invaders.rom");
        var x = is.read();
        arr.add(toHexString((byte) x));
        while (x != -1) {
            x = is.read();
            arr.add(toHexString((byte) x));
        }

        System.out.println(Arrays.toString(arr.toArray()));


    }

    public static void load(Memory mem, String filename, int beginIndex) throws Exception {
        var is = SpaceInvaders.class.getResourceAsStream(filename);
        var x = is.read();
        mem.addMem(x, beginIndex);
        while (x != -1) {
            x = is.read();
            if (x == -1) {
                return;
            } else {
                beginIndex += 1;
                mem.addMem(x, beginIndex);
            }
        }
        is.close();
    }

    public static String toHexString(byte b) {
        return String.format("%02X", b);
    }
}
