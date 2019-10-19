package jcog.learn.ntm.memory.address;

import jcog.learn.ntm.control.Unit;
import jcog.learn.ntm.control.UnitFactory;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class Head   
{

    private final Unit[] _eraseVector;
    private final Unit[] _addVector;
    private final Unit[] _keyVector;
    private final Unit _beta;
    private final Unit _gate;
    private final Unit _shift;
    private final Unit _gama;
    private final int width;

    
    public Unit[] getKeyVector() {
        return _keyVector;
    }

    public Unit getBeta() {
        return _beta;
    }

    public Unit getGate() {
        return _gate;
    }

    public Unit getShift() {
        return _shift;
    }

    public Unit getGamma() {
        return _gama;
    }

    public Unit[] getEraseVector() {
        return _eraseVector;
    }

    public Unit[] getAddVector() {
        return _addVector;
    }

    public Head(int memoryWidth) {
        width = memoryWidth;
        _eraseVector = UnitFactory.getVector(memoryWidth);
        _addVector = UnitFactory.getVector(memoryWidth);
        _keyVector = UnitFactory.getVector(memoryWidth);
        _beta = new Unit(0.0);
        _gate = new Unit(0.0);
        _shift = new Unit(0.0);
        _gama = new Unit(0.0);
    }

    public static int getUnitSize(int memoryRowsM) {
        return (3 * memoryRowsM) + 4;
    }

    public int getUnitSize() {
        return getUnitSize(width);
    }

    public static Head[] getVector(int length, UnaryOperator<Integer> constructorParamGetter) {
        return IntStream.range(0, length).mapToObj(i -> new Head(constructorParamGetter.apply(i))).toArray(Head[]::new);
    }

    public Unit get(int i) {
        if (i < width)
        {
            return _eraseVector[i];
        }
         
        if (i < (width * 2))
        {
            return _addVector[i - width];
        }

        int width3 = width * 3;
        if (i < width3)
        {
            return _keyVector[i - (2 * width)];
        }

        switch (i - width3) {
            case 0: return _beta;
            case 1: return _gate;
            case 2: return _shift;
            case 3: return _gama;
        }
         

        throw new IndexOutOfBoundsException("Index is out of range");
    }

}


