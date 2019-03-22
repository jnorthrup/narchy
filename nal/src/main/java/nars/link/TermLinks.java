package nars.link;

import jcog.data.list.table.Table;
import jcog.decide.Roulette;
import jcog.pri.ScalarValue;
import jcog.sort.RankedN;
import nars.concept.Concept;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Random;
import java.util.function.Predicate;

/** caches an array of tasklinks tangent to an atom */
public final class TermLinks {

    private volatile long updated;

    @Nullable
    private volatile TaskLink[] links;

    public TermLinks(long now, int minUpdateCycles) {
        this.updated = now - minUpdateCycles;
    }

    /** caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference */
    @Nullable public static Term tangent(TaskLinkBag bag, Concept src, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {

//        System.out.println(src);

        String id = bag.id(in, out);

        Reference<TermLinks> matchRef = src.meta(id);
        TermLinks match = matchRef != null ? matchRef.get() : null;

        //TermLinks match = src.meta(id);

        if (match == null) {
            match = new TermLinks(now, minUpdateCycles);
            src.meta(id, new SoftReference<>(match));
            //src.meta(id, match);
        }

        return match.sample( src.term(), bag, punc, filter, in, out, now, minUpdateCycles, rng);
    }

    public boolean refresh(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse, long now, int minUpdateCycles) {
        if (now - updated >= minUpdateCycles) {

            int cap = Math.max(4, (int) Math.ceil(2 * Math.sqrt(itemCount)) /* estimate */);

            RankedN<TaskLink> match = null;

            int i = 0;
            for (TaskLink t : items) {
                if (t == null)
                    continue; //HACK

                float xp = t.priElseZero();
                if (match == null || /*match instanceof Set ||*/ (match instanceof RankedN && xp > match.minValueIfFull())) {
                    Term y = t.other(x, reverse);
                    if (y != null) {

                        if (match == null) {
//                            @Nullable RankedN<TaskLink> existingLinks = this.links;
//                            if (existingLinks==null)
                                match = new RankedN<>(new TaskLink[Math.min(itemCount - i, cap)], (FloatFunction<TaskLink>) TaskLink::pri);
//                            else {
//                                //recycle
//                                //  this will affect other threads that might be reading from it.
//                                //  so use 'clearWeak' atleast it wont nullify values while they might be reading from the array
//                                existingLinks.clearWeak();
//                                match = existingLinks;
//                            }
                        }

                        match.add(t);
                    }
                }
                i++;
            }


            links = match!=null ? match.toArrayIfSameSizeOrRecycleIfAtCapacity(TaskLink.EmptyTaskLinkArray) : null;
            updated = now;
        }

        return links != null;
    }

    @Nullable public Term sample(Predicate<TaskLink> filter, byte punc, Random rng) {
        TaskLink l;
        TaskLink[] ll = links;
        if (ll == null)
            l = null;
        else {
            int li = Roulette.selectRouletteCached(ll.length, (int i)-> {
                TaskLink x = ll[i];
                return filter.test(x) ?
                                Math.max(ScalarValue.EPSILON, x.priPunc(punc)) : Float.NaN;
            }, rng::nextFloat);
            l = li >= 0 ? ll[li] : null;
        }
        return l != null ? l.from() : null;
    }

    public final Term sample(Term  srcTerm, Table<nars.link.TaskLink,nars.link.TaskLink> bag, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
        return sample(srcTerm, bag, bag.capacity(), punc, filter, in, out, now, minUpdateCycles, rng);
    }

    public final Term sample(Term srcTerm, Iterable<TaskLink> items, int itemCount, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
        return refresh(srcTerm, items, itemCount, out, now, minUpdateCycles) ?
                sample(filter, punc, rng) : null;
    }

}
