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

        Set<Part<NAR>> before = n.part.stream().collect(toSet());

        DurPart d = DurPart.on(n, () -> {
            
        });

        n.synch();

        Set<Part<NAR>> during = n.part.stream().collect(toSet());

        d.off();

        n.synch();

        Set<Part<NAR>> after = n.part.stream().collect(toSet());

        assertEquals(before, after);
        assertEquals(before.size()+1, during.size());
    }
}