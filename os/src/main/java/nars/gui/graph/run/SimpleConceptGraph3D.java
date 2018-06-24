package nars.gui.graph.run;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.control.Activate;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.gui.graph.DynamicConceptSpace;
import spacegraph.space2d.Surface;
import spacegraph.space2d.hud.SubOrtho;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space3d.SpaceGraphPhys3D;

import java.util.concurrent.atomic.AtomicBoolean;

import static spacegraph.space2d.container.grid.Gridding.grid;

public class SimpleConceptGraph3D extends DynamicConceptSpace {

    private final AtomicBoolean reloadReady = new AtomicBoolean(false);
    private final TextEdit inputbox;

    private SimpleConceptGraph3D(NAR nar, int visibleNodes, int maxEdgesPerNodeMax) {
        this(nar, () -> nar.attn.active().iterator(),
                visibleNodes, maxEdgesPerNodeMax);
    }

    private SimpleConceptGraph3D(NAR nar, Iterable<Activate> concepts, int maxNodes, int maxEdgesPerNodeMax) {
        super(nar, concepts, maxNodes, maxEdgesPerNodeMax);

        SpaceGraphPhys3D sg = show(1400, 1000, false);


        sg.add(new SubOrtho(grid(
                new AutoSurface<>(sg.dyn.broadConstraints.get(0) /* FD hack */),
                new AutoSurface<>(vis)
        )).posWindow(0, 0, 1f, 0.2f));

        Surface inputPanel =
                (inputbox = new TextEdit() {
                    @Override
                    protected void onKeyCtrlEnter() {
                        reload();
                    }

                }).surface();
        

        sg.add(new SubOrtho(
                inputPanel
        ).posWindow(0, 0.8f, 0.2f, 0.6f));

        float fps = 16;
        nar.startFPS(fps);

    }

    @Override
    public void update(SpaceGraphPhys3D<Concept> s, long dtMS) {
        if (reloadReady.getAndSet(false)) {
            reload();
        }

        super.update(s, dtMS);
    }

    private synchronized void reload() {
        

            

            
            
            

        

            
                try {
                    nar.input(inputbox.text());
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }
            

        

        
    }


    public static void main(String[] args) {


        
        NAR n = NARS.threadSafe();
        new MatrixDeriver(Derivers.nal(n, 1, 8));




        SimpleConceptGraph3D g = new SimpleConceptGraph3D(n,
                /* TODO */ 128, 5);


        

        




        
        























        
        




        


        

        


        
        


















        

        


        

        


        
















    }






























}
