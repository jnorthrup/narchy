package jcog.memoize.byt;

import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;

import java.util.function.Function;

abstract public class ByteMultiHijackMemoize<X extends ByteKeyExternal,Y> implements Memoize<X,Y> {
    final HijackMemoize<X,Y>[] table;

    public ByteMultiHijackMemoize(Function<X, Y> f, int capacity, int reprobes, boolean soft, int levels) {

        this.table = new HijackMemoize[levels];
        //TODO
    }

    abstract int level(ByteKey key, int levels);



//    @Override
//    public final PriProxy computation(X x, Y y) {
//        return x.internal(y, value(x, y));
//    }
//
//    @Override
//    public final PriProxy<X, Y> put(X x, Y y) {
//        PriProxy<X, Y> xy = super.put(x, y);
//        x.close();
//        return xy;
//    }
//
//    @Override
//    public final @Nullable Y apply(X x) {
//        Y y = super.apply(x);
//        x.close();
//        return y;
//    }
//
//
//    public Huffman buildCodec() {
//        return buildCodec(new Huffman(bag.stream().map(b -> bag.key(b).array()),
//                Huffman.fastestCompDecompTime()));
//    }
//
//    public Huffman buildCodec(Huffman h) {
//        //TODO add incremental codec building from multiple ByteHijackMemoize's
//        return h;
//    }
//
//    @Override
//    protected void boost(PriProxy<X, Y> p) {
//        p.priAdd(valueBase(p.x()) * CACHE_HIT_BOOST);
//    }
//    @Override
//    protected void cut(PriProxy<X, Y> p) {
//
//        p.priSub(valueBase(p.x()) * CACHE_SURVIVE_COST);
//    }
//
//    @Override
//    public float value(X x, Y y) {
//        return valueBase(x) * DEFAULT_VALUE;
//    }
//
//    private float valueBase(ByteKey x) {
//        return 1;
//        //return 1/((1+x.length()));
//        //return (float) (1 /(Math.log(1+x.length())));
//        //return 1 /((1+sqr(x.length())));
//        //return 1f/(bag.reprobes * (1+ Util.sqr(x.length())));
//    }
}
