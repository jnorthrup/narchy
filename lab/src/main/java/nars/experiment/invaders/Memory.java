package nars.experiment.invaders;

public class Memory {
    public int a, b, c, d, e, h, l;
    public int int_enable;
    public int sp, pc;
    private final int[] mem;
    boolean cy, p, s, z, ac;

    public Memory() {
        mem = new int[16000];
    }

    public void addMem(int x, int pos) {
        mem[pos] = x;
    }

    public int[] getMem() {
        return mem;
    }
}
