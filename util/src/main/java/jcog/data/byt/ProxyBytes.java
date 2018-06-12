package jcog.data.byt;

public class ProxyBytes implements AbstractBytes {

    protected final AbstractBytes ref;

    public ProxyBytes(AbstractBytes ref) {
        this.ref = ref;
    }

    @Override
    public String toString() {
        return ref.toString();
    }

    @Override
    public final int length() {
        return ref.length();
    }

    @Override
    public final byte at(int index) {
        return ref.at(index);
    }

    @Override
    public final AbstractBytes subSequence(int start, int end) {
        return ref.subSequence(start, end);
    }
}
