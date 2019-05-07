package nars.video;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.func.IntIntToFloatFunction;
import jcog.learn.Autoencoder;
import jcog.math.FloatRange;
import jcog.signal.wave2d.Bitmap2D;
import jcog.util.ArrayUtil;
import nars.$;
import nars.NAR;
import nars.agent.Game;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.concept.sensor.VectorSensor;
import nars.gui.sensor.VectorSensorView;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import java.util.Arrays;
import java.util.Iterator;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.w2cSafe;

/**
 * similar to a convolutional autoencoder
 */
public class AutoclassifiedBitmap extends VectorSensor {

    private static final Logger logger = LoggerFactory.getLogger(AutoclassifiedBitmap.class);

    public final Autoencoder ae;
    private final FasterList<Signal> signals;

    public final FloatRange alpha = new FloatRange(1f, 0, 1);
    public final FloatRange noise = new FloatRange(0.01f, 0, 1);



    public static final MetaBits NoMetaBits = (x, y) -> ArrayUtil.EMPTY_FLOAT_ARRAY;

    private final MetaBits metabits;


    private final IntIntToFloatFunction pixIn;

    /**
     * interpret as the frequency component of each encoded cell
     */
    private final float[][][] encoded;

    private final float[][] pixConf;

    public final float[][] pixRecon;

    private final float[] ins;
    private final int sw, sh;
    private final int nw, nh;
    private final int pw, ph;
    private final Game game;
    private final boolean reconstruct = true;
    private final Term[] feature;

    private Bitmap2D src = null;

    public boolean learn = true;
    public final FloatRange confResolution = new FloatRange(0, 0, 1);


    public Surface newChart() {

        return new Gridding(
                new BitmapMatrixView(ae.W),

                new ObjectSurface<>(this),

                new BitmapMatrixView(pixRecon),

                new VectorSensorView(this, game).withControls()) {
            {
                game.onFrame(() -> forEach(x -> {
                    if (x instanceof BitmapMatrixView)
                        ((BitmapMatrixView) x).updateIfShowing();
                }));
            }
        }
                ;
    }

    @Override
    public int size() {
        return signals.size();
    }

    public AutoclassifiedBitmap alpha(float alpha) {
        this.alpha.set(alpha);
        return this;
    }

    public AutoclassifiedBitmap setResolution(float res) {
        return setResolution(new FloatRange(res, 0, 1));
    }


    public interface MetaBits {
        float[] get(int subX, int subY);
    }

    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(signals, Concept::term);
    }

    /*
    (subX, subY) -> {
            
            return new float[]{subX / ((float) (nx - 1)), subY / ((float) (nx - 1)), cam.Z};
        }
     */

    public AutoclassifiedBitmap(String root, float[][] pixIn, int sw, int sh, int states, Game game) {
        this(root, pixIn, sw, sh, NoMetaBits, states, game);
    }

    public AutoclassifiedBitmap(String root, float[][] pixIn, int sw, int sh, MetaBits metabits, int states, Game game) {
        this($$(root), pixIn, sw, sh, metabits, states, game);
    }

    public AutoclassifiedBitmap(Term root, float[][] pixIn, int sw, int sh, MetaBits metabits, int states, Game game) {
        this(root, (x, y) -> pixIn[x][y],
                pixIn.length, pixIn[0].length,
                sw, sh, metabits, states, game);
    }
    public AutoclassifiedBitmap(Term root, Bitmap2D b, int sw, int sh, int states, Game game) {
        this(root, b, sw, sh, NoMetaBits, states, game);
    }

    public AutoclassifiedBitmap(Term root, Bitmap2D b, int sw, int sh, MetaBits metabits, int states, Game game) {
        this(root, b::brightness,
                b.width(), b.height(),
                sw, sh, metabits, states, game);

        confResolution.set(game.nar.confResolution.floatValue());
        this.src = b;
    }

    /**
     * metabits must consistently return an array of the same size, since now the size of this autoencoder is locked to its dimension
     */
    public AutoclassifiedBitmap(@Nullable Term root, IntIntToFloatFunction pixIn, int pw, int ph, int sw, int sh, MetaBits metabits, int features, Game game) {
        super(root, game.nar());

        NAR nar = game.nar();

        ae = new Autoencoder(sw * sh + metabits.get(0, 0).length, features, game.random());

        this.metabits = metabits;
        this.game = game;

        this.pixIn = pixIn;
        this.sw = sw;
        this.sh = sh;
        this.pw = pw;
        this.ph = ph;
        this.nw = (int) Math.ceil(pw / ((float) sw));
        this.nh = (int) Math.ceil(ph / ((float) sh));
        this.ins = new float[ae.x.length];
        this.pixRecon = new float[pw][ph];

        this.encoded = new float[nw][nh][features];
        this.pixConf = new float[nw][nh];


        this.signals = new FasterList(nw * nh);

        this.feature = new Term[features];
        for (int i = 0; i < features; i++) {
            feature[i] = $.quote(Util.uuid64()); //HACK
        }

        Term r = root;

        short cause = nar.newCause(id).id;

        for (int i = 0; i < nw; i++) {
            for (int j = 0; j < nh; j++) {
                for (int f = 0; f < features; f++) {
                    Term term = coord(r, i, j, f);
                    int x = i, y = j, ff = f;
                    signals.add( new Signal(term, cause, () -> encoded[x][y][ff], nar) );
                }
            }
        }

        logger.info("{} pixels in={},{} ({}) x {},{} x features={} : encoded={}", this, pw, ph, (pw * ph), nw, nh, features, signals.size());
        game.addSensor(this);

        nar.start(this);
    }

    public AutoclassifiedBitmap setResolution(FloatRange res) {
        signals.forEach(s->s.setResolution(res));
        return this;
    }

    @Override
    public final void update(Game g) {
        this.updateAutoclassifier();
        super.update(g);
    }

    @Override
    public Iterator<Signal> iterator() {
        return signals.iterator();
    }

    /**
     * @param root
     * @param x    x coordinate
     * @param y    y coordinate
     * @param f    feature
     */
    protected Term coord(@Nullable Term root, int x, int y, int f) {
        return root!=null ? $.inh($.p(root, $.p(x, y)),feature[f]) : $.inh($.p(x, y), feature[f]);
        //return root!=null ? $.inh($.p(root, $.p(x, y)),feature[f]) : $.inh($.p(x, y), feature[f]);
        //return $.inh(component, feature[f]);
        //return $.inh($.p($.p($.the(x), $.the(y)), root), feature[f]);
        //return $.inh($.p(feature[f], $.p($.the(x), $.the(y))), root);
        //return $.funcImageLast(root, $.p($.the(x), $.the(y)), feature[f]);
    }

    private void updateAutoclassifier() {

        if (src!=null)
            src.updateBitmap();

        float minConf = nar.confMin.floatValue();
        float baseConf = nar.confDefault(BELIEF);


        float alpha = this.alpha.floatValue();
        float noise = this.noise.floatValue();

        int regionPixels = sw * sh;
        float sumErr = 0;

        int states = ae.y.length;
        float outputThresh = //TODO make adjustable
                //1f - (1f / (states - 1));
                1f - (1f / (states / 2f));


        float confRes = confResolution.floatValue();

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

                        ins[p++] = c < ph ? pixIn.value(d, c) : 0.5f;

                    }
                }

                float[] metabits = this.metabits.get(i, j);
                for (float m : metabits) {
                    ins[p++] = m;
                }

                short[] po = null;
                if (learn) {
                    float regionError = ae.put(ins, alpha, noise, (float) 0, true, false, true);
                    sumErr += regionError;


                    float conf;
                    if ((conf = 1f - (regionError / regionPixels)) > 0) {
                        short[] features = ae.max(outputThresh);
                        if (features != null) {

                            conf = Util.round(w2cSafe(c2wSafe(baseConf * conf) / features.length), confRes);
                            if ((pixConf[i][j] = (conf)) >= minConf) {
                                po = features;
                            }

                        }
                    }

                } else {

                    ae.recode(ins, true, false);
                }

                float mult;

                float[] peij = encoded[i][j];

                Arrays.fill(peij, 0);
                if (po != null && po.length > 0) {
                    mult = +1;
                    float f = 0.5f + 0.5f / po.length;
                    for (short ppp : po)
                        peij[ppp] = f;
                } else {
                    mult = -1;
                }


                if (reconstruct) {


                    float[] z = this.ae.z;
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
