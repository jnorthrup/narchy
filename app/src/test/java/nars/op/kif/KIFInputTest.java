package nars.op.kif;

import jcog.Texts;
import jcog.Util;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.GraphMeter;
import nars.*;
import nars.derive.Deriver;
import nars.attention.derive.DefaultDerivePri;
import nars.derive.impl.BatchDeriver;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

        n.memory.on(KIFInput.load);


        n.memory.contents(sumoDir).parallel().forEach(I -> {
            String ii = Texts.unquote(I.toString());
            if (!ii.endsWith(".kif"))
                return;

            if (ii.contains("WorldAirports")) //exclusions
                return;

            String name = ii.substring(ii.lastIndexOf('/') + 1, ii.lastIndexOf('.')).toLowerCase();

            n.memory.copy(I, Atomic.the("file:///tmp/sumo/" + name + ".nalz")).run();
            n.memory.copy(I, Atomic.the("file:///tmp/sumo/" + name + ".nal")).run();
        });

    }

    @Test
    public void testLoad() throws Narsese.NarseseException {
        

        NAR n = NARS.tmp();
                //NARchy.core();

        n.beliefPriDefault.set(0.05f);
        n.conceptActivation.set(0.01f);

        n.input("load(\"file:///tmp/sumo/Merge.kif.nalz\");");
        n.input("load(\"file:///tmp/sumo/Mid-level-ontology.kif.nalz\");");
        n.input("load(\"file:///tmp/sumo/FinancialOntology.kif.nalz\");");
        n.input("load(\"file:///tmp/sumo/Economy.kif.nalz\");");
        n.run(1);
        System.err.println(n.concepts.size() + " concepts");
        n.clear();

        n.logPriMin(System.out, 0.01f);







        Deriver.derivers(n).forEach( (d)->
                ((DefaultDerivePri)(((BatchDeriver)d).budgeting))
                        .gain.set(0.2f) );

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
    public void test1() throws Exception {


        NAR nar = NARS.tmp();
        
        

        

        String I =
                
                
                
                
                "/home/me/sumo/Merge.kif"
                
                
                ;

        String O = "/home/me/d/sumo_merge.nal";
        KIFInput k = new KIFInput(new FileInputStream(I));


        

        nar.inputNarsese(new FileInputStream(O));





















        







        



        
        

        nar.run(10000);


        

        
        


















        



























        nar.run(2500);

        


    }
}