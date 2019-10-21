package jcog.memoize.byt;

import jcog.io.Huffman;
import jcog.memoize.HijackMemoize;
import jcog.pri.PriProxy;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ByteHijackMemoize<X extends ByteKeyExternal,Y> extends HijackMemoize<X,Y> {

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
        return buildCodec(new Huffman(bag.stream().map(new Function<PriProxy<X, Y>, byte[]>() {
            @Override
            public byte[] apply(PriProxy<X, Y> b) {
                return bag.key(b).array();
            }
        }),
                Huffman.fastestCompDecompTime()));
    }

    public static Huffman buildCodec(Huffman h) {
        //TODO add incremental codec building from multiple ByteHijackMemoize's
        return h;
    }

    @Override
    protected void boost(PriProxy<X, Y> p) {
        p.priAdd(valueBase(p.x()) * CACHE_HIT_BOOST);
    }
    @Override
    protected void cut(PriProxy<X, Y> p) {

        p.priSub(valueBase(p.x()) * CACHE_SURVIVE_COST);
    }

    @Override
    public float value(X x, Y y) {
        return valueBase(x) * DEFAULT_VALUE;
    }

    private static float valueBase(ByteKey x) {
        return 1.0F;
        //return 1/((1+x.length()));
        //return (float) (1 /(Math.log(1+x.length())));
        //return 1 /((1+sqr(x.length())));
        //return 1f/(bag.reprobes * (1+ Util.sqr(x.length())));
    }
}
