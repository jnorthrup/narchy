package nars.control;

import jcog.service.Part;
import nars.NAR;
import nars.NARS;
import nars.time.part.DurLoop;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NARPartTest {
    @Test
    void testRemoveDurServiceWhenOff() {
        NAR n = NARS.shell();

        Set<Part<NAR>> before = n.partStream().collect(toSet());

        DurLoop d = n.onDur(() -> {

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