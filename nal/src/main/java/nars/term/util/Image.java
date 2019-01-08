package nars.term.util;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.pri.Ranked;
import jcog.util.ArrayUtils;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.op.Remember;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.EmptyBeliefTable;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.Answer;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.var.Img;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.*;

/** utilities for transforming image compound terms */
public enum Image {;

    public static final int ImageBits = PROD.bit | Op.IMG.bit | INH.bit;

    public static final Functor
            imageNormalize = Functor.f1Inline("imageNormalize", Image::imageNormalize),
            imageInt = Functor.f2Inline("imageInt", Image::imageInt),
            imageExt = Functor.f2Inline("imageExt", Image::imageExt);


//    private static boolean imaged(Term p) {
//        return p.hasAny(Op.IMG) && p.OR(x -> (x == Op.ImgInt || x == Op.ImgExt));
//    }

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
                if (!ss.hasAny(IMG) || (!ss.contains(ImgInt) && !ss.contains(ImgExt))) {

                    int index = ss.indexOf(x);
                    if (index != -1) {
                        Term[] qq = ArrayUtils.prepend(t.sub(1 - prodSub), ss.arrayShared(), Term[]::new);

                        do {
                            qq[index + 1] = target;
                            index = ss.indexOf(x, index);
                        } while (index != -1);

                        Term q = PROD.the(qq);
                        return intOrExt ? INH.the(q, x) : INH.the(x, q);
                    }
                }
            }
        }
        return Bool.Null;
    }

    public static Term imageNormalize(Term x) {

        if (!(x instanceof Compound))
            return x;

        Op xo = x.op();
        if (xo ==NEG) {
            Term u = x.unneg();
            Term y = imageNormalize(u);
            if (y==u)
                return x; //unchanged
            else
                return y.neg();
        }

        if (xo !=INH || !x.hasAll(ImageBits))
            return x;

        Term y = normalize((Compound) x);

        return y;
    }


    public static Term normalize(Compound x) {
        return normalize(x, true);
    }


    public static boolean imageNormalizable(Term x) {
        return x instanceof Compound && x.op()==INH && x.hasAll(ImageBits) && normalize((Compound)x, false)==null;
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


            Term u;
            if (isInt) {

                u = INH.the(ss.sub(0), PROD.the(Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p)));

            } else {

                u = INH.the(PROD.the(Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s)), pp.sub(0));

            }

            if (u instanceof Bool)
                return u; //WTF


            return Image.imageNormalize(u);//.negIf(negated);
        }

        return x;


    }


    public static class ImageBeliefTable extends EmptyBeliefTable {

        /**
         * the term of the concept which this relies on for storage of beliefs, and thus remains consistent with
         */
        public final Term image, normal;
        private final boolean beliefOrGoal;

        @Nullable
        transient TaskConcept host = null;

        public ImageBeliefTable(Term image, boolean beliefOrGoal) {

            Term imageNormalized = Image.imageNormalize(image);
            this.normal = imageNormalized;
            if (normal instanceof Bool)
                throw new WTF();

            this.image = image;

            this.beliefOrGoal = beliefOrGoal;
        }

        @Override
        public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
            throw new TODO();
            //not necessary to proxy
//            BeliefTable table = table();
//            if (table == null)
//                return;
        }

        private BeliefTable table() {
            @Nullable Concept h = host;
            return (h == null) ? null : (beliefOrGoal ? h.beliefs() : h.goals());
        }


        @Override
        public boolean isEmpty() {
            BeliefTable t = table();
            //if t is null, it just means this table isnt currently linked to the referent table.  so must report false
            return t != null && t.isEmpty();

        }

        @Override
        public void sample(Answer m) {
            match(m); //HACK
        }

        @Override
        public void match(Answer m) {
            BeliefTable table = relink(m.nar, true);
            if (table == null)
                return;

            //TODO rewrite matched entries?
            table.match(m);

            int results = m.tasks.size();
            if (results > 0) {
            //HACK apply this as an add-on transformation to a final result, not every intermediate possible result
                Ranked<Task>[] tt = m.tasks.items;
                for (int i = 0; i < results; i++) {
                    tt[i].set(new ImgTermTask(image, (tt[i].x)));
                }
            }
        }

        @Override
        public void add(Remember r, NAR nar) {
            BeliefTable table = relink(nar, true);
            Task originalInput = r.input;
            if (table == null) {
                r.forget(originalInput);
                return;
            }

            SpecialTermTask transformedInput = new SpecialTermTask(normal, originalInput);
            if (originalInput.isCyclic())
                transformedInput.setCyclic(true);

            r.setInput(transformedInput, host);

            table.add(r, nar);


//            if (r.forgotten.containsInstance(transformedInput))
//                return; //wasnt added


//            if (rememberance.contains(transformedInput))
//                rememberance.replaceAll((x)->x == transformedInput ? originalInput : x); //for TaskEvent emission
//            else {
//            rememberance.replaceAll(x -> {
//                if (x == transformedInput)
//                    return originalInput;
////               if (x instanceof Reaction) {
////                   if (((Reaction)x).task == transformedInput)
////                       return true;
////               }
////                if (x instanceof TaskLinkTask) {
////                    if (((TaskLinkTask)x).task == transformedInput)
////                        return true;
////                }
//
//               return x;
//            });

//            if (!transformedInput.isDeleted()) {
//                if (r.remembered != null) {
//                    r.remembered.remove(transformedInput); //if it's present, it may not
//                }
//                r.remember(originalInput);
//            }
//            }
        }

        @Nullable private BeliefTable relink(NAR n, boolean conceptualize) {
            Concept h = host;
            if (h == null || h.isDeleted()) {
                h = (host = (TaskConcept) n.concept(normal, conceptualize)); //TODO set atomically
                if (h == null)
                    return null;
            }
            return beliefOrGoal ? h.beliefs() : h.goals();
        }

        private static class ImgTermTask extends SpecialTermTask {
            public ImgTermTask(Term t, Task x) {
                super(t, x);
            }

            @Override
            protected boolean inheritCyclic() {
                return true; //cyclic propagation is fine here but only here
            }
        }
    }
}