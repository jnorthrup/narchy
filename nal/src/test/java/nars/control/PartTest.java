package nars.control;

import jcog.service.Service;
import nars.NAR;
import nars.NARS;
import nars.time.part.DurPart;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PartTest {
    @Test
    void testRemoveDurServiceWhenOff() {
        NAR n = NARS.shell();

        Set<Service<NAR>> before = n.part.stream().collect(toSet());

        DurPart d = DurPart.on(n, () -> {
            
        });

        n.synch();

        Set<Service<NAR>> during = n.part.stream().collect(toSet());

        d.off();

        n.synch();

        Set<Service<NAR>> after = n.part.stream().collect(toSet());

        assertEquals(before, after);
        assertEquals(before.size()+1, during.size());
    }
}