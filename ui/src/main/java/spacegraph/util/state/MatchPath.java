package spacegraph.util.state;

import jcog.data.list.FasterList;

public class MatchPath extends FasterList<String> {

    public static final String STAR = "*";

    public MatchPath(int estSize) {
        super(estSize);
    }
}
