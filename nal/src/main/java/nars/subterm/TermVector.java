package nars.subterm;

import jcog.Util;
import nars.The;
import nars.subterm.util.SubtermMetadataCollector;
import nars.subterm.util.TermMetadata;
import nars.term.Term;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * what differentiates TermVector from TermContainer is that
 * a TermVector specifically for subterms.  while both
 * can be
 */
public abstract class TermVector extends TermMetadata implements Subterms, The /*, Subterms.SubtermsBytesCached */ {

    transient boolean normalized;
    private final boolean the;

    /** called by IntrinSubterms */
    TermVector(SubtermMetadataCollector intrinMetadata) {
        super(intrinMetadata);
        the = true;
    }

    static final Predicate<Term> isThe = Term::the;

    protected TermVector(Term[] terms) {
        super(terms);
        this.the = Util.and(isThe, terms);
    }

    @Override
    public int indexOf(Term t, int after) {
        return impossibleSubTerm(t) ? -1 : Subterms.super.indexOf(t, after);
    }

    @Override
    public final int hashCodeSubterms() {
        //assert(hash == Subterms.super.hashCodeSubterms());
        return hash;
    }

    @Override
    public final boolean these() {
        return the;
    }

    /**
     * if the compound tracks normalization state, this will set the flag internally
     */
    @Override
    public void setNormalized() {
        normalized = true;
    }


    @Override
    public boolean isNormalized() {
        return normalized;
    }


    @Override
    public abstract Term sub(int i);

    @Override
    public String toString() {
        return Subterms.toString(this);
    }



    @Override
    public abstract Iterator<Term> iterator();
    

//    protected transient byte[] bytes = null;

//    @Override
//    public void appendTo(ByteArrayDataOutput out) {
//        byte[] b = this.bytes;
//        if (b ==null) {
//            Subterms.super.appendTo(out);
//        } else {
//            out.write(b);
//        }
//    }

//    @Override
//    public void acceptBytes(DynBytes constructedWith) {
//        if (bytes == null)
//            bytes = constructedWith.arrayCopy(1 /* skip op byte */);
//    }


}
