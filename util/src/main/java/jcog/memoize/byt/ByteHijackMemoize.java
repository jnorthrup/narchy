package jcog.memoize.byt;

import jcog.io.Huffman;
import jcog.memoize.HijackMemoize;
import jcog.pri.PriProxy;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ByteHijackMemoize<X extends ByteKey.ByteKeyExternal,Y> extends HijackMemoize<X,Y> {

    public ByteHijackMemoize(Function<X, Y> f, int capacity, int reprobes, boolean soft) {
        super(f, capacity, reprobes, soft);
    }

    @Override
    public final PriProxy computation(X x, Y y) {
        return x.internal(y, value(x, y));
    }

    @Override
    public final PriProxy<X, Y> put(X x, Y y) {
        PriProxy<X, Y> xy = super.put(x, y);
        x.close();
        return xy;
    }

    @Override
    public final @Nullable Y apply(X x) {
        Y y = super.apply(x);
        x.close();
        return y;
    }


    public Huffman buildCodec() {
        return buildCodec(new Huffman(bag.stream().map(b -> bag.key(b).array()),
                Huffman.fastestCompDecompTime()));
    }

    public Huffman buildCodec(Huffman h) {
        //TODO add incremental codec building from multiple ByteHijackMemoize's
        return h;
    }

    @Override
    public float value(X x, Y y) {
        return 1f/(bag.reprobes * (1+x.length()));
        //return 1f/(bag.reprobes * (1+ Util.sqr(x.length())));
    }
}
