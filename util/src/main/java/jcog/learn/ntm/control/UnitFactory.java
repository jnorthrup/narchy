package jcog.learn.ntm.control;


import java.util.ArrayList;
import java.util.List;

public class UnitFactory {


    @Deprecated public static Unit[] getVector(int vectorSize) {
        List<Unit> list = new ArrayList<>();
        for (int i = 0; i < vectorSize; i++) {
            Unit unit = new Unit();
            list.add(unit);
        }
        Unit[] vector = list.toArray(new Unit[0]);
        return vector;
    }






    @Deprecated public static Unit[][] getTensor2(int x, int y) {
        List<Unit[]> list = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            Unit[] vector = getVector(y);
            list.add(vector);
        }
        Unit[][] tensor = list.toArray(new Unit[0][]);

        return tensor;
    }

    public static Unit[][][] getTensor3(int x, int y, int z) {
        List<Unit[][]> list = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            Unit[][] tensor2 = getTensor2(y, z);
            list.add(tensor2);
        }
        Unit[][][] tensor = list.toArray(new Unit[0][][]);

        return tensor;
    }

}


