package nars.op.kif;

import jcog.Texts;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.GraphMeter;
import nars.*;
import nars.derive.deriver.SimpleDeriver;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.List;

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

    @Test public void testGenerate() {
        String sumoDir = "file:///home/me/sumo/";

        NAR n = NARS.shell();

        n.memory.on(KIFInput.load);

        n.memory.contents(sumoDir).parallel().forEach(I -> {
            String ii = Texts.unquote(I.toString());
            if (!ii.endsWith(".kif"))
                return;

            String name = ii.substring(ii.lastIndexOf('/')+1, ii.lastIndexOf('.'));
            Term O = Atomic.the("file:///tmp/sumo/" + name + ".kif.nalz");
            Runnable r = n.memory.copy(I, O);
            r.run();
        });

    }

    @Test public void testLoad() throws Narsese.NarseseException {
        Param.DEBUG = true;

        NAR n =
                //NARS.tmp();
                NARchy.core();

        n.input("load(\"file:///tmp/sumo/FinancialOntology.kif.nalz\");");
        n.input("load(\"file:///tmp/sumo/Merge.kif.nalz\");");
        n.input("load(\"file:///tmp/sumo/Economy.kif.nalz\");");
        n.input("load(\"file:///tmp/sumo/Mid-level-ontology.kif.nalz\");");
        n.run(1);
        System.err.println(n.concepts.size() + " concepts");
        n.clear();

        //n.log();
        n.onTask(x->{
            if (x.isGoal() && x.conf() > 0.1f) {
                System.out.println(x.proof());
            }
        });


        SimpleDeriver.forTasks(n,
            List.of(
                n.inputTask("$1.0 possesses(I,#everything)!"),
                n.inputTask("$1.0 Getting(I,#everything)!"),
                n.inputTask("$1.0 uses(I,#anything)!"),
                n.inputTask("$1.0 ChangeOfPossession(#everything,I)!"),
                n.inputTask("(I-->economy).") //i am an economy
            ));

        //n.input("$1.0 (--(I <-> #everyoneElse) && --possesses(#everyoneElse, #something))!");

        n.input("$1.0 --Giving(I,#anything)!");

        n.input("$1.0 --ChemicalDecomposition(I,#1)!"); //chemical decomposition==>combustion lol
        n.input("$1.0 exploits(I,#anything)!");
        n.run(15000);
        n.stats().forEach((s,v)->System.out.println(s + "\t" + v));
    }

    @Test public void test1() throws Exception {


            NAR nar = NARS.tmp();
            //MetaGoal.Perceive.set(e.emotion.want, -0.1f);
            //e.emotion.want(MetaGoal.Perceive, -0.1f);

            //new PrologCore(e);

            String I =
                    //"/home/me/sumo/Biography.kif"
                    //"/home/me/sumo/Military.kif"
                    //"/home/me/sumo/ComputerInput.kif"
                    //"/home/me/sumo/FinancialOntology.kif"
                    "/home/me/sumo/Merge.kif"
                    //"/home/me/sumo/emotion.kif"
                    //"/home/me/sumo/Weather.kif"
                    ;

            String O = "/home/me/d/sumo_merge.nal";
            KIFInput k = new KIFInput(new FileInputStream(I));

            k.output(O);

            //nar.log();

            nar.inputNarsese(new FileInputStream(O));

//        final PrintStream output = System.out;
//        for (Term x : k.beliefs) {
//            output.println(x + ".");
//            try {
//                nar.believe(x);
//            } catch (Exception e) {
//                logger.error("{} {}", e.getMessage(), x);
//            }
//            try {
//                nar.input("$0.01$ " + x + ".");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

//https://github.com/ontologyportal/sumo/blob/master/tests/TQG1.kif.tq
//(time 240)
//(instance Org1-1 Organization)
//(query (exists (?MEMBER) (member ?MEMBER Org1-1)))
//(answer yes)
            //e.clear();
//        nar.believe("Organization:org1");
//        e.question("member(?1, org1)?", ETERNAL, (q,a)->{
//            System.out.println(a);
//        });
//        nar.ask($.$$("(org1<->?1)"), ETERNAL, QUESTION, (t)->{
//           System.out.println(t);
//        });
            //e.believe("accountHolder(xyz,1)");
//        e.ask($.$safe("(EmotionalState<->?1)"), ETERNAL, QUESTION, (t)->{
//           System.out.println(t);
//        });
            //e.believe("attribute(xyz,Philosopher)");
            //e.input("(xyz<->?1)?");

            nar.run(10000);
//        Thread.sleep(1000);
//        e.run(1000);
            //e.conceptsActive().forEach(s -> System.out.println(s));

            //(($_#AGENT,#OBJECT)-->needs)==>($_#AGENT,#OBJECT)-->wants)).
            //String rules = "((%AGENT,%OBJECT)-->needs), %X |- ((%AGENT,%OBJECT)-->wants), (Belief:Identity)\n";


//        TrieDeriver miniDeriver =
//                //new TrieDeriver(PremiseRuleSet.rules(false, "nal6.nal"));
//                TrieDeriver.get(new PremiseRuleSet(
//                        k.impl.parallelStream().map(tt -> {
//                            try {
//                                return PremiseRuleSet.parse(tt.getOne() + ", () |- " + tt.getTwo() + ", (Belief:Identity)\n");
//                            } catch (Exception e1) {
//                                //e1.printStackTrace();
//                                return null;
//                            }
//                        }).filter(Objects::nonNull).toArray(PremiseRule[]::new)
//                ) );


//        miniDeriver.print(System.out);

            //d.clear();


//        e.onTask(t -> {
//           if (t.isInput()) {
//               //d.forEachTask(b -> {
//                   miniDeriver.test(new Derivation(
//                           e,
//                           budgeting,
//                           Param.UnificationStackMax
//                   ) {
//                       @Override
//                       public void derive(Task x) {
//                           e.input(x);
//                       }
//                   }.restartC(new Premise( t, Terms.ZeroProduct, null, 1f), Param.UnificationTTLMax));
//               //});
//           }
//        });
//        e.input("[Physical]:X.");
//        e.input("[Atom]:Y.");
//        e.input("[Electron]:E.");
//        e.input("[Proton]:P.");
//        e.input("contains(X,Y).");
//        e.input("([Integer]:1 && [Integer]:3).");
//        e.input("starts(A,B).");
//        e.input("[GovernmentFn]:A.");
//        e.input("[WealthFn]:B.");
            nar.run(2500);
//        d.conceptsActive().forEach(System.out::println);
            //d.concept("[Phrase]").print();


    }
}