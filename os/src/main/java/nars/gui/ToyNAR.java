package nars.gui;

import jcog.pri.bag.Bag;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.derive.Derivers;
import nars.derive.impl.ZipperDeriver;
import nars.gui.graph.run.BagregateConceptGraph2D;
import nars.index.concept.AbstractConceptIndex;
import nars.index.concept.HijackConceptIndex;
import nars.link.Activate;
import nars.task.ITask;
import nars.task.util.TaskBuffer;
import nars.term.Term;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.OKSurface;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.windo.GraphEdit;

import java.util.stream.Stream;

public class ToyNAR {

    public static void main(String[] args) {
        GraphEdit<Surface> g = GraphEdit.window(1000, 800);
        NAR n = new NARS().index(
                //new SimpleConceptIndex(128, 0.9f, true)
                new HijackConceptIndex(512, 4)
        )

                .get();


        Bag<Term, Activate> activeConcepts = ((AbstractConceptIndex) n.concepts).active;
        //g.add(new BagView<>(activeConcepts, n)).posRel(0.5f, 0.5f, 0.25f, 0.15f);

        //((TaskBuffer.BagTaskBuffer)(n.input)).valve.set(0);
        Bag<ITask, ITask> inputTasks = ((TaskBuffer.BagTaskBuffer) (n.input)).tasks;
        //g.add(new BagView<>(inputTasks, n)).posRel(0.5f, 0.5f, 0.25f, 0.15f);

        Stream<Task> allTasks = n.tasks();
        //g.add(new BagView<>(new Bagregate(Stream.concat(allTasks, activeConcepts.stream()), 512, 0.1f).bag, n)).posRel(0.5f, 0.5f, 0.25f, 0.15f);

//        Bagregate b = new Bagregate(, 512, 0.5f);
  //      n.onCycle(b::commit);
        //g.add(new BagView<>(b.bag, n)).posRel(0.5f, 0.5f, 0.25f, 0.15f);
        //new ConceptGraph2D(()->Stream.concat(n.tasks(), activeConcepts.stream()).map(x->new PLink<Termed>(x,x.priElseZero())).iterator()
        g.add(BagregateConceptGraph2D.get(n)).posRel(0.5f, 0.5f, 0.35f, 0.35f);


        ZipperDeriver d = new ZipperDeriver(Derivers.nal(n,1,1));
        g.add(new ObjectSurface<>(d,3)).posRel(0.5f, 0.5f, 0.25f, 0.15f);



        g.add(NARui.newNarseseInput(n,
            t->{},
            e->g.add(new OKSurface(e.toString())).posRel(0.5f, 0.5f, 0.25f, 0.25f)
        )).posRel(0.5f, 0.5f, 0.25f, 0.1f);

        //temporary
        g.add(NARui.top(n)).posRel(0.5f,0.5f,0.4f,0.4f);


//        n.input("(a-->b).");
//        n.input("(b-->c).");
//        n.input("(c-->d).");
//        n.input("(d-->e).");
        //n.log(TextEdit.appendable);


        n.startFPS(1f); //n.stop(); //HACK

    }

}
