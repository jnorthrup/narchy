package jcog.learn.ntm.learn;

import jcog.learn.ntm.control.Unit;

public interface WeightUpdaterBase extends IWeightUpdater {
    

    

    @Override
    default void updateWeight(Unit[] data) {
        for (var unit : data)         {
            updateWeight(unit);
        }
    }


    @Override
    default void updateWeight(Unit[][] data) {
        for (var units : data) {
            updateWeight(units);
        }
    }

    @Override
    default void updateWeight(Unit[][][] data) {
        for (var units : data) {
            updateWeight(units);
        }
    }

}


