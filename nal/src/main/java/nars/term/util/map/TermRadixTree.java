package nars.term.util.map;

import jcog.data.byt.AbstractBytes;
import jcog.data.byt.ArrayBytes;
import jcog.data.byt.RecycledDynBytes;
import jcog.tree.radix.MyRadixTree;
import nars.io.IO;
import nars.io.TermIO;
import nars.term.Term;
import nars.term.atom.Atomic;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class TermRadixTree<X> extends MyRadixTree<X> {

    /**
     * target with volume byte prepended for sorting by volume
     */
    public static AbstractBytes termByVolume(Term x) {

        int vol = x.volume();

        //TermBytes y = new TermBytes(vol * 4 + 64 /* ESTIMATE */);
		try (RecycledDynBytes y = RecycledDynBytes.get()) {

            y.writeShort(vol);

            TermIO.the.write(x, y);

            return new ArrayBytes(y.arrayCopy());
        }
    }

    @Override
    public final X put(X value) {
        return put(key(value), value);
    }


    public final X put(Term key, X value) {
        return put(key(key), value);
    }
    public final X putIfAbsent(Term key, X value) {
        return putIfAbsent(key(key), value);
    }

    /** must override if X is not instanceof Termed */
    public static AbstractBytes key(Object k) {
        if (k instanceof Atomic) {
            return new ArrayBytes(((Atomic)k).bytes());
        } else {

//            try (RecycledDynBytes d = RecycledDynBytes.get()) { //termBytesEstimate(t) /* estimate */);
//                TermIO.the.write(t, d);
//                return d.arrayCopy();
//            }
            return new ArrayBytes(IO.termToBytes(((Term) k).term()));
        }
    }


    public X get(Term key) {
        return get(key(key));
    }
}
