package jcog.learn.ntm.control;

import java.util.function.Consumer;

public class Unit {
    public double value;
    public double grad;

    public Unit() {

    }

    public Unit(double value) {
        this.value = value;
        this.grad = (double) 0;
    }

    public String toString() {
        return "<" + value + ',' + grad + '>';
    }

    private static Consumer<Unit[]> vectorUpdateAction(Consumer<Unit> updateAction) {
        return (units) -> {
            for (Unit unit : units)
                updateAction.accept(unit);
        };
    }

    static Consumer<Unit[][]> tensor2UpdateAction(Consumer<Unit> updateAction) {
        Consumer<Unit[]> vectorUpdateAction = vectorUpdateAction(updateAction);
        return (units) -> {
            for (Unit[] unit : units)
                vectorUpdateAction.accept(unit);
        };
    }

    static Consumer<Unit[][][]> tensor3UpdateAction(Consumer<Unit> updateAction) {
        Consumer<Unit[][]> tensor2UpdateAction = tensor2UpdateAction(updateAction);
        return (units) -> {
            for (Unit[][] unit : units)
                tensor2UpdateAction.accept(unit);
        };
    }

    public final void setDelta(double target) {
        grad = value - target;
    }

    public double getValue() {
        return value;
    }
}


