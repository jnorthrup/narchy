//package jcog.net.attn;
//
//import il.technion.tinytable.TinyCountingTable;
//import jcog.Util;
//
//
//public class TagDigest extends TinyCountingTable implements TagSet {
//
//    static final int PRI_GRANULARITY = 10;
//    private final String id;
//
//    public TagDigest(String id, int capacity) {
//        super(6, capacity, 5);
//        this.id = id;
//    }
//
//    @Override
//    public String id() {
//        return id;
//    }
//
//    /**
//     * sets the priority of a tag
//     */
//    @Override
//    public boolean pri(String tag, float pri) {
//        float existing = pri(tag);
//        if (Util.equals(existing, pri, 1f/PRI_GRANULARITY))
//            return false;
//
//        set(tag, (int) Math.ceil(pri * PRI_GRANULARITY));
//        return true;
//    }
//
//    /**
//     * gets the priority of a value
//     */
//    @Override
//    public float pri(String tag) {
//        long p = get(tag);
//        return p / ((float) PRI_GRANULARITY);
//    }
//
//    @Override
//    public void clear() {
//        throw new UnsupportedOperationException("possible?");
//    }
//
//}
