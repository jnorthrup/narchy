package spacegraph.space3d;

import jcog.list.FasterList;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * maintains a set of objects which are used as input for representation in a SpaceGraph
 * @param X input "key" object type
 * @param Y visualized "value" spatial type
 */
abstract public class AbstractSpace<X>  {

    



    protected final List<SpaceTransform<X>> transforms = new FasterList();

    public AbstractSpace with(SpaceTransform<X>... t) {
        Collections.addAll(this.transforms, t);
        return this;
    }

    public void start(SpaceGraphPhys3D<X> space) {

    }

    public void stop() {


    }


























    /** needs to call update(space) for each active item */
    public void update(SpaceGraphPhys3D<X> s, long dtMS) {

        List<SpaceTransform<X>> ll = this.transforms;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++)
            ll.get(i1).update(s, dtMS);

    }

    public abstract void forEach(Consumer<? super Spatial<X>> each);

}
