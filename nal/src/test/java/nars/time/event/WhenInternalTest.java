package nars.time.event;

import nars.NAR;
import nars.NARS;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhenInternalTest {

    @Test void test1() {
        NAR n = NARS.tmp();

        List<? extends WhenInternal> aList = n.when().collect(toList());
        assertEquals(new HashSet(aList).size(), aList.size(), ()->"duplicate events found");

        Map<Term, List<WhenInternal>> a = n.atMap();

        assertTrue(a.size() > 1);

        a.forEach((c, ee) -> {
            System.out.println(c);
            ee.forEach(e->System.out.println("\t" + e.term()));
        });

    }
}