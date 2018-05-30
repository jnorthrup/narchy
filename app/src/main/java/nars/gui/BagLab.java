package nars.gui;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.math.random.XorShift128PlusRandom;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.$;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.util.math.Color3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static jcog.pri.op.PriMerge.plus;
import static spacegraph.space2d.container.grid.Gridding.col;
import static spacegraph.space2d.container.grid.Gridding.row;

/**
 * Created by me on 11/29/16.
 */
public class BagLab {

    public static final int BINS = 64;

    int histogramResetPeriod = 64;
    int iteration;


    final Bag<Integer, PriReference<Integer>> bag;
    private final List<FloatSlider> inputSliders;
    private final int uniques;

    float[] selectionHistogram = new float[BINS];

    public BagLab(Bag<Integer, PriReference<Integer>> bag) {
        super();
        this.bag = bag;

        this.uniques = bag.capacity() * 2;

        int inputs = 10;
        inputSliders = $.newArrayList(inputs);
        for (int i = 0; i < inputs; i++)
            inputSliders.add(new FloatSlider(0.5f, 0, 1));


    }

    public Surface surface() {
        return row(
                col(inputSliders),
                
                col(
                        NARui.pane("Bag Selection Distribution (0..1)", new HistogramChart(
                                () -> selectionHistogram, new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f))),
                        NARui.pane("Bag Content Distribution (0..1)", new HistogramChart(
                                () -> bag.histogram(new float[10]), new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))
                )
                
        );
    }




























    public static void main(String[] arg) {

        BagLab bagLab = new BagLab(
                new CurveBag(plus, new HashMap(), 256)
                
                
        );


        SpaceGraph.window(
                bagLab.surface(), 1200, 800);

        long delayMS = 30;
        
        while (true) {
            bagLab.update();
            Util.sleep(delayMS);
        }
        
    }


    private synchronized void update() {


        int inputRate = 20;
        for (int j = 0; j < inputRate; j++) {
            int n = inputSliders.size();
            for (int i = 0; i < n; i++) {
                if (Math.random() < inputSliders.get(i).value()) {
                    float p = (i + (float) Math.random()) / (n - 1);
                    
                    bag.put(new PLink<>((int) Math.floor(Math.random() * uniques), p));
                }
            }
        }

        bag.commit();


        int bins = selectionHistogram.length;
        float sampleBatches = 1;
        int batchSize = 32;

        if (iteration++ % histogramResetPeriod == 0)
            Arrays.fill(selectionHistogram, 0);

        

        XorShift128PlusRandom rng = new XorShift128PlusRandom(1);

        List<PriReference<Integer>> sampled = $.newArrayList(1024);
        for (int i = 0; i < (int) sampleBatches; i++) {
            sampled.clear();

            
            bag.sample(rng, batchSize, (Consumer<PriReference<Integer>>) sampled::add);

            
            for (PriReference<Integer> sample : sampled) {
                if (sample != null) {
                    float p = sample.priElseZero();
                    selectionHistogram[Util.bin(p, bins - 1)]++;
                } else {
                    break;
                }
            }
        }

    }

}
