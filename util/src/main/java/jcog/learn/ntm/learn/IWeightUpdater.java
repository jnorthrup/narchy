package jcog.learn.ntm.learn;

import jcog.learn.ntm.control.UMatrix;
import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;

public interface IWeightUpdater {
    void reset();

    void updateWeight(Unit data);

    @Deprecated void updateWeight(Unit[] data);
    void updateWeight(UVector data);


    default void updateWeight(final UMatrix data) {
        for (UVector v : data.row) {
            updateWeight(v);
        }
    }

    @Deprecated void updateWeight(Unit[][] data);

    void updateWeight(Unit[][][] data);
}


