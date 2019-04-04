package nars.web;

/** temporary minimal client-side Task representation */
public class TaskJson {
    final String term;
    final String truth; //TODO: final Truth truth;
    final char punc;
    final long start, end;
    float pri;

    public TaskJson(String term, String truth, char punc, long start, long end, float pri) {
        this.term = term;
        this.truth = truth;
        this.punc = punc;
        this.start = start;
        this.end = end;
        this.pri = pri;
    }

    @Override
    public String toString() {
        return new StringBuilder().append('$').append(pri).append(' ').append(term).append(punc).append(' ').append(truth).append(' ').append(start).append(' ').append(end).toString();
    }
}
