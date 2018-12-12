package nars.term.util.builder;

import jcog.memoize.byt.ByteHijackMemoize;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.InternedCompound;

import java.util.UUID;

import static nars.Op.PROD;

/** memoizes certain term operations in addition to interning */
public class MemoizingTermBuilder extends InterningTermBuilder {

    //TODO <Term,Term>
    private final ByteHijackMemoize<InternedCompound,Term> normalize;
    private final ByteHijackMemoize<InternedCompound,Term> root;

    public MemoizingTermBuilder() {
        this(UUID.randomUUID().toString(), deepDefault, maxInternedVolumeDefault, DEFAULT_SIZE);
    }

    public MemoizingTermBuilder(String id, boolean deep, int volInternedMax, int cacheSizePerOp) {
        super(id, deep, volInternedMax, cacheSizePerOp);


        root = newOpCache("root", j -> super.root((Compound) j.sub0()), cacheSizePerOp);

        normalize = newOpCache("normalize", j -> super.normalize((Compound) j.sub0(), (byte) 0), cacheSizePerOp);

//        concept = newOpCache("concept", j -> super.concept((Compound) j.sub0()), cacheSizePerOp);

    }

    @Override
    public Term normalize(Compound x, byte varOffset) {

//        if (!x.hasVars())
//            throw new WTF();

        if (varOffset == 0 && internable(x))
            return normalize.apply(InternedCompound.get(PROD, x)); //new LighterCompound(PROD, x, NORMALIZE)));
        else
            return super.normalize(x, varOffset);

    }


//    @Override
//    public Term concept(Compound x) {
//        if (!x.the())
//            return super.concept(x);
//        return concept.apply(InternedCompound.get(PROD, x));
//    }

    @Override
    public Term root(Compound x) {
        if (internable(x)) {
            return root.apply(InternedCompound.get(PROD, x));
        }
        return super.root(x);
//        if (x.volume() < 2)
//            throw new WTF();
    }

    protected boolean internable(Compound x) {
        return x.the() && x.volume() < volInternedMax;
    }

    //    private Term _root(InternedCompound i) {
//        return ;
//    }

}
