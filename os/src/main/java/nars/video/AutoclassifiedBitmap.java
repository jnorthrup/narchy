package nars.video;

import com.google.common.collect.Iterables;
import jcog.data.list.FasterList;
import jcog.learn.Autoencoder;
import jcog.math.FloatRange;
import jcog.signal.wave2d.Bitmap2D;
import jcog.util.IntIntToFloatFunction;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.agent.NAgent;
import nars.concept.NodeConcept;
import nars.concept.sensor.AbstractSensor;
import nars.concept.sensor.Signal;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.term.Termed;
import org.apache.commons.lang3.ArrayUtils;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import java.util.Arrays;

import static nars.$.$$;
import static nars.truth.TruthFunctions.w2c;

/**
 * similar to a convolutional autoencoder
 */
public class AutoclassifiedBitmap extends AbstractSensor {

    public final Autoencoder ae;
    private final FasterList<Signal> signals;
    private final CauseChannel<ITask> input;
    public final FloatRange alpha = new FloatRange(1f, 0, 1);
    public final FloatRange noise = new FloatRange(0.01f, 0, 1);;


    public static final MetaBits NoMetaBits = (x, y) -> ArrayUtils.EMPTY_FLOAT_ARRAY;

    private final MetaBits metabits;
    

    private final IntIntToFloatFunction pixIn;

    private final boolean[][][] pixEnable;
    private final float[][] pixConf;

    public final float[][] pixRecon; 

    private final float in[];
    private final int sw, sh;
    private final int nw, nh;
    private final int pw, ph;
    private final NAgent agent;
    private final boolean reconstruct = true;
    public boolean learn = true;


    public Surface newChart() {

        return new Gridding(
            new BitmapMatrixView(ae.W),

            new ObjectSurface(this),

            new BitmapMatrixView(pixRecon)) {
            {
                agent.onFrame(()-> forEach(x -> {
                    if(x instanceof BitmapMatrixView)
                        ((BitmapMatrixView) x).update();
                }));
            }
        };
    }

    public AutoclassifiedBitmap alpha(float alpha) {
        this.alpha.set(alpha);
        return this;
    }

    @Override
    public void update(long last, long now, long next, NAR nar) {
        update(last, now, signals, input);
    }

    public interface MetaBits {
        float[] get(int subX, int subY);
    }
    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(signals, NodeConcept::term);
    }

    /*
    (subX, subY) -> {
            
            return new float[]{subX / ((float) (nx - 1)), subY / ((float) (nx - 1)), cam.Z};
        }
     */

    public AutoclassifiedBitmap(String root, float[][] pixIn, int sw, int sh, int states, NAgent agent) {
        this(root, pixIn, sw, sh, NoMetaBits, states, agent);
    }

    public AutoclassifiedBitmap(String root, float[][] pixIn, int sw, int sh, MetaBits metabits, int states, NAgent agent) {
        this($$(root), pixIn, sw, sh, metabits, states, agent);
    }

    public AutoclassifiedBitmap(Term root, float[][] pixIn, int sw, int sh, MetaBits metabits, int states, NAgent agent) {
        this(root, (x,y)->pixIn[x][y],
                pixIn.length,pixIn[0].length,
                sw, sh, metabits, states, agent);
    }
    public AutoclassifiedBitmap(Term root, Bitmap2D b, int sw, int sh, MetaBits metabits, int states, NAgent agent) {
        this(root, b::brightness,
                b.width(),b.height(),
                sw, sh, metabits, states, agent);
    }
    /**
     * metabits must consistently return an array of the same size, since now the size of this autoencoder is locked to its dimension
     */
    public AutoclassifiedBitmap(Term root, IntIntToFloatFunction pixIn, int pw, int ph, int sw, int sh, MetaBits metabits, int features, NAgent agent) {
        super(root, agent.nar());
        ae = new Autoencoder(sw * sh + metabits.get(0, 0).length, features, agent.random());

        this.metabits = metabits;
        this.agent = agent;

        this.pixIn = pixIn;
        this.sw = sw; 
        this.sh = sh;
        this.pw = pw;
        this.ph = ph;
        this.nw = (int) Math.ceil(pw / ((float) sw));
        this.nh = (int) Math.ceil(ph / ((float) sh)); 
        this.in = new float[ae.x.length];
        this.pixRecon = new float[pw][ph];

        this.pixEnable = new boolean[nw][nh][features];
        this.pixConf = new float[nw][nh];


        this.signals = new FasterList(nw * nh);

        Term r = $.the(root);
        this.input = nar.newChannel(this);
        for (int i = 0; i< nw; i++) {
            for (int j = 0; j < nh; j++) {
                Term coord = coord(r, i, j);
                for (int k = 0; k < features; k++) {
                    Term term = $.prop(coord, $.the(k));
                    int ii = i;  int jj = j; int kk = k;
                    signals.add(
                        new Signal(term, () -> pixEnable[ii][jj][kk] ? 1f : Float.NaN, nar) {
                            @Override
                            protected CauseChannel<ITask> newChannel(NAR n) {
                                return null;
                            }
                        }
                    );
                }
            }
        }
        agent.onFrame(this::update);
    }

    public Term coord(Term root, int i, int j) {
        return $.inh($.p($.the(i), $.the(j)), root);
    }

    public void update() {
        

        float minConf = nar.confMin.floatValue();
        float baseConf = nar.confDefault(Op.BELIEF);


        float alpha = this.alpha.floatValue();
        float noise = this.noise.floatValue();

        int regionPixels = sw * sh;
        float sumErr = 0;

        int states = ae.y.length;
        float outputThresh = //TODO make adjustable
                //1f - (1f / (states - 1));
                1f - (1f / (states/2f) );


        for (int i = 0; i < nw; ) {
            for (int j = 0; j < nh; ) {

                int p = 0;
                int oi = i * sw;
                int oj = j * sh;
                for (int si = 0; si < sw; si++) {
                    int d = si + oi;
                    if (d >= pw)
                        break;


                    for (int sj = 0; sj < sh; sj++) {

                        int c = sj + oj;

                        in[p++] = c < ph ? pixIn.value(d, c) : 0.5f;

                    }
                }

                float[] metabits = this.metabits.get(i, j);
                for (float m : metabits) {
                    in[p++] = m;
                }

                short[] po = null;
                if (learn) {
                    float regionError = ae.put(in, alpha, noise, 0, true, false, true);
                    sumErr += regionError;

                    
                    
                    
                    float evi;
                    if ((evi = 1f - (regionError / regionPixels)) > 0) {
                        short[] features = ae.max(outputThresh);
                        if (features != null) {
                            
                                evi /= features.length;
                                if ((pixConf[i][j] = (baseConf * w2c(evi))) >= minConf) {
                                    po = features;
                                }
                            
                        }
                    }
                    
                } else {
                    
                    ae.recode(in, true, false, true);
                }

                float mult;

                boolean[] peij = pixEnable[i][j];

                Arrays.fill(peij, false);
                if (po!=null) {
                    mult = +1;
                    for (short ppp : po)
                        peij[ppp] = true;
                } else {
                    mult = -1;
                }


                if (reconstruct) {


                    float z[] = this.ae.z;
                    p = 0;
                    for (int si = 0; si < sw; si++) {
                        int d = si + oi;
                        if (d >= pw)
                            break;

                        float[] col = pixRecon[d];
                        for (int sj = 0; sj < sh; sj++) {

                            int c = sj + oj;

                            if (c >= ph)
                                break;

                            col[c] = z[p++] * mult;
                        }
                    }
                }

                j++;
            }

            i++;
        }







































    }




















































































}
