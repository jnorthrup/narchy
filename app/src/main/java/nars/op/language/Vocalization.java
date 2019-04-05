package nars.op.language;

import com.google.common.collect.TreeBasedTable;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.control.Part;
import nars.term.Term;
import nars.time.part.DurPart;
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

public class Vocalization extends Part {

    public static final Term PREPOSITION = $.the("preposition");
    public static final Term PRONOUN = $.the("pronoun");
    /**
     * when, word, truth
     */
    final TreeBasedTable<Long, Term, TruthAccumulator> vocalize = TreeBasedTable.create();
    private final Consumer<Term> speak;
    private final float durationsPerWord;
    private float energy;
    private final float expectationThreshold = 0.5f;

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
                DurPart.on(nar, () -> {
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


        float dur = nar.dur() * durationsPerWord;
        long now = nar.time();
        long startOfNow = now - (int) Math.ceil(dur);
        long endOfNow = now + (int) Math.floor(dur);


        FasterList<Pair<Term, Truth>> pending = new FasterList<>(0);
        synchronized (vocalize) {


            SortedSet<Long> tt = vocalize.rowKeySet().headSet(endOfNow);

            if (!tt.isEmpty()) {
                LongArrayList ll = new LongArrayList(tt.size());
                tt.forEach(ll::add);

                ll.forEach(t -> {
                    Set<Map.Entry<Term, TruthAccumulator>> entries = vocalize.row(t).entrySet();
                    if (t >= startOfNow) {
                        entries.forEach(e -> {
                            Truth x = e.getValue().commitSum();
                            if (x.expectation() > expectationThreshold)
                                pending.add(Tuples.pair(e.getKey(), x));
                        });
                    }
                    entries.clear();
                });
            }
        }
        if (pending.isEmpty())
            return true;


        Term spoken = decide(pending);
        if (spoken != null)
            speak.accept(spoken);


        return true;
    }

    /**
     * default greedy decider by truth expectation
     */
    @Nullable
    private Term decide(FasterList<Pair<Term, Truth>> pending) {
        return pending.max((a, b) -> {
            float ta = a.getTwo().expectation();
            float tb = b.getTwo().expectation();
            int tab = Float.compare(ta, tb);
            if (ta > tb) {
                return tab;
            } else {
                return a.getOne().compareTo(b.getOne());
            }
        }).getOne();
    }
}
