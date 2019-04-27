package nars.task;

import jcog.data.list.FasterList;
import jcog.data.map.CompactArrayMap;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * extended: with meta table
 */
public class NALTaskX extends GenericNALTask implements jcog.data.map.MetaMap {

    private final CompactArrayMap<String, Object> meta = new CompactArrayMap<>();

    NALTaskX(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) throws TaskException {
        super(term, punc, truth, creation, start, end, stamp);
    }

    @Override
    public @Nullable List log(boolean createIfMissing) {
        if (createIfMissing)
            return meta("!", (x) -> new FasterList(1));
        else
            return meta("!");
    }

    @Override
    public <X> X meta(String key, Function<String, X> valueIfAbsent) {
        CompactArrayMap<String, Object> m = this.meta;
        return m != null ? (X) m.computeIfAbsent(key, valueIfAbsent) : null;
    }

    @Override
    public Object meta(String key, Object value) {
        CompactArrayMap<String, Object> m = this.meta;
        if (m != null) m.put(key, value);
        return value;
    }

    @Override
    public <X> X meta(String key) {
        CompactArrayMap<String, Object> m = this.meta;
        return m != null ? (X) m.get(key) : null;
    }
}
