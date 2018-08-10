package nars.time;

import jcog.TODO;
import nars.term.Term;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

public abstract class Event implements LongObjectPair<Term> {

    public final Term id;
    private final int hash;

    Event(Term id, int hash) {
        this.id = id;
        this.hash = hash;

    }


    abstract public long start();

    abstract public long end();

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        Event e = (Event) obj;
        return (hash == e.hash) && (start() == e.start()) && (end() == e.end()) && id.equals(e.id);
    }


    @Override
    public final String toString() {
        long s = start();

        if (s == TIMELESS) {
            return id.toString();
        } else if (s == ETERNAL) {
            return id + "@ETE";
        } else {
            long e = end();
            if (e == s)
                return id + "@" + s;
            else
                return id + "@" + s + ".." + e;
        }
    }

    @Override
    public long getOne() {
        return start();
    }

    @Override
    public Term getTwo() {
        return id;
    }

    @Override
    public int compareTo(LongObjectPair<Term> o) {
        throw new TODO();
    }

    abstract public long dur(); //        return end() - start();

}
