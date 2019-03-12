package nars.term.util;

import jcog.Util;
import jcog.util.ArrayUtils;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.op.Remember;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.dynamic.DynamicTaskTable;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.Answer;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.var.Img;
import org.jetbrains.annotations.Nullable;

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
                    Term y = normalize((Compound)u);
                    if (!y.equals(u))
                        return y.neg();
                }
            } else if (xo == INH) {
                return normalize((Compound) x);
            }
        }

        return x;
    }


    public static Term normalize(Compound x) {
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
        public void match(Answer m) {
            BeliefTable table = table(m.nar, false);
            if (table == null)
                return;

            table.match(m);

            int results = m.tasks.size();
            if (results > 0) {
            //HACK apply this as an addAt-on transformation to a final result, not every intermediate possible result
                Task[] tt = m.tasks.items;
                Term image = this.term;
                for (int i = 0; i < results; i++) {
                    tt[i] = new ImgTermTask(image, (tt[i]));
                }
            }
        }

        @Override
        public void add(Remember r, NAR nar) {
            BeliefTable table = table(nar, true);
            Task imageInput = r.input;
            if (table == null) {
                r.forget(imageInput);
                return;
            }

            Term normalTerm = normal.hasAny(Temporal) ?
                    /* if temporal, normal is not necessarily equal to the task's target */
                    Image.imageNormalize(imageInput.term())
                    :
                    normal;

            SpecialTermTask transformedInput = new SpecialTermTask(normalTerm, imageInput);
            if (imageInput.isCyclic())
                transformedInput.setCyclic(true);

            Term image = this.term;
            r.setInput(transformedInput, (TaskConcept)nar.conceptualizeDynamic(image));

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

        @Nullable private BeliefTable table(NAR n, boolean conceptualize) {
            Concept h = host(n, conceptualize);
            if (h == null)
                return null;
            else
                return beliefOrGoal ? h.beliefs() : h.goals();
        }

        @Nullable private TaskConcept host(NAR n, boolean conceptualize) {
            return (TaskConcept) n.concept(normal, conceptualize);
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