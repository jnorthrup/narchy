package nars.experiment.invaders;

public class Memory {
    public int a;
    public int b;
    public int c;
    public int d;
    public int e;
    public int h;
    public int l;
    public int int_enable;
    public int sp;
    public int pc;
    private final int[] mem;
    boolean cy;
    boolean p;
    boolean s;
    boolean z;
    boolean ac;

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
