package nars.cfg;

import nars.cfg.method.CGMethod;
import nars.cfg.method.MethodCallGraph;
import org.jgrapht.alg.KShortestPaths;

/**
 * Created by me on 1/15/15.
 */
public final class MethodCFG{

    public static void main(String[] args) throws ClassNotFoundException {



        MethodCallGraph g = new MethodCallGraph()
                .addClasses("nars.nal", true)
                .addClasses("nars.nal.rule", true)
                .addClasses("nars.nal.rule.concept", true)
                .addClass("nars.nal.rule.ConceptFire")
                .addClass("nars.nal.rule.concept.ConceptFireTask")
                .addClass("nars.nal.rule.concept.ConceptFireTaskTerm")
                .addClass("nars.nal.NAL")
                ;

        System.out.println(g.vertexSet().size() + " vert , "  + g.edgeSet().size() + " edge");
        


        System.out.println("Methods: ");
        list("\tMETHOD ", g.vertexSet());

        System.out.println("Entry methods: ");
        list("\tENTRYMETHOD ", g.getEntryPoints());

        /*System.out.println(" Exit methods: ");
        list("\tEXITMETHOD ", g.getExitPoints());*/


        CGMethod fireConcept = g.method("nars.nal.rule.ConceptFire#rule([])");
        CGMethod derivedTask = g.method("nars.nal.NAL#deriveTask([nars.nal.entity.Task, boolean, boolean, nars.nal.entity.Task, nars.nal.entity.Sentence, nars.nal.entity.Sentence, nars.nal.entity.Task])");
        

        KShortestPaths fromFireConcept = new KShortestPaths(g, fireConcept, 500, 500);

        System.out.println(fireConcept + " to " + derivedTask);
        list("  DERIVATION_EDGE ", fromFireConcept.getPaths(derivedTask));

        
        

        
        System.out.println(g.vertexSet().size() + "V|" + g.edgeSet().size() + 'E');
    }

    public static void list(String prefix, Iterable x) {
        if (x != null)
            for (Object y : x) {
                System.out.println(prefix + y);
            }
        System.out.println();
    }
}
