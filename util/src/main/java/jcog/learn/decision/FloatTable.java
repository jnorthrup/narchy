package jcog.learn.decision;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.data.list.FasterList;

import java.io.PrintStream;
import java.util.List;

/**
 * table of float[]'s of uniform length, each column labeled by a unique header (H)
 */
@Deprecated public class FloatTable<H> {

    public final List<float[]> rows = new FasterList();
    public final H[] cols;

    @SafeVarargs
    public FloatTable(H... cols) {
        this.cols = cols;
    }

    public FloatTable<H> add(float... row) {
        assert (row.length == cols.length);
        rows.add(row);
        return this;
    }

    public FloatTable<H> print(PrintStream out) {
        System.out.println(Joiner.on("\t").join(cols));
        for (float[] row : rows) {
            String s = Texts.INSTANCE.n4(row);
            out.println(s);
        }
        return this;
    }

    public int size() {
        return rows.size();
    }
}
