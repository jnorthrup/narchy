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
    public final PriProxy computation(X x, Y y) {
        return ((ByteKey.ByteKeyExternal) x).internal(y, value(x, y));
    }


    public Huffman buildCodec() {
        return buildCodec(new Huffman(bag.stream().map(b -> bag.key(b).array()),
                Huffman.fastestCompDecompTime()));
    }

    public Huffman buildCodec(Huffman h) {
        //TODO add incremental codec building from multiple ByteHijackMemoize's
        return h;
    }

}
