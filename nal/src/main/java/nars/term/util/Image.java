package nars.term.util;

import jcog.Util;
import jcog.util.ArrayUtils;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.subterm.Subterms;
import nars.table.dynamic.DynamicTaskTable;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.Answer;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.var.Img;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.*;

/** utilities for transforming image compound terms */
public enum Image {;

    public static final int ImageBits = PROD.bit | Op.IMG.bit | INH.bit;

    public static final Functor
            imageNormalize = Functor.f1Inline("imageNormalize", Image::imageNormalize),
            imageInt = Functor.f2Inline("imageInt", Image::imageInt),
            imageExt = Functor.f2Inline("imageExt", Image::imageExt);

    public static Term imageInt(Term t, Term x) {
        return image(true, t, x);
    }

    public static Term imageExt(Term t, Term x) {
        return image(false, t, x);
    }

    public static Term image(boolean intOrExt, Term t, Term x) {


        if (t.op() == INH) {

            int prodSub = intOrExt ? 1 : 0;

            Term s = t.sub(prodSub);

            if (s.op() == PROD) {

                Img target = intOrExt ? ImgInt : ImgExt;

                Subterms ss = s.subterms();
                if (!ss.has(IMG) || (!ss.contains(ImgInt) && !ss.contains(ImgExt))) {

                    int index = ss.indexOf(x);
                    if (index != -1) {
                        Term[] qq = ArrayUtils.prepend(t.sub(1 - prodSub), ss.arrayShared(), Term[]::new);

                        do {
                            qq[index + 1] = target;
                        } while ((index = ss.indexOf(x, index)) != -1);

                        Term q = PROD.the(qq);
                        return intOrExt ? INH.the(q, x) : INH.the(x, q);
                    }
                }
            }
        }
        return Bool.Null;
    }

    public static Term imageNormalize(Term x) {

        if (x instanceof Compound && x.hasAll(ImageBits)) {
            Op xo = x.op();
            if (xo == NEG) {
                Term u = x.unneg();
                if (u instanceof Compound && u.op() == INH) {
                    Term y = _imgNormalize((Compound)u).normalize();
                    if (!y.equals(u))
                        return y.neg();
                }
            } else if (xo == INH) {
                return _imgNormalize((Compound) x).normalize();
            }
        }

        return x;
    }


    public static Term _imgNormalize(Compound x) {
        return normalize(x, true);
    }


    public static boolean imageNormalizable(Subterms x) {
        return x.hasAll(Image.ImageBits) && !x.AND(Image::imageSubtermNormalizable);
    }

    private static boolean imageSubtermNormalizable(Term x) {
        return
                !x.isNormalized()
                    ||
                (x instanceof Compound && x.op()==INH &&
                        x.hasAll(ImageBits) &&
                        normalize((Compound)x, false)==null);
    }

    /** assumes that input is INH op has been tested for all image bits */
    @Nullable private static Term normalize(Compound x, boolean actuallyNormalize) {
//        boolean negated;
//
//        Term t;
        //Op o = t.op();
//        if (o == NEG) {
//            negated = true;
//            t = z.unneg();
//            o = t.op();
//        } else {
//            t = z;
//            negated = false;
//        }

        //assert(!(o==NEG));
        //if (o == INH /*&& t.hasAll(ImageBits)*/) {
        assert(x.op()==INH);

        Term s = x.sub(0);
        Subterms ss = null;
        boolean isInt = s.op() == PROD && (ss = s.subterms()).contains(Op.ImgInt);// && !ss.contains(Op.ImgExt);

        Term p = x.sub(1);
        Subterms pp = null;
        boolean isExt = p.op() == PROD && (pp = p.subterms()).contains(Op.ImgExt);// && !pp.contains(Op.ImgInt);


        boolean normalizable = isInt ^ isExt;

        if (!actuallyNormalize) {
            return normalizable ? null : x;
        }

        if (normalizable) {


            Term subj, pred;
            if (isInt) {

                subj = ss.sub(0);
                pred = PROD.the(Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p));

            } else {

                subj = PROD.the(Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s));
                pred = pp.sub(0);

            }

            return imageNormalize(INH.the(subj, pred));
        }

        return x;


    }


    public static class ImageBeliefTable extends DynamicTaskTable {

        /**
         * the target of the concept which this relies on for storage of beliefs, and thus remains consistent with
         */
        public final Term normal;



        public ImageBeliefTable(Term image, boolean beliefOrGoal) {
            super(image, beliefOrGoal);

            Term imageNormalized = Image.imageNormalize(image);
            assert(!image.equals(imageNormalized) && imageNormalized.op()==INH);
            this.normal = imageNormalized;
        }

        @Override
        public @Nullable Task match(long start, long end, boolean forceProject, @Nullable Term template, Predicate<Task> filter, int dur, NAR nar) {
            Task t = super.match(start, end, forceProject, template, filter, dur, nar);
            if (t!=null) {
                //wrap the result as an image
                return new ImageTermTask(this.term, t);
            }
            return null;
        }

        @Override
        public void match(Answer t) {
            //forward to the host concept's appropriate table
            Concept h = host(t.nar, false);
            if (h==null)
                return;

            if (!(h instanceof TaskConcept))
                return; //TODO if this happens: may be a NodeConcept in certain cases involving $ vars.  investigate

            (beliefOrGoal ? h.beliefs() : h.goals()).match(t);
        }

        //
//        @Nullable private BeliefTable table(NAR n, boolean conceptualize) {
//            Concept h = host(n, conceptualize);
//            if (!(h instanceof TaskConcept))
//                return null; //TODO if this happens: may be a NodeConcept in certain cases involving $ vars.  investigate
//            else
//                return beliefOrGoal ? h.beliefs() : h.goals();
//        }

        @Nullable private Concept host(NAR n, boolean conceptualize) {
            return n.concept(normal, conceptualize);
        }

        public static class ImageTermTask extends SpecialTermTask {
            public ImageTermTask(Term t, Task x) {
                super(t, x);
            }

            @Override
            protected boolean inheritCyclic() {
                return true; //cyclic propagation is fine here but only here
            }
        }
    }
}