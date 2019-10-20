package nars.op.language;

import com.google.common.collect.TreeBasedTable;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.util.TruthAccumulator;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

public class Vocalization extends NARPart {

    public static final Term PREPOSITION = $.the("preposition");
    public static final Term PRONOUN = $.the("pronoun");
    /**
     * when, word, truth
     */
    final TreeBasedTable<Long, Term, TruthAccumulator> vocalize = TreeBasedTable.create();
    private final Consumer<Term> speak;
    private final float durationsPerWord;
    private float energy;
    private static final float expectationThreshold = 0.5f;

    public Vocalization(NAR nar, float durationsPerWord, Consumer<Term> speak) {
        super();
        this.durationsPerWord = durationsPerWord;
        this.speak = speak;
        this.energy = 0;
        nar.add(this);
    }

    @Override
    protected void starting(NAR nar) {
        on(
                nar.onDur(() -> {
                    energy = Math.min(1f, energy + 1f / (this.durationsPerWord));
                    if (energy >= 1f) {
                        energy = 0;
                        next();
                    }
                }),
                nar.eventClear.on(this::clear)
        );

    }

    public void speak(@Nullable Term word, long when, @Nullable Truth truth) {
        if (word == null)
            return;


//        if (when < nar.time() - nar.dur() * durationsPerWord) {
//            return;
//        }

        TruthAccumulator ta;
        synchronized (vocalize) {
            ta = vocalize.get(when, word);
            if (ta == null) {
                ta = new TruthAccumulator();
                vocalize.put(when, word, ta);
            }
        }

        ta.add(truth);

//        System.out.println(when + " " + word + " " + truth);

    }

    private void clear() {
        synchronized (vocalize) {
            vocalize.clear();
        }
    }

    public boolean next() {


        var dur = nar.dur() * durationsPerWord;
        var now = nar.time();
        var startOfNow = now - (int) Math.ceil(dur);
        var endOfNow = now + (int) Math.floor(dur);


        var pending = new FasterList<Pair<Term, Truth>>(0);
        synchronized (vocalize) {


            var tt = vocalize.rowKeySet().headSet(endOfNow);

            if (!tt.isEmpty()) {
                var ll = new LongArrayList(tt.size());
                for (var aLong : tt) {
                    ll.add(aLong);
                }

                ll.forEach(t -> {
                    var entries = vocalize.row(t).entrySet();
                    if (t >= startOfNow) {
                        for (var e : entries) {
                            var x = e.getValue().commitSum();
                            if (x.expectation() > expectationThreshold)
                                pending.add(Tuples.pair(e.getKey(), x));
                        }
                    }
                    entries.clear();
                });
            }
        }
        if (pending.isEmpty())
            return true;


        var spoken = decide(pending);
        if (spoken != null)
            speak.accept(spoken);


        return true;
    }

    /**
     * default greedy decider by truth expectation
     */
    private @Nullable
    static Term decide(FasterList<Pair<Term, Truth>> pending) {
        return pending.max((a, b) -> {
            var ta = a.getTwo().expectation();
            var tb = b.getTwo().expectation();
            if (ta > tb) {
                var tab = Float.compare(ta, tb);
                return tab;
            } else {
                return a.getOne().compareTo(b.getOne());
            }
        }).getOne();
    }
}
