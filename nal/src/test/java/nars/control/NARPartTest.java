package nars.control;

import jcog.service.Part;
import nars.NAR;
import nars.NARS;
import nars.time.part.DurPart;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NARPartTest {
    @Test
    void testRemoveDurServiceWhenOff() {
        NAR n = NARS.shell();

        Set<Part<NAR>> before = n.partStream().collect(toSet());

        DurPart d = DurPart.on(n, () -> {
            
        });

        n.synch();

        Set<Part<NAR>> during = n.partStream().collect(toSet());

        d.off();

        n.synch();

        Set<Part<NAR>> after = n.partStream().collect(toSet());

        assertEquals(before, after);
        assertEquals(before.size()+1, during.size());
    }
}