package jcog.data.set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
 * TreeSet data structure for O(logn) complexity
 * from: https://github.com/rhenvar/TreeSet/blob/master/TreeSet.java
 */
public class MetalTreeSet<T extends Comparable> extends AbstractSet<T> implements Iterable<T>, SortedSet<T> {

	private TreeNode<T> root = null;
	private int size = 0;

	public MetalTreeSet() {

	}

	public MetalTreeSet(T[] initialValues) {
		for (T t : initialValues)
			addFast(t);
	}

	public final boolean add(T t) {
		int sizeBefore = size;
		addFast(t);
		return (size > sizeBefore);
	}

	public final void addFast(T t) {
		this.root = add(root, t);
	}

	private TreeNode<T> add(final TreeNode<T> n, T y) {
		if (n != null) {
			T x = n.x;
			int c = x != y ? x.compareTo(y) : 0;
			if (c > 0) {
				n.left = add(n.left, y);
			} else if (c < 0) {
				n.right = add(n.right, y);
			}

			return n;
		} else {
			this.size++;
			return new TreeNode<>(y);
		}
	}

	public boolean remove(Object t) {
		int sizeBefore = size;
		removeFast(t);
		return (sizeBefore > size);
	}

	public void removeFast(Object t) {
		this.root = remove(root, t);
	}

	private TreeNode<T> remove(final TreeNode<T> n, Object y) {
		if (n != null) {
			T x = n.x;
			TreeNode<T> nr = n.right, nl = n.left;
			int c = x != y ? x.compareTo(y) : 0;
			if (c == 0) {
				size--;
				if (nr == null) {
					return nl;
				} else if (nl == null) {
					return nr;
				} else {
					TreeNode<T> min = nr.first();
					min.right = remove(nr, min.x);
					min.left = nl;
					return min;
				}
			} else if (c > 0) {
				n.left = remove(nl, y);
			} else {
				n.right = remove(nr, y);
			}
		}
		return n;
	}

	public final boolean contains(T t) {
		return this.contains(root, t);
	}

	private boolean contains(TreeNode<T> n, T y) {
		if (n != null) {
			T x = n.x;
			int xt = x != y ? x.compareTo(y) : 0;
			return xt == 0 || contains(xt > 0 ? n.left : n.right, y);
		}
		return false;
	}

	public int size() {
		return this.size;
	}

	@Nullable
	@Override
	public Comparator<? super T> comparator() {
		throw new UnsupportedOperationException();
	}

	
	@Override
	public SortedSet<T> subSet(T t, T e1) {
		throw new UnsupportedOperationException();
	}

	
	@Override
	public SortedSet<T> headSet(T t) {
		throw new UnsupportedOperationException();
	}

	
	@Override
	public SortedSet<T> tailSet(T t) {
		throw new UnsupportedOperationException();
	}

	public T first() {
		return this.firstNode().x;
	}

	public T last() {
		return this.lastNode().x;
	}

	@Nullable
	private TreeNode<T> firstNode() {
		return root!=null ? root.first() : null;
	}


	@Nullable
	private TreeNode<T> lastNode() {
		return root!=null ? root.last() : null;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public <X> X[] toArray(X[] target) {
		int size = this.size;
		if (target.length < size)
			target = Arrays.copyOf(target, size);

		if (size > 0)
			root.toArray(target, 0);

		return target;
	}

	@Override
	public String toString() {
		return Joiner.on(',').join(this);
	}

	@Override
	public Iterator<T> iterator() {
		switch (size) {
			case 0:
				return Util.emptyIterator;
			case 1:
				return Iterators.singletonIterator(root.x);
			case 2:
				return ArrayIterator.iterator(root.x, ((root.left!=null ? root.left : root.right).x));
			//TODO optimized 3 cases
			default:
				return new TreeSetIterator<>(root, size);
		}
	}

	public void clear() {
		size = 0;
		root = null;
	}


	private static final class TreeNode<T> {
		public final T x;
		public TreeNode<T> left, right; //<-- TODO type Object and delay instantiation of actual node until it becomes necessary

		public TreeNode(T t) {
			this.x = t;
		}

		public boolean isLeaf() {
			return left == null && right == null;
		}

		public @Nullable MetalTreeSet.TreeNode<T> first() {
			return left == null ? this : left.first();
		}

		public @Nullable MetalTreeSet.TreeNode<T> last() {
			return right == null ? this : right.last();
		}

		int toArray(Object[] target, int i) {

			if (left != null)
				i = left.toArray(target, i);

			target[i++] = x;

			if (right != null)
				i = right.toArray(target, i);

			return i;
		}

	}

	private final static class TreeSetIterator<T> extends FasterList<TreeNode<T>> implements Iterator<T> {

		private TreeNode<T> next;

		private TreeSetIterator(TreeNode<T> root, int size) {
			super(size / 2 /* TODO better size estimate, log2(n) or something */);
			TreeNode<T> n = root;
			do {
				this.add(n);
			} while ((n = n.left) != null);
			this.next = getLast();
		}

		@Override
		public boolean hasNext() {
			return !isEmpty();
		}

		@Override
		public T next() {
//			if (!this.hasNext())
//				throw new IllegalStateException("You have attempted to get the next value on an empty set!");
			TreeNode<T> n = this.removeLast();
			if (n.right != null) {
				TreeNode<T> r = n.right;
				TreeNode<T> l = r.left;

				this.add(r);

				while (l != null) {
					this.add(l);
					l = l.left;
				}
			}
			return (this.next = n).x;
		}

		@Override
		public void remove() {
//			if (this.next == null)
//				throw new IllegalStateException("You have attempted to remove a value from an empty set!");

			//MetalTreeSet.this.remove(next.x);
			throw new TODO();
		}
	}
}
