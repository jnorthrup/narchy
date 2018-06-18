package nars.control;

import jcog.Service;
import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NARServiceTest {
    @Test
    void testRemoveDurServiceWhenOff() {
        NAR n = NARS.shell();

        Set<Service<NAR>> before = n.services.stream().collect(toSet());

        DurService d = DurService.on(n, () -> {
            
        });

        n.synch();

        Set<Service<NAR>> during = n.services.stream().collect(toSet());

        d.off();

        n.synch();

        Set<Service<NAR>> after = n.services.stream().collect(toSet());

        assertEquals(before, after);
        assertEquals(before.size()+1, during.size());
    }
}