package nars.nal;

import jcog.signal.meter.FunctionMeter;
import jcog.signal.meter.event.DoubleMeter;
import nars.util.Timed;


public class EventValueControlSensor extends ControlSensor {

    DoubleMeter e;
    final Timed timed;
    final FunctionMeter<? extends Number> logicSensor;
    final double adaptContrast;

    /*default value if not exist */
    public EventValueControlSensor(Timed n, FunctionMeter logicSensor, int quantization, int sampleWindow, double adaptContrast) {
        super(quantization);
        e = new DoubleMeter("_");
        timed = n;
        this.logicSensor = logicSensor;
        this.adaptContrast = adaptContrast;
    }
    public EventValueControlSensor(Timed n, DoubleMeter signal, int min, int max, int quantization, int sampleWindow) {
        super((double) min, (double) max, quantization);
        e = new DoubleMeter("_");
        timed = n;
        logicSensor = signal;
        adaptContrast = (double) 0;
    }

    @Override
    public void update() {
        e.set( logicSensor.getValue(null, 0).doubleValue() );
    }

    @Override
    public double get() {
        return e.get();
        



        
    }
}
