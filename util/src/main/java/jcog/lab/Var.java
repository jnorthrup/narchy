package jcog.lab;

import jcog.table.DataTable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;


abstract public class Var<X,Y> implements Comparable<Var> {

    public final BiFunction<X,Y,Y> set;
    public final Function<X,Y> get;
    public final String id;

    /** last set value */
    private Y value;

    /** transduces a generic floating point value to a change in a property of the experiment subject */
    public Var(String id, Function<X,Y> get, BiFunction<X,Y,Y> set) {
        this.id = id;
        this.get = get;
        this.set = set;
    }

    @Override
    public final int compareTo(Var var) {
        return id.compareTo(var.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public String toString() {
        return id;
    }

    /** returns any unknown meta-parameters necessary for this tweak to be used */
    public List<String> unknown(Map<String,Object> hints) {
        return Collections.emptyList();
    }

    public final Y get(X example) {
        return get == null ? value : get.apply(example);
    }

    public Y set(X subject, Y value) {
        value = filter(value);
        return set.apply(subject, this.value = value);
    }

    public Y filter(Y value) {
        return value;
    }


    abstract public boolean ready();


    /** creates a (read-only) sensor proxy to the current value of this variable */
    public Sensor<X, Y> sense() {
        return new VarProxySensor<>(this);
    }

    final static class VarProxySensor<X,Y> extends Sensor<X,Y> {

        private final Var<X,Y> var;

        public VarProxySensor(Var v) {
            super(v.id);
            this.var = v;
        }

        @Override public Y apply(X x) {
            return var.get(x);
        }

        @Override
        public void addToSchema(DataTable data) {
            data.defineNumeric(id);
        }
    }

}
