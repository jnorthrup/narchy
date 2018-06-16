package jcog.lab;

import jcog.io.Schema;
import jcog.lab.util.ExperimentRun;

public class LabelSensor<X> extends Sensor<X,String> {
    String cur = "";

    public LabelSensor(String id) {
        super(id);
    }

    public void set(String cur) {
        this.cur = cur;
    }

    public String get() {
        return cur;
    }

    @Override
    public String apply(Object o) {
        return get();
    }

    /** convenience method */
    public jcog.lab.LabelSensor record(String value, ExperimentRun trial) {
        set(value);
        trial.record();
        return this;
    }

    @Override
    public void addToSchema(Schema data) {
        data.defineText(id);
    }
}
