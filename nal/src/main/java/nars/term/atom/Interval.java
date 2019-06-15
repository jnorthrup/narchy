package nars.term.atom;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.data.byt.DynBytes;
import jcog.data.byt.util.IntCoding;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.TermException;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.tuple.primitive.ByteIntPair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;

import java.io.DataInput;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.INTERVAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/** atom containing a sequential mapping of subterm indices to integers,
 *  which could represent time, space
 *  or some other 1-D quantity
 *
 *  used for conjunction sequences
  */
public final class Interval extends AbstractAtomic implements The, Iterable<ByteIntPair> {

    /** the subterm index column */
    private final byte[] key;
    /** the integer value column */
    private final int[] value;

    public static Interval read(DataInput in) throws IOException {
        int n = in.readUnsignedByte();
        byte[] subterm = new byte[n];
        int[] value = new int[n];
        for (int i = 0; i < n; i++) {
            subterm[i] = in.readByte();
            value[i] = IntCoding.readZigZagInt(in);
        }
        return the(subterm, value);
    }

    public static Interval the(byte[] subterm, int[] value) {
        int n = subterm.length;
        if (n !=value.length)
            throw new TermException(Interval.class + " requires same # of subterms as values");
        DynBytes b = new DynBytes(n * value.length*4);
        b.writeByte(INTERVAL.id);
        b.writeUnsignedByte(n);
        for (int i = 0; i < n; i++) {
            b.writeByte(subterm[i]);
            b.writeZigZagInt(value[i]);
        }
        return new Interval(subterm, value, b.compact());
    }

    private Interval(byte[] key, int[] value, byte[] bytes) {
        super(bytes);
        this.key = key;
        this.value = value;
    }

    @Override
    public int structure() {
        return 0;
    }

//    @Override
//    public int volume() {
//        return 0;
//    }
//
//    @Override
//    public int complexity() {
//        return 0;
//    }


    @Override
    public final Op op() {
        return INTERVAL;
    }

    public final int size() { return key.length; }

    public Stream<ByteIntPair> stream() {
        return IntStream.range(0,size()).mapToObj(i->pair(key[i], value[i]));
    }

    public Stream<ObjectIntPair<Term>> stream(Subterms s) {
        return stream().map(p -> pair(s.sub(p.getOne()), p.getTwo()));
    }

    @Override public Iterator<ByteIntPair> iterator() {
        return stream().iterator();
    }

    public Iterator<ObjectIntPair<Term>> iterator(Subterms s) {
        return stream(s).iterator();
    }

    public boolean AND(Subterms s, ObjectIntPredicate<Term> each) {
        int n = size();
        for (int i = 0; i < n; i++) {
            if (!each.accept(s.sub(key[i]), value[i]))
                return false;
        }
        return true;
    }

    public boolean OR(Subterms s, ObjectIntPredicate<Term> each) {
        return !AND(s, (ss,v) -> !each.accept(ss,v));
    }

    @Override
    public String toString() {
        return "\"" + Joiner.on(",").join(this) + "\""; //HACK
    }

    public Term key(int i, Subterms subterms) {
        return subterms.sub(this.key[i]);
    }
    public Term key(int i, Term[] subterms) {
        return subterms[this.key[i]];
    }

    public int value(int i) {
        return value[i];
    }

    public int valueFirst() {
        return value[0];
    }
    public int valueLast() {
        return value[value.length-1];
    }

    public int keyCount() {
        return Util.max(key);
    }
}
