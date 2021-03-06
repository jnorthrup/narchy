package nars.experiment;


import jcog.Util;
import jcog.exe.Loop;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.Agent;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.game.GameTime;
import nars.game.NAct;
import nars.game.SimpleReward;
import nars.game.sensor.AbstractSensor;
import nars.game.util.RLBooster;
import nars.gui.NARui;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.video.SwingBitmap2D;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static jcog.learn.ql.dqn3.DQN3.Option.TD_ERROR_CLAMP;
import static nars.$.*;
import static spacegraph.SpaceGraph.window;

/** NARkanoid */
public class ArkaNAR extends GameX {

    static boolean numeric = false;
    static boolean cam = true;
    static float fps = 25f;

    public final FloatRange ballSpeed = new FloatRange(1.25f, 0.04f, 6f);


    //final int visW = 48, visH = 32;
    //final int visW = 24, visH = 16;
    static final int visW = 20;
    static final int visH = 16;
    Bitmap2DSensor<ScaledBitmap2D> cc;
    //final int visW = 8, visH = 6;


    float paddleSpeed;


    final Arkanoid noid;



    public static class RLBoosterOnly {

        public static void main(String[] args) {

            Term id = INSTANCE.$$("rl");
            float FPS = 25f;
            NAR n = GameX.Companion.baseNAR(FPS, 2);

            //GameX a = new FZero(id, n);
            //GameX a = new ArkaNAR(id, n, true, false);
            GameX a = new PoleCart(id, n, 0.03f);

            RLBooster rl = new RLBooster(true, a,
                    2, 4, new IntIntToObjectFunction<Agent>() {
                @Override
                public Agent apply(int i, int o) {
                    return new DQN3(i, o, Map.of(
                            DQN3.Option.ALPHA, 0.05,
                            DQN3.Option.GAMMA, 0.5,
//                    DQN3.Option.EPSILON, 0.05,
                            DQN3.Option.LEARNING_STEPS_PER_ITERATION, 24.0,
                            DQN3.Option.NUM_HIDDEN_UNITS, 8.0,
                            TD_ERROR_CLAMP, 1.0
                    ));
                }
            }

//                    (i, o) -> new HaiQae(i, o)

            );
            n.add(a);

            window(new Splitting(
                        NARui.game(a),
                        0.5f,
                        new Gridding(
                            //new VectorSensorView(a.cc, a).withControls(), //TODO RL View
                            NARui.rlbooster(rl)
                        )
                    ).resizeable(), 1024, 800);

            Loop loop = n.startFPS(FPS);


        }

    }


    public static class MultiArkaNAR {
        public static void main(String[] args) {

            NAR n = Companion.initC(40.0F, new Consumer<NAR>() {
                @Override
                public void accept(NAR nn) {

                    ArkaNAR a = new ArkaNAR(INSTANCE.$$("(noid,a)"), nn, cam, numeric);
                    a.ballSpeed.set(0.7f * a.ballSpeed.floatValue());

                    ArkaNAR b = new ArkaNAR(INSTANCE.$$("(noid,b)"), nn, cam, numeric);
                    b.ballSpeed.set(0.33f * a.ballSpeed.floatValue());

//                window(new Gridding(
//                        new Gridding(NARui.game(a), new VectorSensorView(a.cc, a).withControls()),
//                        new Gridding( NARui.game(b), new VectorSensorView(b.cc, b).withControls())), 800, 800);

                }
            });

        }
    }

    public static void main(String[] args) {

        Companion.initC(fps* 2.0F, new Consumer<NAR>() {
            @Override
            public void accept(NAR n) {

                ArkaNAR a = new ArkaNAR(n, cam, numeric);
                n.add(a);
                window(new VectorSensorChart(a.cc, a).withControls(), 800, 800);

            }
        });

    }


    public ArkaNAR(NAR nar, boolean cam, boolean numeric) {
        this(Atomic.atom("noid"),nar, cam, numeric);
    }

    public ArkaNAR(Term id, NAR n, boolean cam, boolean numeric) {
        super(id, GameTime.fps(fps), n);


        noid = new Arkanoid();


        paddleSpeed = 40.0F * noid.BALL_VELOCITY;


        initUnipolar();
        //initBipolarDirect();
        //initBipolarRelative();
        //initPushButton();

        if (cam) {

            cc = senseCamera(new IntIntToObjectFunction<Term>() {
                @Override
                public Term apply(int x, int y) {
                    return $.INSTANCE.inh(id, $.INSTANCE.p(x, y));
                }
            }, new ScaledBitmap2D(
                    new SwingBitmap2D(noid)
                    , visW, visH
            )/*.blur()*/);
            cc.resolution(0.1f);
        }


        if (numeric) {
            float numRes = 0.2f;
            float resX = 0.02f;
            AbstractSensor px = senseNumberBi($.INSTANCE.inh(id,"px"), (new FloatSupplier() {
                @Override
                public float asFloat() {
                    return noid.paddle.x / (float) noid.getWidth();
                }
            })).resolution(resX);
            px.resolution(numRes);
            AbstractSensor dx = senseNumberBi($.INSTANCE.inh(id, "dx"), (new FloatSupplier() {
                @Override
                public float asFloat() {
                    return 0.5f + 0.5f * (noid.ball.x - noid.paddle.x) / (float) noid.getWidth();
                }
            })).resolution(resX);
            dx.resolution(numRes);
            AbstractSensor cx = senseNumberBi($.INSTANCE.inh(id, $.INSTANCE.p("b", "x")), (new FloatSupplier() {
                @Override
                public float asFloat() {
                    return (noid.ball.x / (float) noid.getWidth());
                }
            })).resolution(resX);
            cx.resolution(numRes);
            float resY = 0.02f;
            AbstractSensor cy = senseNumberBi($.INSTANCE.inh(id , $.INSTANCE.p("b", "y")), (new FloatSupplier() {
                @Override
                public float asFloat() {
                    return 1f - (noid.ball.y / (float) noid.getHeight());
                }
            })).resolution(resY);
            cy.resolution(numRes);
//            window(NARui.beliefCharts(dx.components(), nar), 500, 500);

        }

        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/

        onFrame(noid::next);

        SimpleReward dontDie = (SimpleReward) reward("die", (float) 0, new FloatSupplier() {
            @Override
            public float asFloat() {
                return (float) Math.min(1, noid.die - noid.prevDie);
            }
        });
        //dontDie.addGuard(true,false);


        reward("score", new FloatSupplier() {
            @Override
            public float asFloat() {
                return (float) Math.min(1, noid.score - noid.prevScore);
            }
        });
        //s.setDefault($.t(0.5f, 0.9f));

        /*actionTriState*/


//        nar.onTask(t->{
//            if (t.isGoal()) {
//                if (!t.isInput()) {
//                    if (t.isEternal())
//                        System.err.println(t);
//                    else
//                        System.out.println(t);
//                }
//            }
////           if (t.isQuest()) {
////               nar.concepts.stream().filter(x -> x.op() == IMPL && x.sub(1).equals(t.target())).forEach(i -> {
////                   //System.out.println(i);
////                   //nar.que(i.sub(0), QUEST, t.start(), t.end());
////                   nar.want(i.sub(0), Tense.Present,1f, 0.9f);
////               });
////           }
//        },GOAL);

    }

    private void initBipolarRelative() {
        actionBipolar($.INSTANCE.the("X"), false, new FloatToFloatFunction() {
            @Override
            public float valueOf(float dx) {


                if (noid.paddle.move(dx * paddleSpeed))
                    return dx;
                else
                    return (float) 0;
            }
        });
    }

    private void initBipolarDirect() {
        actionBipolar($.INSTANCE.the("X"), true, new FloatToFloatFunction() {
            @Override
            public float valueOf(float dx) {
                noid.paddle.set(dx / 2f + 0.5f);
                return dx;
            }
        });
    }

    private void initPushButton() {
        actionPushButtonMutex($.INSTANCE.inh(id,NAct.NEG), $.INSTANCE.inh(id,NAct.POS),
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return b && noid.paddle.move(-paddleSpeed);
                    }
                },
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return b && noid.paddle.move(+paddleSpeed);
                    }
                }
        );


    }

    private void initUnipolar() {
        actionUnipolar($.INSTANCE.inh(id,NAct.NEG), true, new FloatToFloatFunction() {
                    @Override
                    public float valueOf(float prev) {
                        return (float) 0;
                    }
                },
                //u -> u > 0.5f && noid.paddle.move(-paddleSpeed * 2 * Util.sqr(2 * (u - 0.5f))) ? u : 0);
                new FloatToFloatFunction() {
                    @Override
                    public float valueOf(float u) {
                        return noid.paddle.move(-paddleSpeed * u) ? u : (float) 0;
                    }
                });
        actionUnipolar($.INSTANCE.inh(id,NAct.POS), true, new FloatToFloatFunction() {
                    @Override
                    public float valueOf(float prev) {
                        return (float) 0;
                    }
                },
                //u -> u > 0.5f && noid.paddle.move(+paddleSpeed * 2 * Util.sqr(2 * (u - 0.5f))) ? u : 0);
                new FloatToFloatFunction() {
                    @Override
                    public float valueOf(float u) {
                        return noid.paddle.move(+paddleSpeed * u) ? u : (float) 0;
                    }
                });
    }



    /**
     * https:
     */
    public class Arkanoid extends Canvas implements KeyListener {

        private int prevScore = 0;
        private int prevDie = 0;
        int score = 0;
        int die = 0;


        public static final int SCREEN_WIDTH = 250;
        public static final int SCREEN_HEIGHT = 250;

        public static final int BLOCK_LEFT_MARGIN = 4;
        public static final int BLOCK_TOP_MARGIN = 15;

        public static final float BALL_RADIUS = 15.0f;
        public float BALL_VELOCITY = 0.5f;

        public static final float PADDLE_WIDTH = 30.0f;
        public static final float PADDLE_HEIGHT = 20.0f;

        public static final float BLOCK_WIDTH = 40.0f;
        public static final float BLOCK_HEIGHT = 15.0f;

        public static final int COUNT_BLOCKS_X = 5;
        public static final int COUNT_BLOCKS_Y = 2; /* 3 */

        public static final float FT_STEP = 4.0f;


        /* GAME VARIABLES */

        public Arkanoid() {

            setSize(SCREEN_WIDTH, SCREEN_HEIGHT);

//            this.setUndecorated(false);
//            this.setResizable(false);
//
//
//            if (visible)
//                this.setVisible(true);

            paddle.x = (float) (SCREEN_WIDTH / 2);


//            setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
//
//            new Timer(1000 / FPS, (e) -> {
//                repaint();
//            }).start();

            reset();
        }

        @Override
        public void paint(Graphics g) {
//            super.paint(g);
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            ball.draw(g);
            paddle.draw(g);
            for (Brick brick : bricks) {
                brick.draw(g);
            }
        }

        public final Paddle paddle = new Paddle((float) (SCREEN_WIDTH / 2), (float) SCREEN_HEIGHT - PADDLE_HEIGHT);
        public final Ball ball = new Ball(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
        public final Collection<Brick> bricks = Collections.newSetFromMap(new ConcurrentHashMap());


        abstract class GameObject {
            abstract float left();

            abstract float right();

            abstract float top();

            abstract float bottom();
        }

        class Rectangle extends GameObject {

            public float x;
            public float y;
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
                x = Util.clamp(x + dx, sizeX, (float) SCREEN_WIDTH - sizeX);
                return !Util.equals(px, x, 1f);
            }


            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect((int) (left()), (int) (top()), (int) sizeX, (int) sizeY);
            }

            public void set(float freq) {
                x = freq * (float) SCREEN_WIDTH;
            }

            public float moveTo(float target, float paddleSpeed) {
                target = target * (float) SCREEN_WIDTH;

                if (Math.abs(target - x) <= paddleSpeed) {
                    x = target;
                } else if (target < x) {
                    x -= paddleSpeed;
                } else {
                    x += paddleSpeed;
                }

                x = Math.min(x, (float) (SCREEN_WIDTH - 1));
                x = Math.max(x, (float) 0);

                return x / (float) SCREEN_WIDTH;
            }
        }


        final AtomicInteger brickSerial = new AtomicInteger(0);

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

            public float x;
            public float y;
            float radius = BALL_RADIUS;


            public float velocityX;
            public float velocityY;

            Ball(int x, int y) {
                this.x = (float) x;
                this.y = (float) y;
                setVelocityRandom();
            }

            public void setVelocityRandom() {
                this.setVelocity(BALL_VELOCITY, (float) (Math.random() * -Math.PI * (double) (2.0F / 3f) + -Math.PI - Math.PI / 6.0));
            }

            public void setVelocity(float speed, float angle) {
                this.velocityX = (float) Math.cos((double) angle) * speed;
                this.velocityY = (float) Math.sin((double) angle) * speed;
            }

            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillOval((int) left(), (int) top(), (int) radius * 2,
                        (int) radius * 2);
            }

            void update(Paddle paddle) {
                x += velocityX * FT_STEP;
                y += velocityY * FT_STEP;

                if (left() < (float) 0)
                    velocityX = BALL_VELOCITY;
                else if (right() > (float) SCREEN_WIDTH)
                    velocityX = -BALL_VELOCITY;
                if (top() < (float) 0) {
                    velocityY = BALL_VELOCITY;
                } else if (bottom() > (float) SCREEN_HEIGHT) {
                    velocityY = -BALL_VELOCITY;
                    x = paddle.x;
                    y = paddle.y - 50.0F;
                    die++;
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


            bricks.clear();

            for (int iX = 0; iX < COUNT_BLOCKS_X; ++iX) {
                for (int iY = 0; iY < COUNT_BLOCKS_Y; ++iY) {
                    bricks.add(new Brick((float) (iX + 1) * (BLOCK_WIDTH + 3.0F) + (float) BLOCK_LEFT_MARGIN,
                            (float) (iY + 2) * (BLOCK_HEIGHT + 3.0F) + (float) BLOCK_TOP_MARGIN));
                }
            }

        }


        public void reset() {
            initializeBricks(bricks);
            ball.x = (float) (SCREEN_WIDTH / 2);
            ball.y = (float) (SCREEN_HEIGHT / 2);
            ball.setVelocityRandom();
        }


        public float next() {
            prevDie = die;
            prevScore = score;

            BALL_VELOCITY = ballSpeed.floatValue();


            ball.update(paddle);
            testCollision(paddle, ball);


            Iterator<Brick> it = bricks.iterator();
            while (it.hasNext()) {
                Brick brick = it.next();
                testCollision(brick, ball);
                if (brick.destroyed) {
                    it.remove();
                }
            }


            return (float) score;
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




































