package jcog.memoize.byt;

import jcog.io.Huffman;
import jcog.memoize.HijackMemoize;
import jcog.pri.PriProxy;

import java.util.function.Function;

public class ByteHijackMemoize<X extends ByteKey,Y> extends HijackMemoize<X,Y> {

    public ByteHijackMemoize(Function<X, Y> f, int capacity, int reprobes) {
        super(f, capacity, reprobes);
    }

    @Override
    public final PriProxy<X, Y> computation(X x, Y y) {
        return new ByteKey.ByteKeyInternal(x.key, x.hash, y, value(x, y));
    }

    @Override
    protected final boolean keyEquals(Object k, PriProxy p) {
        return p.equals(k);
    }


    public Huffman buildCodec() {
        return buildCodec(new Huffman(stream().map((b) -> key(b).key),
                Huffman.fastestCompDecompTime()));
    }

    public Huffman buildCodec(Huffman h) {
        //TODO add incremental codec building from multiple ByteHijackMemoize's
        return h;
    }

}
