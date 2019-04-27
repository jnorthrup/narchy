package nars.link;

import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.decide.Roulette;
import jcog.pri.ScalarValue;
import jcog.sort.RankedN;
import nars.concept.Concept;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/** caches an array of tasklinks tangent to an atom */
public final class TermLinks {

    private volatile long updated;

    final static TaskLink[] EmptyTaskLinksArray = new TaskLink[0];
    private final FasterList<TaskLink> links = new FasterList(0, EmptyTaskLinksArray);

    private final AtomicBoolean busy = new AtomicBoolean(false);

    protected int cap(int bagSize) {
        return Math.max(2, (int) Math.ceil(1.5f * Math.sqrt(bagSize)) /* estimate */);
        //return bagSize;
    }

    private TermLinks(long now, int minUpdateCycles) {
        this.updated = now - minUpdateCycles;
    }

    /** caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference */
    @Nullable
    static Term tangent(TaskLinkBag bag, Concept src, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {

//        System.out.println(src);

        String id = bag.id(in, out);


//        Reference<TermLinks> matchRef = src.meta(id);
//        TermLinks match = matchRef != null ? matchRef.get() : null;

        TermLinks match = src.meta(id);

        if (match == null) {
            //src.meta(id, new SoftReference<>(match));
            match = new TermLinks(now, minUpdateCycles);
            src.meta(id, match);
        }

        return match.sample( src.term(), bag, punc, filter, in, out, now, minUpdateCycles, rng);
    }

    public boolean refresh(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse, long now, int minUpdateCycles) {
        if (now - updated >= minUpdateCycles) {

            if (!busy.compareAndSet(false, true))
                return false;
            try {
                /*synchronized (links)*/ {
                    int cap = cap(itemCount);

                    RankedN<TaskLink> match = null;

                    int i = 0;
                    for (TaskLink t : items) {
//                if (t == null)
//                    continue; //HACK

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


                    links.clear();
                    if (match != null) {
                        int ms = match.size();
                        if (ms > 0) {
                            links.ensureCapacity(ms);
                            match.forEach(links::addFast);
                        }
                    }

                }
            } finally {
                busy.set(false);
            }

            updated = now;
        }

        return !links.isEmpty();
    }


    @Nullable public Term sample(Predicate<TaskLink> filter, byte punc, Random rng) {
        TaskLink l;
        @Nullable FasterList<TaskLink> ll = links;
        int lls = ll.size();
        if (lls == 0)
            return null;
        else {
            TaskLink[] lll = ll.array();
            lls = Math.min(lll.length, lls);
            if (lls == 0) return null;
            else if (lls == 1) l = lll[0];
            else {
                int li = Roulette.selectRouletteCached(lls, (int i) -> {
                    TaskLink x = lll[i];
                    return (x != null && filter.test(x)) ?
                            Math.max(ScalarValue.EPSILON, x.priPunc(punc)) : Float.NaN;
                }, rng::nextFloat);
                l = li >= 0 ? lll[li] : null;
            }
            return l != null ? l.from() : null;
        }

    }

    public final Term sample(Term  srcTerm, Table<?, TaskLink> bag, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
        return sample(srcTerm, bag, bag.capacity(), punc, filter, in, out, now, minUpdateCycles, rng);
    }

    public final Term sample(Term srcTerm, Iterable<TaskLink> items, int itemCount, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
        return refresh(srcTerm, items, itemCount, out, now, minUpdateCycles) ?
                sample(filter, punc, rng) : null;
    }


}
