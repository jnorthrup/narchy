package nars.link;

import jcog.decide.Roulette;
import jcog.pri.bag.Bag;
import jcog.sort.RankedTopN;
import jcog.sort.TopN;
import nars.concept.NodeConcept;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Random;
import java.util.Set;

/** caches an array of tasklinks tangent to an atom */
public final class AtomLinks {

    private volatile long updated;

    @Nullable
    private volatile TaskLink[] links;

    public AtomLinks(long now, int minUpdateCycles) {
        this.updated = now - minUpdateCycles;
    }

    /** caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference */
    @Nullable public static Term tangent(TaskLinkBag bag, NodeConcept src, TaskLink except, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {

        String id = bag.id(in, out);

        Reference<AtomLinks> matchRef = src.meta(id);
        AtomLinks match = matchRef != null ? matchRef.get() : null;
        if (match == null) {
            match = new AtomLinks(now, minUpdateCycles);
            src.meta(id, new SoftReference<>(match));
        }

        return match.sample( (Atomic)(src.term()), bag, except, in, out, now, minUpdateCycles, rng);
    }

    public boolean refresh(Atomic x, Iterable<TaskLink> items, int itemCount, boolean in, boolean out, long now, int minUpdateCycles) {
        if (now - updated >= minUpdateCycles) {

            int cap = Math.max(4, (int) Math.ceil(Math.sqrt(itemCount))); //heuristic

            Iterable<TaskLink> match = null;

            for (TaskLink t : items) {
                if (t == null) continue; //HACK
                float xp = t.priElseZero();
                if (match == null || match instanceof Set || (match instanceof TopN && xp > ((TopN)match).minValueIfFull())) {
                    Term y = atomOther(x, t, in, out);
                    if (y != null) {

                        if (match == null)
                            match = new UnifiedSet<>(cap+1, 0.99f);

                        if (match instanceof Set) {
                            if (((Set)match).add(t)) {
                                if (((Set)match).size() >= cap) {
                                    //upgrade to TopN
                                    RankedTopN<TaskLink> mm = new RankedTopN<>(new TaskLink[cap], (FloatFunction<TaskLink>) TaskLink::pri);
                                    match.forEach(mm::add);
                                    match = mm;
                                }
                            }
                        } else if (match instanceof TopN) {
                            ((TopN)match).add(t);
                        }

                    }
                }
            }
            if (match == null)
                links = null;
            else if (match instanceof Set) {
                links = ((Set<TaskLink>) match).toArray(
                            links!=null && links.length == ((Set)match).size() ?
                                links /* recycle */
                                :
                                TaskLink.EmptyTaskLinkArray
                        );
            } else if (match instanceof TopN)
                links = ((TopN<TaskLink>)match).toArrayIfSameSizeOrRecycleIfAtCapacity(links);

            updated = now;
        }

        return links != null;
    }

    public Term sample(TaskLink except, Random rng) {
        TaskLink l;
        TaskLink[] ll = links;
        if (ll == null)
            l = except;
        else {
            switch (ll.length) {
                case 0:
                    l = except; //only option (rare)
                    break;
                case 1:
                    l = ll[0]; //only option
                    break;
                case 2:
                    TaskLink a = ll[0], b = ll[1];
                    l = a.equals(except) ? b : a; //choose the one which is not 'except'
                    break;
                //TODO optimized case 3
                default:
                    l = ll[Roulette.selectRouletteCached(ll.length, (int i) -> {
                        TaskLink t = ll[i];
                        if (t.equals(except))
                            return 0f;
                        else
                            return t.priElseZero();
                    }, rng)];
                    break;
            }
        }

        return l != null ? l.source() : null;
    }

    public final Term sample(Atomic srcTerm, Bag<TaskLink,TaskLink> bag, TaskLink except, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
        return sample(srcTerm, bag, bag.size(), except, in, out, now, minUpdateCycles, rng);
    }

    public final Term sample(Atomic srcTerm, Iterable<TaskLink> items, int itemCount, TaskLink except, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
        return refresh(srcTerm, items, itemCount, in, out, now, minUpdateCycles) ?
                sample(except, rng) : null;
    }

    @Nullable static private Term atomOther(Term x, TaskLink t, boolean in, boolean out) {
        Term tt = t.target();
        if (out && x.equals(tt)) {
            Term y = t.source();
            return y; //!t.isSelf() ? y : null;
        }
        if (in && x.equals(t.source())) {
            Term y = tt;
            return y; //!t.isSelf() ? y : null;
        }
        return null;
    }
}
