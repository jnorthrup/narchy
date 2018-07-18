package nars.gui;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.TruthWave;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.text.Label;
import spacegraph.video.Draw;

import static nars.gui.BeliefTableChart.renderWaveLine;


public class BeliefTableChart2 extends DurSurface<Stacking> implements MetaFrame.Menu {

    final Term term;
    private final TruthGrid beliefGrid, goalGrid;

    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */

    private long start, end;

    class TruthGrid extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

        private final TruthWave wave;
        private final boolean beliefOrGoal;
        private final BeliefTableChart.Colorize colorize;


        public TruthGrid(int tDiv, int fDiv, boolean beliefOrGoal) {
          super(tDiv, fDiv);
          wave = new TruthWave(tDiv);
          this.beliefOrGoal = beliefOrGoal;
                this.colorize = beliefOrGoal ?
                            (ggl, frq, cnf) -> {
                                float a = 0.65f + 0.2f * cnf;
                                ggl.glColor4f(0.25f + 0.75f * cnf, 0.1f * (1f - cnf), 0, a);
                            } :
                            (ggl, frq, cnf) -> {
                                float a = 0.65f + 0.2f * cnf;
                                ggl.glColor4f(0.1f * (1f - cnf), 0.25f + 0.75f * cnf, 0, a);
                            };
        }


        void update(Concept c, long start, long end) {
            wave.project(c, beliefOrGoal, start, end, w, nar);
        }

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {
            super.paint(gl, surfaceRender);
            Draw.bounds(bounds, gl, ggl->{
                renderWaveLine((start +  end)/2L /*hack*/, start, end, ggl, wave, (f,c)->f, colorize);
            });
        }

        @Override
        public int update(int x, int y) {
            return Draw.rgbInt(nar.random().nextFloat(), 0, 0);
        }
    }

    public BeliefTableChart2(NAR n, Termed term) {
        super(new Stacking(), n);
        this.term = term.term();
        the.add(beliefGrid = new TruthGrid(16, 10, true));
        the.add(goalGrid = new TruthGrid(16, 10, false));
    }

    @Override
    public void update() {

        Concept ccd = nar.conceptualize(term/* lookup by term, not the termed which could be a dead instance */);
        if (ccd == null)
            return;

        long now = nar.time();
        int dur = nar.dur();

        //TODO different time modes
        start = now - 32 * dur;
        end = now + 32 * dur;

        if (beliefGrid.showing())
            beliefGrid.update(ccd, start, end);
        if (goalGrid.showing())
            goalGrid.update(ccd, start, end);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + term + "]";
    }

    @Override
    protected void paintIt(GL2 ggl) {


    }

    @Override
    public Surface menu() {
        return new Gridding(
            new Label(term.toString()),
            PushButton.awesome("search-plus").click(() -> NARui.conceptWindow(term, nar))
        );
    }


}
