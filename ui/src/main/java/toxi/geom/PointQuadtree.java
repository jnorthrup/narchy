/*
 *   __               .__       .__  ._____.           
 * _/  |_  _______  __|__| ____ |  | |__\_ |__   ______
 * \   __\/  _ \  \/  /  |/ ___\|  | |  || __ \ /  ___/
 *  |  | (  <_> >    <|  \  \___|  |_|  || \_\ \\___ \ 
 *  |__|  \____/__/\_ \__|\___  >____/__||___  /____  >
 *                   \/       \/             \/     \/ 
 *
 * Copyright (c) 2006-2011 Karsten Schmidt
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * http://creativecommons.org/licenses/LGPL/2.1/
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 */

package toxi.geom;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Implements a spatial subdivision tree to work efficiently with large numbers
 * of 2D particles. This quadtree can only be used for particle type objects and
 * does NOT support 2D mesh geometry as other forms of quadtree might do.
 * 
 * For further reference also see the QuadtreeDemo in the /examples folder.
 * 
 */
public class PointQuadtree<V extends Vec2D> extends Rect implements SpatialIndex<V> {

    private static final int LeafCapacity = 4;
    private static final float MinLeafDimension = 0.0000001f;

    public enum Type {
        EMPTY,
        BRANCH,
        LEAF
    }

    private final PointQuadtree parent;
    private PointQuadtree childNW, childNE, childSW, childSE;

    private Type type;

    private Set<V> values = null;
    private final float mx;
    private final float my;

    public PointQuadtree() {
        this(null, 0.5f, 0.5f, 1, 1);
    }

    public PointQuadtree(float x, float y, float w, float h) {
        this(null, x, y, w, h);
    }

    public PointQuadtree(PointQuadtree parent, float x, float y, float w, float h) {
        super(x, y, w, h);
        this.parent = parent;
        this.type = Type.EMPTY;
        mx = x + w * 0.5f;
        my = y + h * 0.5f;
    }

    public PointQuadtree(Rect r) {
        this(null, r.x, r.y, r.width, r.height);
    }

    private void balance() {
        switch (type) {
            case EMPTY:
            case LEAF:
                if (parent != null) {
                    parent.balance();
                }
                break;

            case BRANCH:
                PointQuadtree<V> leaf = null;
                if (childNW.type != Type.EMPTY) {
                    leaf = childNW;
                }
                if (childNE.type != Type.EMPTY) {
                    if (leaf != null) {
                        break;
                    }
                    leaf = childNE;
                }
                if (childSW.type != Type.EMPTY) {
                    if (leaf != null) {
                        break;
                    }
                    leaf = childSW;
                }
                if (childSE.type != Type.EMPTY) {
                    if (leaf != null) {
                        break;
                    }
                    leaf = childSE;
                }
                if (leaf == null) {
                    type = Type.EMPTY;
                    childNW = childNE = childSW = childSE = null;
                } else if (leaf.type == Type.BRANCH) {
                    break;
                } else {
                    type = Type.LEAF;
                    childNW = childNE = childSW = childSE = null;
                    //value = leaf.value;
                    values = leaf.values;
                }
                if (parent != null) {
                    parent.balance();
                }
        }
    }


    @Override
    public void clear() {
        childNW = childNE = childSW = childSE = null;
        type = Type.EMPTY;
        values = null;
    }

    public PointQuadtree findNode(V p) {
        switch (type) {
            case EMPTY:
                return null;
            case LEAF:
                return contains(p) ? this : null; //value.x == x && value.y == y ? this : null;
            case BRANCH:
                return quadrant(p.x, p.y).findNode(p);
            default:
                throw new IllegalStateException("Invalid node type");
        }
    }

    boolean contains(V p) {
        return (values!=null && values.contains(p));
    }


    private PointQuadtree quadrant(float x, float y) {
        if (x < mx) {
            return y < my ? childNW : childSW;
        } else {
            return y < my ? childNE : childSE;
        }
    }

    /**
     *
     * @param p
     * @return
     */
    @Override
    public boolean index(V p) {
        if (containsPoint(p)) {
            switch (type) {
                case EMPTY:
                    setPoint(p);
                    return true;

                case LEAF:
                    if (size() < LeafCapacity || Math.max(width,height) <= MinLeafDimension) {
                        return values.add(p);
                    } else {
                        if (values.contains(p))
                            return false;
                        else {
                            split();
                            return quadrant(p.x, p.y).index(p);
                        }
                    }


                case BRANCH:
                    return quadrant(p.x, p.y).index(p);
            }
        } else {
//            if (parent == null && stretchPoint(p, MinLeafArea)) {
//                //resize the entire tree
//                //experimental
//                split();
//                boolean added = quadrant(p.x, p.y).index(p);
//                return added;
//            }
        }
        return false;
    }

    @Override
    public boolean isIndexed(V p) {
        return findNode(p) != null;
    }

    @Override
    public void itemsWithinRadius(Vec2D p, float radius,
                                  Consumer<V> results) {
        if (intersectsCircle(p, radius)) {
            if (type == Type.LEAF) {
                if (values!=null) {
                    for (V value : values) {
                        if (value.distanceToSquared(p) < radius * radius) {
                            results.accept(value);
                        }
                    }
                }
            } else if (type == Type.BRANCH) {
                if (childNW!=null) childNW.itemsWithinRadius(p, radius, results);
                if (childNE!=null) childNE.itemsWithinRadius(p, radius, results);
                if (childSW!=null) childSW.itemsWithinRadius(p, radius, results);
                if (childSE!=null) childSE.itemsWithinRadius(p, radius, results);
            }
        }
    }

//    public List<Vec2D> itemsWithinRect(Rect bounds, List<Vec2D> results) {
//        if (bounds.intersectsRect(this)) {
//            if (type == Type.LEAF) {
//                if (bounds.containsPoint(value)) {
//                    if (results == null) {
//                        results = new ArrayList<>();
//                    }
//                    results.add(value);
//                }
//            } else if (type == Type.BRANCH) {
//                PointQuadtree[] children = new PointQuadtree[] {
//                        childNW, childNE, childSW, childSE
//                };
//                for (int i = 0; i < 4; i++) {
//                    if (children[i] != null) {
//                        results = children[i].itemsWithinRect(bounds, results);
//                    }
//                }
//            }
//        }
//        return results;
//    }

    public void prewalk(Consumer<PointQuadtree> visitor) {
        switch (type) {
            case LEAF:
                visitor.accept(this);
                break;

            case BRANCH:
                visitor.accept(this);
                childNW.prewalk(visitor);
                childNE.prewalk(visitor);
                childSW.prewalk(visitor);
                childSE.prewalk(visitor);
                break;
        }
    }

    @Override
    public boolean reindex(V p, Consumer<V> each) {
        unindex(p);
        each.accept(p);
        return index(p);
    }


    private void setPoint(V p) {
        assert(values == null);
        if (type == Type.BRANCH) {
            throw new IllegalStateException("invalid node type: BRANCH");
        }
        type = Type.LEAF;
        values = new UnifiedSet(LeafCapacity, 0.99f);
        values.add(p);
    }

    @Override
    public int size() {
        return values!=null ? values.size() : 0;
    }

    private void split() {
        split(x, y, width, height);
    }

    private void split(float x, float y, float w, float h) {
        Set<V> oldPoints = values;
        values = null;

        type = Type.BRANCH;

        float w2 = w * 0.5f;
        float h2 = h * 0.5f;

        childNW = new PointQuadtree(this, x, y, w2, h2);
        childNE = new PointQuadtree(this, x + w2, y, w2, h2);
        childSW = new PointQuadtree(this, x, y + h2, w2, h2);
        childSE = new PointQuadtree(this, x + w2, y + h2, w2, h2);

        if (oldPoints!=null) {
            for (V v : oldPoints)
                index(v);
        }
    }

    @Override
    public boolean unindex(V p) {
        PointQuadtree node = findNode(p);
        if (node != null) {
            boolean removed = node.values.remove(p);
            assert(removed);
            if (node.values.isEmpty()) {
                node.type = Type.EMPTY;
                node.values = null;
            } else {
                node.balance();
            }
            return true;
        } else {
            return false;
        }
    }
}
