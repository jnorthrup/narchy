package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import jcog.Util;
import jcog.exe.Exe;
import jcog.exe.Loop;
import jcog.learn.LivePredictor;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.attention.What;
import nars.concept.Concept;
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
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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


	static final float fps = 25;
	private float speed = 1;

	public static void main(String[] arg) {
		//polecart(-1);

		//int instances = 2; int threadsEach = 1;
		int instances = 1;
		int threadsEach = 4;
		for (int i = 0; i < instances; i++)
			runRTNet((n) -> {
					PoleCart p = new PoleCart(
						instances > 1 ?
							$.p(Atomic.the(PoleCart.class.getSimpleName()), n.self()) :
							$.the(PoleCart.class.getSimpleName()), n);
					Iterable<? extends Termed> predicting = Iterables.concat(
						p.angX.sensors, p.angY.sensors, p.angVel.sensors, p.xVel.sensors, p.x.sensors
					);
					What what = p.what();

					if (beliefPredict) {
						new BeliefPredict(
							predicting,
//                java.util.List.of(
//                        x, xVel,
//                        angVel, angX, angY),
							8,
							Math.round(6 * n.dur()),
							3,
							new LivePredictor.LSTMPredictor(0.1f, 1),
							//new LivePredictor.MLPPredictor(0.01f),
							what
						);
					}
					if (impiler) {

						Loop.of(() -> {

							//TODO
							//Impiler.impile(what);

							for (Concept a : p.actions()) {
								ImpilerDeduction d = new ImpilerDeduction(n);
								@Nullable LongToObjectFunction<Truth> dd = d.estimator(a.term(), false);
								if (dd != null)
									a.meta("impiler", (Object) dd);
//							d.get(a.term(), n.time(), false).forEach(t -> {
//								System.out.println(t);
//								what.accept(t);
//							});
							}
						}).setFPS(1f);
						n.onDur(() -> {
							double dur = n.dur() * 3;
							long now = n.time();
							for (Concept a : p.actions()) {
								LongToObjectFunction<Truth> dd = a.meta("impiler");
								if (dd != null) {
									for (int pp = 1; pp < 4; pp++) {
										long w = Math.round(now + (2 + pp) * dur);
										Truth x = dd.apply(w);
										if (x != null) {
											//System.out.println(a.term() + "\t" + x);
											n.want(n.priDefault(GOAL), a.term(), Math.round(w - dur), w, x.freq(), x.conf()); //HACK
										}
									}
								}
							}
						});
					}

					if (rl) {

						RLBooster bb = new RLBooster(p, (ii, o) ->
							//new HaiQae(i, 12, o).alpha(0.01f).gamma(0.9f).lambda(0.9f),
							new DQN3(ii, o, Map.of(

							)),
							2, 3, false);
						bb.conf.set(0.01f);
						window(NARui.rlbooster(bb), 500, 500);

					}
					n.start(p);

					return p;
				},
				threadsEach, fps * 2, 8);


	}


//    public static class RL {
//        public static void main(String[] args) {
//            runRL(n -> {
//
//                try {
//                    PoleCart p = new PoleCart($.the("rl"), n);
//
//                    p.tau.set(0.004f);
//
//                    return p;
//                } catch (Exception e) {
//
//                    e.printStackTrace();
//                    return null;
//                }
//            }, fps, fps);
//
//        }
//    }

	private final JPanel panel;


	Dimension offDimension;
	Image offImage;
	Graphics offGraphics;


	double pos, posDot, angle, angleDot;

	final float posMin = -2f, posMax = +2f;
	float velMax = 10;
	boolean manualOverride;


	static final double cartMass = 1.;
	static final double poleMass = 0.1;
	static final double poleLength = 1f;
	static final double gravity = 9.8;
	static final double forceMag =
		10;
	//100.;
	//200;
	public final FloatRange tau = new FloatRange(
		//0.007f, 0.001f, 0.02f
		0.019f, 0.001f, 0.05f
	);
	//0.01;
	//0.005;
	//0.0025f;
	static final double fricCart = 0.00005;
	static final double fricPole =
		0.01f;
	static final double totalMass = cartMass + poleMass;
	static final double halfPole = 0.5 * poleLength;
	static final double poleMassLength = poleLength * poleMass;
	static final double fourthirds = 4. / 3.;


	DigitizedScalar angX;
	DigitizedScalar angY;

	DigitizedScalar angVel;


	volatile double action, actionLeft, actionRight;

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
		action = 0;


		/**
		 returnObs.doubleArray[0] = theState.getX();
		 returnObs.doubleArray[1] = theState.getXDot();
		 returnObs.doubleArray[2] = theState.getTheta();
		 returnObs.doubleArray[3] = theState.getThetaDot();
		 */


		this.x = senseNumberBi($.p("x", id), () -> ((float) (pos - posMin) / (posMax - posMin)));
		this.xVel = senseNumberBi($.p(id, $.the("d"), $.the("x")),
			new FloatNormalized(() -> (float) posDot)
		);


//        angX.resolution(0.02f);
//        angY.resolution(0.02f);
		this.angX = senseNumberBi($.p(id, $.the("ang"), $.the("x")),
			() -> (float) (0.5f + 0.5f * (Math.sin(angle))));
		this.angY = senseNumberBi($.p(id, $.the("ang"), $.the("y")),
			() -> (float) (0.5f + 0.5f * (Math.cos(angle))));


		this.angVel = senseNumberBi($.p(id, $.the("d"), $.the("ang")),
			new FloatNormalized(() -> (float) angleDot)
		);



		//initBipolar();
		initUnipolar();

		if (speedControl) {
			actionUnipolar($.inh(id, "S"), (s) -> {
				speed = Util.sqr(s * 2);
			});
		}


		this.panel = new JPanel(new BorderLayout()) {
			public final Stroke stroke = new BasicStroke(4);

			@Override
			public void paint(Graphics g) {
				update(g);
			}

			@Override
			public void update(Graphics g) {
				action = -actionLeft + actionRight;
				Dimension d = panel.getSize();
				Color cartColor = Color.ORANGE;
				Color trackColor = Color.GRAY;


				if ((offGraphics == null)
					|| (d.width != offDimension.width)
					|| (d.height != offDimension.height)) {
					offDimension = d;
					offImage = panel.createImage(d.width, d.height);
					offGraphics = offImage.getGraphics();
				}


				float clearRate = 0.5f;
				offGraphics.setColor(new Color(0, 0, 0, clearRate));
				offGraphics.fillRect(0, 0, d.width, d.height);


				double[] xs = {-2.5, 2.5, 2.5, 2.3, 2.3, -2.3, -2.3, -2.5};
				double[] ys = {-0.4, -0.4, 0., 0., -0.2, -0.2, 0, 0};
				int[] pixxs = new int[8];
				int[] pixys = new int[8];
				for (int i = 0; i < 8; i++) {
					pixxs[i] = pixX(d, xs[i]);
					pixys[i] = pixY(d, ys[i]);
				}
				offGraphics.setColor(trackColor);
				offGraphics.fillPolygon(pixxs, pixys, 8);


//                String msg = "Position = " + n2(pos) + " Angle = " + n2(angle) + " angleDot = " + n2(angleDot);
//                offGraphics.drawString(msg, 20, d.height - 20);


				offGraphics.setColor(cartColor);
				offGraphics.fillRect(pixX(d, pos - 0.2), pixY(d, 0), pixDX(d, 0.4), pixDY(d, -0.2));

				((Graphics2D) offGraphics).setStroke(stroke);


				offGraphics.drawLine(pixX(d, pos), pixY(d, 0),
					pixX(d, pos + Math.sin(angle) * poleLength),
					pixY(d, poleLength * Math.cos(angle)));


				if (action != 0) {
					int signAction = (action > 0 ? 1 : (action < 0) ? -1 : 0);
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
				if (e.getKeyChar() == 'o') {
					manualOverride = !manualOverride;
					System.out.println("manualOverride=" + manualOverride);
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					actionLeft = +1;
					actionRight = 0;
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					actionRight = +1;
					actionLeft = 0;
				} else if (e.getKeyChar() == ' ') {
					actionLeft = actionRight = 0;

				}
			}
		});


		Reward r = rewardNormalized("balanced", -1, +1, this::update);

		//new RewardBooster(r);


//        window(NARui.beliefCharts(predicting, nar)/*x, xVel, angVel, angX, angY)*/, 700, 700);


		Exe.runLater(() ->
			window(NARui.beliefCharts(nar, this.sensors.stream()
				.flatMap(s -> Streams.stream(s.components()))
				.collect(toList())), 900, 900)
		);
	}

	public void initBipolar() {
		final float SPEED = 1f;
		BiPolarAction F = actionBipolarFrequencyDifferential(id, false, (x) -> {
			float a =
				x * SPEED;
			//(x * x * x) * SPEED;
			this.actionLeft = a < 0 ? -a : 0;
			this.actionRight = a > 0 ? a : 0;
			return x;
		});
	}

	float power(float a) {
		return a * speed;
		//return Util.sqrt(a);
	}

	public void initUnipolar() {
		GoalActionConcept L = actionUnipolar(
			//$.funcImg("mx", id, $.the(-1))
			$.inh(id, "L"), (a) -> {
				if (!manualOverride) {
					actionLeft = a > 0.5f ? power((a - 0.5f) * 2) : 0;
					//action = Util.clampBi((float) (action + a * a));
				}
				return a > 0.5f ? a : 0;
			});
		GoalActionConcept R = actionUnipolar(
			//$.funcImg("mx", id, $.the(+1))
			$.inh(id, "R"), (a) -> {
				if (!manualOverride) {
					actionRight = a > 0.5f ? power((a - 0.5f) * 2) : 0;
					//action = Util.clampBi((float) (action - a * a));
				}
				return a > 0.5f ? a : 0;
			});

		//curiosity.enable.set(false);
//		curiosity.goal.set(false);
//		L.goalDefault($.t(0.5f, 0.1f), nar);
//		R.goalDefault($.t(0.5f, 0.1f), nar);
	}


	protected float update() {


		double force = forceMag * action;

		double sinangle = Math.sin(angle);
		double cosangle = Math.cos(angle);
		double angleDotSq = angleDot * angleDot;
		double common = (force + poleMassLength * angleDotSq * sinangle
			- fricCart * (posDot < 0 ? -1 : 0)) / totalMass;

		double angleDDot = (gravity * sinangle - cosangle * common
			- fricPole * angleDot / poleMassLength) /
			(halfPole * (fourthirds - poleMass * cosangle * cosangle /
				totalMass));
		double posDDot = common - poleMassLength * angleDDot * cosangle /
			totalMass;


		float tau = this.tau.floatValue();
		pos += posDot * tau;


		if ((pos >= posMax) || (pos <= posMin)) {

			pos = Util.clamp((float) pos, posMin, posMax);


			posDot = -1f /* restitution */ * posDot;


		}

		posDot += posDDot * tau;
		posDot = Math.min(+velMax, Math.max(-velMax, posDot));

		angle += angleDot * tau;
		angleDot += angleDDot * tau;

		/**TODO
		 **/


		if (drawFinished.compareAndSet(true, false))
			SwingUtilities.invokeLater(panel::repaint);


		float rewardLinear = (float) (Math.cos(angle));
		return rewardLinear;
		//return rewardLinear * rewardLinear * rewardLinear;


	}


	int pixX(Dimension d, double v) {
		return (int) Math.round((v + 2.5) / 5.0 * d.width);
	}


	int pixDX(Dimension d, double v) {
		return (int) Math.round(v / 5.0 * d.width);
	}

	int pixY(Dimension d, double v) {
		return (int) Math.round(d.height - (v + 0.5f) / 2.0 * d.height);
	}

	public int pixDY(Dimension d, double v) {
		return (int) Math.round(-v / 2.0 * d.height);
	}

	void resetPole() {
		pos = 0.;
		posDot = 0.;
		angle = 0.;
		angleDot = 0.;
	}


}