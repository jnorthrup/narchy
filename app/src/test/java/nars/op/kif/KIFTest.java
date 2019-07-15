package nars.op.kif;

import jcog.Texts;
import jcog.Util;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.GraphMeter;
import nars.*;
import nars.attention.TaskLinkWhat;
import nars.memory.RadixTreeMemory;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.TermTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static nars.$.$$;

@Disabled
class KIFTest {

    @Test
    public void test1() throws IOException, Narsese.NarseseException {

        String I =
                //"/home/me/sumo/Merge.kif";
                //"/home/me/sumo/tinySUMO.kif";
                "/home/me/sumo/ComputerInput.kif";

        //String O = "/home/me/d/sumo_merge.nal";
        KIF k = KIF.file(I);
//        k.tasks.forEach(bb -> System.out.println(bb));

        NAR n = new NARS().index(new RadixTreeMemory(128*1024)).get();
        n.termVolMax.set(24);
        n.beliefPriDefault.amp(0.01f);


        TaskLinkWhat w = (TaskLinkWhat) n.what();
        w.links.decay.set(0.25f);
        w.links.links.capacity(1024);

        n.input(k.tasks());
        n.log();
        //n.input("$1.0 ({?ACT}-->JoystickMotion)?");
        n.input("$1.0 classIntersection(?1,?2)?");
        n.run(1000);
        w.links.links.print();

    }

    @Test
    void testFormulaToArgs1() {
        TermTest.assertEq(
                "(&&,reflexiveOn(#RELATION,#CLASS),({#RELATION}-->SymmetricRelation),({#RELATION}-->TransitiveRelation))",
                new KIF().
                        formulaToTerm("(and (instance ?RELATION TransitiveRelation) (instance ?RELATION SymmetricRelation) (reflexiveOn ?RELATION ?CLASS))", 1)
        );
    }

    @Test
    public void testSUMOViaMemory() {
        String sumo =
                "Transportation";
        //"People";
        //"Merge";
        //"Law";
        String inURL = "file:///home/me/sumo/" + sumo + ".kif";

        NAR n = NARS.shell();
        n.memoryExternal.on(KIF.load);


        Term I = $$(inURL);
        Term O =
                //n.self();
                //Atomic.the("stdout");
                Atomic.the("file:///tmp/x.nalz");

        Runnable r = n.memoryExternal.copy(I, O);
        r.run();


    }

    @Test
    public void testSUMOViaMemory2() {
        String sumo =
                "Merge";
        //"People";
        //"Merge";
        //"Law";
        String inURL = "file:///home/me/sumo/" + sumo + ".kif";

        NAR n = NARS.
                tmp();


        n.memoryExternal.on(KIF.load);


        Term I = $.quote(inURL);
        Term O =
                //Atomic.the("stdout");
                //Atomic.the("file:///tmp/x.nal");
                n.self();

        n.log();

        Runnable r = n.memoryExternal.copy(I, O);
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

            }
        });


        structure.nodeSet().forEach((t) -> {
            System.out.println(t + " " +
                    GraphMeter.clustering((AdjGraph) structure, t)
            );
        });


    }

    @Test
    public void testGenerate() {
        String sumoDir = "file:///home/me/sumo/";

        try {
            Files.createDirectory(Paths.get("/tmp/sumo"));
        } catch (IOException e) {
            //e.printStackTrace();
        }

        NAR n = NARS.shell();

        n.memoryExternal.on(KIF.load);


        n.memoryExternal.contents(sumoDir).parallel().forEach(I -> {
            String ii = Texts.unquote(I.toString());
            if (!ii.endsWith(".kif"))
                return;

            if (ii.contains("WorldAirports")) //exclusions
                return;

            String name = ii.substring(ii.lastIndexOf('/') + 1, ii.lastIndexOf('.')).toLowerCase();

            n.memoryExternal.copy(I, Atomic.the("file:///tmp/sumo/" + name + ".nalz")).run();
            n.memoryExternal.copy(I, Atomic.the("file:///tmp/sumo/" + name + ".nal")).run();
        });

    }

    @Test
    public void testLoad() throws Narsese.NarseseException {


        NAR n = NARS.tmp();
        //NARchy.core();

        n.beliefPriDefault.pri(0.05f);

        n.input("load(\"file:///tmp/sumo/merge.nalz\");");
//        n.input("load(\"file:///tmp/sumo/Mid-level-ontology.kif.nalz\");");
//        n.input("load(\"file:///tmp/sumo/FinancialOntology.kif.nalz\");");
//        n.input("load(\"file:///tmp/sumo/Economy.kif.nalz\");");
        n.run(1);
        System.err.println(n.memory.size() + " concepts");
        n.clear();

        n.logPriMin(System.out, 0.01f);


//        Deriver.derivers(n).forEach( (d)->
//                ((DefaultDerivePri)(((BatchDeriver)d).budgeting))
//                        .gain.setAt(0.2f) );

        n.input("$1.0 possesses(I,#everything)!");
        n.input("$1.0 uses(#anything, I).");
        n.input("$1.0 --Dead:{I}!");
        n.input("$1.0 Human:{I}.");
        n.input("$1.0 --needs(I, #all)!");
        n.input("$1.0 --lacks(I, #anything)!");
        n.input("$1.0 benefits(#all, I)!");
        n.input("$1.0 --suffers(#anything, I)!");
        n.input("$1.0 income(I, #money, #anyReason)!");
        n.input("$1.0 --(I-->Hungry)!");
        n.input("$1.0 --(I-->Thirsty)!");
        n.input("$1.0 enjoys(I,?x)?");
        n.input("$1.0 dislikes(I,?x)?");
        n.input("$1.0 needs(I,?x)?");
        n.input("$1.0 wants(I,?x)?");
        n.input("$1.0 patient(?what,I)?");


        n.startFPS(10f);
        Util.sleepMS(1000 * 40);
        n.stop();

    }


    @Test
    public void testCapabilityExtension() throws IOException {

        KIF k = KIF.file("/home/me/sumo/Merge.kif");
        k.tasks.forEach(bb -> System.out.println(bb));

    }
}