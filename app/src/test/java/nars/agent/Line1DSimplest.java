package nars.agent;

import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.concept.sensor.Signal;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends NAgent {

    

    /**
     * the target value
     */
    public final FloatRange i = new FloatRange(0.5f, 0, 1f);
    public final FloatRange o = new FloatRange(0.5f, 0, 1f);

    public final FloatRange speed = new FloatRange(0.25f, 0f, 0.5f);




    /**
     * the current value
     */

    public final Signal in;


    public Line1DSimplest() {
        super( );


        in = senseNumber(
                $.the("x"),
                
                this.i
        );
        senseNumber(
                $.the("y"),
                
                this.o
        );

        
        
        initBipolar();
























































    }

    private void initDualToggle() {
        actionToggle($.$$("y:up"), (b) -> {
            if(b) {
                o.set(Util.unitize(o.floatValue() + speed.floatValue()));
                System.out.println(o);
            }
        });
        actionToggle($.$$("y:down"), (b) -> {
            if (b) {
                o.set(Util.unitize(o.floatValue() - speed.floatValue()));
                System.out.println(o);
            }
        });
    }


    private void initDualUnipolar() {
        float[] x = new float[2];
        onFrame(() -> {
            float d = x[0] - x[1];
            this.o.set(Util.unitize(o.floatValue() + d * speed.floatValue()));
        });
        actionUnipolar($.p("up"), d -> {
            
            synchronized (o) {
                
                
                






                    
                return x[0] = d;



            }
        });

        actionUnipolar($.p("down"), d -> {
            
            synchronized (o) {
                
                
                
                
                    




                return x[1] = d;



            }
        });



    }

    public void initBipolar() {
        actionBipolar($.the("y"), v -> {
            if (v == v) {
                o.set(
                        Util.unitize(o.floatValue() + v * speed.floatValue())
                );



            }

            return v;

            

            






            
            
        });
    }

    @Override
    protected float act() {
        float dist = Math.abs(
                i.floatValue() -
                        o.floatValue()
        );

        

        float h = ((1f - dist)-0.5f)*2f;
        
        return h;
        
        






        
        
    }


    public float target() {
        return i.asFloat();
    }

    public void target(float v) {
        i.set(v);
    }
}
