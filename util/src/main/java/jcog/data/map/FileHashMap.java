//package jcog.data.map;
//
//
//import com.google.common.collect.Iterators;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Function;
//
///**
// * from: https://github.com/bmc/javautil
// * <p>
// * <p><tt>FileHashMap</tt> implements a <tt>java.util.Map</tt> that keeps
// * the keys in memory, but stores the values as serialized objects in a
// * random access disk file. When an application attempts to access the
// * value associated with a given key, the <tt>FileHashMap</tt> object
// * determines the location of the serialized value object, seeks to that
// * location in the random access file, and reads and deserializes the value
// * object. In a sense, a <tt>FileHashMap</tt> is akin to a simple, classic
// * indexed sequential file. This approach gives a <tt>FileHashMap</tt>
// * object the following characteristics:</p>
// * <p>
// * <ul>
// * <li>Because the map keys are cached in memory, access to the key space is
// * relatively efficient.
// * <li>Since the values are stored in a file, access to the values is
// * slower than if they were in memory, but you can store a lot
// * more data in a <tt>FileHashMap</tt> object than in a wholly
// * memory-resident object, such as a <tt>HashMap</tt> or
// * <tt>TreeMap</tt>.
// * </ul>
// * <p>
// * <p><b>File Name Conventions</b></p>
// * <p>
// * <p>A <tt>FileHashMap</tt> is instantiated with a base file name (i.e., a
// * file name without an extension). This base file name specifies the path
// * to the file(s) that used to store the map's data; the
// * <tt>FileHashMap</tt> tacks the following extensions onto the prefix to
// * arrive at the actual file names.</p>
// * <p>
// * <blockquote>
// * <table>
// * <caption>Extensions and Meanings</caption>
// * <tr>
// * <th>Extension</th>
// * <th>Meaning</th>
// * </tr>
// * <p>
// * <tr>
// * <td>.ix</td>
// * <td>The saved in-memory index. This file is created only if the
// * <tt>FileHashMap</tt> is not marked as transient. (See below.)
// * The {@link #INDEX_FILE_SUFFIX <tt>INDEX_FILE_SUFFIX</tt>}
// * constant defines this string.</td>
// * </tr>
// * <p>
// * <tr>
// * <td>.db</td>
// * <td>The on-disk data (value) file, where the serialized objects are
// * stored.
// * The {@link #DATA_FILE_SUFFIX <tt>DATA_FILE_SUFFIX</tt>}
// * constant defines this string.</td>
// * </tr>
// * </table>
// * </blockquote>
// * <p>
// * <p>For instance, if you create a <tt>FileHashMap</tt> with the
// * statement</p>
// * <p>
// * <blockquote><pre>
// * Map map = new FileHashMap("/tmp/mymap");
// * </pre></blockquote>
// * <p>
// * <p>the serialized value objects will be stored in file "/tmp/mymap.db",
// * and the index (if saved) will be stored in "/tmp/mymap.ix".</p>
// * <p>
// * <p><b>Transient versus Persistent Maps</b></p>
// * <p>
// * <p>A <tt>FileHashMap</tt> is persistent by default. When a
// * <tt>FileHashMap</tt> object is finalized or explicitly closed
// * (using the {@link #close close()} method), its in-memory index is saved
// * to disk. You can reopen the saved map by instantiating another
// * <tt>FileHashMap</tt> object, and specifying the same file prefix. The
// * new <tt>FileHashMap</tt> object will load its initial in-memory index
// * from the saved index; any modifications to the new object will be
// * written back to the on-disk index with the new object finalized or
// * closed.</p>
// * <p>
// * <p>A <tt>FileHashMap</tt> can be marked as non-persistent, or transient,
// * by passing the {@link #TRANSIENT <tt>TRANSIENT</tt>} flag to the
// * constructor. A transient map is not saved; its disk files are
// * removed when the map is finalized or manually closed. You cannot create
// * a transient map using a file prefix that specifies an existing, saved
// * map. That is, the disk files for a transient map must not exist at the
// * time the map is created.</p>
// * <p>
// * <p><b>Optimizations</b></p>
// * <p>
// * <p>The <tt>FileHashMap</tt> class attempts to optimize access to the
// * disk-resident values and to conserve memory use. These optimizations
// * include the following specific measures.</p>
// * <p>
// * <p><u>Sequential access to values</u></p>
// * <p>
// * <p>The iterators attempt to minimize file seeking while looping over the
// * stored values. The {@link #values values()} method returns a
// * <tt>Set</tt> whose iterator loops through the values in the order they
// * were written to the data file. Traversing the value set via the iterator
// * will access the <tt>FileHashMap</tt>'s data file sequentially, from top
// * to bottom. For example, the following code fragment loops through a
// * <tt>FileHashMap</tt>'s values; because the iterator returns the values
// * in the order they appear in the data file, the code fragment accesses
// * the data file sequentially:</p>
// * <p>
// * <blockquote><pre>
// * FileHashMap map = new FileHashMap("myfile");
// * <p>
// * for (Iterator values = map.values().iterator(); values.hasNext(); )
// * {
// *     Object value = it.next();
// *     System.out.println(value);
// * }
// * </pre></blockquote>
// * <p>
// * <p>Similarly, the <tt>Set</tt> objects returned by the
// * {@link #keySet keySet()} and {@link #entrySet entrySet()}
// * methods use iterators that return the keys in the order that
// * the associated values appear in the disk file. For example, the following
// * code fragment also accesses the data file sequentially:</p>
// * <p>
// * <blockquote><pre>
// * FileHashMap map = new FileHashMap("myfile");
// * <p>
// * for (Iterator keys = map.keySet().iterator(); keys.hasNext(); )
// * {
// *     Object key   = it.next();
// *     Object value = map.get(key);
// *     System.out.println("key=" + key + ", value=" + value);
// * }
// * </pre></blockquote>
// * <p>
// * <p>Note that this optimization strategy can be foiled in a number of
// * ways. For instance, the sequential access behavior can be thwarted if a
// * second thread is accessing the map while the first thread is iterating
// * over it, or if the thread that's iterating over the values is
// * simultaneously inserting values in the table.</p>
// * <p>
// * <p>Accessing the keys, without reading the values, does not result in
// * any file access at all, because the keys are cached in memory. See
// * the next section for more details.</p>
// * <p>
// * <p><u>Memory Conservation</u></p>
// * <p>
// * <p>The values stored in the map are serialized and written to a data
// * file; they are not cached in memory at all. Therefore, you can store
// * a lot more objects in a <tt>FileHashMap</tt> before running out of
// * memory (assuming you have enough disk space available).</p>
// * <p>
// * <p>The keys <i>are</i> stored in memory, however. Each key is associated
// * with a special internal object that keeps track of the location and
// * length of the associated value in the disk file. As you place more items
// * in a <tt>FileHashMap</tt>, the index will grow, consuming memory. But it
// * will consume far less memory than if you use an in-memory <tt>Map</tt>,
// * such as a <tt>java.util.HashMap</tt>, which stores the keys <i>and</i>
// * the values in memory.
// * <p>
// * <p>The <tt>Iterator</tt> and <tt>Set</tt> objects returned by the
// * {@link #entrySet entrySet()} and {@link #values values()}
// * methods contain proxy objects, not real values. That is, they do
// * <i>not</i> actually contain any values. Instead, they contain proxies
// * for the values, objects that specify the locations of the values in the
// * disk file. A value is loaded from disk only when you actually attempt to
// * retrieve it from the <tt>Iterator</tt> or <tt>Set</tt>.
// * <p>
// * <p><b>Reclaiming Gaps in the File</b></p>
// * <p>
// * <p>Normally, when you remove an object from the map, the space where the
// * object was stored in the data file is not reclaimed. This strategy allows
// * for faster insertions, since new objects are always added to the end of
// * the disk file. However, for long-lived <tt>FileHashMap</tt> objects that
// * are periodically modified, this strategy may not be appropriate. For that
// * reason, you can pass a special {@link #RECLAIM_FILE_GAPS} flag to the
// * constructor. If specified, the flag tells the object to keep track of
// * "gaps" in the file, and reuse them if possible. When a new object is
// * inserted into the map, and {@link #RECLAIM_FILE_GAPS} is enabled, the
// * object will attempt to find the smallest unused area in the file to
// * accommodate the new object. It will only add the new object to the end
// * of the file (enlarging the file) if it cannot find a suitable gap.</p>
// * <p>
// * <p>This mode is not the default, because it can add time to processing.
// * However, it does not access the file at all; the file gap maintenance
// * logic uses in-memory data only. So, while it adds a small amount of
// * computational overhead, the difference between running with
// * {@link #RECLAIM_FILE_GAPS} enabled and running with it disabled should not
// * be dramatic.</p>
// * <p>
// * <p><b>Restrictions</b></p>
// * <p>
// * <p>This class currently has the following restrictions and unimplemented
// * behavior.</p>
// * <p>
// * <ul>
// * <li>An object cannot be stored in a <tt>FileHashMap</tt> unless it
// * implements <tt>java.io.Serializable</tt>.
// * <li>The maximum size of a serialized stored object is confined to
// * a 32-bit integer. This restriction is unlikely to cause anyone
// * problems, and it keeps the keyspace down.
// * <li>To prevent multiple Java VMs from updating the file containing the
// * serialized values, this class detect throws an exception if it
// * detects an attempted concurrent modification of a map. (Locking
// * the map, then synchronizing the in-memory indexes across multiple
// * updaters, is non-trivial. The most obvious method that leaps to
// * mind, mapping the underlying <tt>java.nio.channels.FileChannel</tt>
// * object to map the index file into memory, isn't guaranteed to work.
// * Cccording to the JDK documentation, "Whether changes made to the
// * content or size of the underlying file, by this program or another,
// * are propagated to the [mapped memory] buffer is unspecified. The rate
// * at which changes to the buffer are propagated to the file is
// * unspecified.")
// * </ul>
// */
//public class FileHashMap<K, V> extends AbstractMap<K, V> {
//    /*----------------------------------------------------------------------*\
//                             Public Constants
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * Index file suffix.
//     */
//    public static final String INDEX_FILE_SUFFIX = ".ix";
//
//    /**
//     * Data file suffix.
//     */
//    public static final String DATA_FILE_SUFFIX = ".db";
//
//    /**
//     * Constructor flag value: If specified, the disk files will not be
//     * created if they don't exist; instead, the constructor will throw an
//     * exception. By default, if the files don't exist, the constructor
//     * creates them.
//     */
//    public static final int NO_CREATE = 0x01;
//
//    /**
//     * Constructor flag value: If specified, the on-disk hash table is
//     * considered to be transient and will be removed when this object is
//     * closed or finalized. Note that this flag cannot be combined with
//     * {@link #NO_CREATE}. Also, if <tt>TRANSIENT</tt> is specified to the
//     * constructor, but the on-disk files already exist, the constructor
//     * will throw an exception unless {@link #FORCE_OVERWRITE} is also
//     * specified.
//     */
//    public static final int TRANSIENT = 0x02;
//
//    /**
//     * Constructor flag value: Ignored unless {@link #TRANSIENT} is also
//     * specified, <tt>FORCE_OVERWRITE</tt> tells the constructor to
//     * overwrite the on-disk files if they already exist, instead of
//     * throwing an exception.
//     */
//    public static final int FORCE_OVERWRITE = 0x04;
//
//    /**
//     * Constructor flag value: Tells the object to reclaim unused space in
//     * the file, whenever possible. If this flag is not specified, then the
//     * space occupied by objects removed from the hash table is not
//     * reclaimed. This flag is not persistent. It's possible to create a
//     * persistent <tt>FileHashMap</tt> without using this flag, and later
//     * reopen the on-disk map via a new <tt>FileHashMap</tt> object that
//     * does have this flag set. Whenever the flag is set, the in-memory
//     * <tt>FileHashMap</tt> object attempts to reuse gaps in the file. When
//     * the flag is not set, the in-memory <tt>FileHashMap</tt> object
//     * ignores gaps in the file.
//     */
//    public static final int RECLAIM_FILE_GAPS = 0x08;
//
//    /*----------------------------------------------------------------------*\
//                             Private Constants
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * Version stamp, written to the index file. Used to detect invalid files
//     * and older incompatible versions.
//     */
//    private static final String VERSION_STAMP =
//            "org.clapper.util.misc.FileHashMap-1.0";
//
//    /**
//     * Used to validate the set of flags passed to the constructor. Negate
//     * this value, AND it with a passed-in flags value, and the result had
//     * better be 0. Otherwise, someone has set an unsupported flag value.
//     */
//    private static final int ALL_FLAGS_MASK = NO_CREATE
//            | TRANSIENT
//            | FORCE_OVERWRITE
//            | RECLAIM_FILE_GAPS;
//
//    /**
//     * initial size of an output write buffer
//     */
//    private static final int WRITE_BUFFER_SIZE = 16 * 1024;
//
//    /*----------------------------------------------------------------------*\
//                           Private Inner Classes
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * Stores the location of an on-disk serialized value. Used by
//     * <tt>FileHashMap</tt> for its in-memory index and returned sets and
//     * iterators. This class is not publicly accessible. It's stored in a
//     * separate file, because inner classes cannot be serialized.
//     */
//    static class FileHashMapEntry<K> implements Serializable, Comparable<FileHashMapEntry<?>> {
//    /*----------------------------------------------------------------------*\
//                         Private Static Variables
//    \*----------------------------------------------------------------------*/
//
//        /**
//         * See JDK 1.5 version of java.io.Serializable
//         */
//        private static final long serialVersionUID = 1L;
//
//    /*----------------------------------------------------------------------*\
//                            Private Data Items
//    \*----------------------------------------------------------------------*/
//
//        /**
//         * The file position.
//         */
//        private long filePosition = -1;
//
//        /**
//         * The length of the stored, serialized object
//         */
//        private int objectSize = -1;
//
//        /**
//         * The caller's key (i.e., the key the caller of FileHashMap.put()
//         * specified).
//         */
//        private K key;
//
//    /*----------------------------------------------------------------------*\
//                               Constructors
//    \*----------------------------------------------------------------------*/
//
//        /**
//         * Create a new <tt>FileHashMapEntry</tt> that records the location
//         * and length of an item stored in the data file portion of a
//         * <tt>FileHashMap</tt> obejct.
//         *
//         * @param pos  The object's file position. The object may or may not
//         *             actually have been written there yet.
//         * @param size The stored object's serialized size, if known, or -1
//         *             if the object has never been written. A non-negative
//         *             size value will typically be passed when an existing
//         *             <tt>FileHashMap</tt> is being reloaded from disk.
//         * @param key  The caller's key (i.e., the key the caller of
//         *             <tt>FileHashMap.put()</tt> specified). May be null.
//         * @see #getFilePosition
//         * @see #getObjectSize
//         * @see #setObjectSize
//         * @see FileHashMap#put
//         */
//        FileHashMapEntry(long pos, int size, K key) {
//            this.filePosition = pos;
//            this.objectSize = size;
//            this.key = key;
//        }
//
//        /**
//         * Create an entry with no associated key. Used primarily to record
//         * file gaps. In that case, the object size is really the gap size.
//         *
//         * @param pos  The object's file position. The object may or may not
//         *             actually have been written there yet.
//         * @param size The gap size.
//         * @see #getFilePosition
//         * @see #getObjectSize
//         * @see #setObjectSize
//         */
//        FileHashMapEntry(long pos, int size) {
//            this(pos, size, null);
//        }
//
//    /*----------------------------------------------------------------------*\
//                              Public Methods
//    \*----------------------------------------------------------------------*/
//
//        /**
//         * Compares this object with the specified object for order. Returns a
//         * negative integer, zero, or a positive integer as this object is less
//         * than, equal to, or greater than the specified object. The comparison
//         * key for a <tt>FileHashMapEntry</tt> is the file position value.
//         *
//         * @param o The other object
//         */
//        @Override
//        public int compareTo(FileHashMapEntry o) {
//            FileHashMapEntry other = o;
//            Long thisPos = this.filePosition;
//            Long otherPos = other.filePosition;
//
//            return thisPos.compareTo(otherPos);
//        }
//
//        /**
//         * Display a string version of the contents of this object. Mostly
//         * useful for debugging.
//         *
//         * @return a string representation of the contents of this object
//         */
//        public String toString() {
//            return ("FileHashMapEntry[filePosition=" +
//                    filePosition +
//                    ", objectSize=" +
//                    objectSize +
//                    ", key=" +
//                    ((key == null) ? "<null>" : key) +
//                    "]");
//        }
//
//    /*----------------------------------------------------------------------*\
//                          Package-visible Methods
//    \*----------------------------------------------------------------------*/
//
//        /**
//         * Get the caller's key (i.e., the key the caller passed to
//         * <tt>FileHashMap.put()</tt>).
//         *
//         * @return the key
//         * @see #setKey
//         * @see FileHashMap#put
//         */
//        K getKey() {
//            return key;
//        }
//
//        /**
//         * Change the key for this entry
//         *
//         * @param newKey the new key to use
//         * @see #getKey
//         */
//        void setKey(K newKey) {
//            this.key = newKey;
//        }
//
//        /**
//         * Get the file position for this entry.
//         *
//         * @return the file position
//         * @see #setFilePosition
//         */
//        long getFilePosition() {
//            return this.filePosition;
//        }
//
//        /**
//         * Set the file position for this entry.
//         *
//         * @param pos the new file position
//         * @see #getFilePosition
//         */
//        void setFilePosition(long pos) {
//            this.filePosition = pos;
//        }
//
//        /**
//         * Get the number of bytes the serialized object occupies in the
//         * random access file.
//         *
//         * @return the number of bytes occupied by the object
//         * @see #setObjectSize
//         */
//        int getObjectSize()
//                throws IllegalStateException {
//            assert (this.objectSize > 0) : "No object stored yet";
//            return this.objectSize;
//        }
//
//        /**
//         * Get the number of bytes the serialized object occupies in the
//         * random access file.
//         *
//         * @param size the number of bytes occupied by the object
//         * @see #getObjectSize
//         */
//        void setObjectSize(int size) {
//            this.objectSize = size;
//        }
//    }
//
//    /**
//     * Wraps the values data file and other administrative references related
//     * to it.
//     */
//    private static class ValuesFile {
//        private final RandomAccessFile file;
//
//        ValuesFile(File f) throws FileNotFoundException {
//            this.file = new RandomAccessFile(f, "rw");
//        }
//
//        RandomAccessFile getFile()
//                throws ConcurrentModificationException {
//            return file;
//        }
//
//        void close()
//                throws IOException {
//            // Lock is implicitly released on close.
//            file.close();
//        }
//    }
//
//    /**
//     * Comparator for FileHashMapEntry objects. Sorts by natural order,
//     * which is file position.
//     */
//    private class FileHashMapEntryComparator
//            implements Comparator<FileHashMapEntry<K>> {
//        private FileHashMapEntryComparator() {
//            // Nothing to do
//        }
//
//        @Override
//        public int compare(FileHashMapEntry<K> o1, FileHashMapEntry<K> o2) {
//            return o1.compareTo(o2);
//        }
//
//        public boolean equals(Object o) {
//            // This fail to compile with generics:
//            //
//            // return (o instanceof FileHashMapEntryComparator);
//            //
//            // But this works:
//
//            return (this.getClass().isInstance(o));
//        }
//
//    }
//
//    /**
//     * Comparator for FileHashMapEntry objects. Sorts by object size, then
//     * file position. Used for ordering gap entries.
//     */
//    private class FileHashMapEntryGapComparator
//            implements Comparator<FileHashMapEntry<K>> {
//        private FileHashMapEntryGapComparator() {
//            // Nothing to do
//        }
//
//        @Override
//        public int compare(FileHashMapEntry<K> o1, FileHashMapEntry<K> o2) {
//            int cmp;
//
//            if ((cmp = o1.getObjectSize() - o2.getObjectSize()) == 0)
//                cmp = (int) (o1.getFilePosition() - o2.getFilePosition());
//
//            return cmp;
//        }
//
//        public boolean equals(Object o) {
//            // This fail to compile with generics:
//            //
//            // return (o instanceof FileHashMapEntryGapComparator);
//            //
//            // But this works:
//
//            return (this.getClass().isInstance(o));
//        }
//
//    }
//
//    /**
//     * Internal iterator that loops through the FileHashMapEntry objects in
//     * sorted order, by file position. Used to implement other iterators.
//     */
//    private class EntryIterator implements Iterator<FileHashMapEntry<K>> {
//        final List<FileHashMapEntry<K>> entries;
//        final Iterator<FileHashMapEntry<K>> iterator;
//        FileHashMapEntry<K> currentEntry;
//
//        /**
//         * The expectedSize value that the iterator believes that the backing
//         * Map should have.  If this expectation is violated, the iterator
//         * has detected concurrent modification.
//         */
//        private final int expectedSize;
//
//        EntryIterator() {
//            entries = FileHashMap.this.getSortedEntries();
//            iterator = entries.iterator();
//            expectedSize = entries.size();
//        }
//
//        @Override
//        public boolean hasNext() {
//            return iterator.hasNext();
//        }
//
//        @Override
//        public FileHashMapEntry<K> next() {
//            if (expectedSize != FileHashMap.this.indexMap.size())
//                throw new ConcurrentModificationException();
//
//            if (hasNext())
//                currentEntry = iterator.next();
//
//            else {
//                currentEntry = null;
//                throw new NoSuchElementException();
//            }
//
//            return currentEntry;
//        }
//
//        @Override
//        public void remove() {
//            if ((currentEntry != null) && (expectedSize > 0)) {
//                K key = currentEntry.getKey();
//                V value = FileHashMap.this.remove(key);
//                if (value != null) {
//                    currentEntry = hasNext() ? iterator.next() : null;
//                }
//            }
//        }
//    }
//
//    /**
//     * Specialized iterator for looping through the value set. The iterator
//     * loops through the FileHashMapEntry items, which have been sorted by
//     * file position; each call to next() causes the iterator to load the
//     * appropriate value. This approach (a) iterates through the value file
//     * sequentially, and (b) demand-loads the values, so they're not all
//     * loaded into memory at the same time.
//     */
//    private class ValueIterator implements Iterator<V> {
//        final EntryIterator it;
//
//        private ValueIterator() {
//            it = new EntryIterator();
//        }
//
//        @Override
//        public boolean hasNext() {
//            return it.hasNext();
//        }
//
//        @Override
//        public V next() {
//            return FileHashMap.this.readValueNoError(it.next());
//        }
//
//        @Override
//        public void remove() {
//            throw new UnsupportedOperationException();
//        }
//    }
//
//    /**
//     * Shallow set that contains pointers to the on-disk values. Values
//     * are loaded only when referenced.
//     */
//    private class ValueSet extends AbstractSet<V> {
//        final ValuesFile valuesDB = FileHashMap.this.valuesDB;
//
//        @Override
//        public void clear() {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean contains(Object o) {
//            boolean containsIt = false;
//
//            // Loop through values sequentially, to optimize file access.
//
//            seekTo(0);
//            for (Iterator<V> it = new ValueIterator(); it.hasNext(); ) {
//                V obj = it.next();
//
//                if (obj.equals(o)) {
//                    containsIt = true;
//                    break;
//                }
//            }
//
//            return containsIt;
//        }
//
//        @Override
//        public boolean containsAll(Collection c) {
//            boolean containsThem = true;
//
//            // Loop through values sequentially, to optimize file access.
//
//            seekTo(0);
//            for (Iterator<V> it = new ValueIterator(); it.hasNext(); ) {
//                V obj = it.next();
//
//                if (!c.contains(obj)) {
//                    containsThem = false;
//                    break;
//                }
//            }
//
//            return containsThem;
//        }
//
//        public boolean equals(Object o) {
//            boolean eq = false;
//            Collection other = (Set) o;
//
//            if (other.size() == size())
//                eq = this.containsAll(other);
//
//            return eq;
//        }
//
//        public int hashCode() {
//            int result = 0;
//
//            // Loop through values sequentially, to optimize file access.
//
//            seekTo(0);
//            for (Iterator<V> it = new ValueIterator(); it.hasNext(); ) {
//                Object obj = it.next();
//
//                result += obj.hashCode();
//            }
//
//            return result;
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return indexMap.isEmpty();
//        }
//
//        @Override
//        public Iterator<V> iterator() {
//            return new ValueIterator();
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            return (FileHashMap.this.remove(o) != null);
//        }
//
//        @Override
//        public int size() {
//            return currentSize();
//        }
//
//        private void seekTo(long pos) {
//            try {
//                valuesDB.getFile().seek(pos);
//            } catch (IOException ex) {
//            }
//        }
//    }
//
//    /**
//     * Necessary to support the Map.entrySet() routine. Each object
//     * returned by FileHashMap.entrySet().iterator() is of this type. Each
//     * EntrySetEntry object provides an alternate, user-acceptable view of
//     * a FileHashMapEntry.
//     */
//    private class EntrySetEntry implements Map.Entry<K, V> {
//        private final FileHashMapEntry<K> entry;
//
//        EntrySetEntry(FileHashMapEntry<K> entry) {
//            this.entry = entry;
//        }
//
//        @Override
//        public String toString() {
//            return entry.key + "@" + entry.filePosition + ".." + (entry.filePosition + entry.objectSize);
//        }
//
//        public boolean equals(Object o) {
//            Map.Entry<K, V> mo = (Map.Entry<K, V>) o;
//            Object thisKey = getKey();
//
//            // Adapted directly from Map.Entry javadoc.
//            K mok = mo.getKey();
//            if (mok == null ? thisKey == null : mok.equals(thisKey)) {
//
//                V mov = mo.getValue();
//                Object thisValue = getValue();
//                return mov == null ? thisValue == null : mov.equals(thisValue);
//            }
//            return false;
//        }
//
//        @Override
//        public K getKey() {
//            return entry.getKey();
//        }
//
//        @Override
//        public V getValue() {
//            return FileHashMap.this.readValueNoError(entry);
//        }
//
//        public int hashCode() {
//            // Adapted directly from the Map.Entry javadoc
//
//            V value = getValue();
//            K key = getKey();
//
//            return (key == null ? 0 : key.hashCode()) ^
//                    (value == null ? 0 : value.hashCode());
//        }
//
//        @Override
//        public V setValue(V value) {
//            throw new UnsupportedOperationException();
//        }
//    }
//
//    /**
//     * The actual entry set returned by FileHashMap.entrySet(). The values
//     * are demand-loaded. The iterator() and toArray() methods ensure that
//     * the keys are returned in an order that causes sequential access to the
//     * values file.
//     */
//    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
//        private EntrySet() {
//            // Nothing to do
//        }
//
//        @Override
//        public void clear() {
//            throw new UnsupportedOperationException();
//        }
//
//        public boolean contains(Map.Entry<K, V> o) {
//            return FileHashMap.this.containsValue(o.getValue());
//        }
//
//        @Override
//        public Iterator<Map.Entry<K, V>> iterator() {
//            return Iterators.transform(new EntryIterator(), EntrySetEntry::new);
//        }
//
//        public boolean equals(Object obj) {
//            boolean eq = (this == obj);
//
//            if (!eq)
//                eq = super.equals(obj);
//
//            return eq;
//        }
//
//        public boolean remove(FileHashMapEntry<K> o) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean removeAll(Collection c) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean retainAll(Collection c) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public int size() {
//            return FileHashMap.this.size();
//        }
//    }
//
//    /**
//     * Implements a key set -- the set of keys the caller used to store
//     * values in the FileHashMap. The iterator() and toArray() methods ensure
//     * that the keys are returned in a manner that optimizes looping through
//     * the associated values (as with the various iterators, above).
//     */
//    private class KeySet extends AbstractSet<K> {
//        ArrayList<K> keys;
//
//        private KeySet() {
//            // Nothing to do
//        }
//
//        @Override
//        public void clear() {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean contains(Object o) {
//            return FileHashMap.this.indexMap.containsKey(o);
//        }
//
//        @Override
//        public boolean containsAll(Collection c) {
//            boolean contains = true;
//            Iterator it = c.iterator();
//
//            while (contains && it.hasNext())
//                contains = FileHashMap.this.indexMap.containsKey(it.next());
//
//            return contains;
//        }
//
//        public boolean equals(Object o) {
//            Collection<K> so = (Set<K>) o;
//            boolean eq = false;
//            Set<K> myKeys = FileHashMap.this.indexMap.keySet();
//
//            if (so.size() == myKeys.size()) {
//                eq = true;
//                Iterator<K> it = myKeys.iterator();
//                while (eq) {
//                    K myKey = it.next();
//
//                    if (!so.contains(myKey))
//                        eq = false;
//                }
//            }
//
//            return eq;
//        }
//
//        public int hashCode() {
//            return FileHashMap.this.indexMap.keySet().hashCode();
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return FileHashMap.this.indexMap.isEmpty();
//        }
//
//        @Override
//        public Iterator<K> iterator() {
//            return Iterators.transform(new EntryIterator(), (FileHashMapEntry<K> x) -> x.key);
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean removeAll(Collection c) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean retainAll(Collection c) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public int size() {
//            return FileHashMap.this.currentSize();
//        }
//
//        private synchronized void loadKeyArray() {
//            if (keys == null) {
//                List<FileHashMapEntry<K>> entries = FileHashMap.this.getSortedEntries();
//                keys = new ArrayList<>(entries.size());
//                for (FileHashMapEntry<K> entry : entries)
//                    keys.add(entry.getKey());
//            }
//        }
//    }
//
//    /*----------------------------------------------------------------------*\
//                           Private Instance Data
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * The index, cached in memory. Each entry in the list is a
//     * FileHashMapEntry object. This index is stored on disk, in the index
//     * file.
//     */
//    private final Map<K, FileHashMapEntry<K>> indexMap;
//
//    /**
//     * The file prefix with which this object was created.
//     */
//    private String filePrefix;
//
//    /**
//     * The index file.
//     */
//    private File indexFilePath;
//
//    /**
//     * The values file.
//     */
//    private File valuesDBPath;
//
//    /**
//     * The open values database.
//     */
//    private ValuesFile valuesDB;
//
//    /**
//     * The flags specified to the constructor.
//     */
//    private final int flags;
//
//
//    /**
//     * Whether or not the object is still valid. See close().
//     */
//    private final AtomicBoolean valid = new AtomicBoolean(true);
//
//    /**
//     * The value returned by the entrySet() method. It's created the first
//     * time entrySet() is called.
//     */
//    private EntrySet entrySetResult;
//
//    /**
//     * A set of gaps in the file, ordered sequentially by file position.
//     * Each entry in the set is a FileHashMapEntry with no associated
//     * object or key. This reference will be non-null only if the
//     * RECLAIM_FILE_GAPS flag was passed to the constructor.
//     */
//    private TreeSet<FileHashMapEntry<K>> fileGaps;
//
//    /*----------------------------------------------------------------------*\
//                            Private Class Data
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * For log messages
//     */
//    private static final Logger log = LoggerFactory.getLogger(FileHashMap.class);
//
//    /*----------------------------------------------------------------------*\
//                               Constructors
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * <p>Create a new transient <tt>FileHashMap</tt> object. The transient
//     * disk files will be in the normal temporary directory and will have
//     * the prefix "FileHashMap". The names of the associated disk files are
//     * guaranteed to be unique, and will go away when the object is
//     * finalized or when the Java VM goes away. Call this constructor is
//     * identical to calling {@link #FileHashMap(String)} with a null
//     * <tt>filePrefix</tt> parameter.</p>
//     * <p>
//     * <p>Note that this constructor implicitly sets the {@link #TRANSIENT}
//     * flag.</p>
//     *
//     * @throws IOException Unable to create temp file
//     * @see #FileHashMap(String, int)
//     * @see #FileHashMap(String)
//     */
//    public FileHashMap() {
//        this(null);
//    }
//
//    /**
//     * <p>Create a new transient <tt>FileHashMap</tt> object. The transient
//     * disk files will be in the normal temporary directory and will have
//     * the specified temporary file prefix. If no temporary file prefix is
//     * specified (i.e., the <tt>tempFilePrefix</tt> parameter is null), then
//     * "fhm" will be used. The names of the associated disk files are
//     * guaranteed to be unique, and they will go away when the object is
//     * finalized or when the Java VM goes away.</p>
//     * <p>
//     * <p>Note that this constructor implicitly sets the {@link #TRANSIENT}
//     * flag.</p>
//     *
//     * @param tempFilePrefix the prefix to use with the temporary file, or
//     *                       null for default prefix "fhm"
//     * @throws IOException Unable to create temp file
//     * @see #FileHashMap(String, int)
//     * @see #FileHashMap()
//     */
//    public FileHashMap(String tempFilePrefix) {
//        this.flags = TRANSIENT;
//        this.filePrefix = tempFilePrefix;
//
//        if (filePrefix == null)
//            filePrefix = UUID.randomUUID().toString();
//
//        try {
//            this.valuesDBPath = File.createTempFile(filePrefix, DATA_FILE_SUFFIX);
//            this.valuesDBPath.deleteOnExit();
//
//            this.valuesDB = new ValuesFile(this.valuesDBPath);
//            this.indexMap = new ConcurrentHashMap();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * <p>Create a new <tt>FileHashMap</tt> object that will read its data
//     * from and/or store its data in files derived from the specified
//     * prefix. The prefix is a file prefix onto which data and index
//     * suffixes will be appended.</p>
//     * <p>
//     * <p><u>Values for the <tt>flag</tt> parameter</u></p>
//     * <p>
//     * <p>The <tt>flags</tt> parameter consists of a set of logically OR'd
//     * constants.
//     * <p>
//     * <p>If <tt>(flags &amp; NO_CREATE)</tt> is zero, the constructor will
//     * attempt to create the index files if they don't exist. Otherwise, it
//     * expects to find a saved hash map, and it will throw an exception if
//     * the files are not present.</p>
//     * <p>
//     * <p>If <tt>(flags &amp; TRANSIENT)</tt> is non-zero, the constructor will
//     * attempt to create a transient map. A transient map is not saved; its
//     * disk files are removed when the map is finalized or manually closed.
//     * You cannot create a transient map using a file prefix that specifies
//     * an existing, saved map. Specifying the <tt>TRANSIENT</tt> flag
//     * causes any <tt>NO_CREATE</tt> flag to be ignored.</p>
//     * <p>
//     * <p>For example, the following statement creates a transient
//     * <tt>FileHashMap</tt>:</p>
//     * <p>
//     * <blockquote><pre>
//     * {@code
//     * Map map = new FileHashMap ("/my/temp/dir", FileHashMap.TRANSIENT);
//     * }
//     * </pre></blockquote>
//     * <p>
//     * <p>whereas this statements opens a persistent <tt>FileHashMap</tt>,
//     * creating it if it doesn't already exist:</p>
//     * <p>
//     * <blockquote><pre>
//     * {@code
//     * Map map = new FileHashMap ("/my/map/dir", FileHashMap.CREATE);
//     * }
//     * </pre></blockquote>
//     *
//     * @param pathPrefix The pathname prefix to the files to be used
//     * @param flags      Flags that control the disposition of the files.
//     *                   A value of 0 means no flags are set. See above
//     *                   for details.
//     * @throws FileNotFoundException    The specified hash files do not
//     *                                  exist, and the {@link #NO_CREATE}
//     *                                  flag was specified.
//     * @throws ClassNotFoundException   Failed to deserialize an object
//     * @throws VersionMismatchException Bad or unsupported version stamp
//     *                                  in <tt>FileHashMap</tt> index file
//     * @throws ObjectExistsException    One or both of the files already
//     *                                  exist, but the {@link #TRANSIENT}
//     *                                  flag was set and the
//     *                                  {@link #FORCE_OVERWRITE} flag was
//     *                                  <i>not</i> set.
//     * @throws IOException              Other errors
//     * @see #NO_CREATE
//     * @see #TRANSIENT
//     * @see #FileHashMap()
//     * @see #FileHashMap(String)
//     */
//    public FileHashMap(String pathPrefix, int flags)
//            throws
//            ClassNotFoundException,
//            IOException {
//        assert (((~ALL_FLAGS_MASK) & flags) == 0);
//
//        int filesFound = 0;
//
//        this.filePrefix = pathPrefix;
//        this.flags = flags;
//
//        valuesDBPath = new File(pathPrefix + DATA_FILE_SUFFIX);
//        indexFilePath = new File(pathPrefix + INDEX_FILE_SUFFIX);
//
//        if ((flags & TRANSIENT) != 0)
//            flags &= (~NO_CREATE);
//
//        if (valuesDBPath.exists())
//            filesFound++;
//        if (indexFilePath.exists())
//            filesFound++;
//
//        if ((filesFound > 0) && ((flags & TRANSIENT) != 0)) {
//            if ((flags & FORCE_OVERWRITE) == 0) {
//                throw new RuntimeException(
//                        ("FileHashMap.diskFilesExist" +
//                                "One or both of the hash table files (\"{0}\" " +
//                                "and/or \"{1}\") already exists, but the " +
//                                "FileHashMap.FORCE_OVERWRITE constructor flag " +
//                                "was not set." +
//                                Arrays.toString(new Object[]
//                                        {
//                                                valuesDBPath.getName(),
//                                                indexFilePath.getName()
//                                        })));
//            }
//
//            // FORCE_OVERWRITE is set. Wipe out the files, and reset the
//            // existence count.
//
//            valuesDBPath.delete();
//            indexFilePath.delete();
//            filesFound = 0;
//        }
//
//        switch (filesFound) {
//            case 0:
//                if ((flags & NO_CREATE) != 0) {
//                    // Can't localize this one. It's not one of our exceptions.
//
//                    throw new FileNotFoundException
//                            ("On-disk hash table \"" +
//                                    pathPrefix +
//                                    "\" does not exist, and the " +
//                                    "FileHashMap.NO_CREATE flag was set.");
//                }
//
//                this.valuesDB = new ValuesFile(this.valuesDBPath);
//                this.indexMap = new ConcurrentHashMap();
//                break;
//
//            case 1:
//                throw new RuntimeException(
//                        (
//                                "FileHashMap.halfMissing" +
//                                        "One of the hash table files exists (\"{0}\" " +
//                                        "or \"{1}\") exists, but the other one does " +
//                                        "not." +
//                                        Arrays.toString(new Object[]
//                                                {
//                                                        valuesDBPath.getName(),
//                                                        indexFilePath.getName()
//                                                })));
//
//            case 2:
//                valuesDB = new ValuesFile(valuesDBPath);
//                ObjectInputStream objStream;
//                String version;
//
//                objStream = new ObjectInputStream
//                        (new FileInputStream(this.indexFilePath));
//                version = (String) objStream.readObject();
//
//                if (!version.equals(VERSION_STAMP)) {
//                    throw new RuntimeException(//Package.BUNDLE_NAME,
//                            "FileHashMap.versionMismatch" +
//                                    "FileHashMap version mismatch in index file " +
//                                    "\"{0}\". Expected version \"{1}\", found " +
//                                    "version \"{2}\"" +
//                                    Arrays.toString(new Object[]
//                                            {
//                                                    indexFilePath.getName(),
//                                                    VERSION_STAMP,
//                                                    version
//                                            }) +
//                                    VERSION_STAMP +
//                                    version);
//                }
//
//                // This typecast will generate an "unchecked cast" exception.
//                // Unfortunately, there's no way around it (other than to avoid
//                // making calls like this). See
//                // http://www.langer.camelot.de/GenericsFAQ/JavaGenericsFAQ.html#Technicalities
//
//                indexMap = (Map<K, FileHashMapEntry<K>>) objStream.readObject();
//                break;
//
//            default:
//                throw new RuntimeException();
//        }
//
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }));
//
//        if ((flags & RECLAIM_FILE_GAPS) != 0)
//            findFileGaps();
//
//
//    }
//
//    /*----------------------------------------------------------------------*\
//                              Finalizer
//    \*----------------------------------------------------------------------*/
//
////    /**
////     * Finalizer
////     *
////     * @throws Throwable on error
////     */
////    @Override
////    protected void finalize()
////            throws Throwable {
////        try {
////            close();
////        } catch (IOException ex) {
////            log.error("Error during finalize", ex);
////        }
////
////        super.finalize();
////    }
//
//    /*----------------------------------------------------------------------*\
//                              Public Methods
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * <p>Removes all mappings from this map. The data file is cleared by
//     * closing it, deleting it, and reopening it. If an I/O error occurs at
//     * any point, this object will be closed and marked invalid.</p>
//     */
//    @Override
//    public synchronized void clear() {
//        checkValidity();
//        indexMap.clear();
//
//        try {
//            // Implement the clear operation by truncating the data file.
//            valuesDB.getFile().getChannel().truncate(0);
//        } catch (IOException ex) {
//            valid.set(false);
//            log.error("Failed to truncate FileHashMap file \"{}\"", valuesDBPath.getPath(), ex);
//        }
//    }
//
//    /**
//     * <p>Close this map. If the map is not marked as transient, this method
//     * saves the index to disk. Otherwise, it removes both the index file
//     * and data file.</p>
//     *
//     * @throws NotSerializableException Can't save index (bug)
//     * @throws IOException              Error writing index
//     * @see #save
//     */
//    public synchronized void close() throws IOException {
//        if (valid.get()) {
//            if ((flags & TRANSIENT) != 0) {
//                // Be sure to remove the data files.
//
//                if (valuesDB != null) {
//                    valuesDB.close();
//                    valuesDB = null;
//                }
//
//                deleteMapFiles();
//            } else {
//                save();
//            }
//
//            valid.set(false);
//        }
//    }
//
//    /**
//     * <p>Returns <tt>true</tt> if this map contains a mapping for the
//     * specified key. Since the keys are cached in an in-memory index, this
//     * method does not have to access the data file, and is fairly
//     * cheap.</p>
//     *
//     * @param key key whose presence in this map is to be tested
//     * @return <tt>true</tt> if this map contains a mapping for the specified
//     * key, <tt>false</tt> otherwise.
//     */
//    @Override
//    public boolean containsKey(Object key) {
//        return indexMap.containsKey(key);
//    }
//
//    /**
//     * <p>Returns <tt>true</tt> if this map maps one or more keys that are
//     * mapped to the specified value. Because it must iterate through some
//     * portion of the on-disk value set, this method can be slow.</p>
//     *
//     * @param value value whose presence in this map is to be tested.
//     * @return <tt>true</tt> if this map maps one or more keys to the
//     * specified value, <tt>false</tt> otherwise.
//     */
//    @Override
//    public boolean containsValue(Object value) {
//        checkValidity();
//        return new ValueSet().contains(value);
//    }
//
//    /**
//     * Deletes the files backing this <tt>FileHashMap</tt>. This method
//     * implicitly calls {@link #close}.
//     */
//    public void delete() {
//        try {
//            close();
//        } catch (IOException ex) {
//        }
//
//        deleteMapFiles();
//    }
//
//    /**
//     * <p>Returns a "thin" set view of the mappings contained in this map.
//     * Each element in the returned set is a <tt>Map.Entry</tt>; each
//     * <tt>Map.Entry</tt> contains a key and a proxy for the associated value.
//     * The map's values themselves are not loaded into memory until requested
//     * via a call to <tt>Map.Entry.getValue()</tt>.</p>
//     * <p>
//     * <p>The set is backed by the map, so changes to the map are reflected
//     * in the set. If the map is modified while an iteration over the set
//     * is in progress, the results of the iteration are undefined. Neither
//     * the set nor its associated iterator supports any of the
//     * set-modification methods (e.g., <tt>add()</tt>, <tt>remove()</tt>,
//     * etc). If you attempt to call any of those methods, the called method
//     * will throw an <tt>UnsupportedOperationException</tt>.</p>
//     * <p>
//     * <p>NOTE: Calling this method on an invalid or closed
//     * <tt>FileHashMap</tt> will result in an exception.</p>
//     *
//     * @return a set view of the mappings contained in this map.
//     * @see #keySet
//     * @see #values
//     * @see UnsupportedOperationException
//     */
//    @Override
//    public Set<Map.Entry<K, V>> entrySet() {
//        checkValidity();
//
//        if (entrySetResult == null)
//            entrySetResult = new EntrySet();
//
//        return entrySetResult;
//    }
//
//    /**
//     * <p>Compares the specified object with this map for equality.  Returns
//     * <tt>true</tt> if the given object is also a map and the two Maps
//     * represent the same mappings.  More formally, two maps <tt>t1</tt> and
//     * <tt>t2</tt> represent the same mappings if
//     * <tt>t1.entrySet().equals(t2.entrySet())</tt>.  This ensures that the
//     * <tt>equals</tt> method works properly across different implementations
//     * of the <tt>Map</tt> interface.</p>
//     * <p>
//     * <p><b>Warning:</b>: Because this method must compare the actual
//     * values stored in the map, and the values in a file, this method can
//     * be quite slow.</p>
//     *
//     * @param o object to be compared for equality with this map.
//     * @return <tt>true</tt> if the specified object is equal to this map.
//     */
//    public boolean equals(Object o) {
//        checkValidity();
//
//        return super.equals(o);
//    }
//
//    /**
//     * <p>Returns the value associated with the the specified key.  Returns
//     * <tt>null</tt> if the map contains no mapping for this key.</p>
//     *
//     * @param key key whose associated value is to be returned.
//     * @return the value to which this map maps the specified key, or
//     * <tt>null</tt> if the map contains no mapping for this key.
//     * @throws ClassCastException   if the key is of an inappropriate type for
//     *                              this map.
//     * @throws NullPointerException key is <tt>null</tt>
//     * @see #containsKey(Object)
//     */
//    @Override
//    public V get(Object key) {
//        checkValidity();
//        FileHashMapEntry<K> entry = indexMap.get(key);
//        return entry != null ? readValueNoError(entry) : null;
//
//    }
//
//    /**
//     * <p>Returns the hash code value for this map. The hash code of a map
//     * is defined to be the sum of the hash codes of each entry in the
//     * map's <tt>entrySet()</tt> view. This ensures that
//     * <tt>t1.equals(t2)</tt> implies that
//     * <tt>t1.hashCode()==t2.hashCode()</tt> for any two maps <tt>t1</tt>
//     * and <tt>t2</tt>, as required by the general contract of
//     * Object.hashCode.</p>
//     * <p>
//     * <p><b>Warning:</b>: The recommended hash code for each entry in the
//     * <tt>entrySet()</tt> view is a combination of hash codes for the
//     * entry's key and the entry's value. (See <tt>java.util.Map.Entry</tt>
//     * for details.) Because the values in a <tt>FileHashMap</tt> object
//     * are stored in a file, this method can be quite slow.</p>
//     *
//     * @return the hash code value for this map.
//     * @see Object#hashCode()
//     * @see Object#equals(Object)
//     * @see #equals(Object)
//     */
//    public int hashCode() {
//        checkValidity();
//
//        return super.hashCode();
//    }
//
//    /**
//     * Determine whether the <tt>FileHashMap</tt> is valid or not. Once
//     * a <tt>FileHashMap</tt> has been closed, it is invalid and can no
//     * longer be used.
//     *
//     * @return <tt>true</tt> if this object is valid, <tt>false</tt> if not
//     */
//    public boolean isValid() {
//        return valid.get();
//    }
//
//    /**
//     * <p>Returns a <tt>Set</tt> containing all the keys in this map. Since
//     * the keys are cached in memory, this method is relatively efficient.
//     * The keys are returned in an order that optimizes sequential access
//     * to the data file; see the <b>Optimizations</b> section in the main
//     * class documentation for details.</p>
//     * <p>
//     * <p>The set is backed by the map, so changes to the map are reflected
//     * in the set. If the map is modified while an iteration over the set
//     * is in progress, the results of the iteration are undefined. Neither
//     * the set nor its associated iterator supports any of the
//     * set-modification methods (e.g., <tt>add()</tt>, <tt>remove()</tt>,
//     * etc). If you attempt to call any of those methods, the called method
//     * will throw an <tt>UnsupportedOperationException</tt>.</p>
//     *
//     * @return a set view of the keys contained in this map.
//     */
//    @Override
//    public Set<K> keySet() {
//        checkValidity();
//        return new KeySet();
//    }
//
//    /**
//     * <p>Associates the specified value with the specified key in this map.
//     * If the map previously contained a mapping for this key, the old
//     * value is replaced. This map class does not permit a null value to be
//     * stored.</p>
//     *
//     * @param key   key with which the specified value is to be associated.
//     * @param value value to be associated with the specified key.
//     * @return previous value associated with specified key, or <tt>null</tt>
//     * if there was no mapping for key.
//     * @throws ClassCastException       if the class of the specified key or
//     *                                  value prevents it from being stored
//     *                                  in this map.
//     * @throws IllegalArgumentException Value not serializable, or I/O error
//     *                                  while attempting to serialize value.
//     * @throws NullPointerException     the specified key or value is
//     *                                  <tt>null</tt>.
//     */
//    @Override
//    public V put(K key, V value) throws ClassCastException, IllegalArgumentException, NullPointerException {
//        return put(key, value, true);
//    }
//
//    public void putAsync(K key, V value) throws ClassCastException, IllegalArgumentException, NullPointerException {
//        put(key, value, false);
//    }
//
//    V put(K key, V value, boolean read) throws ClassCastException, IllegalArgumentException, NullPointerException {
//
//        if (key == null)
//            throw new NullPointerException("null key parameter");     // NOPMD
//
//        if (value == null)
//            throw new NullPointerException("null value parameter");   // NOPMD
//
//        if (!(value instanceof Serializable))
//            throw new IllegalArgumentException("Value is not serializable.");
//
//        // NOTE: We don't check the key for serializability. It's perfectly
//        // reasonable to use unserializable keys if the hash map is transient.
//        // Unserializable keys will be caught on save, for persistent maps.
//
//        V result;
//        checkValidity();
//
//        result = null; //remove(key, read);
//
//        FileHashMapEntry<K> prev = indexMap.compute(key, (k,p) -> {
//            try {
//                return writeValue(k, value);
//            } catch (IOException e) {
//                e.printStackTrace();
//                return null;
//            }
//        });
//        if (prev != null) {
//            if (read) {
//                try {
//                    result = readValue(prev);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            if ((flags & RECLAIM_FILE_GAPS) != 0)
//                findFileGaps();
//        }
//
//
//        return result;
//    }
//
//    /**
//     * <p>Removes the mapping for this key from this map, if present.
//     * <b>Note:</b> The space occupied by the serialized value in the
//     * data file is <i>not</i> coalesced at this point.</p>
//     *
//     * @param key key whose mapping is to be removed from the map.
//     * @return previous value associated with specified key, or <tt>null</tt>
//     * if there was no mapping for key.
//     */
//    @Override
//    public V remove(Object key) {
//        return remove(key, true);
//    }
//
//    public V remove(Object key, boolean readOld) {
//        checkValidity();
//
//        V result = null;
//
//        // We do nothing with the space in the data file for any existing
//        // item. It remains in the data file, but is unreferenced.
//
//        FileHashMapEntry<K> entry;
//        if ((entry = indexMap.remove(key)) != null) {
//            result = readOld ? readValueNoError(entry) : null;
//
//            if ((flags & RECLAIM_FILE_GAPS) != 0) {
//                // Have to recalculate gaps, since we may be able to coalesce
//                // this returned space with ones to either side of it.
//
//                //log.debug("Removed value for key \"{}\" at pos={}, size={}. Re-figuring gaps.", key, entry.getFilePosition(), entry.getObjectSize());
//                findFileGaps();
//            }
//        }
//
//        return result;
//    }
//
//
//    /**
//     * <p>Save any in-memory index changes to disk without closing the map.
//     * You can call this method even if the map is marked as temporary;
//     * on a temporary map, <tt>save()</tt> simply returns without doing
//     * anything.</p>
//     *
//     * @throws IOException              Error saving changes to disk.
//     * @throws NotSerializableException Can't save index because it contains
//     *                                  one or more objects that cannot be
//     *                                  serialized.
//     * @see #close
//     */
//    public void save()
//            throws IOException {
//        checkValidity();
//
//        if (((flags & TRANSIENT) == 0))
//            saveIndex();
//    }
//
//    /**
//     * <p>Returns the number of key-value mappings in this map. If the map
//     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
//     * <tt>Integer.MAX_VALUE</tt>. This method queries the in-memory key
//     * index, so it is relatively efficient.</p>
//     *
//     * @return the number of key-value mappings in this map.
//     */
//    @Override
//    public int size() {
//        checkValidity();
//        return currentSize();
//    }
//
//    /**
//     * <p>Returns a collection view of the values contained in this map. The
//     * returned collection is a "thin" view of the values contained in
//     * this map. The collection contains proxies for the actual disk-resident
//     * values; the values themselves are not loaded until a
//     * <tt>Collection</tt> method such as <tt>contains()</tt> is called.</p>
//     * <p>
//     * <p>The collection is backed by the map, so changes to the map are
//     * reflected in the set. If the map is modified while an iteration over
//     * the set is in progress, the results of the iteration are undefined.
//     * The set does not support any of the <tt>add()</tt> methods.</p>
//     * <p>
//     * <p><b>Warning:</b>: The <tt>toArray()</tt> methods can be dangerous,
//     * since they will attempt to load every value from the data file into
//     * an in-memory array.</p>
//     *
//     * @return a collection view of the values contained in this map.
//     * @see #keySet
//     * @see #values
//     */
//    @Override
//    public Collection<V> values() {
//        checkValidity();
//
//        return new ValueSet();
//    }
//
//    /*----------------------------------------------------------------------*\
//                              Private Methods
//    \*----------------------------------------------------------------------*/
//
//    /**
//     * Throw an exception if the object isn't valid.
//     */
//    private void checkValidity() {
//        if (!valid.get())
//            throw new IllegalStateException("Invalid FileHashMap object");
//    }
//
//    /**
//     * Determine the size of this map. Basically, this method just consolidates
//     * the size-determination logic in one place.
//     *
//     * @return the size
//     */
//    private int currentSize() {
//        return indexMap.size();
//    }
//
//    /**
//     * Locate gaps in the file by traversing the index. Initializes or
//     * reinitializes the fileGaps instance variable.
//     */
//    private void findFileGaps() {
//        log.debug("Looking for file gaps.");
//
//        if (fileGaps == null)
//            fileGaps = new TreeSet<>(new FileHashMapEntryGapComparator());
//        else
//            fileGaps.clear();
//
//        if (currentSize() > 0) {
//            List<FileHashMapEntry<K>> entries = getSortedEntries();
//            FileHashMapEntry<K> previous = null;
//            Iterator<FileHashMapEntry<K>> it = entries.iterator();
//
//            // Handle the first one specially.
//
//            FileHashMapEntry<K> entry = it.next();
//            long pos = entry.getFilePosition();
//            int size = entry.getObjectSize();
//
//            if (pos > 0) {
//                // There's a gap at the beginning.
//
//                log.debug("First entry is at pos {}, size={}", pos, size);
//                size = (int) pos;
//                log.debug("Gap at position 0 of size {}", size);
//                fileGaps.add(new FileHashMapEntry<>(0L, size));
//            }
//
//            previous = entry;
//
//            while (it.hasNext()) {
//                entry = it.next();
//
//                long previousPos = previous.getFilePosition();
//                long possibleGapPos = previousPos + previous.getObjectSize();
//                pos = entry.getFilePosition();
//
//                assert (pos > previousPos);
//                if (possibleGapPos != pos) {
//                    int gapSize = (int) (pos - possibleGapPos);
//
//                    log.debug("Gap at position {} of size {}", possibleGapPos, gapSize);
//                    fileGaps.add(new FileHashMapEntry<>(possibleGapPos,
//                            gapSize));
//                }
//
//                previous = entry;
//            }
//        }
//    }
//
//    /**
//     * Get the list of FileHashMapEntry pointers for the values, sorted
//     * in ascending file position order.
//     * <p>
//     * TODO use Stream<>
//     *
//     * @return A sorted list of FileHashMapEntry objects.
//     */
//    private List<FileHashMapEntry<K>> getSortedEntries() {
//        // Get the set of values in the hash index. Each value is a
//        // FileHashMapEntry object.
//
//        List<FileHashMapEntry<K>> vals = new ArrayList<>(indexMap.values());
//
//        // Sort the list.
//
//        vals.sort(new FileHashMapEntryComparator());
//
//        return vals;
//    }
//
//    @Override
//    public V computeIfAbsent(K key,
//                             Function<? super K, ? extends V> mappingFunction) {
//
//        V v;
//        synchronized (indexMap) {
//            if ((v = get(key)) == null) {
//                V newValue;
//                if ((newValue = mappingFunction.apply(key)) != null) {
//                    put(key, newValue);
//                    return newValue;
//                }
//            }
//        }
//
//        return v;
//    }
//
//
//    /**
//     * Read an object from a specific location in the random access file
//     * data file.
//     *
//     * @param entry The FileHashMapEntry specifying the object's location
//     * @return the object
//     * @throws IOException            Read error
//     * @throws ClassNotFoundException The serialized class read from the
//     *                                file cannot be loaded
//     * @throws IllegalStateException  No object stored at location (i.e.,
//     *                                no size value has been set)
//     * @see #getFilePosition
//     * @see #writeObject
//     */
//    private V readValue(FileHashMapEntry<K> entry)
//            throws IOException,
//            ClassNotFoundException,
//            IllegalStateException {
//        int size = entry.getObjectSize();
//        byte byteBuf[] = new byte[size];
//        int sizeRead;
//        ObjectInputStream objStream;
//
//        // Load the serialized object into memory.
//
//        RandomAccessFile valuesFile;
//        synchronized (this) {
//            valuesFile = valuesDB.getFile();
//            valuesFile.seek(entry.getFilePosition());
//
//        }
//        if ((sizeRead = valuesFile.read(byteBuf)) != size) {
//            throw new IOException("Expected to read " +
//                    size + "-byte serialized object from " +
//                    " on-disk data file. Got only " +
//                    sizeRead + " bytes.");
//        }
//        // Use a ByteArrayInputStream and an ObjectInputStream to read
//        // the actual object itself.
//
//        objStream = new ObjectInputStream(new ByteArrayInputStream(byteBuf));
//
//        // This typecast will generate an "unchecked cast" exception.
//        // Unfortunately, there's no way around it (other than to avoid
//        // making calls like this). See
//        // http://www.langer.camelot.de/GenericsFAQ/JavaGenericsFAQ.html#Technicalities
//
//        return (V) objStream.readObject();
//    }
//
//    /**
//     * Read an object without throwing a checked exception on error.
//     *
//     * @param entry The FileHashMapEntry specifying the object's location
//     * @return the object, or null if not found
//     */
//    private V readValueNoError(FileHashMapEntry<K> entry) {
//        V obj = null;
//
//        try {
//            obj = readValue(entry);
//        } catch (IOException | ClassNotFoundException ex) {
//        }
//
//        return obj;
//    }
//
//    /**
//     * Save the index to its disk file.
//     *
//     * @throws IOException on error
//     */
//    private synchronized void saveIndex()
//            throws IOException {
//        ObjectOutputStream objStream;
//
//        objStream = new ObjectOutputStream
//                (new FileOutputStream(this.indexFilePath));
//        objStream.writeObject(VERSION_STAMP);
//        objStream.writeObject(indexMap);
//
//        if (log.isDebugEnabled()) {
//            List<FileHashMapEntry<K>> entries = getSortedEntries();
//
//            //log.debug("Just saved index. Total entries={}", currentSize());
//            //log.debug("Index values follow.");
//            for (FileHashMapEntry<K> entry : entries) {
//                long pos = entry.getFilePosition();
//                int size = entry.getObjectSize();
//
//                //log.debug("    pos={}, size={}", pos, size);
//            }
//        }
//    }
//
//    /**
//     * Write an object to the end of the data file, recording its position
//     * and length in a FileHashMapEntry object. Note: The object to be
//     * stored must implement the <tt>Serializable</tt> interface.
//     *
//     * @param key The object's key (specified by the caller of
//     *            FileHashMap.put())
//     * @param obj The object to serialize and store
//     * @return the FileHashMapEntry object that records the location of
//     * the stored object
//     * @throws IOException              Write error
//     * @throws NotSerializableException Object isn't serializable
//     * @see #getFilePosition
//     * @see #readValue
//     */
//    private FileHashMapEntry<K> writeValue(K key, V obj)
//            throws IOException {
//        ObjectOutputStream objStream;
//        ByteArrayOutputStream byteStream;
//        int size;
//        long filePos = -1;
//
//        // Serialize the object to a byte buffer.
//
//        byteStream = new ByteArrayOutputStream(WRITE_BUFFER_SIZE);
//        objStream = new ObjectOutputStream(byteStream);
//        objStream.writeObject(obj);
//        size = byteStream.size();
//
//        byte[] ba = byteStream.toByteArray();
//
//        synchronized (indexMap) {
//            // Find a location for the object.
//
//            if ((flags & RECLAIM_FILE_GAPS) != 0)
//                filePos = findBestFitGap(size);
//
//            RandomAccessFile valuesFile = this.valuesDB.getFile();
//            if (filePos == -1)
//                filePos = valuesFile.length();
//
//            valuesFile.seek(filePos);
//
//            // Write the bytes of the serialized object.
//
//            valuesFile.write(ba);
//
//            // Return the entry.
//        }
//
//        return new FileHashMapEntry<>(filePos, size, key);
//    }
//
//    /**
//     * Finds the smallest gap that can hold a serialized object.
//     *
//     * @param objectSize the size of the serialized object
//     * @return the file position, or -1 if not found
//     */
//    private long findBestFitGap(int objectSize) {
//        long result = -1;
//
//        //log.debug("Finding smallest gap for {}-byte object", objectSize);
//
//        assert (fileGaps != null);
//        for (Iterator<FileHashMapEntry<K>> it = fileGaps.iterator();
//             it.hasNext(); ) {
//            FileHashMapEntry<K> gap = it.next();
//
//            long pos = gap.getFilePosition();
//            int size = gap.getObjectSize();
//
//            //log.debug("Gap: pos={}, size={}", pos, size);
//            if (size >= objectSize) {
//                //log.debug("Found it.");
//                result = pos;
//
//                if (size > objectSize) {
////                    log.debug("Gap size is larger than required. Making " +
////                            "smaller gap.");
//
//                    // Remove it and re-insert it, since the gap list is
//                    // sorted by size.
//
//                    it.remove();
//
//                    pos += objectSize;
//                    size -= objectSize;
//                    gap.setFilePosition(pos);
//                    gap.setObjectSize(size);
//
//                    log.debug("Saving new, smaller gap: pos={}, size={}", pos, size);
//
//                    fileGaps.add(gap);
//                }
//
//                break;
//            }
//        }
//
//        log.debug("findBestFitGap: returning {}", result);
//        return result;
//    }
//
//    private void deleteMapFiles() {
//
//        if (valuesDBPath != null) {
//            valuesDBPath.delete();
//            valuesDBPath = null;
//        }
//
//        if (indexFilePath != null) {
//            indexFilePath.delete();
//            indexFilePath = null;
//        }
//    }
//}