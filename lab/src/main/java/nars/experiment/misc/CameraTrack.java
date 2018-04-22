//package nars.experiment.misc;
//
//import com.google.common.collect.Iterables;
//import nars.$;
//import nars.NAR;
//import nars.Param;
//import nars.learn.Agent;
//import nars.learn.ql.DQN;
//import nars.nar.Default;
//import nars.term.Term;
//import nars.term.Termed;
//import nars.time.Tense;
//import nars.util.math.FloatSupplier;
//import nars.util.math.FloatNormalized;
//import nars.util.signal.SensorConcept;
//import nars.video.SwingCamera;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
//import org.eclipse.collections.api.tuple.Twin;
//import org.eclipse.collections.impl.tuple.Tuples;
//
//import javax.swing.*;
//import java.awt.*;
//import java.util.ArrayList;
//import java.util.Map;
//
///**
// * Created by me on 7/5/16.
// */
//public class CameraTrack implements DiscreteEnvironment {
//
//
//    private final JPanel scene;
//    private final NARCamera cam;
//    private final NAR nar;
//    private final CameraDebugPane overlay;
//
//    MutableFloat rt = new MutableFloat(), gt = new MutableFloat(), bt = new MutableFloat();
//    Map<Term, SensorConcept> sensors;
//
//
//    public CameraTrack(int sw, int sh, int width, int height, NAR nar) {
//        this.nar = nar;
//        this.scene = new JPanel() {
//
//            int cycle;
//
//            float rotationspeed = 1f/1350;
//
//            @Override
//            protected void paintComponent(Graphics g) {
//                g.setColor(Color.BLACK);
//                g.fillRect(0, 0, sw, sh);
//
//                //background pattern
//                g.setColor(new Color(20,20,20));
//                int gr = 16;
//                for (int i = 0; i < sw; i+=gr) {
//                    for (int j = 0; j < sh; j+=gr) {
//                        g.fillRect(i, j, gr/2, gr/2);
//                    }
//                }
//
////                g.setColor(Color.DARK_GRAY);
////                circle(g, sw / 2, sh / 2, sw / 4);
//
//                float theta = cycle++ * rotationspeed;
//                float r = sw/3f;
//
//
////                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));    circle(g, r, theta, (int)r);
////                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));  circle(g, r, theta + 1 * Math.PI/2f, (int)r);
////                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));   circle(g, r, theta + 2 * Math.PI/2f, (int)r);
//                g.setColor(new Color(0, 0, 255+nar.random.nextInt(1))); circle(g, r, theta + 3 * Math.PI/2f, (int)r);
//            }
//
//            public void circle(Graphics g, int x, int y, int r) {
//                g.fillOval(x -r/2, y -r/2, r, r);
//            }
//
//            public void circle(Graphics g, float r, double theta, int rad) {
//                double dx = Math.cos(theta) * r;
//                double dy = Math.sin(theta) * r;
//                g.fillOval((int)(getWidth()/2 + dx - rad/2), (int)(getHeight()/2 + dy - rad/2), rad, rad);
//
//            }
//
//        };
//
//        scene.setSize(sw, sh);
//
//        JFrame win = new JFrame();
//        win.setSize(sw, sh);
//        win.setContentPane(scene);
//
//        SwingCamera swingCam = new SwingCamera(scene);
//        overlay = new CameraDebugPane(swingCam);
//
//
//        win.setVisible(true);
//        win.setResizable(false);
//
//
//
//
//
//        sensors = $.newHashMap(width*height*3);
//
//        this.cam = new NARCamera(getClass().getSimpleName(), nar, swingCam,
//                (x, y) -> $.p($.the(x), $.the(y))
//                //(x, y) -> NARCamera.quadp(0, x, y, width, height)
//        );
//        cam.input(0,0,sw,sh);
//        swingCam.output(width,height);
//        NARCamera.newWindow(swingCam);
//
//
//        //nar.logSummaryGT(System.out, 0.75f);
//
//        //Global.DEBUG = true;
//        nar.onTask(tt -> {
//            //detect eternal derivations
////            if (!tt.isInput() && tt.isEternal() && tt.isBelief())
////                System.err.println(tt.proof());
////            if (!tt.isInput() && tt.isGoal()) {
////                if (tt.term().op()==INH && tt.term().term(1).op()==Op.OPER)
////                    System.err.println(tt.proof());
////            }
//            /*if (!tt.isInput() && tt.term().hasAny(Op.VAR_DEP))
//                System.err.println(tt.proof());*/
//        });
//        //nar.logSummaryGT(System.out, 0.2f);
//
////        for (int x = 0; x < width; x++) {
////            for (int y = 0; y < height; y++) {
////                final int xx = x;
////                final int yy = y;
////                Compound t = (Compound) cam.p(x,y).term();
////
////                //addSensor(nar, sensors, t, $.the("red"), () -> thresh(cam.red(xx, yy)));
////                //addSensor(nar, sensors, t, $.the("gre"), () -> thresh(cam.green(xx, yy)));
////                addSensor(nar, sensors, t, $.the("blu"), () -> thresh(cam.blue(xx, yy)));
////            }
////        }
//
////        nar.onFrame(nn-> {
////            nar.believe((Compound)$.sim($.p($.the("frame"), $.the(""+nar.time())), cam.snapshot()), Tense.Present, 1f, 0.99f);
////        });
//
//        nar.onFrame(nn-> {
//
//
//
//            nar.believe($.prop($.the("topHalf"), $.the("blu")), Tense.Present,
//                    0.5f + 0.5f * cam.blue((x,y)->y<cam.cam.height()), 0.95f);
//            nar.believe($.prop($.the("botHalf"), $.the("blu")), Tense.Present,
//                    0.5f + 0.5f * cam.blue((x,y)->y>cam.cam.height()), 0.95f);
//            nar.believe($.prop($.the("leftHalf"), $.the("blu")), Tense.Present,
//                    0.5f + 0.5f * cam.blue((x,y)->x<cam.cam.width()), 0.95f);
//            nar.believe($.prop($.the("rightHalf"), $.the("blu")), Tense.Present,
//                    0.5f + 0.5f * cam.blue((x,y)->x>cam.cam.width()), 0.95f);
//        });
//
////        {
////            BagChart.show((Default) nar);
////        }
//
//        java.util.List<Termed> charted = new ArrayList();
//
////        charted.add($.$("[red]"));
////        charted.add($.$("[gre]"));
////        charted.add($.$("[blu]"));
////        charted.add($.$("(#x-->[red])"));
////        charted.add($.$("(#x-->[gre])"));
//
//        Iterables.addAll( charted, Iterables.concat(
////            PongEnvironment.numericSensor("reddish", "low", "high", nar,
////                    () -> (float)/*Math.sqrt*/(rt.floatValue()),
////                    0.9f).resolution(0.05f),
////            PongEnvironment.numericSensor("greenness", "low", "high", nar,
////                    () -> (float)/*Math.sqrt*/(gt.floatValue()),
////                    0.9f).resolution(0.05f),
//            Pong.numericSensor("blueness", "low", "high", nar,
//                    () -> bt.floatValue(),
//                    0.9f).resolution(0.05f)
//        ));
//        nar.goal("(blueness-->low)", Tense.Eternal, 0f, 1f);
//        nar.goal("(blueness-->high)", Tense.Eternal, 1f, 1f);
//
//        nar.onFrame(nn-> {
//            update();
//
//
//            //cam.controller.act(bt.floatValue() - (rt.floatValue() + gt.floatValue()/2f), (float[])null);
//        });
//
//
//        //charted.add($.$("(#x-->blu)"));
//        //charted.add(/*nar.goal*/$.$("(blueness-->high)")/*, Tense.Eternal, 1f, 0.99f)*/.term());
//        //charted.add(/*nar.goal*/$.$("(redness-->high)")/*, Tense.Eternal, 0f, 0.99f)*/.term());
//        //charted.add(/*nar.goal*/$.$("(greenness-->high)")/*, Tense.Eternal, 0f, 0.99f)*/.term());
//        //charted.add(nar.goal($.$("(#x-->[red])"), Tense.Eternal, 0f, 0.95f).term());
//        //charted.add(nar.goal($.$("(#x-->[green])"), Tense.Eternal, 0f, 0.95f).term());
////        charted.add(nar.ask($.$("(#x-->blu)")));
//
//        //Iterables.addAll(charted, cam.controller.rewardConcepts);
//        charted.addAll(cam.controller.actions);
//
//        Iterables.addAll(charted, Pong.numericSensor(()->cam.x, nar,0.5f,
//                "(camx --> [low])", "(camx --> [mid])", "(camx --> [high])").resolution(0.05f));
//        Iterables.addAll(charted, Pong.numericSensor(()->cam.y, nar,0.5f,
//                "(camy --> [low])", "(camy --> [mid])", "(camy --> [high])").resolution(0.05f));
//        Iterables.addAll(charted, Pong.numericSensor(()->cam.z, nar,0.5f,
//                "(camz --> [low])", "(camz --> [mid])", "(camz --> [high])").resolution(0.05f));
//
////            charted.add(nar.ask($.$("(?x-->red)").term()));
////            charted.add(nar.ask($.$("(?x<->g)").term()));
////            charted.add(nar.ask($.$("(?x<->b)").term()));
//
////        new BeliefTableChart(nar, charted)
////                .timeRadius(400)
////                .show(600, 900)
////        ;
//    }
//
//    public void update() {
//        scene.repaint();
//        overlay.repaint();
//
//        rt.setValue(0);
//        gt.setValue(0);
//        bt.setValue(0);
//        cam.update((x,y,t,r,g,b)->{
//            rt.add(r);
//            gt.add(g);
//            bt.add(b);
//        });
//
//        float r = bt.floatValue() - (rt.floatValue() + gt.floatValue() / 2f);
//        reward(r);
//
//    }
//
//    private float thresh(float v) {
//        return v > 0.5f ? 1f : 0f;
//    }
//
//    final DescriptiveStatistics rewardStat = new DescriptiveStatistics(128);
//
//    public void reward(float r) {
//        cam.controller.reward = r;
//        rewardStat.addValue(r);
//        /*if (nar.time() % 100 == 0) {
//            System.out.println("reward~= " + rewardStat.getMean());
//        }*/
//    }
//
////    public void addSensor(NAR nar, Map<Term, SensorConcept> sensors, Compound t, Term componentTerm, FloatSupplier component) {
////        Compound tr = $.instprop(t.term(), componentTerm);
////        sensors.put(tr, new SensorConcept( tr, nar,
////                component,
////                f -> {
////                    return $.t(f, 0.75f);
////                }).resolution(0.1f));//.pri(0.1f));
////    }
//
//    @Override
//    public Twin<Integer> start() {
//        return null;
//    }
//
//    @Override
//    public float pre(int t, float[] ins) {
//        return 0;
//    }
//
//    @Override
//    public void post(int t, int action, float[] ins, Agent a) {
//
//    }
//
//    public static void main(String[] args) {
//        Default nar = new Default(1024, 4, 2, 2);
//        nar.cyclesPerFrame.set(64);
//        nar.beliefConfidence(0.7f);
//        nar.goalConfidence(0.7f);
//        nar.confMin.setValue(0.3f);
//
//        //new MySTMClustered(nar, 16, '.', 4);
//
//        CameraTrack cam = new CameraTrack(256, 256, 16, 16, nar);
//
//
//        //nar.log();
//
////        nar.run(16512);
////
////        NAR.printTasks(nar,false);
////        //nar.loop(50f);
//
//        new DiscreteEnvironment() {
//
//            @Override
//            public Twin<Integer> start() {
//                return Tuples.twin(4+1+2,7);
//            }
//
//            final FloatSupplier rn = new FloatNormalized(()->
//                cam.bt.floatValue()
//            );
//
//
//            @Override
//            public float pre(int t, float[] ins) {
//                int w = cam.cam.cam.width();
//                int h = cam.cam.cam.height();
//                ins[0] = cam.cam.blue((x,y)->x<w);
//                ins[1] = cam.cam.blue((x,y)->x>w);
//                ins[2] = cam.cam.blue((x,y)->y<h);
//                ins[3] = cam.cam.blue((x,y)->y>h);
//
//                ins[4] = cam.cam.blue((x,y)-> (x >= w/4 && x<=3*w/4 && y >= h/4 && y <=3*h/4));
//
//                ins[5] = ((float)cam.cam.x) / cam.cam.cam.width();
//                ins[6] = ((float)cam.cam.y) / cam.cam.cam.height();
//
//                float r = rn.asFloat()-0.5f;
//                System.out.println(r);
//                return r;
//            }
//
//            @Override
//            public void post(int t, int action, float[] ins, Agent a) {
//                System.out.println(t + " action=" + action);
//                float s = 16;
//                switch (action) {
//                    case 0: break; //nothing
//                    case 1:  cam.cam.move(s, 0, 0); break;
//                    case 2:  cam.cam.move(-s, 0, 0); break;
//                    case 3:  cam.cam.move(0, s, 0); break;
//                    case 4:  cam.cam.move(0, -s, 0); break;
//                    case 5:  cam.cam.move(0, 0, 1); break;
//                    case 6:  cam.cam.move(0, 0, -1); break;
//                }
//                cam.update();
//            }
//
//        }.run(
//            //new HaiQAgent()
//            new DQN()
//        , 55500);
//    }
//
//    public static final class CameraDebugPane extends JPanel {
//
//
//        private final SwingCamera cam;
//
//        public CameraDebugPane(SwingCamera cam) {
//            super();
//
//            setSize(cam.inWidth(), cam.inHeight());
//
//            this.cam = cam;
//
//            //setOpaque(false);
//
//        }
//
//        @Override
//        public void paint(Graphics g) {
//            super.paint(g);
//        }
//
//        @Override
//        public void update(Graphics g) {
//            super.update(g);
//        }
//
//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//                //float reward = cam.controller.reward;
//                //g.setColor(Color.getHSBColor(cam.controller.happy.beliefs().expectation(nar.time()), 0.5f, 0.5f));
//
//                g.setColor(Color.WHITE);
//                Rectangle r = cam.input;
//                g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
//
//        }
//
//    }
//}
