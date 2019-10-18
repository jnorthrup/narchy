package jcog.learn.ntm.control;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class UnitFactory {


    @Deprecated public static Unit[] getVector(int vectorSize) {
        Unit[] vector = IntStream.range(0, vectorSize).mapToObj(i -> new Unit()).toArray(Unit[]::new);
        return vector;
    }






    @Deprecated public static Unit[][] getTensor2(int x, int y) {
        Unit[][] tensor = IntStream.range(0, x).mapToObj(i -> getVector(y)).toArray(Unit[][]::new);

        return tensor;
    }

    public static Unit[][][] getTensor3(int x, int y, int z) {
        Unit[][][] tensor = IntStream.range(0, x).mapToObj(i -> getTensor2(y, z)).toArray(Unit[][][]::new);

        return tensor;
    }

}


