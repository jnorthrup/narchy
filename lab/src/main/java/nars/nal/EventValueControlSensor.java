package nars.nal;

import jcog.signal.meter.FunctionMeter;
import jcog.signal.meter.event.DoubleMeter;
import nars.util.TimeAware;


public class EventValueControlSensor extends ControlSensor {

    DoubleMeter e;
    final TimeAware timeAware;
    final FunctionMeter<? extends Number> logicSensor;
    final double adaptContrast;

    /*default value if not exist */
    public EventValueControlSensor(TimeAware n, FunctionMeter logicSensor, int quantization, int sampleWindow, double adaptContrast) {
        super(quantization);
        e = new DoubleMeter("_");
        timeAware = n;
        this.logicSensor = logicSensor;
        this.adaptContrast = adaptContrast;
    }
    public EventValueControlSensor(TimeAware n, DoubleMeter signal, int min, int max, int quantization, int sampleWindow) {
        super(min, max, quantization);
        e = new DoubleMeter("_");
        timeAware = n;
        logicSensor = signal;
        adaptContrast = 0;
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
