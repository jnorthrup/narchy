package nars.op.kif;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.junit.jupiter.api.Test;

class KIFInputTest {

    @Test
    public void testSUMOViaMemory() throws Narsese.NarseseException {
        String sumo =
                //"People";
                //"Merge";
                "Law";
        String inURL = "file:///home/me/sumo/" + sumo + ".kif";

        NAR n = NARS.shell();
        n.memory.on(KIFInput.load);


        Term I = $.quote(inURL);
        Term O =
            //n.self();
            //Atomic.the("stdout");
            Atomic.the("file:///tmp/x.nal");

        Runnable r = n.memory.copy(I, O);
        r.run();

        //n.input("copy(\" + inURL + "\", \"file:///tmp/" + sumo + ".nal\")");
    }
    @Test
    public void testSUMOViaMemory2() {
        String sumo =
                //"People";
                //"Merge";
                "Law";
        String inURL = "file:///home/me/sumo/" + sumo + ".kif";

        NAR n = NARS.shell();
        n.memory.on(KIFInput.load);


        Term I = $.quote(inURL);
        Term O =
                //Atomic.the("stdout");
                //Atomic.the("file:///tmp/x.nal");
                n.self();

        n.log();

        Runnable r = n.memory.copy(I, O);
        r.run();

        n.run(1);

        //n.input("copy(\" + inURL + "\", \"file:///tmp/" + sumo + ".nal\")");
    }
}