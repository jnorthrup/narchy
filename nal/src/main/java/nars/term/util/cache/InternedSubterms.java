//package nars.target.util;
//
//import com.google.common.io.ByteArrayDataOutput;
//import jcog.data.byt.HashCachedBytes;
//import jcog.pri.PriProxy;
//import jcog.pri.UnitPri;
//import nars.Op;
//import nars.subterm.Subterms;
//import nars.target.Term;
//
//import java.util.Arrays;
//
//public final class InternedSubterms extends UnitPri implements PriProxy<InternedSubterms, Subterms> {
//    private final int hash;
//
//    private final byte[] subs;
//
//    private transient Term[] rawSubs;
//
//
//    private Subterms y = null;
//
//    public InternedSubterms(Term... subs) {
//        this.rawSubs = subs;
//
//        HashCachedBytes key = new HashCachedBytes(32 * subs.length);
//        for (Term s : subs)
//            s.appendTo((ByteArrayDataOutput) key);
//
//        this.subs = key.array();
//        this.hash = key.hashCode();
//    }
//
//    @Override
//    public Subterms get() {
//        return y;
//    }
//
//    @Override
//    public int hashCode() {
//        return hash;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//
//        InternedSubterms p = (InternedSubterms) obj;
//        return hash == p.hash && Arrays.equals(subs, p.subs);
//    }
//
//
//    @Override
//    public final InternedSubterms x() {
//        return this;
//    }
//
//    public Subterms compute() {
//        Term[] rawSubs = this.rawSubs;
//        this.rawSubs = null;
//        return Op.terms.theSubterms(rawSubs);
//    }
//}
