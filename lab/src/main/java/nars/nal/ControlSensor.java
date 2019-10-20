package nars.nal;


public abstract class ControlSensor {

    public final NumericRange range;
    public final int quantization;

    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public ControlSensor(int quantization) {
        range = new NumericRange();
        this.quantization = quantization;
    }

    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public ControlSensor(double min, double max, int quantization) {
        range = new NumericRange((min + max) / 2.0, (max - min) / 2.0);
        this.quantization = quantization;
    }
    
    
    /** returns next index */

    
    public void update() { }

    
    public abstract double get();

    /** returns next index */
    public int vectorize(double[] d, int index) {
        range.vectorizeSmooth(d, index, get(), quantization);
        return quantization;
    }

    public void adaptContrast(double rate, double center) {
        range.adaptiveContrast(rate, center);
    }
}
