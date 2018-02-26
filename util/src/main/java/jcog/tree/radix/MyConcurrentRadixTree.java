package jcog.tree.radix;

import jcog.data.byt.AbstractBytes;
import jcog.data.byt.ArrayBytes;
import jcog.list.FasterList;
import jcog.sort.SortedArray;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.System.arraycopy;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * seh's modifications to radix tree
 * <p>
 * An implementation of {@link RadixTree} which supports lock-free concurrent reads, and allows items to be added to and
 * to be removed from the tree <i>atomically</i> by background thread(s), without blocking reads.
 * <p/>
 * Unlike reads, writes require locking of the tree (locking out other writing threads only; reading threads are never
 * blocked). Currently write locks are coarse-grained; in fact they are tree-level. In future branch-level write locks
 * might be added, but the current implementation is targeted at high concurrency read-mostly use cases.
 *
 * @author Niall Gallagher
 * @modified by seth
 */
public class MyConcurrentRadixTree<X> /* TODO extends ReentrantReadWriteLock */ implements /*RadixTree<X>,*/Serializable, Iterable<X> {


    public interface Prefixed {
        byte getIncomingEdgeFirstCharacter();
    }

//    static class NodeCharacterKey implements Prefixed {
//        private final byte character;
//
//        public NodeCharacterKey(byte character) {
//            this.character = character;
//        }
//
//        @Override
//        public byte getIncomingEdgeFirstCharacter() {
//            return this.character;
//        }
//    }

    public interface Node extends Prefixed, Serializable {

        AbstractBytes getIncomingEdge();

        @Nullable Object getValue();

        @Nullable Node getOutgoingEdge(byte var1);

        void updateOutgoingEdge(Node var1);

        FasterList<Node> getOutgoingEdges();
    }

    /**
     * default factory
     */
    protected static Node createNode(AbstractBytes edgeCharacters, Object value, FasterList<Node> childNodes, boolean isRoot) {
        if (edgeCharacters == null) {
            throw new IllegalStateException("The edgeCharacters argument was null");
        } else if (!isRoot && edgeCharacters.length() == 0) {
            throw new IllegalStateException("Invalid edge characters for non-root node: " + edgeCharacters);
        } else if (childNodes == null) {
            throw new IllegalStateException("The childNodes argument was null");
        } else {
            //ensureNoDuplicateEdges(childNodes);
            return childNodes.isEmpty() ?
                    ((value instanceof VoidValue) ?
                            new ByteArrayNodeLeafVoidValue(edgeCharacters) :
                            ((value != null) ?
                                    new ByteArrayNodeLeafWithValue(edgeCharacters, value) :
                                    new ByteArrayNodeLeafNullValue(edgeCharacters))) :
                    ((value instanceof VoidValue) ?
                            innerVoid(edgeCharacters, childNodes) :
                            ((value == null) ?
                                    innerNull(edgeCharacters, childNodes) :
                                    inner(edgeCharacters, value, childNodes)));
        }
    }

    static public ByteArrayNodeDefault inner(AbstractBytes in, Object value, FasterList<Node> outs) {
        if (outs.size() > 1)
            Collections.sort(outs, NODE_COMPARATOR);
        return new ByteArrayNodeDefault(in.array(), value, outs);
    }

    static public ByteArrayNodeNonLeafVoidValue innerVoid(AbstractBytes in, FasterList<Node> outs) {
        if (outs.size() > 1)
            Collections.sort(outs, NODE_COMPARATOR);
        return new ByteArrayNodeNonLeafVoidValue(in.array(), outs);
    }

    static public ByteArrayNodeNonLeafNullValue innerNull(AbstractBytes in, FasterList<Node> outs) {
        if (outs.size() > 1)
            Collections.sort(outs, NODE_COMPARATOR);
        return new ByteArrayNodeNonLeafNullValue(in.array(), outs);
    }

    final static Comparator<? super Prefixed> NODE_COMPARATOR = Comparator.comparingInt(Prefixed::getIncomingEdgeFirstCharacter);

    //    private static int cmp(byte o1, byte o2) {
//        return o1 - o2;
//    }

    static AbstractBytes getCommonPrefix(AbstractBytes first, AbstractBytes second) {
        if (first == second) return first;

        int minLength = Math.min(first.length(), second.length());

        for (int i = 0; i < minLength; ++i) {
            if (first.at(i) != second.at(i)) {
                return first.subSequence(0, i);
            }
        }

        return first.subSequence(0, minLength);
    }

    static AbstractBytes subtractPrefix(AbstractBytes main, AbstractBytes prefix) {
        int startIndex = prefix.length();
        int mainLength = main.length();
        return (startIndex > mainLength ? AbstractBytes.EMPTY : main.subSequence(startIndex, mainLength));
    }

    static int search(Node[] a, int size, byte key) {
        if (size == 0) {
            return -1;
        } else if (size >= SortedArray.BINARY_SEARCH_THRESHOLD) {
            return binarySearch(key, size, a);
        } else {
            return linearSearch(key, a);
        }
    }

    private static int linearSearch(byte key, Node[] a) {
        for (int i = 0, aLength = a.length; i < aLength; i++) {
            Node x = a[i];
            if (x == null)
                break; //early end of array
            if (x.getIncomingEdgeFirstCharacter() == key)
                return i;
        }
        return -1;
    }

    private static int binarySearch(byte key, int size, Node[] a) {
        int high = size-1;
        int low = 0;
        while (low <= high) {
            int midIndex = (low + high) >>> 1;
            int cmp = a[midIndex].getIncomingEdgeFirstCharacter() - key;

            if (cmp < 0) low = midIndex + 1;
            else if (cmp > 0) high = midIndex - 1;
            else return midIndex; // key found
        }
        return -(low + 1);  // key not found
    }


    static final class ByteArrayNodeDefault extends NonLeafNode {
        private final Object value;

        public ByteArrayNodeDefault(byte[] edgeCharSequence, Object value, FasterList<Node> outgoingEdges) {
            super(edgeCharSequence, outgoingEdges);
            this.value = value;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

    }

    static class ByteArrayNodeLeafVoidValue extends ArrayBytes implements Node {
        //public final byte[] incomingEdgeCharArray;

        public ByteArrayNodeLeafVoidValue(AbstractBytes edgeCharSequence) {
            super(edgeCharSequence.array());
            //this.incomingEdgeCharArray = edgeCharSequence.array();
        }

        @Override
        public AbstractBytes getIncomingEdge() {
            return this;
        }

        @Override
        public final byte getIncomingEdgeFirstCharacter() {
            return this.bytes[0];
        }

        @Override
        public Object getValue() {
            return VoidValue.SINGLETON;
        }

        @Override
        public Node getOutgoingEdge(byte edgeFirstCharacter) {
            return null;
        }

        @Override
        public void updateOutgoingEdge(Node childNode) {
            throw new IllegalStateException("Cannot update the reference to the following child node for the edge starting with \'" + childNode.getIncomingEdgeFirstCharacter() + "\', no such edge already exists: " + childNode);
        }

        @Override
        public FasterList<Node> getOutgoingEdges() {
            return emptyList;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node{");
            sb.append("edge=").append(this.getIncomingEdge());
            sb.append(", value=").append(VoidValue.SINGLETON);
            sb.append(", edges=[]");
            sb.append("}");
            return sb.toString();
        }
    }


    abstract static class NonLeafNode extends FasterList /*CopyOnWriteArrayList*/<Node> implements Node {
        public final byte[] incomingEdgeCharArray;
        //private final AtomicReferenceArray<Node> outgoingEdges;
        //public final CopyOnWriteArrayList<Node> outgoingEdges;

        protected NonLeafNode(@NotNull byte[] incomingEdgeCharArray, @NotNull FasterList<Node> outs) {
            super(outs.size(), outs.array());
            this.incomingEdgeCharArray = incomingEdgeCharArray;
        }

        @NotNull @Override
        public AbstractBytes getIncomingEdge() {
            return new ArrayBytes(this.incomingEdgeCharArray);
        }

        @Override
        public byte getIncomingEdgeFirstCharacter() {
            return this.incomingEdgeCharArray[0];
        }

        @Nullable @Override
        public final Node getOutgoingEdge(byte edgeFirstCharacter) {
            //AtomicReferenceArrayListAdapter childNodesList = new AtomicReferenceArrayListAdapter<>(childNodes);
            //NodeCharacterKey searchKey = new NodeCharacterKey(edgeFirstCharacter);

            Node[] a = array();
            int index = MyConcurrentRadixTree.search(a, size(), edgeFirstCharacter);
            if (index < 0)
                return null;
            return a[index];
        }

        @Override
        public final void updateOutgoingEdge(@NotNull Node childNode) {
            //AtomicReferenceArrayListAdapter childNodesList = new AtomicReferenceArrayListAdapter<>(childNodes);
            //NodeCharacterKey searchKey = new NodeCharacterKey(edgeFirstCharacter);

            int index = MyConcurrentRadixTree.search(array(), size(), childNode.getIncomingEdgeFirstCharacter());
            if (index < 0) {
                throw new IllegalStateException("Cannot update the reference to the following child node for the edge starting with \'" + childNode.getIncomingEdgeFirstCharacter() + "\', no such edge already exists: " + childNode);
            } else {
                set(index, childNode);
            }
        }

        @Override
        public FasterList<Node> getOutgoingEdges() {
            return this;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node{");
            sb.append("edge=").append(this.getIncomingEdge());
            sb.append(", value=" + getValue());
            sb.append("}");
            return sb.toString();
        }
    }

    static final class ByteArrayNodeNonLeafNullValue extends NonLeafNode {

        protected ByteArrayNodeNonLeafNullValue(byte[] incomingEdgeCharArray, FasterList<Node> outgoingEdges) {
            super(incomingEdgeCharArray, outgoingEdges);
        }

        @Override
        public Object getValue() {
            return null;
        }

        //        public ByteArrayNodeNonLeafNullValue(ByteSeq edgeCharSequence, FasterList<Node> outgoingEdges) {
//            super(edgeCharSequence, outgoingEdges.toArray(new Node[outgoingEdges.size()]));
//        }

//        static public ByteArrayNodeNonLeafNullValue the(ByteSeq in, Node... outs) {
//
//            Arrays.sort(outs, NODE_COMPARATOR);
//
//            return new ByteArrayNodeNonLeafNullValue(in.array(), new CopyOnWriteArrayList<>(outs));
//        }


    }

    static final class ByteArrayNodeLeafWithValue extends ByteArrayNodeLeafVoidValue {

        private final Object value;

        public ByteArrayNodeLeafWithValue(AbstractBytes edgeCharSequence, Object value) {
            super(edgeCharSequence);
            this.value = value;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node{");
            sb.append("edge=").append(new String(this.bytes));
            sb.append(", value=").append(this.value);
            sb.append(", edges=[]");
            sb.append("}");
            return sb.toString();
        }
    }

    static final class ByteArrayNodeLeafNullValue extends ByteArrayNodeLeafVoidValue {

        public ByteArrayNodeLeafNullValue(AbstractBytes edgeCharSequence) {
            super(edgeCharSequence);
        }

        @Override
        public Object getValue() {
            return null;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node{");
            sb.append("edge=").append(this.getIncomingEdge());
            sb.append(", value=null");
            sb.append(", edges=[]");
            sb.append("}");
            return sb.toString();
        }
    }

    static final class ByteArrayNodeNonLeafVoidValue extends NonLeafNode {


        public ByteArrayNodeNonLeafVoidValue(byte[] edgeCharSequence, FasterList<Node> outgoingEdges) {
            super(edgeCharSequence, outgoingEdges);
        }

        @Override
        public Object getValue() {
            return VoidValue.SINGLETON;
        }

    }


    public Node root;

    // Write operations acquire write lock.
    // Read operations are lock-free by default, but can be forced to acquire read locks via constructor flag...
    // If non-null true, force reading threads to acquire read lock (they will block on writes).
    @Nullable
    private final Lock readLock;
    @NotNull
    private final Lock writeLock;


    final AtomicInteger estSize = new AtomicInteger(0);

    /**
     * Creates a new {@link MyConcurrentRadixTree} which will use the given {@link NodeFactory} to create nodes.
     *
     * @param nodeFactory An object which creates {@link Node} objects on-demand, and which might return node
     *                    implementations optimized for storing the values supplied to it for the creation of each node
     */
    public MyConcurrentRadixTree() {
        this(false);
    }

    /**
     * Creates a new {@link MyConcurrentRadixTree} which will use the given {@link NodeFactory} to create nodes.
     *
     * @param nodeFactory         An object which creates {@link Node} objects on-demand, and which might return node
     *                            implementations optimized for storing the values supplied to it for the creation of each node
     * @param restrictConcurrency If true, configures use of a {@link ReadWriteLock} allowing
     *                            concurrent reads, except when writes are being performed by other threads, in which case writes block all reads;
     *                            if false, configures lock-free reads; allows concurrent non-blocking reads, even if writes are being performed
     *                            by other threads
     */
    public MyConcurrentRadixTree(boolean restrictConcurrency) {

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.writeLock = readWriteLock.writeLock();
        this.readLock = restrictConcurrency ? readWriteLock.readLock() : null;

        clear();
    }

    private final static FasterList<Node> emptyList = new FasterList(0, new Node[] {});

    public void clear() {
        acquireWriteLock();
        try {
            this.root = createNode(AbstractBytes.EMPTY, null, emptyList, true);
        } finally {
            releaseWriteLock();
        }
    }

    // ------------- Helper methods for serializing writes -------------

    /**
     * essentially a version number which increments each acquired write lock, to know if the tree has changed
     */
    final static AtomicInteger writes = new AtomicInteger();

    public final int acquireWriteLock() {
        writeLock.lock();
        return writes.incrementAndGet();
    }

    public final void releaseWriteLock() {
        writeLock.unlock();
    }


    public final void acquireReadLockIfNecessary() {
        if (readLock != null)
            readLock.lock();
    }

    public final void releaseReadLockIfNecessary() {
        if (readLock != null)
            readLock.unlock();
    }


    public final X put(Pair<AbstractBytes, X> value) {
        return put(value.getOne(), value.getTwo());
    }


    public X put(X value) {
        throw new UnsupportedOperationException("subclasses can implement this by creating their own key and calling put(k,v)");
    }

    /**
     * {@inheritDoc}
     */
    public final X put(AbstractBytes key, X value) {
//        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
//        O existingValue = (O) putInternal(key, value, true);  // putInternal acquires write lock
//        return existingValue;

        return compute(key, value, (k, r, existing, v) -> v);
    }

    /**
     * {@inheritDoc}
     */
    public final X putIfAbsent(AbstractBytes key, X newValue) {
        return compute(key, newValue, (k, r, existing, v) ->
                existing != null ? existing : v
        );
    }

    @NotNull
    public final X putIfAbsent(@NotNull AbstractBytes key, @NotNull Supplier<X> newValue) {
        return compute(key, newValue, (k, r, existing, v) ->
                existing != null ? existing : v.get()
        );
    }

    /**
     * {@inheritDoc}
     */
    public X getValueForExactKey(AbstractBytes key) {
        acquireReadLockIfNecessary();
        try {
            SearchResult searchResult = searchTree(key);
            if (searchResult.classification.equals(SearchResult.Classification.EXACT_MATCH)) {
                @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
                X value = (X) searchResult.found.getValue();
                return value;
            }
            return null;
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<AbstractBytes> getKeysStartingWith(AbstractBytes prefix) {
        acquireReadLockIfNecessary();
        try {
            SearchResult searchResult = searchTree(prefix);
            Node nodeFound = searchResult.found;
            switch (searchResult.classification) {
                case EXACT_MATCH:
                    return getDescendantKeys(prefix, nodeFound);
                case KEY_ENDS_MID_EDGE:
                    // Append the remaining characters of the edge to the key.
                    // For example if we searched for CO, but first matching node was COFFEE,
                    // the key associated with the first node should be COFFEE...
                    AbstractBytes edgeSuffix = getSuffix(nodeFound.getIncomingEdge(), searchResult.charsMatchedInNodeFound);
                    prefix = concatenate(prefix, edgeSuffix);
                    return getDescendantKeys(prefix, nodeFound);
                default:
                    // Incomplete match means key is not a prefix of any node...
                    return Collections.emptySet();
            }
        } finally {
            releaseReadLockIfNecessary();
        }
    }


    /**
     * {@inheritDoc}
     */
    public Iterable<X> getValuesForKeysStartingWith(AbstractBytes prefix) {
        acquireReadLockIfNecessary();
        try {
            SearchResult searchResult = searchTree(prefix);
            Node Found = searchResult.found;
            switch (searchResult.classification) {
                case EXACT_MATCH:
                    return getDescendantValues(prefix, Found);
                case KEY_ENDS_MID_EDGE:
                    // Append the remaining characters of the edge to the key.
                    // For example if we searched for CO, but first matching node was COFFEE,
                    // the key associated with the first node should be COFFEE...
                    return getDescendantValues(
                            concatenate(
                                    prefix,
                                    getSuffix(
                                            Found.getIncomingEdge(),
                                            searchResult.charsMatchedInNodeFound)),
                            Found);
                default:
                    // Incomplete match means key is not a prefix of any node...
                    return Collections.emptySet();
            }
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<Pair<AbstractBytes, X>> getKeyValuePairsForKeysStartingWith(AbstractBytes prefix) {
        acquireReadLockIfNecessary();
        try {
            SearchResult searchResult = searchTree(prefix);
            SearchResult.Classification classification = searchResult.classification;
            Node f = searchResult.found;
            switch (classification) {
                case EXACT_MATCH:
                    return getDescendantKeyValuePairs(prefix, f);
                case KEY_ENDS_MID_EDGE:
                    // Append the remaining characters of the edge to the key.
                    // For example if we searched for CO, but first matching node was COFFEE,
                    // the key associated with the first node should be COFFEE...
                    AbstractBytes edgeSuffix = getSuffix(f.getIncomingEdge(), searchResult.charsMatchedInNodeFound);
                    prefix = concatenate(prefix, edgeSuffix);
                    return getDescendantKeyValuePairs(prefix, f);
                default:
                    // Incomplete match means key is not a prefix of any node...
                    return Collections.emptySet();
            }
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(@NotNull AbstractBytes key) {
        acquireWriteLock();
        try {
            SearchResult searchResult = searchTree(key);
            return removeHavingAcquiredWriteLock(searchResult, false);
        } finally {
            releaseWriteLock();
        }
    }

    public boolean remove(@NotNull SearchResult searchResult, boolean recurse) {
        acquireWriteLock();
        try {
            return removeHavingAcquiredWriteLock(searchResult, recurse);
        } finally {
            releaseWriteLock();
        }
    }

    /**
     * allows subclasses to override this to handle removal events. return true if removal is accepted, false to reject the removal and reinsert
     */
    protected boolean onRemove(X removed) {

        return true;
    }

    public boolean removeHavingAcquiredWriteLock(SearchResult searchResult, boolean recurse) {
        SearchResult.Classification classification = searchResult.classification;
        switch (classification) {
            case EXACT_MATCH:
                Node found = searchResult.found;
                Node parent = searchResult.parentNode;

                Object v = found.getValue();
                if (!recurse && ((v == null) || (v == VoidValue.SINGLETON))) {
                    // This node was created automatically as a split between two branches (implicit node).
                    // No need to remove it...
                    return false;
                }

                List<X> reinsertions = new FasterList<>(0);

                if (v != null && v != VoidValue.SINGLETON) {
                    X xv = (X) v;
                    boolean removed = tryRemove(xv);
                    if (!recurse) {
                        if (!removed)
                            return false; //remove was disabled for this entry
                    } else {
                        if (!removed) {
                            reinsertions.add(xv); //continue removing below then reinsert afterward
                        }
                    }
                }


                // Proceed with deleting the node...
                FasterList<Node> childEdges = found.getOutgoingEdges();
                int numChildren = childEdges.size();
                if (numChildren > 0) {
                    if (!recurse) {
                        if (numChildren > 1) {
                            // This node has more than one child, so if we delete the value from this node, we still need
                            // to leave a similar node in place to act as the split between the child edges.
                            // Just delete the value associated with this node.
                            // -> Clone this node without its value, preserving its child nodes...
                            @SuppressWarnings("NullableProblems")
                            Node cloned = createNode(found.getIncomingEdge(), null, found.getOutgoingEdges(), false);
                            // Re-add the replacement node to the parent...
                            parent.updateOutgoingEdge(cloned);
                        } else if (numChildren == 1) {
                            // Node has one child edge.
                            // Create a new node which is the concatenation of the edges from this node and its child,
                            // and which has the outgoing edges of the child and the value from the child.
                            Node child = childEdges.get(0);
                            AbstractBytes concatenatedEdges = concatenate(found.getIncomingEdge(), child.getIncomingEdge());
                            Node mergedNode = createNode(concatenatedEdges, child.getValue(), child.getOutgoingEdges(), false);
                            // Re-add the merged node to the parent...
                            parent.updateOutgoingEdge(mergedNode);
                        }
                    } else {
                        //collect all values from the subtree, call onRemove for them. then proceed below with removal of this node and its value
                        forEach(found, (k, f) -> {
                            boolean removed = tryRemove(f);
                            if (!removed) {
                                reinsertions.add(f);
                            }
                        });
                        numChildren = 0;
                    }
                }

                if (numChildren == 0) {

                    if (reinsertions.size() == 1) {
                        //this was a leaf node that was prevented from being removed.
                        //in this case make no further changes
                        return false;
                    }

                    // Node has no children. Delete this node from its parent,
                    // which involves re-creating the parent rather than simply updating its child edge
                    // (this is why we need parentNodesParent).
                    // However if this would leave the parent with only one remaining child edge,
                    // and the parent itself has no value (is a split node), and the parent is not the root node
                    // (a special case which we never merge), then we also need to merge the parent with its
                    // remaining child.

                    FasterList<Node> currentEdgesFromParent = parent.getOutgoingEdges();
                    // Create a list of the outgoing edges of the parent which will remain
                    // if we remove this child...

                    int cen = currentEdgesFromParent.size();

                    FasterList<Node> newEdgesOfParent = new FasterList<>(0, new Node[cen] );
                    boolean differs = false;
                    for (int i = 0; i < cen; i++) {
                        Node node = currentEdgesFromParent.get(i);
                        if (node != found) {
                            newEdgesOfParent.add(node);
                        } else {
                            differs = true;
                        }
                    }
                    if (!differs)
                        newEdgesOfParent = currentEdgesFromParent; //re-use original

                    // Note the parent might actually be the root node (which we should never merge)...
                    boolean parentIsRoot = (parent == root);
                    Node newParent;
                    if (newEdgesOfParent.size() == 1 && parent.getValue() == null && !parentIsRoot) {
                        // Parent is a non-root split node with only one remaining child, which can now be merged.
                        Node parentsRemainingChild = newEdgesOfParent.get(0);
                        // Merge the parent with its only remaining child...
                        AbstractBytes concatenatedEdges = concatenate(parent.getIncomingEdge(), parentsRemainingChild.getIncomingEdge());
                        newParent = createNode(concatenatedEdges, parentsRemainingChild.getValue(), parentsRemainingChild.getOutgoingEdges(), parentIsRoot);
                    } else {
                        // Parent is a node which either has a value of its own, has more than one remaining
                        // child, or is actually the root node (we never merge the root node).
                        // Create new parent node which is the same as is currently just without the edge to the
                        // node being deleted...
                        newParent = createNode(parent.getIncomingEdge(), parent.getValue(), newEdgesOfParent, parentIsRoot);
                    }
                    // Re-add the parent node to its parent...
                    if (parentIsRoot) {
                        // Replace the root node...
                        this.root = newParent;
                    } else {
                        // Re-add the parent node to its parent...
                        searchResult.parentNodesParent.updateOutgoingEdge(newParent);
                    }
                }


                reinsertions.forEach(this::put);

                return true;
            default:
                return false;
        }
    }


    private final boolean tryRemove(X v) {
        estSize.decrementAndGet();
        return onRemove(v);
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<AbstractBytes> getClosestKeys(AbstractBytes candidate) {
        acquireReadLockIfNecessary();
        try {
            SearchResult searchResult = searchTree(candidate);
            SearchResult.Classification classification = searchResult.classification;
            switch (classification) {
                case EXACT_MATCH:
                    return getDescendantKeys(candidate, searchResult.found);
                case KEY_ENDS_MID_EDGE:
                    // Append the remaining characters of the edge to the key.
                    // For example if we searched for CO, but first matching node was COFFEE,
                    // the key associated with the first node should be COFFEE...
                    AbstractBytes edgeSuffix = getSuffix(searchResult.found.getIncomingEdge(), searchResult.charsMatchedInNodeFound);
                    candidate = concatenate(candidate, edgeSuffix);
                    return getDescendantKeys(candidate, searchResult.found);
                case INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE: {
                    // Example: if we searched for CX, but deepest matching node was CO,
                    // the results should include node CO and its descendants...
                    AbstractBytes keyOfParentNode = getPrefix(candidate, searchResult.charsMatched - searchResult.charsMatchedInNodeFound);
                    AbstractBytes keyOfNodeFound = concatenate(keyOfParentNode, searchResult.found.getIncomingEdge());
                    return getDescendantKeys(keyOfNodeFound, searchResult.found);
                }
                case INCOMPLETE_MATCH_TO_END_OF_EDGE:
                    if (searchResult.charsMatched == 0) {
                        // Closest match is the root node, we don't consider this a match for anything...
                        break;
                    }
                    // Example: if we searched for COFFEE, but deepest matching node was CO,
                    // the results should include node CO and its descendants...
                    AbstractBytes keyOfNodeFound = getPrefix(candidate, searchResult.charsMatched);
                    return getDescendantKeys(keyOfNodeFound, searchResult.found);
            }
            return Collections.emptySet();
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<X> getValuesForClosestKeys(AbstractBytes candidate) {
        acquireReadLockIfNecessary();
        try {
            SearchResult searchResult = searchTree(candidate);
            SearchResult.Classification classification = searchResult.classification;
            switch (classification) {
                case EXACT_MATCH:
                    return getDescendantValues(candidate, searchResult.found);
                case KEY_ENDS_MID_EDGE:
                    // Append the remaining characters of the edge to the key.
                    // For example if we searched for CO, but first matching node was COFFEE,
                    // the key associated with the first node should be COFFEE...
                    AbstractBytes edgeSuffix = getSuffix(searchResult.found.getIncomingEdge(), searchResult.charsMatchedInNodeFound);
                    candidate = concatenate(candidate, edgeSuffix);
                    return getDescendantValues(candidate, searchResult.found);
                case INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE: {
                    // Example: if we searched for CX, but deepest matching node was CO,
                    // the results should include node CO and its descendants...
                    AbstractBytes keyOfParentNode = getPrefix(candidate, searchResult.charsMatched - searchResult.charsMatchedInNodeFound);
                    AbstractBytes keyOfNodeFound = concatenate(keyOfParentNode, searchResult.found.getIncomingEdge());
                    return getDescendantValues(keyOfNodeFound, searchResult.found);
                }
                case INCOMPLETE_MATCH_TO_END_OF_EDGE:
                    if (searchResult.charsMatched == 0) {
                        // Closest match is the root node, we don't consider this a match for anything...
                        break;
                    }
                    // Example: if we searched for COFFEE, but deepest matching node was CO,
                    // the results should include node CO and its descendants...
                    AbstractBytes keyOfNodeFound = getPrefix(candidate, searchResult.charsMatched);
                    return getDescendantValues(keyOfNodeFound, searchResult.found);
            }
            return Collections.emptySet();
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    static AbstractBytes getPrefix(AbstractBytes input, int endIndex) {
        return endIndex > input.length() ? input : input.subSequence(0, endIndex);
    }

    public static AbstractBytes getSuffix(AbstractBytes input, int startIndex) {
        return (startIndex >= input.length() ? AbstractBytes.EMPTY : input.subSequence(startIndex, input.length()));
    }


    static AbstractBytes concatenate(AbstractBytes a, AbstractBytes b) {
        int aLen = a.length();
        int bLen = b.length();
        byte[] c = new byte[aLen + bLen];
        a.toArray(c, 0);
        b.toArray(c, aLen);
        return new ArrayBytes(c);
    }


    /**
     * {@inheritDoc}
     */
    public Iterable<Pair<AbstractBytes, Object>> getKeyValuePairsForClosestKeys(AbstractBytes candidate) {
        acquireReadLockIfNecessary();
        try {
            SearchResult searchResult = searchTree(candidate);
            SearchResult.Classification classification = searchResult.classification;
            switch (classification) {
                case EXACT_MATCH:
                    return getDescendantKeyValuePairs(candidate, searchResult.found);
                case KEY_ENDS_MID_EDGE:
                    // Append the remaining characters of the edge to the key.
                    // For example if we searched for CO, but first matching node was COFFEE,
                    // the key associated with the first node should be COFFEE...
                    AbstractBytes edgeSuffix = getSuffix(searchResult.found.getIncomingEdge(), searchResult.charsMatchedInNodeFound);
                    candidate = concatenate(candidate, edgeSuffix);
                    return getDescendantKeyValuePairs(candidate, searchResult.found);
                case INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE: {
                    // Example: if we searched for CX, but deepest matching node was CO,
                    // the results should include node CO and its descendants...
                    AbstractBytes keyOfParentNode = getPrefix(candidate, searchResult.charsMatched - searchResult.charsMatchedInNodeFound);
                    AbstractBytes keyOfNodeFound = concatenate(keyOfParentNode, searchResult.found.getIncomingEdge());
                    return getDescendantKeyValuePairs(keyOfNodeFound, searchResult.found);
                }
                case INCOMPLETE_MATCH_TO_END_OF_EDGE:
                    if (searchResult.charsMatched == 0) {
                        // Closest match is the root node, we don't consider this a match for anything...
                        break;
                    }
                    // Example: if we searched for COFFEE, but deepest matching node was CO,
                    // the results should include node CO and its descendants...
                    AbstractBytes keyOfNodeFound = getPrefix(candidate, searchResult.charsMatched);
                    return getDescendantKeyValuePairs(keyOfNodeFound, searchResult.found);
            }
            return Collections.emptySet();
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return size(this.root);
    }

    public int size(Node n) {
        acquireReadLockIfNecessary();
        try {
            return _size(n);
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    public int sizeIfLessThan(Node n, int limit) {
        acquireReadLockIfNecessary();
        try {
            return _sizeIfLessThan(n, limit);
        } finally {
            releaseReadLockIfNecessary();
        }
    }

    private static int _size(Node n) {
        int sum = 0;
        Object v = n.getValue();
        if (aValue(v))
            sum++;

        FasterList<Node> l = n.getOutgoingEdges();
        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            sum += _size(l.get(i));
        }

        return sum;
    }

    /**
     * as soon as the limit is exceeded, it returns -1 to cancel the recursion iteration
     */
    private static int _sizeIfLessThan(Node n, int limit) {
        int sum = 0;
        Object v = n.getValue();
        if (aValue(v))
            sum++;

        FasterList<Node> l = n.getOutgoingEdges();
        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            int s = _size(l.get(i));
            if (s < 0)
                return -1; //cascade
            sum += s;
            if (sum > limit)
                return -1;
        }

        return sum;
    }

    /**
     * estimated size
     */
    public int sizeEst() {
        return estSize.get();
    }

    @Override
    public void forEach(Consumer<? super X> action) {
        forEach(this.root, action);
    }

    public final void forEach(Node start, Consumer<? super X> action) {
        Object v = start.getValue();
        if (aValue(v))
            action.accept((X) v);

        FasterList<Node> l = start.getOutgoingEdges();
        for (Node child : l)
            forEach(child, action);
    }

    public final void forEach(Node start, BiConsumer<AbstractBytes, ? super X> action) {
        Object v = start.getValue();
        if (aValue(v))
            action.accept(start.getIncomingEdge(), (X) v);

        FasterList<Node> l = start.getOutgoingEdges();
        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            forEach(l.get(i), action);
        }
    }

    public static boolean aValue(Object v) {
        return (v != null) && v != VoidValue.SINGLETON;
    }

    // ------------- Helper method for put() -------------
    Object putInternal(CharSequence key, Object value, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public SearchResult random(float descendProb, Random rng) {
        return random(root, null, null, descendProb, rng);
    }

    @NotNull
    public SearchResult random(Node subRoot, float descendProb, Random rng) {
        return random(subRoot, root, null, descendProb, rng);
    }

    @NotNull
    public SearchResult random(@NotNull SearchResult at, float descendProb, Random rng) {
        Node current, parent, parentParent;
        //if (at != null && at.found != null) {
        current = at.found;
        parent = at.parentNode;
        parentParent = at.parentNodesParent;
        return random(current, parent, parentParent, descendProb, rng);
    }

    @NotNull
    public SearchResult random(Node current, Node parent, Node parentParent, float descendProb, Random rng) {

        //}

        while (true) {
            FasterList<Node> c = current.getOutgoingEdges();
            int s = c.size();
            if (s == 0) {
                break; //select it
            } else {
                if (rng.nextFloat() < descendProb) {
                    int which = rng.nextInt(s);
                    Node next = c.get(which);

                    parentParent = parent;
                    parent = current;
                    current = next;
                } else {
                    break; //select it
                }
            }
        }

        return new SearchResult(current, parent, parentParent);
    }


    public interface QuadFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    /**
     * Atomically adds the given value to the tree, creating a node for the value as necessary. If the value is already
     * stored for the same key, either overwrites the existing value, or simply returns the existing value, depending
     * on the given value of the <code>overwrite</code> flag.
     *
     * @param key       The key against which the value should be stored
     * @param newValue  The value to store against the key
     * @param overwrite If true, should replace any existing value, if false should not replace any existing value
     * @return The existing value for this key, if there was one, otherwise null
     */
    <V> X compute(@NotNull AbstractBytes key, V value, QuadFunction<AbstractBytes, SearchResult, X, V, X> computeFunc) {
//        if (key.length() == 0) {
//            throw new IllegalArgumentException("The key argument was zero-length");
//        }


        int version;
        X newValue, foundX;
        SearchResult result;
        int matched;
        Object foundValue;
        Node found;

        version = writes.intValue();

        acquireReadLockIfNecessary();
        try {

            // Note we search the tree here after we have acquired the write lock...
            result = searchTree(key);
            found = result.found;
            matched = result.charsMatched;
            foundValue = found != null ? found.getValue() : null;
            foundX = ((matched == key.length()) && (foundValue != VoidValue.SINGLETON)) ? ((X) foundValue) : null;
        } finally {
            releaseReadLockIfNecessary();
        }

        newValue = computeFunc.apply(key, result, foundX, value);

        if (newValue != foundX) {

            int version2 = acquireWriteLock();
            try {

                if (version + 1 != version2) {
                    //search again because the tree has changed since the initial lookup
                    result = searchTree(key);
                    found = result.found;
                    matched = result.charsMatched;
                    foundValue = found != null ? found.getValue() : null;
                    foundX = ((matched == key.length()) && (foundValue != VoidValue.SINGLETON)) ? ((X) foundValue) : null;
                    if (foundX == newValue)
                        return newValue; //no change; the requested value has already been supplied since the last access
                }

                SearchResult.Classification classification = result.classification;

                if (foundX == null)
                    estSize.incrementAndGet();

                FasterList<Node> oedges = found.getOutgoingEdges();
                switch (classification) {
                    case EXACT_MATCH:
                        // Search found an exact match for all edges leading to this node.
                        // -> Add or update the value in the node found, by replacing
                        // the existing node with a new node containing the value...

                        // First check if existing node has a value, and if we are allowed to overwrite it.
                        // Return early without overwriting if necessary...

                        if (newValue != foundValue) {
                            //clone and reattach
                            cloneAndReattach(result, found, foundValue, oedges);
                        }
                        break;
                    case KEY_ENDS_MID_EDGE: {
                        // Search ran out of characters from the key while in the middle of an edge in the node.
                        // -> Split the node in two: Create a new parent node storing the new value,
                        // and a new child node holding the original value and edges from the existing node...
                        AbstractBytes keyCharsFromStartOfNodeFound = key.subSequence(matched - result.charsMatchedInNodeFound, key.length());
                        AbstractBytes commonPrefix = getCommonPrefix(keyCharsFromStartOfNodeFound, found.getIncomingEdge());
                        AbstractBytes suffixFromExistingEdge = subtractPrefix(found.getIncomingEdge(), commonPrefix);


                        // Create new nodes...
                        Node newChild = createNode(suffixFromExistingEdge, foundValue, oedges, false);

                        Node newParent = createNode(commonPrefix, newValue, new FasterList(new Node[] { newChild }), false);

                        // Add the new parent to the parent of the node being replaced (replacing the existing node)...
                        result.parentNode.updateOutgoingEdge(newParent);

                        break;
                    }
                    case INCOMPLETE_MATCH_TO_END_OF_EDGE:
                        // Search found a difference in characters between the key and the start of all child edges leaving the
                        // node, the key still has trailing unmatched characters.
                        // -> Add a new child to the node, containing the trailing characters from the key.

                        // NOTE: this is the only branch which allows an edge to be added to the root.
                        // (Root node's own edge is "" empty string, so is considered a prefixing edge of every key)

                        // Create a new child node containing the trailing characters...
                        AbstractBytes keySuffix = key.subSequence(matched, key.length());

                        Node newChild = createNode(keySuffix, newValue, emptyList, false);

                        // Clone the current node adding the new child...
                        int numEdges = oedges.size();
                        Node[] edgesArray;
                        if (numEdges > 0) {
                            edgesArray = new Node[numEdges+1];
                            arraycopy(oedges.array(), 0, edgesArray, 0, numEdges);
                            edgesArray[numEdges] = newChild;
                        } else {
                            edgesArray = new Node[] { newChild };
                        }

                        cloneAndReattach(result, found, foundValue, new FasterList(edgesArray.length, edgesArray));

                        break;

                    case INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE:
                        // Search found a difference in characters between the key and the characters in the middle of the
                        // edge in the current node, and the key still has trailing unmatched characters.
                        // -> Split the node in three:
                        // Let's call node found: NF
                        // (1) Create a new node N1 containing the unmatched characters from the rest of the key, and the
                        // value supplied to this method
                        // (2) Create a new node N2 containing the unmatched characters from the rest of the edge in NF, and
                        // copy the original edges and the value from NF unmodified into N2
                        // (3) Create a new node N3, which will be the split node, containing the matched characters from
                        // the key and the edge, and add N1 and N2 as child nodes of N3
                        // (4) Re-add N3 to the parent node of NF, effectively replacing NF in the tree

                        AbstractBytes suffixFromKey = key.subSequence(matched, key.length());

                        // Create new nodes...
                        Node n1 = createNode(suffixFromKey, newValue, emptyList, false);

                        AbstractBytes keyCharsFromStartOfNodeFound = key.subSequence(matched - result.charsMatchedInNodeFound, key.length());
                        AbstractBytes commonPrefix = getCommonPrefix(keyCharsFromStartOfNodeFound, found.getIncomingEdge());
                        AbstractBytes suffixFromExistingEdge = subtractPrefix(found.getIncomingEdge(), commonPrefix);

                        Node n2 = createNode(suffixFromExistingEdge, foundValue, oedges, false);
                        @SuppressWarnings("NullableProblems")
                        Node n3 = createNode(commonPrefix, null, new FasterList(2, new Node[] { n1, n2} ), false);

                        result.parentNode.updateOutgoingEdge(n3);

                        // Return null for the existing value...
                        break;

                    default:
                        // This is a safeguard against a new enum constant being added in future.
                        throw new IllegalStateException("Unexpected classification for search result: " + result);
                }
            } finally {
                releaseWriteLock();
            }
        }

        return newValue;
    }

    private void cloneAndReattach(SearchResult searchResult, Node found, Object foundValue, FasterList<Node> edges) {
        AbstractBytes ie = found.getIncomingEdge();
        boolean root = ie.length() == 0;

        Node clonedNode = createNode(ie, foundValue, edges, root);

        // Re-add the cloned node to its parent node...
        if (root) {
            this.root = clonedNode;
        } else {
            searchResult.parentNode.updateOutgoingEdge(clonedNode);
        }
    }

    // ------------- Helper method for finding descendants of a given node -------------

    /**
     * Returns a lazy iterable which will return {@link CharSequence} keys for which the given key is a prefix.
     * The results inherently will not contain duplicates (duplicate keys cannot exist in the tree).
     * <p/>
     * Note that this method internally converts {@link CharSequence}s to {@link String}s, to avoid set equality issues,
     * because equals() and hashCode() are not specified by the CharSequence API contract.
     */
    @SuppressWarnings("JavaDoc")
    Iterable<AbstractBytes> getDescendantKeys(final AbstractBytes startKey, final Node startNode) {
        return new DescendantKeys(startKey, startNode);
    }

    /**
     * Returns a lazy iterable which will return values which are associated with keys in the tree for which
     * the given key is a prefix.
     */
    @SuppressWarnings("JavaDoc")
    <O> Iterable<O> getDescendantValues(final AbstractBytes startKey, final Node startNode) {
        return new Iterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return new LazyIterator<O>() {
                    Iterator<NodeKeyPair> descendantNodes = lazyTraverseDescendants(startKey, startNode).iterator();

                    @Override
                    protected O computeNext() {
                        // Traverse to the next matching node in the tree and return its key and value...
                        while (descendantNodes.hasNext()) {
                            NodeKeyPair nodeKeyPair = descendantNodes.next();
                            Object value = nodeKeyPair.node.getValue();
                            if (value != null) {
                                // Dealing with a node explicitly added to tree (rather than an automatically-added split node).

                                // We have to cast to generic type here, because Node objects are not generically typed.
                                // Background: Node objects are not generically typed, because arrays can't be generically typed,
                                // and we use arrays in nodes. We choose to cast here (in wrapper logic around the tree) rather than
                                // pollute the already-complex tree manipulation logic with casts.
                                @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
                                O valueTyped = (O) value;
                                return valueTyped;
                            }
                        }
                        // Finished traversing the tree, no more matching nodes to return...
                        return endOfData();
                    }
                };
            }
        };
    }

    /**
     * Returns a lazy iterable which will return {@link KeyValuePair} objects each containing a key and a value,
     * for which the given key is a prefix of the key in the {@link KeyValuePair}. These results inherently will not
     * contain duplicates (duplicate keys cannot exist in the tree).
     * <p/>
     * Note that this method internally converts {@link CharSequence}s to {@link String}s, to avoid set equality issues,
     * because equals() and hashCode() are not specified by the CharSequence API contract.
     */
    @SuppressWarnings("JavaDoc")
    <O> Iterable<Pair<AbstractBytes, O>> getDescendantKeyValuePairs(final AbstractBytes startKey, final Node startNode) {
        return new Iterable<Pair<AbstractBytes, O>>() {
            @Override
            public Iterator<Pair<AbstractBytes, O>> iterator() {
                return new LazyIterator<Pair<AbstractBytes, O>>() {
                    Iterator<NodeKeyPair> descendantNodes = lazyTraverseDescendants(startKey, startNode).iterator();

                    @Override
                    protected Pair<AbstractBytes, O> computeNext() {
                        // Traverse to the next matching node in the tree and return its key and value...
                        while (descendantNodes.hasNext()) {
                            NodeKeyPair nodeKeyPair = descendantNodes.next();
                            Object value = nodeKeyPair.node.getValue();
                            if (value != null) {
                                // Dealing with a node explicitly added to tree (rather than an automatically-added split node).

                                // Call the transformKeyForResult method to allow key to be transformed before returning to client.
                                // Used by subclasses such as ReversedRadixTree implementations...

                                return pair(transformKeyForResult(nodeKeyPair.key), (O) value);
                            }
                        }
                        // Finished traversing the tree, no more matching nodes to return...
                        return endOfData();
                    }
                };
            }
        };
    }


    /**
     * Traverses the tree using depth-first, preordered traversal, starting at the given node, using lazy evaluation
     * such that the next node is only determined when next() is called on the iterator returned.
     * The traversal algorithm uses iteration instead of recursion to allow deep trees to be traversed without
     * requiring large JVM stack sizes.
     * <p/>
     * Each node that is encountered is returned from the iterator along with a key associated with that node,
     * in a NodeKeyPair object. The key will be prefixed by the given start key, and will be generated by appending
     * to the start key the edges traversed along the path to that node from the start node.
     *
     * @param startKey  The key which matches the given start node
     * @param startNode The start node
     * @return An iterator which when iterated traverses the tree using depth-first, preordered traversal,
     * starting at the given start node
     */
    protected Iterable<NodeKeyPair> lazyTraverseDescendants(final AbstractBytes startKey, final Node startNode) {
        return new Iterable<NodeKeyPair>() {
            @Override
            public Iterator<NodeKeyPair> iterator() {
                return new LazyIterator<NodeKeyPair>() {

                    final Deque<NodeKeyPair> stack =
                            //new LinkedList<NodeKeyPair>();
                            new ArrayDeque();

                    {
                        stack.push(new NodeKeyPair(startNode, startKey));
                    }

                    @Override
                    protected NodeKeyPair computeNext() {
                        Deque<NodeKeyPair> stack = this.stack;

                        if (stack.isEmpty()) {
                            return endOfData();
                        }
                        NodeKeyPair current = stack.pop();
                        FasterList<Node> childNodes = current.node.getOutgoingEdges();

                        // -> Iterate child nodes in reverse order and so push them onto the stack in reverse order,
                        // to counteract that pushing them onto the stack alone would otherwise reverse their processing order.
                        // This ensures that we actually process nodes in ascending alphabetical order.
                        for (int i = childNodes.size() - 1; i >= 0; i--) {
                            Node child = childNodes.get(i);
                            stack.push(new NodeKeyPair(child,
                                    concatenate(current.key, child.getIncomingEdge())
                            ));
                        }
                        return current;
                    }
                };
            }
        };
    }


    /**
     * Encapsulates a node and its associated key. Used internally by {@link #lazyTraverseDescendants}.
     */
    protected static final class NodeKeyPair {
        public final Node node;
        public final AbstractBytes key;

        public NodeKeyPair(Node node, AbstractBytes key) {
            this.node = node;
            this.key = key;
        }
    }

    /**
     * A hook method which may be overridden by subclasses, to transform a key just before it is returned to
     * the application, for example by the {@link #getKeysStartingWith(CharSequence)} or the
     * {@link #getKeyValuePairsForKeysStartingWith(CharSequence)} methods.
     * <p/>
     * This hook is expected to be used by  {@link com.googlecode.concurrenttrees.radixreversed.ReversedRadixTree}
     * implementations, where keys are stored in the tree in reverse order but results should be returned in normal
     * order.
     * <p/>
     * <b>This default implementation simply returns the given key unmodified.</b>
     *
     * @param rawKey The raw key as stored in the tree
     * @return A transformed version of the key
     */
    protected AbstractBytes transformKeyForResult(AbstractBytes rawKey) {
        return rawKey;
    }


    // ------------- Helper method for searching the tree and associated SearchResult object -------------

    /**
     * Traverses the tree and finds the node which matches the longest prefix of the given key.
     * <p/>
     * The node returned might be an <u>exact match</u> for the key, in which case {@link SearchResult#charsMatched}
     * will equal the length of the key.
     * <p/>
     * The node returned might be an <u>inexact match</u> for the key, in which case {@link SearchResult#charsMatched}
     * will be less than the length of the key.
     * <p/>
     * There are two types of inexact match:
     * <ul>
     * <li>
     * An inexact match which ends evenly at the boundary between a node and its children (the rest of the key
     * not matching any children at all). In this case if we we wanted to add nodes to the tree to represent the
     * rest of the key, we could simply add child nodes to the node found.
     * </li>
     * <li>
     * An inexact match which ends in the middle of a the characters for an edge stored in a node (the key
     * matching only the first few characters of the edge). In this case if we we wanted to add nodes to the
     * tree to represent the rest of the key, we would have to split the node (let's call this node found: NF):
     * <ol>
     * <li>
     * Create a new node (N1) which will be the split node, containing the matched characters from the
     * start of the edge in NF
     * </li>
     * <li>
     * Create a new node (N2) which will contain the unmatched characters from the rest of the edge
     * in NF, and copy the original edges from NF unmodified into N2
     * </li>
     * <li>
     * Create a new node (N3) which will be the new branch, containing the unmatched characters from
     * the rest of the key
     * </li>
     * <li>
     * Add N2 as a child of N1
     * </li>
     * <li>
     * Add N3 as a child of N1
     * </li>
     * <li>
     * In the <b>parent node of NF</b>, replace the edge pointing to NF with an edge pointing instead
     * to N1. If we do this step atomically, reading threads are guaranteed to never see "invalid"
     * data, only either the old data or the new data
     * </li>
     * </ol>
     * </li>
     * </ul>
     * The {@link SearchResult#classification} is an enum value based on its classification of the
     * match according to the descriptions above.
     *
     * @param key a key for which the node matching the longest prefix of the key is required
     * @return A {@link SearchResult} object which contains the node matching the longest prefix of the key, its
     * parent node, the number of characters of the key which were matched in total and within the edge of the
     * matched node, and a {@link SearchResult#classification} of the match as described above
     */
    SearchResult searchTree(AbstractBytes key) {
        Node parentNodesParent = null;
        Node parentNode = null;
        Node currentNode = root;
        int charsMatched = 0, charsMatchedInNodeFound = 0;

        final int keyLength = key.length();
        outer_loop:
        while (charsMatched < keyLength) {
            Node nextNode = currentNode.getOutgoingEdge(key.at(charsMatched));
            if (nextNode == null) {
                // Next node is a dead end...
                //noinspection UnnecessaryLabelOnBreakStatement
                break outer_loop;
            }

            parentNodesParent = parentNode;
            parentNode = currentNode;
            currentNode = nextNode;
            charsMatchedInNodeFound = 0;
            AbstractBytes currentNodeEdgeCharacters = currentNode.getIncomingEdge();
            for (int i = 0, numEdgeChars = currentNodeEdgeCharacters.length(); i < numEdgeChars && charsMatched < keyLength; i++) {
                if (currentNodeEdgeCharacters.at(i) != key.at(charsMatched)) {
                    // Found a difference in chars between character in key and a character in current node.
                    // Current node is the deepest match (inexact match)....
                    break outer_loop;
                }
                charsMatched++;
                charsMatchedInNodeFound++;
            }
        }
        return new SearchResult(key, currentNode, charsMatched, charsMatchedInNodeFound, parentNode, parentNodesParent);
    }

    /**
     * Encapsulates results of searching the tree for a node for which a given key is a prefix. Encapsulates the node
     * found, its parent node, its parent's parent node, and the number of characters matched in the current node and
     * in total.
     * <p/>
     * Also classifies the search result so that algorithms in methods which use this SearchResult, when adding nodes
     * and removing nodes from the tree, can select appropriate strategies based on the classification.
     */
    public static final class SearchResult {
        public final AbstractBytes key;
        public final Node found;
        public final int charsMatched;
        public final int charsMatchedInNodeFound;
        public final Node parentNode;
        public final Node parentNodesParent;
        public final Classification classification;

        enum Classification {
            EXACT_MATCH,
            INCOMPLETE_MATCH_TO_END_OF_EDGE,
            INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE,
            KEY_ENDS_MID_EDGE,
            INVALID // INVALID is never used, except in unit testing
        }

        public SearchResult(Node found, Node parentNode, Node parentParentNode) {
            this(null, found, -1, -1, parentNode, parentParentNode, found != null ? Classification.EXACT_MATCH : Classification.INVALID);
        }

        SearchResult(AbstractBytes key, Node found, int charsMatched, int charsMatchedInNodeFound, Node parentNode, Node parentNodesParent) {
            this(key, found, charsMatched, charsMatchedInNodeFound, parentNode, parentNodesParent, classify(key, found, charsMatched, charsMatchedInNodeFound));
        }

        SearchResult(AbstractBytes key, Node found, int charsMatched, int charsMatchedInNodeFound, Node parentNode, Node parentNodesParent, Classification c) {
            this.key = key;
            this.found = found;
            this.charsMatched = charsMatched;
            this.charsMatchedInNodeFound = charsMatchedInNodeFound;
            this.parentNode = parentNode;
            this.parentNodesParent = parentNodesParent;

            // Classify this search result...
            this.classification = c;
        }

        protected static SearchResult.Classification classify(AbstractBytes key, Node nodeFound, int charsMatched, int charsMatchedInNodeFound) {
            int len = nodeFound.getIncomingEdge().length();
            int keyLen = key.length();
            if (charsMatched == keyLen) {
                if (charsMatchedInNodeFound == len) {
                    return SearchResult.Classification.EXACT_MATCH;
                } else if (charsMatchedInNodeFound < len) {
                    return SearchResult.Classification.KEY_ENDS_MID_EDGE;
                }
            } else if (charsMatched < keyLen) {
                if (charsMatchedInNodeFound == len) {
                    return SearchResult.Classification.INCOMPLETE_MATCH_TO_END_OF_EDGE;
                } else if (charsMatchedInNodeFound < len) {
                    return SearchResult.Classification.INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE;
                }
            }
            throw new IllegalStateException("Unexpected failure to classify SearchResult");
        }

        @Override
        public String toString() {
            return "SearchResult{" +
                    "key=" + key +
                    ", nodeFound=" + found +
                    ", charsMatched=" + charsMatched +
                    ", charsMatchedInNodeFound=" + charsMatchedInNodeFound +
                    ", parentNode=" + parentNode +
                    ", parentNodesParent=" + parentNodesParent +
                    ", classification=" + classification +
                    '}';
        }
    }


    private class DescendantKeys extends LazyIterator<AbstractBytes> implements Iterable<AbstractBytes> {
        private final AbstractBytes startKey;
        private final Node startNode;
        private Iterator<NodeKeyPair> descendantNodes;

        public DescendantKeys(AbstractBytes startKey, Node startNode) {
            this.startKey = startKey;
            this.startNode = startNode;
        }

        @Override
        public Iterator<AbstractBytes> iterator() {
            descendantNodes = lazyTraverseDescendants(startKey, startNode).iterator();
            return this;
        }

        @Override
        protected AbstractBytes computeNext() {
            // Traverse to the next matching node in the tree and return its key and value...
            Iterator<NodeKeyPair> nodes = this.descendantNodes;
            while (nodes.hasNext()) {
                NodeKeyPair nodeKeyPair = nodes.next();
                Object value = nodeKeyPair.node.getValue();
                if (value != null) {
                    // Dealing with a node explicitly added to tree (rather than an automatically-added split node).

                    // Call the transformKeyForResult method to allow key to be transformed before returning to client.
                    // Used by subclasses such as ReversedRadixTree implementations...
                    AbstractBytes optionallyTransformedKey = transformKeyForResult(nodeKeyPair.key);

                    // -> Convert the CharSequence to a String before returning, to avoid set equality issues,
                    // because equals() and hashCode() is not specified by the CharSequence API contract...
                    return optionallyTransformedKey;
                }
            }
            // Finished traversing the tree, no more matching nodes to return...
            return endOfData();
        }


    }

//    public void forEach(Consumer<? super O> c) {
//        //TODO rewrite as pure forEach visitor
//        this.forEach(c);
//    }

    @Override
    public Iterator<X> iterator() {
        return getValuesForKeysStartingWith(AbstractBytes.EMPTY).iterator();
    }


    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrint(root, sb, "", true, true);
        return sb.toString();
    }

    public void prettyPrint(Appendable appendable) {
        prettyPrint(root, appendable, "", true, true);
    }

    static void prettyPrint(Node node, Appendable sb, String prefix, boolean isTail, boolean isRoot) {
        try {
            StringBuilder ioException = new StringBuilder();
            if (isRoot) {
                ioException.append("○");
                if (node.getIncomingEdge().length() > 0) {
                    ioException.append(" ");
                }
            }

            ioException.append(node.getIncomingEdge());
            if (node.getValue() != null) {
                ioException.append(" (").append(node.getValue()).append(")");
            }

            sb.append(prefix).append(isTail ? (isRoot ? "" : "└── ○ ") : "├── ○ ").append(ioException).append("\n");
            List children = node.getOutgoingEdges();

            for (int i = 0; i < children.size() - 1; ++i) {
                prettyPrint((Node) children.get(i), sb, prefix + (isTail ? (isRoot ? "" : "    ") : "│   "), false, false);
            }

            if (!children.isEmpty()) {
                prettyPrint((Node) children.get(children.size() - 1), sb, prefix + (isTail ? (isRoot ? "" : "    ") : "│   "), true, false);
            }

        } catch (IOException var8) {
            throw new IllegalStateException(var8);
        }
    }

}
