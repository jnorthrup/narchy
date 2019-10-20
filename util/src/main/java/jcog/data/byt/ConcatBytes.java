package jcog.data.byt;

public class ConcatBytes implements AbstractBytes {
    final AbstractBytes a;
    final AbstractBytes b;


    public ConcatBytes(AbstractBytes a, AbstractBytes b) {
        this.a = a;
        this.b = b;

    }

    @Override
    public int length() {
        return a.length() + b.length();
    }

    @Override
    public byte at(int index) {
        int al = a.length();
        return at(index, al);
    }

    protected byte at(int index, int al) {
        if ( index < al) {
            return a.at(index);
        } else {
            index -= al;
            return b.at(index);
        }
    }

    @Override
    public AbstractBytes subSequence(int start, int end) {
        byte[] x = new byte[end-start];
        int al = a.length();
        for (int i = start, j = 0; i < end; i++) {
            x[j++] = at(i, al);
        }
        return new RawBytes(x);
    }
}
