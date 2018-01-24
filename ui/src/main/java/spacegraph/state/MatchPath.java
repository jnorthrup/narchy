package spacegraph.state;

import jcog.list.FasterList;

public class MatchPath extends FasterList<String> {

    public static final String STAR = "*";

    public MatchPath(int estSize) {
        super(estSize);
    }
}
