package nars.gui;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.HijackBag;
import jcog.math.random.XorShift128PlusRandom;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.$;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.util.math.Color3f;
import spacegraph.video.Draw;
import spacegraph.space2d.widget.meter.MatrixView;
import spacegraph.space2d.widget.slider.FloatSlider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

import static jcog.pri.op.PriMerge.plus;
import static spacegraph.space2d.container.Gridding.col;
import static spacegraph.space2d.container.Gridding.row;

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
                //new GridSurface(VERTICAL,
                col(
                        Vis.pane("Bag Selection Distribution (0..1)", new HistogramChart(
                                () -> selectionHistogram, new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f))),
                        Vis.pane("Bag Content Distribution (0..1)", new HistogramChart(
                                () -> bag.histogram(new float[10]), new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))
                )
                //,hijackVis((HijackBag)bag)
        );
    }

    private static Surface hijackVis(HijackBag bag) {
        int c = bag.capacity();
        int w = (int) Math.ceil(Math.sqrt(1 + c));
        int h = c / w;
        return new MatrixView(w, h, (x, y, gl) -> {
            try {
                Object m = bag.map;
                float p = -1;
                if (m != null) {
                    int i = y * w + x;
                    if (i < c) {

                        Object val = ((AtomicReferenceArray) m).get(i);
                        if (val != null)
                            p = bag.priElse(val, 0);
                    }
                }
                Draw.colorBipolar(gl, p);
            } catch (RuntimeException e) {

            }
            return 0;
        });

    }


    public static void main(String[] arg) {

        BagLab bagLab = new BagLab(
                new CurveBag(plus, new HashMap(), 256)
                //new DefaultHijackBag<>(PriMerge.plus, 1024, 8)
                //new BloomBag<>(32, (i) -> Shorts.toByteArray(i.shortValue()))
        );


        SpaceGraph.window(
                bagLab.surface(), 1200, 800);

        long delayMS = 30;
        //new Thread(()->{
        while (true) {
            bagLab.update();
            Util.sleep(delayMS);
        }
        //}).start();
    }


    private synchronized void update() {


        int inputRate = 20;
        for (int j = 0; j < inputRate; j++) {
            int n = inputSliders.size();
            for (int i = 0; i < n; i++) {
                if (Math.random() < inputSliders.get(i).value()) {
                    float p = (i + (float) Math.random()) / (n - 1);
                    //float q = (float)Math.random(); //random quality
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

        //System.out.println(bag.size());

        XorShift128PlusRandom rng = new XorShift128PlusRandom(1);

        List<PriReference<Integer>> sampled = $.newArrayList(1024);
        for (int i = 0; i < (int) sampleBatches; i++) {
            sampled.clear();

            //System.out.println(h + " " + v);
            bag.sample(rng, batchSize, (Consumer<PriReference<Integer>>) sampled::add);

            //BLink<Integer> sample = bag.sample();
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
