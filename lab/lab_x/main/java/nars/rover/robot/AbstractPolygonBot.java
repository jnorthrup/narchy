package nars.rover.robot;

import nars.NAR;
import nars.Symbols;
import nars.concept.Concept;
import nars.nal.nal7.Tense;
import nars.rl.gng.NeuralGasNet;
import nars.rover.Material;
import nars.rover.Sim;
import nars.task.TaskSeed;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DefaultTruth;
import nars.util.event.FrameReaction;
import spacegraph.space2d.phys.callbacks.DebugDraw;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Created by me on 8/3/15.
 */
public abstract class AbstractPolygonBot extends Robotic {

    public AbstractPolygonBot(String id, NAR nar) {
        super(id);
        this.nar = nar;
    }

    public final NAR nar;
    final Deque<Vec2> positions = new ArrayDeque();
    final List<Sense> senses = new ArrayList();
    public float linearThrustPerCycle = 8f;
    public float angularSpeedPerCycle = 0.44f * 0.25f;
    int mission = 0;
    
    int motionPeriod = 3;
    public Vec2 point1 = new Vec2();
    public Vec2 point2 = new Vec2();
    public Vec2 d = new Vec2();
    boolean feel_motion = true; 
    protected void thrustRelative(float f) {
        if (f == 0) {
            torso.setLinearVelocity(new Vec2());
        } else {
            thrust(0, f * linearThrustPerCycle);
        }
    }

    protected void rotateRelative(float f) {
        rotate(f * angularSpeedPerCycle);
    }

    protected void curious(float freq, float conf) {
        nar.input("motor(random)! %" + freq + "|" + conf + "%");
    }

    protected void addAxioms() {

        
        if (nar.time() < 50) {
            curious(0.9f, 0.7f);
        }
        else {
            curious(0.75f, 0.05f);
        }



        
        
        
        

        

        /*
        for (int i = 0; i < 2; i++) {
            String x = "lessThan(" + XORShiftRandom.global.nextInt(10) + "," +
                    XORShiftRandom.global.nextInt(10) + ")?";

            nar.input(x);
        }
        */












    }

    public void inputMission() {

        addAxioms();

        nar.goal("<goal --> [health]>", 1.00f, 0.90f);

        nar.believe("<goal --> [health]>", Tense.Present, 0.50f, 0.99f); 

        try {
            if (mission == 0) {
                
                

                
                nar.input("<goal --> [food]>! :|:");
                nar.goal("<goal --> [food]>. :|:", 0.50f, 0.99f); 


                
                
                
                
                
                
            } else if (mission == 1) {
                
                
                nar.input("moved(0)! %1.00;0.9%");
                nar.input("<goal --> [food]>! %0.00;0.9%");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public void taste(Body eatable, float distance) {





    }

    public void eat(Body eaten) {
        Material m = (Material)eaten.getUserData();
        if (m instanceof Sim.FoodMaterial) {
            nar.input("<goal --> [food]>. :|: %0.90;0.90%");
            nar.input("<goal --> [health]>. :|: %0.90;0.90%");
        }
        else if (m instanceof Sim.PoisonMaterial) {
            nar.input("<goal --> [food]>. :|: %0.00;0.90%");
            nar.input("<goal --> [health]>. :|: %0.00;0.90%");
        }
        else {
            return;
        }

        @Deprecated int sz = 48;
        float x = (float) Math.random() * sz - sz / 2f;
        float y = (float) Math.random() * sz - sz / 2f;
        
        eaten.setTransform(new Vec2(x * 2.0f, y * 2.0f), eaten.getAngle());
    }

    public DebugDraw getDraw() {
        return draw;
    }


    public void step(int time) {
        if (sim.cnt % sim.missionPeriod == 0) {
            inputMission();
        }

        for (Sense v : senses) {
            v.step(true, true);
        }
        /*if(cnt>=do_sth_importance) {
        cnt=0;
        do_sth_importance+=decrease_of_importance_step; 
        nar.addInput("(^motor,random)!");
        }*/
        if (feel_motion && sim.cnt % motionPeriod == 0) {
            feelMotion();
        }
        /*if (Math.random() < curiosity) {
            randomAction();
        }*/

        nar.frame();


    }

    public void thrust(float angle, float force) {
        angle += torso.getAngle();
        
        Vec2 v = new Vec2((float) Math.cos(angle) * force, (float) Math.sin(angle) * force);
        torso.setLinearVelocity(v);
        
    }

    public void rotate(float v) {
        torso.setAngularVelocity(v);
        
        
    }

    protected abstract void feelMotion();

    public void stop() {
        torso.setAngularVelocity(0);
        torso.setLinearVelocity(new Vec2());
    }

    @FunctionalInterface
    public interface ConceptDesire {
        public float getDesire(Concept c);
    }


    /** maps a scalar changing quality to a frequency value, with autoranging
     *  determined by a history window of readings
     * */
    public static class AutoRangeTruthFrequency {
        final NeuralGasNet net;
        float threshold;

        public AutoRangeTruthFrequency(float thresho) {
            net = new NeuralGasNet(1,4);
            this.threshold = thresho;
        }

        /** returns estimated frequency */
        public float observe(float value) {
            if (Math.abs(value) < threshold ) {
                return 0.5f;
            }

            learn(value);

            double[] range = net.getDimensionRange(0);
            return (float) getFrequency(range, value);
        }

        protected double getFrequency(double[] range, float value) {
            double proportional;

            if ((range[0] == range[1]) || (!Double.isFinite(range[0])))
                proportional = 0.5;
            else
                proportional = (value - range[0]) / (range[1] - range[0]);

            if (proportional > 1f) proportional = 1f;
            if (proportional < 0f) proportional = 0f;

            return proportional;
        }

        protected void learn(float value) {
            net.learn(value);
        }


    }

    public static class BipolarAutoRangeTruthFrequency extends AutoRangeTruthFrequency {

        public BipolarAutoRangeTruthFrequency() {
            this(0);
        }

        public BipolarAutoRangeTruthFrequency(float thresh) {
            super(thresh);
        }

        @Override protected void learn(float value) {
            
            super.learn(Math.abs(value));
            super.learn(0);
        }

        protected double getFrequency(double[] range, float value) {
            double proportional;

            if ((0 == range[1]) || (!Double.isFinite(range[0])))
                proportional = 0.5;
            else
                proportional = ((value) / (range[1]))/2f + 0.5f;

            

            if (proportional > 1f) proportional = 1f;
            if (proportional < 0f) proportional = 0f;

            return proportional;
        }
    }

    public static class SimpleAutoRangeTruthFrequency  {
        private final Compound term;
        private final NAR nar;
        private final AutoRangeTruthFrequency model;

        public SimpleAutoRangeTruthFrequency(NAR nar, Compound term, AutoRangeTruthFrequency model) {
            super();
            this.term = term;
            this.nar = nar;
            this.model = model;

            model.net.setEpsW(0.04f);
            model.net.setEpsN(0.01f);
        }

        public void observe(float value) {
            float freq = model.observe(value);
            
            

            float conf = 0.75f;

            TaskSeed ts = nar.task(term).belief().present().truth(freq, conf);
            Task t = ts.normalized();
            if (t!=null)
                nar.input(t);

            
        }
    }

    public abstract static class CycleDesire extends FrameReaction {

        private final ConceptDesire desireFunction;
        private final Term term;
        private final NAR nar;
        transient private Concept concept;
        boolean feedbackEnabled = true;
        float threshold = 0f;

        /** budget to apply if the concept is not active */
        private Budget remember = new Budget(0.5f, Symbols.GOAL, new DefaultTruth(1.0f, 0.9f));

        public CycleDesire(String term, ConceptDesire desireFunction, NAR nar) {
            this(desireFunction, (Term)nar.term(term), nar);
        }

        public CycleDesire(ConceptDesire desireFunction, Term term, NAR nar) {
            super(nar);
            this.desireFunction = desireFunction;
            this.nar = nar;
            this.term = term;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + concept + "]";
        }

        public void setFeedback(boolean feedback) {
            this.feedbackEnabled = feedback;
        }

        public float getDesireIfConceptMissing() { return 0; }

        /** @return feedback belief value, or Float.NaN to not apply it */
        abstract float onFrame(float desire);

        @Override
        public void onFrame() {

            Concept c = getConcept();

            if (c != null) {
                float d = desireFunction.getDesire(c);

                if (d > threshold) {
                    float feedback = onFrame(d);

                    if (feedbackEnabled && Float.isFinite(feedback))
                        nar.input(getFeedback(feedback));
                }
            }
            else {
                onFrame(getDesireIfConceptMissing());
            }

        }

        public Task getFeedback(float feedback) {
            
            return nar.task((Compound) term).present().belief().truth(feedback, 0.99f).normalized();
        }

        public Concept getConcept() {

            if (concept == null) {
                concept = nar.concept(term);
            }

            
            if (concept == null) {
                concept = nar.memory.conceptualize(term, remember);
            }

            return concept;
        }
    }

    /** bipolar cycle desire, which resolves two polar opposite desires into one */
    public abstract static class BiCycleDesire  {

        private final CycleDesire positive;
        private final CycleDesire negative;

        public float positiveDesire, negativeDesire;
        float threshold = 0f;
        private NAR nar;

        public BiCycleDesire(String positiveTerm, String negativeTerm, ConceptDesire desireFunction, NAR n) {
            this.nar = n;
            this.positive = new CycleDesire(positiveTerm, desireFunction, n) {

                @Override
                float onFrame(final float desire) {
                    positiveDesire = desire;
                    return Float.NaN;
                }
            };
            
            this.negative = new CycleDesire(negativeTerm, desireFunction, n) {

                @Override
                float onFrame(float negativeDesire) {
                    Rover.BiCycleDesire.this.negativeDesire = negativeDesire;

                    frame(positiveDesire, negativeDesire);


                    return Float.NaN;
                }

            };
        }

        protected void frame(final float positiveDesire, final float negativeDesire) {

            float net = positiveDesire - negativeDesire;
            boolean isPos = (net > 0);
            if (!isPos)
                net = -net;

            float posFeedback = 0, negFeedback = 0;
            if (net > threshold) {
                final float feedback = onFrame(net, isPos);
                if (Float.isFinite(feedback)) {
                    if (isPos) {
                        posFeedback = feedback;
                        nar.input(this.positive.getFeedback(posFeedback));
                        nar.input(this.negative.getFeedback(0));











                    } else {
                        negFeedback = feedback;
                        nar.input(this.negative.getFeedback(negFeedback));
                        nar.input(this.positive.getFeedback(0));












                    }
                }

            }

        }

        abstract float onFrame(float desire, boolean positive);

    }

    public interface Sense {

        void step(boolean input, boolean draw);

    }


}
