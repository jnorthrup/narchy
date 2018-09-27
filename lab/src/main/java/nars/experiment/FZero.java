package nars.experiment;

import jcog.Util;
import jcog.learn.pid.MiniPID;
import jcog.math.FloatAveraged;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.action.ActionConcept;
import nars.concept.action.BiPolarAction;
import nars.concept.action.SwitchAction;
import nars.concept.sensor.AbstractSensor;
import nars.concept.sensor.DigitizedScalar;
import nars.concept.sensor.Signal;
import nars.sensor.Bitmap2DSensor;
import nars.time.Tense;
import nars.video.AutoclassifiedBitmap;
import nars.video.Scale;
import org.apache.commons.math3.util.MathUtils;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import spacegraph.SpaceGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import static nars.$.$$;
import static nars.Op.INH;
import static nars.agent.FrameTrigger.fps;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {

    public static final double DRAG = 0.98;
    private final FZeroGame fz;
    public Bitmap2DSensor<Scale> c;

    float fwdSpeed = 7;
    float rotSpeed = 0.15f;
    static float fps = 40f;


    public static void main(String[] args) {
        NAgentX.runRT(FZero::new, fps);
    }

    public FZero(NAR n) {
        super("fz",
                fps(fps),
                n);


//        Param.DEBUG = true;

        this.fz = new FZeroGame();


        //Param.DEBUG= true;
//        Param.DEBUG_ENSURE_DITHERED_OCCURRENCE = true;
//        Param.DEBUG_ENSURE_DITHERED_DT = true;
//        Param.DEBUG_ENSURE_DITHERED_TRUTH = true;
//        Param.DEBUG_EXTRA = true;

        Scale vision = new Scale(() -> fz.image,
                24, 18
                //8, 8
        );
        onFrame(vision::update);
        vision.update();
        //c = senseCamera($.func("cam", id), vision/*.blur()*/);//.diff()
                //.resolution(0.05f);
                ;

        int nx = 3;
        AutoclassifiedBitmap camAE = new AutoclassifiedBitmap($.p($.the("cae"), id), vision, nx, nx, (subX, subY) -> {
            return new float[]{/*cc.X, cc.Y*/};
        }, 4, this);
        camAE.alpha(0.1f);
        SpaceGraph.window(camAE.newChart(), 500, 500);

        ActionConcept F = initUnipolarLinear(5f);

//        initToggleLeftRight();
//        initToggleFwdStop();
//        window(new Gridding(
//                //new CameraSensorView(c, this).withControls(),
//                NARui.beliefCharts(actions, nar)), 400, 400);


        //initTankContinuous();
        BiPolarAction A =
                //initBipolarRotateRelative(true, 1f);
                //initBipolarRotateAbsolute(true);
                //initBipolarRotateDirect(false, 0.9f);
                initBipolarRotateDirect(false, 0.2f);

//        window(new Gridding(
//                //new CameraSensorView(c, this).withControls(),
//                NARui.beliefCharts(nar, F, A.pos, A.neg)), 400, 400);




        float r = 0.05f;
        Signal dVelX = senseNumberDifference($.func("vel", id, $$("x")), () -> (float) fz.vehicleMetrics[0][7]).resolution(r);
        Signal dVelY = senseNumberDifference($.func("vel", id, $$("y")), () -> (float) fz.vehicleMetrics[0][8]).resolution(r);
        Signal dAccel = senseNumberDifference($.func("accel", id), () -> (float) fz.vehicleMetrics[0][6]).resolution(r);

        FloatSupplier playerAngle = Util.compose(() -> (float) fz.playerAngle,
                new FloatAveraged(0.5f, true));
        Signal dAngVel = senseNumberDifference($.func("ang", id, $$("dTheta")), playerAngle).resolution(r);

        int angles = 9;
        AbstractSensor ang = senseNumber(angle ->
                        //$.func("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/, () ->
                        $.func("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/, () ->
                        (float) (0.5 + 0.5 * MathUtils.normalizeAngle(fz.playerAngle, 0) / (Math.PI)),
                angles,
                DigitizedScalar.Needle
                //DigitizedScalar.FuzzyNeedle
        ).resolution(r);

//        nar.goal($.sim($.func("ang", id, $.varDep(1)),$.func("ang", id, $.varDep(2)).neg()), Tense.ETERNAL);
//        nar.onTask(t -> {
//           if (t.isBelief() && t.toString().contains("ang"))
//               System.out.println(t);
//        });
//        onFrame(()->{
//            int j = 0;
//           for (int i = 0; i < angles; i++) {
//               if (i == j)
//                   System.out.print(" -- ");
//               else {
//                   Term t = $.sim($.func("ang", id, $.the(0)), $.func("ang", id, $.the(i)));
//                   Truth tr = nar.beliefTruth(t, nar.time());
//                   if (tr == null) {
//                       try {
//                           nar.input(t + "? |");
//                       } catch (Narsese.NarseseException e) {
//                           e.printStackTrace();
//                       }
//                   }
//                   System.out.print(" " + tr);
//               }
//           }
//           System.out.println();
//        });


        rewardNormalized("race", -1, +1, ()->{
            double distance = fz.vehicleMetrics[0][1];
            double deltaDistance = (distance - lastDistance);


            lastDistance = distance;

            fz.update();

            float race =
                    ((float)
                            //-(FZeroGame.FULL_POWER - ((float) fz.power)) / FZeroGame.FULL_POWER +
                            deltaDistance / (fps * 0.4f));

//        float r = (deltaDistance > 0) ? (float) (deltaDistance / (fps * 0.2)) : -1f;

            fz.power = Math.max(FZeroGame.FULL_POWER * 0.5f, Math.min(FZeroGame.FULL_POWER, fz.power * 1.15f));

            float bias = 0.25f;
            return race - bias;
        });

        //reward("noCollide", ()->fz.power >= FZeroGame.FULL_POWER- ScalarValue.EPSILON ? +1 : -1 ); //dont bump edges



    }


    private void actionSwitch() {
        SwitchAction s = new SwitchAction(nar, (a) -> {


            fz.rotVel = 0.1f;

            float conf = 0.05f;
            switch (a) {
                case 0: {
                    fz.thrust = true;
                    nar.want(INH.the($.the("fwd"), id), Tense.Present, 1f, conf);
                    nar.want(INH.the($.the("brake"), id), Tense.Present, 0f, conf);
                    break;
                }
                case 1: {
                    fz.thrust = false;
                    fz.left = fz.right = false;
                    nar.want(INH.the($.the("brake"), id), Tense.Present, 1f, conf);
                    nar.want(INH.the($.the("fwd"), id), Tense.Present, 0f, conf);
                    nar.want(INH.the($.the("left"), id), Tense.Present, 0f, conf);
                    nar.want(INH.the($.the("right"), id), Tense.Present, 0f, conf);
                    break;
                }
                case 2: {
                    fz.left = true;
                    fz.right = false;
                    nar.want(INH.the($.the("left"), id), Tense.Present, 1f, conf);

                    nar.want(INH.the($.the("brake"), id), Tense.Present, 0f, conf);
                    nar.want(INH.the($.the("right"), id), Tense.Present, 0f, conf);
                    break;
                }
                case 3: {
                    fz.right = true;
                    fz.left = false;
                    nar.want(INH.the($.the("right"), id), Tense.Present, 1f, conf);
                    nar.want(INH.the($.the("left"), id), Tense.Present, 0f, conf);
                    nar.want(INH.the($.the("brake"), id), Tense.Present, 0f, conf);
                    break;
                }


            }
            return true;
        }, INH.the($.the("fwd"), id),
                INH.the($.the("brake"), id),
                INH.the($.the("left"), id),
                INH.the($.the("right"), id)

        );
        addSensor(s);
        //window(NARui.beliefCharts(s.sensors, nar), 300, 300);
    }

    private void initToggleLeftRight() {

        this.actionPushButtonMutex(
                $.inh($$("left"), id), $.inh($$("right"), id),
                l -> fz.left = l, r -> fz.right = r
        );
    }
    private void initToggleFwdStop() {
        this.actionPushButtonMutex(
                $.inh($$("fwd"), id), $.inh($$("stop"), id),
                f -> fz.thrust = f,
                b -> {
                    if (b) {
                        fz.vehicleMetrics[0][6] *= 0.9f;
                    }
                }
        );
    }

    private void initTankDiscrete() {

        actionToggle($.inh($.the("left"), id), (b) -> {
            fz.left = b;
            fz.thrust = fz.left && fz.right;
        });
        actionToggle($.inh($.the("right"), id), (b) -> {
            fz.right = b;
            fz.thrust = fz.left && fz.right;
        });

    }

    private void initTankContinuous() {

        float res = 0.01f;
        float powerScale = 0.4f;

        final float[] left = new float[1];
        final float[] right = new float[1];

        actionUnipolar($.inh("left", id), (x) -> {
            float power = (x - 0.5f) * 2f * powerScale;
            left[0] = power;
            fz.playerAngle += /*Math.max(0,*/(power - right[0]) * rotSpeed / 2;
            fz.vehicleMetrics[0][6] +=

                    left[0]

                            * fwdSpeed / 2f;
            return x;
        }).resolution(res);

        actionUnipolar($.inh("right", id), (x) -> {
            float power = (x - 0.5f) * 2f * powerScale;
            right[0] = power;
            fz.playerAngle += /*Math.max(0,*/-(power - left[0]) * rotSpeed / 2;
            fz.vehicleMetrics[0][6] +=

                    right[0]

                            * fwdSpeed / 2f;
            return x;
        }).resolution(res);

    }

    public BiPolarAction initBipolarRotateRelative(boolean fair, float rotFactor) {
        final float[] _r = {0};
        final MiniPID rotFilter = new MiniPID(0.5f, 0.3, 0.2f);
        return actionBipolarFrequencyDifferential($.func("turn", id), fair, (r0) -> {

            float r = _r[0] = (float) rotFilter.out(_r[0], r0);

            fz.playerAngle +=

                    r *
                            rotSpeed * rotFactor;
            return r0;
        });
    }

    public BiPolarAction initBipolarRotateDirect(boolean fair, float rotFactor) {

        //final MiniPID rotFilter = new MiniPID(0.3f, 0.3, 0.4f);
        final FloatAveraged lp = new FloatAveraged(0.6f);

        float inputThresh = 0f;
        float curve = //curve exponent
                1;
                //3;

        FloatToFloatFunction d = (dHeading) -> {

            //float ddHeading = Math.abs(dHeading) >= inputThresh ? lp.valueOf(dHeading) : 0;

            fz.playerAngle += Math.pow((dHeading), curve) * rotFactor; //bipolar

            return dHeading;
        };
        BiPolarAction A = actionBipolarFrequencyDifferential(
                //$.func("rotate", id),
                //pn -> CONJ.the(XTERNAL, $.the("rotate"), $.func("rotate", id).negIf(!pn) ),
                //pn -> CONJ.the(XTERNAL, $.func("rotate", id),
                pn -> $.func(pn ? "left" : "right",id)
                //)
                , fair, d);

        A.resolution(0.1f);

        return A;



        //actionUnipolar($.the("heading"), d);
    }

    public void initBipolarRotateAbsolute(boolean fair) {

        final MiniPID rotFilter = new MiniPID(0.1f, 0.1, 0.1f); //LP filter
        FloatToFloatFunction x = (heading) -> {

            fz.playerAngle = (float) rotFilter.out(fz.playerAngle, heading * Math.PI * 2);

            return heading;
        };
        actionBipolarFrequencyDifferential($.p(/*id, */$.the("heading")), fair, x);

    }

    public ActionConcept initUnipolarLinear(float fwdFactor) {
//        final float[] _a = {0};
//        final MiniPID fwdFilter = new MiniPID(0.5f, 0.3, 0.2f);

        return actionUnipolar(/*$.inh(id,*/ $.func("vel", id,  $.the("move")), true, (x) -> Float.NaN /*0.5f*/, (a0) -> {
            float a =
                //_a[0] = (float) fwdFilter.out(_a[0], a0);
                a0;

            float thresh = nar.freqResolution.floatValue();
            if (a > 0.5f + thresh) {
                float thrust = /*+=*/ (2 * (a - 0.5f)) *  (fwdFactor * fwdSpeed);
                fz.vehicleMetrics[0][6] = thrust;
            } else if (a < 0.5f - thresh)
                fz.vehicleMetrics[0][6] *= Util.unitize(Math.max(0.5f, (1f - (0.5f - a) * 2f)));
            else
                return Float.NaN;

            return a0;
        }).resolution(0.1f);
    }

//    protected boolean polarized(@NotNull Task task) {
//        if (task.isQuestionOrQuest())
//            return true;
//        float f = task.freq();
//        return f <= 0.2f || f >= 0.8f;
//    }

    double lastDistance;



    static class FZeroGame extends JFrame {

        public static final int FULL_POWER = 80;
        public static final int MAX_VEL = 20;
        private final JPanel panel;
        public boolean thrust, left, right;
        public double playerAngle;
        public BufferedImage image = new BufferedImage(
                320, 240, BufferedImage.TYPE_INT_RGB);

        public final double[][] vehicleMetrics = new double[10][9];

        boolean[] K = new boolean[65535];
        public double power;
        public int rank;
        double rotVel = 0.06;
        float fwdVel = 1.5f;
        final double VIEWER_X = 159.5;
        final double VIEWER_Y = 32;
        final double VIEWER_Z = -128;
        final double GROUND_Y = 207;

        final int[] screenBuffer = new int[320 * 240];
        final int[][][] projectionMap = new int[192][320][2];
        final int[][][] wiresBitmap = new int[32][256][256];
        final int[][][] bitmaps = new int[6][32][32];
        final byte[][] raceTrack = new byte[512][512];


        final int[] powerOvalY = new int[2];
        boolean onPowerBar;
        final boolean playing = true;

        final BufferedImage[] vehicleSprites = new BufferedImage[10];
        final int[] vehicleSpriteData = new int[64 * 32];

        final Color powerColor = new Color(0xFABEF1);
        final Color darkColor = new Color(0xA7000000, true);
        int wiresBitmapIndex;
        double cos;
        double sin;
        int hitWallCount;
        int paused = 1;
        private final Graphics imageGraphics;
        private final Font largeFont;

        public FZeroGame() {
            powerOvalY[0] = -96;


            for (int spriteIndex = 0; spriteIndex < 10; spriteIndex++) {
                vehicleSprites[spriteIndex] = new BufferedImage(
                        64, 32, BufferedImage.TYPE_INT_ARGB_PRE);
                for (int y = 0, k = 0; y < 32; y++) {
                    for (int x = 0; x < 64; x++, k++) {
                        double dx = (x - 32.0) / 2, dy = y - 26;
                        double dist1 = dx * dx + dy * dy;
                        dx = (x - 31.5) / 2;
                        dy = y - 15.5;
                        double dist2 = dx * dx + dy * dy;
                        dy = y - 17.5;
                        dx = x - 32;
                        double dist3 = dx * dx + dy * dy;
                        if (Math.abs(dist3 - 320) <= 24 || Math.abs(dist3 - 480) <= 24) {
                            vehicleSpriteData[k] = C(
                                    Math.PI * spriteIndex / 1.9,
                                    dist1 / 256,
                                    1) | 0xff000000;
                        } else if (dist2 > 256) {
                            vehicleSpriteData[k] = 0;
                        } else {
                            vehicleSpriteData[k] = C(
                                    Math.PI * spriteIndex / 1.9,
                                    dist1 / 256,
                                    dist1 / 1024 + 1) | 0xff000000;
                        }
                    }
                }
                for (int x = 14; x < 49; x++) {
                    for (int y = 21; y < 27; y++) {
                        vehicleSpriteData[(y << 6) | x] = y == 21 || y == 26 || (x & 1) == 0
                                ? 0xFFCCCCCC : 0xFF000000;
                    }
                }
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        double dx = x - 7.5;
                        double dy = y - 7.5;
                        double dy2 = dy / 1.5;
                        double dist = dx * dx + dy * dy;
                        if (dx * dx + dy2 * dy2 < 64) {
                            dy = y - 4;
                            vehicleSpriteData[(y << 6) | (x + 24)] = C(
                                    3,
                                    dist / 256,
                                    y > 6 && x > 3 && x < 12
                                            || y > 7
                                            || dx * dx + dy * dy < 8 ? 2 : 1) | 0xff000000;
                        }
                        if (dist < 64 || y == 0) {
                            vehicleSpriteData[((16 + y) << 6) | (x + 48)] =
                                    vehicleSpriteData[((16 + y) << 6) | x] = C(
                                            Math.PI * spriteIndex / 1.9,
                                            dist / 64,
                                            1) | 0xff000000;
                        }
                    }
                }
                vehicleSprites[spriteIndex].setRGB(
                        0, 0, 64, 32, vehicleSpriteData, 0, 64);
            }


            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    raceTrack[y][x] = -1;
                }
            }

            for (int y = 0; y < 128; y++) {
                for (int x = 246; x < 261; x++) {
                    raceTrack[y][x] = 0;
                }
            }

            for (int y = 32; y < 96; y++) {
                for (int x = 239; x < 246; x++) {
                    raceTrack[y][x] = (byte) ((x < 244 && y > 33 && y < 94) ? 2 : 0);
                }
            }

            for (int y = 128; y < 512; y++) {
                for (int x = 243; x < 264; x++) {
                    double angle = y * Math.PI / 64;
                    raceTrack[y][x + (int) ((8 * Math.cos(angle) + 24) * Math.sin(angle))]
                            = 0;
                }
            }

            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    if (raceTrack[y][x] >= 0) {
                        for (int i = -1; i < 2; i++) {
                            for (int j = -1; j < 2; j++) {
                                if (raceTrack[0x1FF & (i + y)][0x1FF & (j + x)] == -1) {
                                    raceTrack[y][x] = 1;
                                }
                            }
                        }
                    }
                }
            }


            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    double dx = 15.5 - x;
                    double dy = 15.5 - y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    bitmaps[0][y][x] = 0xFF98A8A8;
                    bitmaps[4][y][x] = 0xFF90A0A0;
                    bitmaps[5][y][x]
                            = (((x >> 3) + (y >> 3)) & 1) == 0 ? 0xFF000000 : 0xFFFFFFFF;
                    bitmaps[2][y][x] = C(4.5, Math.abs(dy) / 16, 1);
                    if (dist < 16) {
                        bitmaps[3][y][x] = 0xFFFFFFFF;
                        bitmaps[1][y][x] = C(5.3, dist / 16, 1 + dist / 256);
                    } else {
                        bitmaps[3][y][x] = bitmaps[1][y][x] = 0xFF98A8A8;
                    }
                }
            }


            for (int y = 0; y < 192; y++) {
                for (int x = 0; x < 320; x++) {
                    double k = (GROUND_Y - VIEWER_Y) / (48 + y - VIEWER_Y);
                    projectionMap[y][x][0] = (int) (k * (x - VIEWER_X) + VIEWER_X);
                    projectionMap[y][x][1] = (int) (VIEWER_Z * (1 - k));
                }
            }


            setTitle("F-Zero 4K");
            setIconImage(vehicleSprites[0]);
            panel = (JPanel) getContentPane();
            panel.setPreferredSize(new Dimension(640, 480));
            panel.setIgnoreRepaint(true);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            pack();
            setLocationRelativeTo(null);
            show();


            imageGraphics = image.getGraphics();
            largeFont = getFont().deriveFont(100f);


        }

        public int C(double angle, double light, double dark) {
            return (D(angle, light, dark) << 16)
                    | (D(angle + 2 * Math.PI / 3, light, dark) << 8)
                    | (D(angle - 2 * Math.PI / 3, light, dark));
        }

        public void update() {


            wiresBitmapIndex = 0x1F & (wiresBitmapIndex + 1);

            if (paused > 0) {
                paused--;
                if (paused == 0) {
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j < 9; j++) {
                            vehicleMetrics[i][j] = 0;
                        }
                    }
                    for (int i = 0; i < 4; i++) {
                        vehicleMetrics[i][0] = 7984 + i * 80;
                    }
                    for (int i = 4; i < 10; i += 3) {
                        vehicleMetrics[i][0] = 7984;
                        vehicleMetrics[i][1] = 16384 * (i - 3);
                        vehicleMetrics[i + 1][0] = 8144;
                        vehicleMetrics[i + 1][1] = vehicleMetrics[i][1] + 2048;
                        vehicleMetrics[i + 2][0] = 8144;
                        vehicleMetrics[i + 2][1] = vehicleMetrics[i][1] + 3840;
                    }
                    power = FULL_POWER;
                    playerAngle = hitWallCount = 0;
                    imageGraphics.setFont(getFont().deriveFont(32f));
                    onPowerBar = false;
                }
            } else if (vehicleMetrics[0][1] < 81984 && power > 0) {


                rank = 1;
                for (int i = 1; i < 4; i++) {
                    if (vehicleMetrics[0][1] < vehicleMetrics[i][1]) {
                        rank++;
                    }
                }


                if (hitWallCount > 0) {
                    hitWallCount--;
                    power -= 1;
                    if (power < 0) {
                        power = 0;
                    }
                }


                if (playing) {
                    boolean L = left || K[KeyEvent.VK_LEFT];
                    boolean R = right || K[KeyEvent.VK_RIGHT];
                    if (L && !R) {
                        playerAngle += rotVel;
                    }
                    if (R && !L) {
                        playerAngle -= rotVel;
                    }
                }
                cos = Math.cos(playerAngle);
                sin = Math.sin(playerAngle);
                vehicleMetrics[0][4] = 0;
                vehicleMetrics[0][5] = 0;
                if (thrust || K[KeyEvent.VK_D]) {

                    if (vehicleMetrics[0][6] < MAX_VEL) {
                        vehicleMetrics[0][6] += fwdVel;
                    }
                } else {
                    vehicleMetrics[0][6] *= 0.99;
                }

                if (playing) {
                    // compute computer-controlled-vehicles velocities
                    for (int i = 1; i < 10; i++) {
                        if ((i < 4 && vehicleMetrics[i][6] < 20.5)
                                || vehicleMetrics[i][6] < 10) {
                            vehicleMetrics[i][6] += 0.2 + i * 0.2;
                        }
                        double targetZ = 11 + vehicleMetrics[i][1];
                        double tz = (targetZ / 32) % 512;
                        double targetX = 7984 + (i & 0x03) * 80;
                        if (i >= 4) {
                            targetX += 32;
                        }

                        if (tz >= 128) {
                            double angle = tz * Math.PI / 64;
                            targetX += ((8 * Math.cos(angle) + 24) * Math.sin(angle)) * 32;
                        }

                        double vx = targetX - vehicleMetrics[i][0];
                        double vz = targetZ - vehicleMetrics[i][1];
                        double mag = Math.sqrt(vx * vx + vz * vz);
                        vehicleMetrics[i][7]
                                = vehicleMetrics[i][2] + vehicleMetrics[i][6] * vx / mag;
                        vehicleMetrics[i][8]
                                = vehicleMetrics[i][2] + vehicleMetrics[i][6] * vz / mag;
                    }

                    // player on power bar?
                    onPowerBar = false;
                    if (raceTrack[0x1FF & (((int) vehicleMetrics[0][1]) >> 5)]
                            [0x1FF & (((int) vehicleMetrics[0][0]) >> 5)] == 2) {
                        onPowerBar = true;
                        for (int i = 0; i < 2; i++) {
                            powerOvalY[i] += 16;
                            if (powerOvalY[i] >= 192) {
                                powerOvalY[i] = -32;
                            }
                        }
                        if (power < 80) {
                            power += 0.2;
                        }
                    }

                    vehicleMetrics[0][7] = vehicleMetrics[0][2]
                            - vehicleMetrics[0][6] * sin;
                    vehicleMetrics[0][8] = vehicleMetrics[0][3]
                            + vehicleMetrics[0][6] * cos;

                    // vehicle hitting something?
                    for (int j = 0; j < 10; j++) {

                        // vehicle hitting another vehicle?
                        for (int i = 0; i < 10; i++) {
                            if (i != j) {
                                double normalX = (vehicleMetrics[j][0]
                                        - vehicleMetrics[i][0]) / 2;
                                double normalZ = vehicleMetrics[j][1]
                                        - vehicleMetrics[i][1];
                                double dist2 = normalX * normalX + normalZ * normalZ;
                                if (dist2 < 1200) {
                                    double dotProduct = normalX * vehicleMetrics[0][7]
                                            + normalZ * vehicleMetrics[0][8];
                                    if (dotProduct < 0) {


                                        double ratio = 2 * dotProduct / dist2;
                                        vehicleMetrics[j][7] = vehicleMetrics[j][2]
                                                = vehicleMetrics[0][7] - normalX * ratio;
                                        vehicleMetrics[j][8] = vehicleMetrics[j][3]
                                                = vehicleMetrics[0][8] - normalZ * ratio;

                                        double restitution = 0.5;
                                        vehicleMetrics[i][2] = -vehicleMetrics[j][2] * restitution;
                                        vehicleMetrics[i][3] = -vehicleMetrics[j][3] * restitution;
                                        if (i == 0) {
                                            power -= 10;
                                            if (power < 0) {
                                                power = 0;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        // vehicle hitting a wall?
                        int vehicleX = ((int) vehicleMetrics[j][0]) >> 5;
                        int vehicleZ = ((int) vehicleMetrics[j][1]) >> 5;
                        for (int z = -2; z <= 2; z++) {
                            for (int x = -2; x <= 2; x++) {
                                if (Math.abs(raceTrack
                                        [0x1FF & (z + vehicleZ)][0x1FF & (x + vehicleX)]) == 1) {
                                    double normalX = vehicleMetrics[j][0]
                                            - (((x + vehicleX) << 5) + 16);
                                    double normalZ = vehicleMetrics[j][1]
                                            - (((z + vehicleZ) << 5) + 16);
                                    double dist2 = normalX * normalX + normalZ * normalZ;
                                    if (dist2 < 2304) {
                                        double dotProduct = normalX * vehicleMetrics[j][7]
                                                + normalZ * vehicleMetrics[j][8];
                                        if (dotProduct < 0) {
                                            double ratio = 2.0 * dotProduct / dist2;
                                            vehicleMetrics[j][7] = vehicleMetrics[j][2]
                                                    = vehicleMetrics[0][7] - normalX * ratio;
                                            vehicleMetrics[j][8] = vehicleMetrics[j][3]
                                                    = vehicleMetrics[0][8] - normalZ * ratio;
                                            vehicleMetrics[j][6] /= 2;
                                            if (j == 0) {
                                                hitWallCount = 5;
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        double velocityMag = vehicleMetrics[j][7] * vehicleMetrics[j][7]
                                + vehicleMetrics[j][8] * vehicleMetrics[j][8];
                        double velocityMaxMag = j == 0 ? 400 : 420;
                        if (velocityMag > velocityMaxMag) {
                            velocityMaxMag = Math.sqrt(velocityMaxMag);
                            velocityMag = Math.sqrt(velocityMag);
                            vehicleMetrics[j][7]
                                    = velocityMaxMag * vehicleMetrics[j][7] / velocityMag;
                            vehicleMetrics[j][8]
                                    = velocityMaxMag * vehicleMetrics[j][8] / velocityMag;
                        }

                        vehicleMetrics[j][0] += vehicleMetrics[j][7];
                        vehicleMetrics[j][1] += vehicleMetrics[j][8];
                        vehicleMetrics[j][2] *= DRAG;
                        vehicleMetrics[j][3] *= DRAG;
                    }
                }
            } else {
                paused = 175;
            }


            double skyRed = 0x65;
            double skyGreen = 0x91;
            for (int y = 0, k = 0; y < 48; y++) {
                int skyColor = 0xFF000000
                        | (((int) skyRed) << 16) | (((int) skyGreen) << 8) | 0xF2;
                for (int x = 0; x < 320; x++, k++) {
                    screenBuffer[k] = skyColor;
                }
                skyRed += 1.75;
                skyGreen += 1.625;
            }


            for (int y = 0, k = 15360; y < 192; y++) {
                for (int x = 0; x < 320; x++, k++) {
                    double X = projectionMap[y][x][0] - VIEWER_X;
                    double Z = projectionMap[y][x][1];
                    int xr = (int) (X * cos - Z * sin + vehicleMetrics[0][0]);
                    int zr = (int) (X * sin + Z * cos + vehicleMetrics[0][1]);

                    int z = 0x1FF & (zr >> 5);
                    int tileIndex = raceTrack[z][0x1FF & (xr >> 5)];
                    if (hitWallCount > 0 && tileIndex == 1) {
                        tileIndex = 3;
                    }
                    if (tileIndex == 0 && z < 128 && (z & 1) == 0) {
                        tileIndex = (z == 2) ? 5 : 4;
                    }
                    if (tileIndex < 0) {

                        screenBuffer[k]
                                = wiresBitmap[wiresBitmapIndex][0xFF & zr][0xFF & xr];
                    } else {
                        screenBuffer[k] = bitmaps[tileIndex][0x1F & zr][0x1F & xr];
                    }
                }
            }

            image.setRGB(0, 0, 320, 240, screenBuffer, 0, 320);


            for (int i = 0; i < 10; i++) {
                double X = vehicleMetrics[i][0] - vehicleMetrics[0][0];
                double Z = vehicleMetrics[i][1] - vehicleMetrics[0][1];
                vehicleMetrics[i][4] = X * cos + Z * sin;
                vehicleMetrics[i][5] = (int) (Z * cos - X * sin);
            }
            for (int z = 1200; z > -127; z--) {
                for (int i = 0; i < 10; i++) {
                    if (z == vehicleMetrics[i][5]) {
                        double k = VIEWER_Z / (VIEWER_Z - z);
                        double upperLeftX
                                = k * (vehicleMetrics[i][4] - 32) + VIEWER_X;
                        double upperLeftY
                                = k * (GROUND_Y - 32 - VIEWER_Y) + VIEWER_Y;
                        double lowerRightX
                                = k * (vehicleMetrics[i][4] + 32) + VIEWER_X;
                        double lowerRightY
                                = k * (GROUND_Y - VIEWER_Y) + VIEWER_Y;
                        imageGraphics.drawImage(vehicleSprites[i],
                                (int) upperLeftX, (int) upperLeftY,
                                (int) (lowerRightX - upperLeftX),
                                (int) (lowerRightY - upperLeftY), null);
                    }
                }
            }


            imageGraphics.setColor(power < 20 && (wiresBitmapIndex & 8) == 0
                    ? Color.WHITE : powerColor);
            imageGraphics.fillRect(224, 20, (int) power, 10);
            imageGraphics.setColor(Color.WHITE);
            imageGraphics.drawRect(224, 20, 80, 10);


            if (onPowerBar) {
                imageGraphics.setColor(Color.GREEN);
                for (int i = 0; i < 2; i++) {
                    imageGraphics.fillOval(96, powerOvalY[i], 128, 32);
                }
            }


            if (power <= 0 || (vehicleMetrics[0][1] >= 81984 && rank > 3)) {

                String failString = "FAIL";
                imageGraphics.setFont(largeFont);
                int width = imageGraphics.getFontMetrics().stringWidth(failString);
                int x = (320 - width) / 2;
                imageGraphics.setColor(darkColor);
                imageGraphics.fillRect(x, 65, width + 5, 90);
                imageGraphics.setColor(Color.RED);
                imageGraphics.drawString(failString, x, 145);
            } else if (vehicleMetrics[0][1] >= 81984) {

                String rankString = Integer.toString(rank);
                imageGraphics.setFont(largeFont);
                int width = imageGraphics.getFontMetrics().stringWidth(rankString);
                int x = (320 - width) / 2;
                imageGraphics.setColor(darkColor);
                imageGraphics.fillRect(x - 5, 65, width + 15, 90);
                imageGraphics.setColor((wiresBitmapIndex & 4) == 0
                        ? Color.WHITE : Color.GREEN);
                imageGraphics.drawString(rankString, x, 145);
            } else {

                imageGraphics.setColor((rank == 4) ? (wiresBitmapIndex & 8) == 0
                        ? Color.WHITE : Color.RED : Color.GREEN);
                imageGraphics.drawString(Integer.toString(rank), 16, 32);
            }

            Graphics panelGraphics = panel.getGraphics();
            if (panelGraphics != null) {
                panelGraphics.drawImage(image, 0, 0, 640, 480, null);
                panelGraphics.dispose();
            }

        }

        public int D(double angle, double light, double dark) {
            return (int) (255 * Math.pow((Math.cos(angle) + 1) / 2, light) / dark);
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            K[e.getKeyCode()] = e.getID() == 401;
        }


    }
}
