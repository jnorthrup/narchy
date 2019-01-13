package nars.term.util;

import jcog.Util;
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
import nars.table.dynamic.DynamicTaskTable;
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


    public static class ImageBeliefTable extends DynamicTaskTable {

        /**
         * the term of the concept which this relies on for storage of beliefs, and thus remains consistent with
         */
        public final Term normal;



        public ImageBeliefTable(Term image, boolean beliefOrGoal) {
            super(image, beliefOrGoal);

            Term imageNormalized = Image.imageNormalize(image);
            assert(!image.equals(imageNormalized) && imageNormalized.op()==INH);
            this.normal = imageNormalized;
        }

        @Override
        public void sample(Answer m) {
            match(m, false);
        }

        @Override
        public void match(Answer m) {
            match(m, true);
        }

        private void match(Answer m, boolean matchOrSample) {
            BeliefTable table = table(m.nar, false);
            if (table == null)
                return;

            if (matchOrSample)
                table.match(m);
            else
                table.sample(m);

            int results = m.tasks.size();
            if (results > 0) {
            //HACK apply this as an add-on transformation to a final result, not every intermediate possible result
                Ranked<Task>[] tt = m.tasks.items;
                Term image = this.term;
                for (int i = 0; i < results; i++) {
                    tt[i].set(new ImgTermTask(image, (tt[i].x)));
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
                    /* if temporal, normal is not necessarily equal to the task's term */
                    Image.imageNormalize(imageInput.term())
                    :
                    normal;

            SpecialTermTask transformedInput = new SpecialTermTask(normalTerm, imageInput);
            if (imageInput.isCyclic())
                transformedInput.setCyclic(true);

            Term image = this.term;
            r.setInput(transformedInput, (TaskConcept)nar.concept(image));

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