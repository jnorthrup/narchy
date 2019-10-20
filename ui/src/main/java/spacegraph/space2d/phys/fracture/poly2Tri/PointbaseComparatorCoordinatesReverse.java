package spacegraph.space2d.phys.fracture.poly2Tri;

import java.util.Comparator;

class PointbaseComparatorCoordinatesReverse implements Comparator {

    public int compare(Object o1, Object o2) {
        var pb1 = (Pointbase) o1;
        var pb2 = (Pointbase) o2;
        return (-pb1.compareTo(pb2));
    }

}
