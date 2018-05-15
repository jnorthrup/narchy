package nars.op.kif;

import nars.*;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

class KIFInputTest {

    @Test
    public void testSUMOViaMemory() throws Narsese.NarseseException {
        String sumo = "People";
        String inURL = "file:///home/me/sumo/" + sumo + ".kif";

        NAR n = NARS.shell();
        n.memory.on(KIFInput.intoNAL);
        Set<Supplier<Stream<Task>>> readers = n.memory.readers($.quote(inURL)).collect(toSet());
        System.out.println(readers);

        //n.input("copy(\" + inURL + "\", \"file:///tmp/" + sumo + ".nal\")");
    }
}