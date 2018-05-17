package nars.op.kif;

import jcog.data.graph.AdjGraph;
import jcog.data.graph.GraphMeter;
import nars.*;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;

@Disabled
class KIFInputTest {

    @Test
    public void testSUMOViaMemory() {
        String sumo =
                "Transportation";
                //"People";
                //"Merge";
                //"Law";
        String inURL = "file:///home/me/sumo/" + sumo + ".kif";

        NAR n = NARS.shell();
        n.memory.on(KIFInput.load);


        Term I = $$(inURL);
        Term O =
            //n.self();
            //Atomic.the("stdout");
            Atomic.the("file:///tmp/x.nalz");

        Runnable r = n.memory.copy(I, O);
        r.run();

        //n.input("copy(\" + inURL + "\", \"file:///tmp/" + sumo + ".nal\")");
    }
    @Test
    public void testSUMOViaMemory2() {
        String sumo =
                "Transportation";
                //"People";
                //"Merge";
                //"Law";
        String inURL = "file:///home/me/sumo/" + sumo + ".kif";

        NAR n = NARS.
                tmp();
                //shell();

        n.memory.on(KIFInput.load);


        Term I = $.quote(inURL);
        Term O =
                //Atomic.the("stdout");
                //Atomic.the("file:///tmp/x.nal");
                n.self();

        n.log();

        Runnable r = n.memory.copy(I, O);
        r.run();


        n.run(10000);

        AdjGraph<Term, Task> structure = new AdjGraph<>(true);
        n.tasks().forEach(t -> {
            switch (t.op()) {
                case INH: {
                    int s = structure.addNode(t.sub(0));
                    int p = structure.addNode(t.sub(1));
                    structure.edge(s, p, t);
                    break;
                }
                //TODO: sim, impl, etc
            }
        });

        //System.out.println(structure);
        structure.nodeSet().forEach((t) -> {
            System.out.println(t + " " +
                    GraphMeter.clustering((AdjGraph)structure, t)
            );
        });


        //n.input("copy(\" + inURL + "\", \"file:///tmp/" + sumo + ".nal\")");
    }
}