package nars.control;

import jcog.Util;
import jcog.service.Part;
import nars.NAR;
import nars.NARS;
import nars.time.part.DurLoop;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NARPartTest {
    @Test
    void testRemoveDurServiceWhenDelete() {
        NAR n = NARS.shell();

        Set<Part<NAR>> before = n.partStream().collect(toSet());

        DurLoop d = n.onDur(() -> {

        });

        Util.sleepMS(100);

        assertTrue(d.isOn());

        d.pause();

        n.synch();

        Set<Part<NAR>> during = n.partStream().collect(toSet());

        d.delete();

        n.synch();

        Set<Part<NAR>> after = n.partStream().collect(toSet());

        assertEquals(before.size()+1, during.size());

        //assertEquals(before, after);
    }
}