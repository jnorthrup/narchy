package org.jbox2d.fracture.poly2Tri.splayTree;

public interface SplayTreeItem {

    Comparable keyValue();

    void increaseKeyValue(double delta);
}
