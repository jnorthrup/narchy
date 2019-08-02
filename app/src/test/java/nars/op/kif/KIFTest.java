package nars.op.kif;

import jcog.Texts;
import jcog.Util;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.GraphMeter;
import nars.*;
import nars.attention.TaskLinkWhat;
import nars.derive.BasicDeriver;
import nars.derive.Derivers;
import nars.derive.premise.PremiseSource;
import nars.link.TaskLinks;
import nars.memory.RadixTreeMemory;
import nars.task.util.PriBuffer;
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
                //"/home/me/sumo/ComputerInput.kif";
//                "/home/me/sumo/Economy.kif",
                "/home/me/sumo/FinancialOntology.kif";

        //String O = "/home/me/d/sumo_merge.nal";
//        k.tasks.forEach(bb -> System.out.println(bb));

        NAR n = new NARS().index(new RadixTreeMemory(128*1024)).get();

        new BasicDeriver(Derivers.nal(n, 1,8, "motivation.nal"));

        new BasicDeriver(Derivers.nal(n, /*NAL*/6, /*NAL*/8), new PremiseSource.IndexExhaustive()); // ~= PROLOG

        n.termVolMax.set(32);
        n.beliefPriDefault.amp(0.01f);


        //TaskLinkWhat w = (TaskLinkWhat) n.what();

        TaskLinkWhat w = n.fork(new TaskLinkWhat($$("sumo_x"), new TaskLinks(), new PriBuffer.DirectPriBuffer<>()));
        w.links.decay.set(0.05f);
        w.links.links.capacity(1024);


        KIF k = KIF.file(I);
        n.input(k.tasks());
        n.log();
        //n.input("$1.0 ({?ACT}-->JoystickMotion)?");
        //n.input("$1.0 classIntersection(?1,?2)?");
        //n.input("$1.0 (#1-->ComputerDisplay)!");
        //n.clear();
        w.clear();
        n.input("$1.0 possesses(I,#everything)!");
        n.input("$1.0 benefits(#all, I)!");
        n.run(100);
        w.links.links.print();

//        n.concepts().forEach(c -> System.out.println(c));

    }
    @Test void test_TQG2() throws Narsese.NarseseException {
        String t =
            "(instance TheKB2_1 ComputerProgram)\n" +
            "(instance Inconsistent Attribute)\n" +
            "\n" +
            "(=>\n" +
            "  (and\n" +
            "    (contraryAttribute ?ATTR1 ?ATTR2)\n" +
            "    (property ?X ?ATTR1)\n" +
            "    (property ?X ?ATTR2))\n" +
            "  (property TheKB2_1 Inconsistent))\n" +
            "\n" +
            "(instance Entity2_1 Organism)\n" +
            "(instance Entity2_2 Organism)\n" +
            "(mother Entity2_1 Entity2_2)\n" +
            "(father Entity2_1 Entity2_2)";
        //(query (property TheKB2_1 Inconsistent))
        //(answer yes)
        NAR n = new NARS().index(new RadixTreeMemory(128*1024)).get();

        new BasicDeriver(Derivers.nal(n, 6,8));

        new BasicDeriver(Derivers.nal(n, /*NAL*/6, /*NAL*/8), new PremiseSource.IndexExhaustive()); // ~= PROLOG

        KIF k = new KIF(t);
        n.input(k.tasks());

        n.input("$1.0 property(TheKB2_1, Inconsistent)?");
        n.run(1000);


    }

    @Test void test_TQG4() throws Narsese.NarseseException {
        String t =
            "(instance Entity4_1 Human)\n" +
            "\n" +
            "(instance DoingSomething4_1 IntentionalProcess)\n" +
            "\n" +
            "(agent DoingSomething4_1 Entity4_1)\n" +
            "\n" +
            "(=>\n" +
            "  (and\n" +
            "    (agent ?PROC ?AGENT)\n" +
            "    (instance ?PROC IntentionalProcess))\n" +
            "  (and\n" +
            "    (instance ?AGENT CognitiveAgent)\n" +
            "    (not\n" +
            "      (Dead ?PROC ?AGENT ) )))";
//            "(query (not (holdsDuring (WhenFn DoingSomething4-1) (attribute Entity4-1 Dead))))\n" +
//            "(answer yes)\n";
        NAR n = new NARS().index(new RadixTreeMemory(128*1024)).get();

        new BasicDeriver(Derivers.nal(n, 5,8));

        new BasicDeriver(Derivers.nal(n, /*NAL*/5, /*NAL*/8), new PremiseSource.IndexExhaustive()); // ~= PROLOG
        n.log();

        KIF k = new KIF(t);
        n.input(k.tasks());

        n.input("$1.0 Dead(DoingSomething4_1,Entity4_1)?");
        n.run(3000);


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