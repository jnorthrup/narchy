package spacegraph.util.state;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.data.list.FasterList;

import java.util.Iterator;
import java.util.function.Function;

public final class StatePath extends FasterList<Context> {

    public static StatePath toRoot(Context here) {
        var p = new StatePath(8);
        var at = here;
        do {
            p.add(at);
        } while ((at = at.parent())!=null);
        p.reverseThis();
        return p;
    }

    private StatePath(int estimatedSize) {
        super(estimatedSize);
    }

    public MatchPath types(boolean innerGlob) {
        return match((c)->c.getClass().getSimpleName(), innerGlob);
    }

    public MatchPath ids(boolean innerGlob) {
        return match(Context::id, innerGlob);
    }

    private MatchPath match(Function<Context, String> pattern, boolean innerGlob) {
        var s = this.size();
        var m = new MatchPath(innerGlob ? s + (s-1) : s);
        for (var i = 0; i < s; i++) {
            var x = this.get(i);
            m.add(pattern.apply(x));
            if (innerGlob && i < s-1) m.add(MatchPath.STAR);
        }
        return m;
    }

    private static Iterator all(String key) {
        throw new TODO();
    }

    public static <X> X first(String key) {
        return (X)(all(key).next());
    }

    @Override
    public String toString() {
        return Joiner.on('/').join(Iterables.transform(this, Context::id));
    }

}
