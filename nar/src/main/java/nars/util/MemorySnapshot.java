package nars.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;

/** a snapshot of a set of concepts */
public class MemorySnapshot {

    public final Multimap<Term,Concept> byAnon = MultimapBuilder.hashKeys().arrayListValues().build();

    public MemorySnapshot() {

    }

    public MemorySnapshot(NAR n) {
        n.memory.forEach(this::add);
    }

    protected void add(Termed /*Concept*/ _c) {
        var c = (Concept)_c;
        var t = c.term();
        //index
        byAnon.put(t.anon(), c);
    }

}
