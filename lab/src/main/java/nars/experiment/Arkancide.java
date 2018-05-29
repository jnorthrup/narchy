package nars.experiment;


import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.term.atom.Atomic;
import nars.util.TimeAware;
import nars.util.signal.Bitmap2DConcepts;
import nars.video.Scale;
import nars.video.SwingBitmap2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Arkancide extends NAgentX {

    static boolean numeric = true;
    static boolean cam = true;

    public final FloatRange ballSpeed = new FloatRange(0.75f, 0.04f, 6f);
    //public final FloatParam paddleSpeed = new FloatParam(2f, 0.1f, 3f);


    final int visW = 48;
    final int visH = 32;

    //final int afterlife = 60;

    static float paddleSpeed;


    final Arkanoid noid;

    private float prevScore;

    public static void main(String[] args) {
        //

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);

        TimeAware timeAware = runRT((NAR n) -> {

            Arkancide a = new Arkancide(n, cam, numeric);
                //a.curiosity.setValue(0.05f);

                //a.trace = true;
                //((RealTime)a.nar.time).durSeconds(0.05f);
                //a.nar.log();



            //new RLBooster(a, HaiQAgent::new, 2 );

            //new Implier(1, a, new float[] { 1 });


            return a;

        }, 30, 30);


//        nar.forEachActiveConcept(c -> {
//            c.forEachTask(t -> {
//                System.out.println(t);
//            });
//        });

        //IO.saveTasksToTemporaryTextFile(nar);

        //System.out.println(ts.db.getInfo());

        //ts.db.close();

        //nar.beliefConfidence(0.75f);
        //nar.goalConfidence(0.75f);
    }


    public Arkancide(NAR nar) {
        this(nar, true, true);
    }

    public Arkancide(NAR nar, boolean cam, boolean numeric)  {
        super("noid", nar);
        //super(nar, HaiQAgent::new);


        noid = new Arkanoid(true);
//        {
//            @Override
//            protected void die() {
//                //nar.time.tick(afterlife); //wont quite work in realtime mode
//                super.die();
//            }
//        };


        paddleSpeed = 50 * noid.BALL_VELOCITY;

        //initBipolarDirect();
        //initBipolarRelative();
        //initUnipolar();
        initToggle();

        float resX = 0.01f; //Math.max(0.01f, 0.5f / visW); //dont need more resolution than 1/pixel_width
        float resY = 0.01f; //Math.max(0.01f, 0.5f / visH); //dont need more resolution than 1/pixel_width

        if (cam) {

            Bitmap2DConcepts<Scale> cc = senseCamera(id /*$.the("cam")*/, new Scale(
                    new SwingBitmap2D(noid)
                    //32, 24
                    , visW, visH
                    //10,4
                    //16,8
            )/*.blur()*/).resolution(0.02f);
//            BufferedImageBitmap2D sw = new Scale(new SwingBitmap2D(noid.canvas), visW, visH).blur();
//            Bitmap2DConcepts cc = senseCamera(id.toString(), sw, visW, visH)
//                    .resolution(0.1f);
//            CameraSensor ccAe = senseCameraReduced($.the("noidAE"), sw, 16)
//                    .resolution(0.25f);

            //senseCameraRetina("noid", noid, visW/2, visH/2, (v) -> $.t(v, alpha));
            //new CameraGasNet($.the("camF"),new Scale(new SwingCamera(noid), 80, 80), this, 64);
        }


        if (numeric) {
            senseNumber($.the("px"), (() -> noid.paddle.x / noid.getWidth())).resolution(resX);
            senseNumber($.the("dx"), (() -> Math.abs(noid.ball.x - noid.paddle.x) / noid.getWidth())).resolution(resX);
            senseNumber($.the("bx"), (() -> (noid.ball.x / noid.getWidth()))).resolution(resX);
            senseNumber($.the("by"), (() -> 1f - (noid.ball.y / noid.getHeight()))).resolution(resY);
            //SensorConcept d = senseNumber("noid:bvx", new FloatPolarNormalized(() -> noid.ball.velocityX)).resolution(0.25f);
            //SensorConcept e = senseNumber("noid:bvy", new FloatPolarNormalized(() -> noid.ball.velocityY)).resolution(0.25f);

            //experimental cheat
//            nar.input("--(paddle-ball):x! :|:",
//                      "--(ball-paddle):x! :|:"
//            );

//            SpaceGraph.window(Vis.beliefCharts(100,
//                    Lists.newArrayList(ab.term(), a.term(), b.term(), c.term()),
//                    nar), 600, 600);
//
//            nar.onTask(t -> {
//                if (//t instanceof DerivedTask &&
//                        //t.isEternal()) {
//                        t.isGoal()) {
//                    System.err.println(t.proof());
//                }
//            });

//            window(Vis.beliefCharts(64, java.util.List.of(px, dx, bx, by), nar), 300, 300);
//
//            new BeliefPredict(
//                    Iterables.concat(actions.keySet(), java.util.List.of(px, dx, bx, by)),
//                    8,
//                    12,
//                    Iterables.concat(actions.keySet(), java.util.List.of(px, dx, bx, by)),
//                    //new LivePredictor.LSTMPredictor(0.1f, 2),
//                    new LivePredictor.MLPPredictor(),
//                    nar
//            );
        }

        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/

//        actionUnipolarTransfer(paddleControl, (v) -> {
//            return noid.paddle.moveTo(v, maxPaddleSpeed);
//        });
        /*actionTriState*/
//        actionBipolar($.the("x"), (s) -> {
////           switch (s) {
////               case 0:
////                   break;
////               case -1:
////               case 1:
////                    if (s > 0 && Util.equals(noid.paddle.x, noid.getWidth(), 1f))
////                        return 0f; //edge
////                    if (s < 0 && Util.equals(noid.paddle.x, 0, 1f))
////                        return 0f; //edge
//                   if (!noid.paddle.move( maxPaddleSpeed * s)) {
//                       return 0f; //against wall
//                   }
////                    break;
////           }
//                    return s;
//
//        });///.resolution(0.1f);

//
//        nar.onTask((t) -> {
//            if (!t.isInput() && (t.isGoal() || t.isEternal())) {
//                System.err.println(t.proof());
//            }
//        });


//
//        nar.onTask(x -> {
//            if (x.isGoal()
//                    && !x.isInput() && (!(x instanceof ActionConcept.CuriosityTask))
//                    //&& x.term().equals(paddleControl)
//            ) {
//                System.err.println(x.proof());
//            }
//        });


//        actionUnipolar($.inh(Atomic.the("paddle"), Atomic.the("nx") ), (v) -> {
//            noid.paddle.moveTo(v, paddleSpeed.floatValue() * maxPaddleSpeed);
//            return true;
//        });
//        action(new ActionConcept( $.func("nx", "paddle"), nar, (b, d) -> {
//
//
//            float pct;
//            if (d != null) {
//                pct = noid.paddle.moveTo(d.freq(), paddleSpeed.floatValue() * maxPaddleSpeed);// * d.conf());
//            } else {
//                pct = noid.paddle.x / Arkanoid.SCREEN_WIDTH; //unchanged
//            }
//            return $.t(pct, nar.confidenceDefault(BELIEF));
//            //return null; //$.t(0.5f, alpha);
//        })/*.feedbackResolution(resX)*/);

        //nar.log();
//        predictors.add( (MutableTask)nar.input(
//                //"(((noid) --> #y) && (add(#x,1) <-> #y))?"
//                "((cam --> ($x,$y)) && (camdiff($x,$y) --> similaritree($x,$y))). %1.00;0.99%"
//        ).get(0) );

    }

    private void initBipolarRelative() {
        actionBipolar($.the("X"), false, (dx) -> {

            //System.out.println(dx);
            if (noid.paddle.move(dx * paddleSpeed))
                return dx;
            else
                return 0;
        });
    }
    private void initBipolarDirect() {
        actionBipolar($.the("X"), true, (dx) -> {
            noid.paddle.set(dx/2f+0.5f);
            return dx;
        });
    }
    private void initToggle() {
        actionPushButtonMutex(Atomic.the("L"), Atomic.the("R"),
                (b) -> { if (b) noid.paddle.move(-paddleSpeed); },
                (b) -> { if (b) noid.paddle.move(+paddleSpeed); }
                );
//        actionPushButton($.the("L"), );
//        actionPushButton($.the("R"), () -> noid.paddle.move(+paddleSpeed));
    }
    private void initUnipolar() {
        actionUnipolar($.the("L"), (u)-> noid.paddle.move(-paddleSpeed*u) ? u : 0);
        actionUnipolar($.the("R"), (u)-> noid.paddle.move(+paddleSpeed*u) ? u : 0);
    }

    @Override
    protected float act() {
        noid.BALL_VELOCITY = ballSpeed.floatValue();
        float nextScore = noid.next();
        float reward = Math.max(-1f, Math.min(1f, nextScore - prevScore));
        this.prevScore = nextScore;
        //if (reward == 0) return Float.NaN;
        return reward;
    }


    /**
     * https://gist.github.com/Miretz/f10b18df01f9f9ebfad5
     */
    public static class Arkanoid extends Frame implements KeyListener {

        int score;

        final static int FPS = 50;

        public static final int SCREEN_WIDTH = 360;
        public static final int SCREEN_HEIGHT = 250;

        public static final int BLOCK_LEFT_MARGIN = 10;
        public static final int BLOCK_TOP_MARGIN = 15;

        public static final float BALL_RADIUS = 10.0f;
        public float BALL_VELOCITY = 0.5f;

        public static final float PADDLE_WIDTH = 40.0f;
        public static final float PADDLE_HEIGHT = 20.0f;

        public static final float BLOCK_WIDTH = 40.0f;
        public static final float BLOCK_HEIGHT = 15.0f;

        public static final int COUNT_BLOCKS_X = 7;
        public static final int COUNT_BLOCKS_Y = 3;

        public static final float FT_STEP = 4.0f;


        /* GAME VARIABLES */

        public Arkanoid(boolean visible) {

//            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setUndecorated(false);
            this.setResizable(false);


//            this.setLocationRelativeTo(null);
            //setIgnoreRepaint(true);
//            addKeyListener(this);
            if (visible)
                this.setVisible(true);

            paddle.x = SCREEN_WIDTH / 2;


            setSize(SCREEN_WIDTH, SCREEN_HEIGHT);

            new Timer(1000/FPS, (e)->{
               repaint();
            }).start();

            reset();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            ball.draw(g);
            paddle.draw(g);
            for (Brick brick : bricks) {
                brick.draw(g);
            }
        }

        public final Paddle paddle = new Paddle(SCREEN_WIDTH / 2, SCREEN_HEIGHT - PADDLE_HEIGHT);
        public final Ball ball = new Ball(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
        public final Collection<Brick> bricks = Collections.newSetFromMap(new ConcurrentHashMap());

        //private float lastFt;
        //private float currentSlice;

        abstract class GameObject {
            abstract float left();

            abstract float right();

            abstract float top();

            abstract float bottom();
        }

        class Rectangle extends GameObject {

            public float x, y;
            public float sizeX;
            public float sizeY;

            @Override
            float left() {
                return x - sizeX / 2.0f;
            }

            @Override
            float right() {
                return x + sizeX / 2.0f;
            }

            @Override
            float top() {
                return y - sizeY / 2.0f;
            }

            @Override
            float bottom() {
                return y + sizeY / 2.0f;
            }

        }


        void increaseScore() {
            score++;
            if (score == (COUNT_BLOCKS_X * COUNT_BLOCKS_Y)) {
                win();
            }
        }

        protected void win() {
            reset();
        }

        protected void die() {
            reset();
        }

        class Paddle extends Rectangle {

//            public float velocity;

            public Paddle(float x, float y) {
                this.x = x;
                this.y = y;
                this.sizeX = PADDLE_WIDTH;
                this.sizeY = PADDLE_HEIGHT;
            }

            /**
             * returns percent of movement accomplished
             */
            public synchronized boolean move(float dx) {
                float px = x;
                x = Util.clamp(x + dx, sizeX, SCREEN_WIDTH - sizeX);
                return !Util.equals(px, x, 1f);
            }
//
//            void update() {
//                x += velocity * FT_STEP;
//            }
//
//            void stopMove() {
//                velocity = 0.0f;
//            }
//
//            void moveLeft() {
//                if (left() > 0.0) {
//                    velocity = -paddleSpeed;
//                } else {
//                    velocity = 0.0f;
//                }
//            }
//
//            void moveRight() {
//                if (right() < SCREEN_WIDTH) {
//                    velocity = paddleSpeed;
//                } else {
//                    velocity = 0.0f;
//                }
//            }

            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect((int) (left()), (int) (top()), (int) sizeX, (int) sizeY);
            }

            public void set(float freq) {
                x = freq * SCREEN_WIDTH;
            }

            public float moveTo(float target, float paddleSpeed) {
                target *= SCREEN_WIDTH;

                if (Math.abs(target - x) <= paddleSpeed) {
                    x = target;
                } else if (target < x) {
                    x -= paddleSpeed;
                } else {
                    x += paddleSpeed;
                }

                x = Math.min(x, SCREEN_WIDTH - 1);
                x = Math.max(x, 0);

                return x / SCREEN_WIDTH;
            }
        }


        static final AtomicInteger brickSerial = new AtomicInteger(0);

        class Brick extends Rectangle implements Comparable<Brick> {

            int id;
            boolean destroyed;

            Brick(float x, float y) {
                this.x = x;
                this.y = y;
                this.sizeX = BLOCK_WIDTH;
                this.sizeY = BLOCK_HEIGHT;
                this.id = brickSerial.incrementAndGet();
            }

            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect((int) left(), (int) top(), (int) sizeX, (int) sizeY);
            }

            @Override
            public int compareTo(Brick o) {
                return Integer.compare(id, o.id);
            }
        }

        class Ball extends GameObject {

            public float x, y;
            float radius = BALL_RADIUS;

            //45 degree angle initially
            public float velocityX;
            public float velocityY;

            Ball(int x, int y) {
                this.x = x;
                this.y = y;
                setVelocityRandom();
            }

            public void setVelocityRandom() {
                this.setVelocity(BALL_VELOCITY, (float) (Math.random() * -Math.PI * (2 / 3f) + -Math.PI - Math.PI / 6)); //angled downward
            }

            public void setVelocity(float speed, float angle) {
                this.velocityX = (float) Math.cos(angle) * speed;
                this.velocityY = (float) Math.sin(angle) * speed;
            }

            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillOval((int) left(), (int) top(), (int) radius * 2,
                        (int) radius * 2);
            }

            void update(Paddle paddle) {
                x += velocityX * FT_STEP;
                y += velocityY * FT_STEP;

                if (left() < 0)
                    velocityX = BALL_VELOCITY;
                else if (right() > SCREEN_WIDTH)
                    velocityX = -BALL_VELOCITY;
                if (top() < 0) {
                    velocityY = BALL_VELOCITY;
                } else if (bottom() > SCREEN_HEIGHT) {
                    velocityY = -BALL_VELOCITY;
                    x = paddle.x;
                    y = paddle.y - 50;
                    score--;
                    die();
                }

            }

            @Override
            float left() {
                return x - radius;
            }

            @Override
            float right() {
                return x + radius;
            }

            @Override
            float top() {
                return y - radius;
            }

            @Override
            float bottom() {
                return y + radius;
            }

        }

        boolean isIntersecting(GameObject mA, GameObject mB) {
            return mA.right() >= mB.left() && mA.left() <= mB.right()
                    && mA.bottom() >= mB.top() && mA.top() <= mB.bottom();
        }

        void testCollision(Paddle mPaddle, Ball mBall) {
            if (!isIntersecting(mPaddle, mBall))
                return;
            mBall.velocityY = -BALL_VELOCITY;
            if (mBall.x < mPaddle.x)
                mBall.velocityX = -BALL_VELOCITY;
            else
                mBall.velocityX = BALL_VELOCITY;
        }

        void testCollision(Brick mBrick, Ball mBall) {
            if (!isIntersecting(mBrick, mBall))
                return;

            mBrick.destroyed = true;

            increaseScore();

            float overlapLeft = mBall.right() - mBrick.left();
            float overlapRight = mBrick.right() - mBall.left();
            float overlapTop = mBall.bottom() - mBrick.top();
            float overlapBottom = mBrick.bottom() - mBall.top();

            boolean ballFromLeft = overlapLeft < overlapRight;
            boolean ballFromTop = overlapTop < overlapBottom;

            float minOverlapX = ballFromLeft ? overlapLeft : overlapRight;
            float minOverlapY = ballFromTop ? overlapTop : overlapBottom;

            if (minOverlapX < minOverlapY) {
                mBall.velocityX = ballFromLeft ? -BALL_VELOCITY : BALL_VELOCITY;
            } else {
                mBall.velocityY = ballFromTop ? -BALL_VELOCITY : BALL_VELOCITY;
            }
        }

        void initializeBricks(Collection<Brick> bricks) {
            // deallocate old bricks
            //synchronized(bricks) {
            bricks.clear();

            for (int iX = 0; iX < COUNT_BLOCKS_X; ++iX) {
                for (int iY = 0; iY < COUNT_BLOCKS_Y; ++iY) {
                    bricks.add(new Brick((iX + 1) * (BLOCK_WIDTH + 3) + BLOCK_LEFT_MARGIN,
                            (iY + 2) * (BLOCK_HEIGHT + 3) + BLOCK_TOP_MARGIN));
                }
            }
            //}
        }


        public void reset() {
            initializeBricks(bricks);
            ball.x = SCREEN_WIDTH / 2;
            ball.y = SCREEN_HEIGHT / 2;
            ball.setVelocityRandom();
        }

        //	void run() {
        //
        //
        //		running = true;
        //
        //		reset();
        //
        //		while (running) {
        //
        //			next();
        //
        //		}
        //
        //		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        //
        //	}

        public float next() {

            //currentSlice += lastFt;

            //for (; currentSlice >= FT_SLICE; currentSlice -= FT_SLICE) {

            ball.update(paddle);
            testCollision(paddle, ball);

            //synchronized (bricks) {
            Iterator<Brick> it = bricks.iterator();
            while (it.hasNext()) {
                Brick brick = it.next();
                testCollision(brick, ball);
                if (brick.destroyed) {
                    it.remove();
                }
            }
            //}

            //}

            return score;
        }



        @Override
        public void keyPressed(KeyEvent event) {


            switch (event.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    paddle.move(-paddleSpeed);
                    break;
                case KeyEvent.VK_RIGHT:
                    paddle.move(+paddleSpeed);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent arg0) {

        }


    }
}

//                {
//                    Default m = new Default(256, 48, 1, 2, n.random,
//                            new CaffeineIndex(new DefaultConceptBuilder(), 2048, false, null),
//                            new RealTime.DSHalf().durSeconds(1f));
//                    float metaLearningRate = 0.9f;
//                    m.confMin.setValue(0.02f);
//                    m.goalConfidence(metaLearningRate);
//                    m.termVolumeMax.setValue(16);
//
//                    MetaAgent metaT = new MetaAgent(a, m); //init before loading from file
//                    metaT.trace = true;
//                    metaT.init();
//
//                    String META_PATH = "/tmp/meta.nal";
//                    try {
//                        m.input(new File(META_PATH));
//                    } catch (IOException e) {
//                    }
//                    getRuntime().addShutdownHook(new Thread(() -> {
//                        try {
//                            m.output(new File(META_PATH), (x) -> {
//                                if (x.isBeliefOrGoal() && !x.isDeleted() && (x.op() == IMPL || x.op() == Op.EQUI || x.op() == CONJ)) {
//                                    //Task y = x.eternalized();
//                                    //return y;
//                                    return x;
//                                }
//                                return null;
//                            });
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }));
//                n.onCycle(metaT.nar::cycle);
//                }

