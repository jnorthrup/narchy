package jcog.pri.bag.impl.tree;

/**
 * TODO still undeveloped
 *
 * some kind of hierarchical/fractal n-ary tree where leaf buckets are
 * designated for specific but a changing (numeric or otherwise) 1-D quantifier
 * on stored items (ex: time).
 *
 * one example of what this allows is something like a sliding r-tree
 * with curved and/or adaptive intervals. for example, as time
 * shifts forward, items will gradually migrate to the left-wise bucket
 * in such a way that the capacity restrictions on each bucket are not
 * violated, and in the event of insufficient capacity, a merge
 * function can apply to reduce 2 or more values to 1.
 *
 * the goal is for each bucket to enforce dimensional locality that makes such
 * merges efficient and relevant.
 *
 * the specific ranges assigned to each bucket need not be equal or regular
 * in any way. for example, a bi-polar logarithmic scale, focusing linearly
 * at some present time frame, and then towards the past and future directions
 * the scale would increase, producing a compressing an infinite temporal horizon
 * in such a way that it does not interfere with present-moment items which
 * would be of highest priority and require the most detail.
 *
 * the leaves can be any kind of collection but i have in mind using
 * mini hijackbags.
 */
public class TreeBag {
}
