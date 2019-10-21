package nars.time.event;

import nars.NAR;
import nars.NARS;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhenInternalTest {

    @Test void one() {
        NAR n = NARS.tmp();

        List<? extends WhenInternal> aList = n.when().collect(toList());
        assertEquals(new HashSet(aList).size(), aList.size(), new Supplier<String>() {
            @Override
            public String get() {
                return "duplicate events found";
            }
        });

        Map<Term, List<WhenInternal>> a = n.whens();

        assertTrue(a.size() > 1);

        for (Map.Entry<Term, List<WhenInternal>> entry : a.entrySet()) {
            Term c = entry.getKey();
            List<WhenInternal> ee = entry.getValue();
            System.out.println(c);
            for (WhenInternal e : ee) {
                System.out.println("\t" + e.term());
            }
        }

    }
}