package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import jcog.Util;
import jcog.exe.Exe;
import jcog.exe.Loop;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.Agent;
import jcog.learn.LivePredictor;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.attention.What;
import nars.concept.Concept;
import nars.game.Game;
import nars.game.Reward;
import nars.game.action.BiPolarAction;
import nars.game.action.GoalActionConcept;
import nars.game.sensor.DigitizedScalar;
import nars.game.util.RLBooster;
import nars.gui.NARui;
import nars.impiler.ImpilerDeduction;
import nars.op.BeliefPredict;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static nars.Op.GOAL;
import static nars.game.GameTime.fps;
import static spacegraph.SpaceGraph.window;

/**
 * adapted from:
 * http:
 * https:
 * <p>
 * see also: https:
 * https://github.com/tensorflow/tfjs-examples/blob/master/cart-pole/cart_pole.js
 */
public class PoleCart extends GameX {


	static final boolean rl = false;
	static final boolean beliefPredict = false;
	static final boolean impiler = false;
	static final boolean speedControl = true;

	private final DigitizedScalar xVel;
	private final DigitizedScalar x;
	private final AtomicBoolean drawFinished = new AtomicBoolean(true);


	static final float fps = 25.0F;
	private float speed = 1.0F;

	public static void main(String[] arg) {
        int instances = 1;
        int threadsEach = 4;
		for (int i = 0; i < instances; i++)
			Companion.runRTNet(threadsEach, fps * 2.0F, 8.0F, new Function<NAR, Game>() {
                        @Override
                        public Game apply(NAR n) {
                            PoleCart p = new PoleCart(
                                    instances > 1 ?
                                            $.INSTANCE.p(Atomic.the(PoleCart.class.getSimpleName()), n.self()) :
                                            $.INSTANCE.the(PoleCart.class.getSimpleName()), n);
                            Iterable<? extends Termed> predicting = Iterables.concat(
                                    p.angX.sensors, p.angY.sensors, p.angVel.sensors, p.xVel.sensors, p.x.sensors
                            );

                            if (beliefPredict) {
                                Exe.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        What what = p.what();
                                        what.nar = n; //HACK
                                        new BeliefPredict(
                                                predicting,
                                                8,
                                                Math.round(6.0F * n.dur()),
                                                3,
                                                new LivePredictor.LSTMPredictor(0.1f, 1),
                                                //new LivePredictor.MLPPredictor(0.01f),
                                                what
                                        );
                                    }
                                });
                            }
                            if (impiler) {

                                Loop.of(new Runnable() {
                                    @Override
                                    public void run() {

                                        //TODO
                                        //Impiler.impile(what);

                                        for (Concept a : p.actions()) {
                                            ImpilerDeduction d = new ImpilerDeduction(n);
                                            @Nullable LongToObjectFunction<Truth> dd = d.estimator(false, a.term());
                                            if (dd != null)
                                                a.meta("impiler", (Object) dd);

                                        }
                                    }
                                }).setFPS(1f);
                                n.onDur(new Runnable() {
                                    @Override
                                    public void run() {
                                        double dur = (double) (n.dur() * 3.0F);
                                        long now = n.time();
                                        for (Concept a : p.actions()) {
                                            LongToObjectFunction<Truth> dd = a.meta("impiler");
                                            if (dd != null) {
                                                for (int pp = 1; pp < 4; pp++) {
                                                    long w = Math.round((double) now + (double) (2 + pp) * dur);
                                                    Truth x = dd.apply(w);
                                                    if (x != null) {
                                                        //System.out.println(a.term() + "\t" + x);
                                                        n.want(n.priDefault(GOAL), a.term(), Math.round((double) w - dur), w, x.freq(), x.conf()); //HACK
                                                    }
                                                }
                                            }
                                        }
                                    }
                                });
                            }

                            if (rl) {

                                RLBooster bb = new RLBooster(false, p, 2, 3, new IntIntToObjectFunction<Agent>() {
                                    @Override
                                    public Agent apply(int ii, int o) {
                                        return new DQN3(ii, o, Map.of(

                                        ));
                                    }
                                }
                                );
                                bb.conf.set(0.01f);
                                window(NARui.rlbooster(bb), 500, 500);

                            }
                            n.add(p);

                            return p;
                        }
                    }
            );


	}

	private final JPanel panel;


	Dimension offDimension;
	Image offImage;
	Graphics offGraphics;


	double pos;
	double posDot;
	double angle;
	double angleDot;

	static final float posMin = -2f;
	static final float posMax = +2f;
	float velMax = 10.0F;
	boolean manualOverride;


	static final double cartMass = 1.;
	static final double poleMass = 0.1;
	static final double poleLength = 1;
	static final double gravity = 9.8;
	static final double forceMag =
            10.0;
	//100.;
	//200;
	public final FloatRange tau = new FloatRange(
		//0.007f, 0.001f, 0.02f
		0.019f, 0.001f, 0.05f
	);
	static final double fricCart = 0.00005;
	static final double fricPole =
            0.01;
	static final double totalMass = cartMass + poleMass;
	static final double halfPole = 0.5 * poleLength;
	static final double poleMassLength = poleLength * poleMass;
	static final double fourthirds = 4. / 3.;


	DigitizedScalar angX;
	DigitizedScalar angY;

	DigitizedScalar angVel;


	volatile double action;
	volatile double actionLeft;
	volatile double actionRight;

	public PoleCart(Term id, NAR nar, float tau) {
		this(id, nar);
		this.tau.set(tau);
	}

	public PoleCart(Term id, NAR nar) {
		super(id, fps(fps), nar);


		pos = 0.;
		posDot = 0.;
		angle = 0.2;
		angleDot = 0.;
		action = (double) 0;


		/**
		 returnObs.doubleArray[0] = theState.getX();
		 returnObs.doubleArray[1] = theState.getXDot();
		 returnObs.doubleArray[2] = theState.getTheta();
		 returnObs.doubleArray[3] = theState.getThetaDot();
		 */


		x = senseNumberBi($.INSTANCE.p("x", id), new FloatSupplier() {
            @Override
            public float asFloat() {
                return (float) (pos - (double) posMin) / (posMax - posMin);
            }
        });
		xVel = senseNumberBi($.INSTANCE.p(id, $.INSTANCE.the("d"), $.INSTANCE.the("x")),
			new FloatNormalized(new FloatSupplier() {
                @Override
                public float asFloat() {
                    return (float) posDot;
                }
            })
		);

		angX = senseNumberTri($.INSTANCE.p(id, $.INSTANCE.the("ang"), $.INSTANCE.the("x")),
                new FloatSupplier() {
                    @Override
                    public float asFloat() {
                        return (float) (0.5 + 0.5 * Math.sin(angle));
                    }
                });
		angY = senseNumberTri($.INSTANCE.p(id, $.INSTANCE.the("ang"), $.INSTANCE.the("y")),
                new FloatSupplier() {
                    @Override
                    public float asFloat() {
                        return (float) (0.5 + 0.5 * Math.cos(angle));
                    }
                });


		angVel = senseNumberBi($.INSTANCE.p(id, $.INSTANCE.the("d"), $.INSTANCE.the("ang")),
			new FloatNormalized(new FloatSupplier() {
                @Override
                public float asFloat() {
                    return (float) angleDot;
                }
            })
		);



		//initBipolar();
		initUnipolar();

		if (speedControl) {
			actionUnipolar($.INSTANCE.inh(id, "S"), new FloatConsumer() {
                @Override
                public void accept(float s) {
                    speed = Util.sqr(s * 2.0F);
                }
            });
		}


		panel = new JPanel(new BorderLayout()) {
			public final Stroke stroke = new BasicStroke(4.0F);

			@Override
			public void paint(Graphics g) {
				update(g);
			}

			@Override
			public void update(Graphics g) {
				action = -actionLeft + actionRight;
                Dimension d = panel.getSize();


				if (offGraphics == null
					|| d.width != offDimension.width
					|| d.height != offDimension.height) {
					offDimension = d;
					offImage = panel.createImage(d.width, d.height);
					offGraphics = offImage.getGraphics();
				}


                float clearRate = 0.5f;
				offGraphics.setColor(new Color((float) 0, (float) 0, (float) 0, clearRate));
				offGraphics.fillRect(0, 0, d.width, d.height);


				double[] xs = {-2.5, 2.5, 2.5, 2.3, 2.3, -2.3, -2.3, -2.5};
				double[] ys = {-0.4, -0.4, 0., 0., -0.2, -0.2, (double) 0, (double) 0};
                int[] pixxs = new int[8];
                int[] pixys = new int[8];
				for (int i = 0; i < 8; i++) {
					pixxs[i] = pixX(d, xs[i]);
					pixys[i] = pixY(d, ys[i]);
				}
                Color trackColor = Color.GRAY;
				offGraphics.setColor(trackColor);
				offGraphics.fillPolygon(pixxs, pixys, 8);


                Color cartColor = Color.ORANGE;
				offGraphics.setColor(cartColor);
				offGraphics.fillRect(pixX(d, pos - 0.2), pixY(d, (double) 0), pixDX(d, 0.4), pixDY(d, -0.2));

				((Graphics2D) offGraphics).setStroke(stroke);


				offGraphics.drawLine(pixX(d, pos), pixY(d, (double) 0),
					pixX(d, pos + Math.sin(angle) * poleLength),
					pixY(d, poleLength * Math.cos(angle)));


				if (action != (double) 0) {
                    int signAction = action > (double) 0 ? 1 : action < (double) 0 ? -1 : 0;
                    int tipx = pixX(d, pos + 0.2 *
						//signAction
						action
					);
                    Color arrowColor = new Color(0.25f, 0.5f + Util.tanhFast(Math.abs((float) action)) / 2f, 0.25f);
                    int tipy = pixY(d, -0.1);
					offGraphics.setColor(arrowColor);
					offGraphics.drawLine(pixX(d, pos), pixY(d, -0.1), tipx, tipy);
					offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy + 4);
					offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy - 4);
				}


				g.drawImage(offImage, 0, 0, panel);

				drawFinished.set(true);
			}

		};


        JFrame f = new JFrame();
		f.setContentPane(panel);
		f.setSize(800, 300);
		f.setVisible(true);

		f.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if ((int) e.getKeyChar() == (int) 'o') {
					manualOverride = !manualOverride;
					System.out.println("manualOverride=" + manualOverride);
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					actionLeft = (double) +1;
					actionRight = (double) 0;
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					actionRight = (double) +1;
					actionLeft = (double) 0;
				} else if ((int) e.getKeyChar() == (int) ' ') {
					actionLeft = actionRight = (double) 0;

				}
			}
		});


        Reward r = rewardNormalized("balanced", -1.0F, (float) +1, this::update);

		Exe.runLater(new Runnable() {
                         @Override
                         public void run() {
                             window(NARui.beliefCharts(nar, sensors.stream()
                                     .flatMap(s -> Streams.stream(s.components()))
                                     .collect(toList())), 900, 900);
                         }
                     }
		);
	}

	public void initBipolar() {
		final float SPEED = 1f;
        BiPolarAction F = actionBipolarFrequencyDifferential(id, false, new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                float a =
                        x * SPEED;
                //(x * x * x) * SPEED;
                actionLeft = (double) (a < (float) 0 ? -a : (float) 0);
                actionRight = (double) (a > (float) 0 ? a : (float) 0);
                return x;
            }
        });
	}

	float power(float a) {
		return a * speed;
		//return Util.sqrt(a);
	}

	public void initUnipolar() {
        GoalActionConcept L = actionUnipolar(
			//$.funcImg("mx", id, $.the(-1))
			$.INSTANCE.inh(id, "L"), new FloatToFloatFunction() {
                    @Override
                    public float valueOf(float a) {
                        if (!manualOverride) {
                            actionLeft = (double) (a > 0.5f ? PoleCart.this.power((a - 0.5f) * 2.0F) : (float) 0);
                            //action = Util.clampBi((float) (action + a * a));
                        }
                        return a > 0.5f ? a : (float) 0;
                    }
                });
        GoalActionConcept R = actionUnipolar(
			//$.funcImg("mx", id, $.the(+1))
			$.INSTANCE.inh(id, "R"), new FloatToFloatFunction() {
                    @Override
                    public float valueOf(float a) {
                        if (!manualOverride) {
                            actionRight = (double) (a > 0.5f ? PoleCart.this.power((a - 0.5f) * 2.0F) : (float) 0);
                            //action = Util.clampBi((float) (action - a * a));
                        }
                        return a > 0.5f ? a : (float) 0;
                    }
                });

	}


	protected float update() {


        double force = forceMag * action;

        double sinangle = Math.sin(angle);
        double cosangle = Math.cos(angle);
        double angleDotSq = angleDot * angleDot;
        double common = (force + poleMassLength * angleDotSq * sinangle
			- fricCart * (double) (posDot < (double) 0 ? -1 : 0)) / totalMass;

        double angleDDot = (gravity * sinangle - cosangle * common
			- fricPole * angleDot / poleMassLength) /
			(halfPole * (fourthirds - poleMass * cosangle * cosangle /
				totalMass));


        float tau = this.tau.floatValue();
		pos += posDot * (double) tau;


		if (pos >= (double) posMax || pos <= (double) posMin) {

			pos = (double) Util.clamp((float) pos, posMin, posMax);


			posDot = -1 /* restitution */ * posDot;


		}

        double posDDot = common - poleMassLength * angleDDot * cosangle /
				totalMass;
		posDot += posDDot * (double) tau;
		posDot = Math.min((double) +velMax, Math.max((double) -velMax, posDot));

		angle += angleDot * (double) tau;
		angleDot += angleDDot * (double) tau;

		/**TODO
		 **/


		if (drawFinished.compareAndSet(true, false))
			SwingUtilities.invokeLater(panel::repaint);


        float rewardLinear = (float) Math.cos(angle);
		return rewardLinear;
		//return rewardLinear * rewardLinear * rewardLinear;


	}


	static int pixX(Dimension d, double v) {
		return (int) Math.round((v + 2.5) / 5.0 * (double) d.width);
	}


	static int pixDX(Dimension d, double v) {
		return (int) Math.round(v / 5.0 * (double) d.width);
	}

	static int pixY(Dimension d, double v) {
		return (int) Math.round((double) d.height - (v + 0.5) / 2.0 * (double) d.height);
	}

	public static int pixDY(Dimension d, double v) {
		return (int) Math.round(-v / 2.0 * (double) d.height);
	}

	void resetPole() {
		pos = 0.;
		posDot = 0.;
		angle = 0.;
		angleDot = 0.;
	}


}