package nars.gui;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.derive.Derivers;
import nars.derive.impl.ZipperDeriver;
import nars.index.concept.AbstractConceptIndex;
import nars.index.concept.SimpleConceptIndex;
import nars.task.util.TaskBuffer;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.OKSurface;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.windo.GraphEdit;

public class ToyNAR {

    public static void main(String[] args) throws Narsese.NarseseException {
        GraphEdit<Surface> g = GraphEdit.window(1000, 800);
        NAR n = new NARS().index(new SimpleConceptIndex(128))
                .get();

        ZipperDeriver d = new ZipperDeriver(Derivers.nal(n,1,1));

        g.add(new BagView<>(((AbstractConceptIndex)n.concepts).active, n)).posRel(0.5f, 0.5f, 0.25f, 0.15f);

        g.add(new ObjectSurface<>(d,3)).posRel(0.5f, 0.5f, 0.25f, 0.15f);

        ((TaskBuffer.BagTaskBuffer)(n.input)).valve.set(0);
        g.add(new BagView<>(((TaskBuffer.BagTaskBuffer)(n.input)).tasks, n)).posRel(0.5f, 0.5f, 0.25f, 0.15f);



        g.add(NARui.newNarseseInput(n,
            t->{},
            e->g.add(new OKSurface(e.toString())).posRel(0.5f, 0.5f, 0.25f, 0.25f)
        )).posRel(0.5f, 0.5f, 0.25f, 0.1f);

        //temporaruy
        g.add(NARui.top(n)).posRel(0.5f,0.5f,0.4f,0.4f);


        n.input("(a-->b).");
        n.input("(b-->c).");
        n.input("(c-->d).");
        n.input("(d-->e).");
        //n.log(TextEdit.appendable);
        n.startFPS(1f);
    }

}
