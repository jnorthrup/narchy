package nars.term.util;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.control.proto.Reaction;
import nars.control.proto.Remember;
import nars.control.proto.TaskLinkTask;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.EmptyBeliefTable;
import nars.task.ITask;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.Answer;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.var.ImDep;
import org.apache.commons.lang3.ArrayUtils;
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

                ImDep target = intOrExt ? ImgInt : ImgExt;

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

    public static Term imageNormalize(Term _t) {
        if (!(_t instanceof Compound) || !_t.hasAll(ImageBits))
            return _t;

        return _imageNormalize(_t);
    }


    public static Term _imageNormalize(Term z) {
        boolean negated;

        Term t;
        Op o = z.op();
        if (o == NEG) {
            negated = true;
            t = z.unneg();
            o = t.op();
        } else {
            t = z;
            negated = false;
        }

        if (o == INH /*&& t.hasAll(ImageBits)*/) {
            Term s = t.sub(0);
            Subterms ss = null;
            boolean isInt = s.op() == PROD && (ss = s.subterms()).contains(Op.ImgInt);// && !ss.contains(Op.ImgExt);

            Term p = t.sub(1);
            Subterms pp = null;
            boolean isExt = p.op() == PROD && (pp = p.subterms()).contains(Op.ImgExt);// && !pp.contains(Op.ImgInt);

            Term u;
            if (isInt && !isExt) {

                u = INH.the(ss.sub(0), PROD.the(Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p)));

            } else if (isExt && !isInt) {

                u = INH.the(PROD.the(Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s)), pp.sub(0));

            } else {
                return z;
            }

            if (!(u instanceof Bool))
                return Image.imageNormalize(u).negIf(negated);

        }

        return z;

    }

    public static class ImageBeliefTable extends EmptyBeliefTable {

        /**
         * the term of the concept which this relies on for storage of beliefs, and thus remains consistent with
         */
        public final Term image, normal;
        private final boolean beliefOrGoal;

        @Nullable
        transient Concept host = null;

        public ImageBeliefTable(Term image, Term imageNormalized, boolean beliefOrGoal) {
            this.image = image;
            this.normal = imageNormalized;
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
            return t == null || t.isEmpty();
        }

        @Override
        public void match(Answer m) {
            BeliefTable table = relink(m.nar, false);
            if (table == null)
                return;

            //TODO rewrite matched entries?
            table.match(m);

            int results = m.tasks.size();
            if (results > 0) {
            //HACK rewrite the tasks directly in the TopN selection
                Object[] tt = m.tasks.items;
                for (int i = 0; i < results; i++) {
                    tt[i] = new SpecialTermTask(image, (Task) tt[i]);
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
            r.setInput(transformedInput, host);
            table.add(r, nar);

            FasterList<ITask> rememberance = r.remembered;
//            if (rememberance.contains(transformedInput))
//                rememberance.replaceAll((x)->x == transformedInput ? originalInput : x); //for TaskEvent emission
//            else {
                if (!transformedInput.isDeleted()) {
                    rememberance.add(new TaskLinkTask(originalInput));
                    rememberance.add(new Reaction(originalInput));
                }
//            }
        }

        @Nullable private BeliefTable relink(NAR n, boolean conceptualize) {
            Concept h = host;
            if (h == null || h.isDeleted()) {
                h = (host = n.concept(normal, conceptualize)); //TODO set atomically
                if (h == null)
                    return null;
            }
            return beliefOrGoal ? h.beliefs() : h.goals();
        }

    }
}