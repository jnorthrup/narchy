package nars.derive.premise;

import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.decide.Roulette;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.concept.Concept;
import nars.derive.model.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinkBag;
import nars.link.TaskLinks;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static nars.Op.ATOM;

abstract public class PremiseSource {

    public abstract void premises(Predicate<Premise> p, When when, TaskLinks links, Derivation d);

    /**
     * samples the tasklink bag for a relevant reversal
     * memoryless, determined entirely by tasklink bag, O(n)
     */
    public static class DirectTangent extends DefaultPremiseSource {

        public static final DirectTangent the = new DirectTangent();

        private DirectTangent() {

        }

        @Override
        protected Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d) {

            //< 1 .. 1.0 isnt good
            float probBase =
                0.5f;
            //0.33f;
            float probDirect =
                //(float) (probBase / Util.sqr(Math.pow(2, target.volume()-1)));
                (float) (probBase / Math.pow(target.volume(), 2));
            //(float) (probBase / Math.pow(2, target.volume()-1));
            //probBase * (target.volume() <= 2 ? 1 : 0);
            //probBase * 1f / Util.sqr(Util.sqr(target.volume()));
            //probBase * 1f / term.volume();
            //probBase * 1f / Util.sqr(term.volume());
            //probBase *  1f / (term.volume() * Math.max(1,(link.from().volume() - term.volume())));

            if (d.random.nextFloat() >= probDirect)
                return null; //term itself

            return sampleReverseMatch(target, link, links, d);

        }

        @Nullable public Term sampleReverseMatch(Term target, TaskLink link, TaskLinks links, Derivation d) {
            final Term[] T = {target};
            links.sampleUnique(d.random, (ll) ->{
                if (ll != link) {
                    Term t = ll.reverseMatch(target);
                    if (t != null) {
                        T[0] = t;
                        return false; //done
                    }
                }
                return true;
            });
            return T[0];
        }
    }

    /**
     * caches ranked reverse atom termlinks in concept meta table
     */
    public static class TangentConceptCaching extends DefaultPremiseSource {

        int ATOM_TANGENT_REFRESH_DURS = 1;

        protected boolean cache(Term target) {
            return target instanceof Atom;
            //return target.volume() <= 3;
        }

        @Override
        @Nullable protected Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d) {

            float probability =
                //0.5f;
                (float) (0.5f / Math.pow(target.volume(), 3));
            //1f/Math.max(2,link.from().volume());
            //1-1f/Math.max(2,link.from().volume());
            //1-1f/(Math.max(1,link.from().volume()-1));
            //1f / (1 + link.from().volume()/2f);
            //1f / (1 + link.from().volume());
            //1 - 1f / (1 + s.volume());
            //1 - 1f / (1 + t.volume());

            if (!(d.random.nextFloat() <= probability))
                return null;

            if (cache(target)) {
                Concept T = d.nar.conceptualize(target);
                if (T != null) {
                    //sample active tasklinks for a tangent match to the atom
                    //Predicate<TaskLink> filter = x -> !link.equals(x);
                    Predicate<TaskLink> filter = ((Predicate<TaskLink>) link::equals).negate();

                    Term z = tangent(links.links,
                        T, task.punc(), filter,
                        false, true,
                        d.time(), Math.round(d.dur() * ATOM_TANGENT_REFRESH_DURS), d.random);
                    return z;
                }
            }

            return DirectTangent.the.sampleReverseMatch(target, link, links, d);
        }

        @Nullable
        static Term tangent(TaskLinkBag bag, Concept src, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {

            //        System.out.println(src);

            String id = bag.id(in, out);


            //        Reference<TermLinks> matchRef = src.meta(id);
            //        TermLinks match = matchRef != null ? matchRef.get() : null;

            Object _match = src.meta(id);
            TaskLinkSnapshot match;
            if (_match!=null) {
                if (_match instanceof Reference)
                    match = (TaskLinkSnapshot) ((Reference) _match).get();
                else
                    match = (TaskLinkSnapshot) _match;
            } else {
                match = new TaskLinkSnapshot(now, minUpdateCycles);
                //src.meta(id, new SoftReference<>(match));
                src.meta(id, match);
            }

            return match.sample(src.term(), bag, punc, filter, in, out, now, minUpdateCycles, rng);
        }


        /**
         * caches an array of tasklinks tangent to an atom
         */
        @Deprecated static final class TaskLinkSnapshot {

            //            final static TaskLink[] EmptyTaskLinksArray = new TaskLink[0];
//            private final FasterList<TaskLink> links = new FasterList(0, EmptyTaskLinksArray);
            final PriArrayBag<TaskLink> links = new PriArrayBag<>(PriMerge.replace, 0);
            private final AtomicBoolean busy = new AtomicBoolean(false);
            private volatile long updated;

            private TaskLinkSnapshot(long now, int minUpdateCycles) {
                this.updated = now - minUpdateCycles;
            }

            /**
             * caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference
             */

            protected int cap(int bagSize) {
                return Math.max(4, (int) Math.ceil(1f * Math.sqrt(bagSize)) /* estimate */);
                //return bagSize;
            }

            public boolean refresh(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse, long now, int minUpdateCycles) {
                if (busy.compareAndSet(false, true)) {
                    try {
                        if (now - updated >= minUpdateCycles) {
                            commit(x, items, itemCount, reverse);
                            updated = now;
                        }
                    } finally {
                        busy.set(false);
                    }
                }

                return !links.isEmpty();
            }

            public void commit(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse) {

                links.capacity(cap(itemCount));

                links.commit();

                for (TaskLink t : items) {
                    Term y = t.other(x, reverse);
                    if (y != null)
                        links.put(t.clone(1));
                }
            }


            @Nullable
            public Term sample(Predicate<TaskLink> filter, byte punc, Random rng) {
                @Nullable Object[] ll = links.items();
                int lls = Math.min(links.size(), ll.length);
                if (lls == 0)
                    return null;
                else {
                    TaskLink l;
                    if (lls == 1) l = (TaskLink) ll[0];
                    else {
                        int li = Roulette.selectRouletteCached(lls, (int i) -> {

                            TaskLink x = (TaskLink) ll[i];
                            return x != null && filter.test(x) ?
                                Math.max(ScalarValue.EPSILON, x.priPunc(punc))
                                :
                                Float.NaN;

                        }, rng::nextFloat);
                        l = li >= 0 ? (TaskLink)ll[li] : null;
                    }
                    return l != null ? l.from() : null;
                }

            }

            public final Term sample(Term srcTerm, Table<?, TaskLink> bag, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
                return sample(srcTerm, bag, bag.capacity(), punc, filter, in, out, now, minUpdateCycles, rng);
            }

            public final Term sample(Term srcTerm, Iterable<TaskLink> items, int itemCount, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
                return refresh(srcTerm, items, itemCount, out, now, minUpdateCycles) ?
                    sample(filter, punc, rng) : null;
            }


        }
    }

    public static class IndexExhaustive extends PremiseSource.TangentConceptCaching {

        @Nullable protected Term tangentRandom(Term target, Derivation d) {
            if (target instanceof Compound && target.hasAny(ATOM)) {
                FasterList<Term> tangent = d.nar.concepts().filter(c -> {

                    Term t = c.term();

                    return
                        t instanceof Compound &&
                            !t.equals(target) &&
//                            t.hasAny(ATOM) &&
                            ((Compound) t).unifiesRecursively(target, z -> z.hasAny(ATOM));

                }).map(Termed::term).collect(Collectors.toCollection(FasterList::new));
                if (!tangent.isEmpty()) {
                    //System.out.println(target + "\t" + tangent);
                    return tangent.get(d.random);
                }
            }
            return null;
        }

        @Override
        protected @Nullable Term forward(Term target, TaskLink link, Task task, Derivation d) {
            Term t = tangentRandom(target, d);
            if (t!=null)
                return t;
            else
                return super.forward(target, link, task, d);
        }
        //        @Override
//        protected Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d) {
//
//            return super.reverse(target, link, task, links, d);
//        }

    }

}
