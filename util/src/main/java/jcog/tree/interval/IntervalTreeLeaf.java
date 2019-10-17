package jcog.tree.interval;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class IntervalTreeLeaf<K extends Comparable<? super K>,V> implements IntervalTreeNode<K, V>, Entry<Between<K>, V> {
	
	private final Between<K> key;
	private final V value;
	
	IntervalTreeLeaf(@NotNull K min, @NotNull K max, V value) {
		this(new Between<>(min, max),value);
	}

	public IntervalTreeLeaf(Between<K> key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
    public @NotNull String toString() {
		return key + "=" + value;
	}

	@Override
	public final boolean isLeaf() {
		return true;
	}

	@Override
    public @NotNull IntervalTreeNode<K, V> getLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
    public @NotNull IntervalTreeNode<K, V> getRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(@NotNull K point) {
		return key.contains(point);
	}
	
	@Override
	public boolean contains(@NotNull Between<K> interval) {
		return key.contains(interval);
	}

	@Override
	public boolean overlaps(@NotNull K low, @NotNull K high) {
		return key.overlaps(low,high);
	}

	@Override
	public boolean overlaps(@NotNull Between<K> interval) {
		return key.overlaps(interval);
	}

	@Override
    public final @NotNull K getLow() {
		return key.getLow();
	}

	@Override
    public final @NotNull K getHigh() {
		return key.getHigh();
	}

	@Override
	public final V getValue() {
		return value;
	}

	@Override
    public @NotNull IntervalTreeNode<K, V> put(@NotNull Between<K> key, V value) {
		IntervalTreeNode<K, V> putNode = new IntervalTreeLeaf<>(key, value);
		return this.key.getLow().compareTo(key.getLow()) < 0 ? new IntervalTreeBranch<>(this, putNode) : new IntervalTreeBranch<>(putNode, this);
	}

	@Override
	public final V getEqual(@NotNull Between<K> range) {
		if (getLow().equals(range.getLow()) && getHigh().equals(range.getHigh())) {
			return getValue();
		}
		return null;
	}


	@Override
    public @Nullable V getContain(@NotNull Between<K> range) {
		return range.contains(key) ? getValue() : null;
	}


	@Override
	public final void getOverlap(@NotNull Between<K> range, @NotNull Consumer<V> accumulator) {
		if(range.overlaps(key)){
			accumulator.accept(getValue());
		}
	}

	@Override
	public final void getOverlap(@NotNull Between<K> range,
								 @NotNull Collection<V> accumulator) {
		if(range.overlaps(key)){
			accumulator.add(getValue());
		}
	}

	@Override
	public void getContain(@NotNull Between<K> range,
						   @NotNull Collection<V> accumulator) {
		if(key.contains(range)){
			accumulator.add(getValue());
		}
	}

	@Override
	public final int size() {
		return 1;
	}

	@Override
	public void values(@NotNull Collection<V> accumulator) {
		accumulator.add(getValue());
	}

	@Override
    public @Nullable IntervalTreeNode<K, V> remove(@NotNull V value) {
		return value.equals(getValue()) ? null : this;
	}

	@Override
	public void entrySet(@NotNull Set<Entry<Between<K>, V>> accumulator) {
		accumulator.add(this);
	}

	@Override
	public final Between<K> getKey() {
		return key;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public final boolean containsValue(V value) {
		return getValue().equals(value);
	}

	@Override
	public final void keySet(@NotNull Set<Between<K>> accumulator) {
		accumulator.add(key);
	}

	@Override
	public final boolean containedBy(@NotNull Between<K> interval) {
		return interval.contains(key);
	}

	@Override
	public void searchContainedBy(@NotNull Between<K> range, @NotNull Collection<V> accumulator) {
		if(containedBy(range)){
			accumulator.add(getValue());
		}
	}

	@Override
	public void forEachContainedBy(Between<K> range, BiConsumer<Between<K>,V> accumulator) {
		if (containedBy(range))
			accumulator.accept(key, getValue());
	}

	@Override
	public Between<K> getRange() {
		return key;
	}

	@Override
	public int maxHeight() {
		return 1;
	}

	@Override
    public @Nullable IntervalTreeNode<K, V> removeAll(@NotNull Collection<V> values) {
		return values.contains(getValue()) ? null : this;
	}

	@Override
	public void averageHeight(@NotNull Collection<Integer> heights, int currentHeight) {
		heights.add(currentHeight + 1);
	}



	@Override
    public @Nullable IntervalTreeNode<K, V> removeOverlapping(@NotNull Between<K> range) {
		if(key.overlaps(range)){
			return null;
		}
		return this;
	}

	@Override
    public @Nullable IntervalTreeNode<K, V> removeContaining(@NotNull Between<K> range) {
		if(key.contains(range)){
			return null;
		}
		return this;
	}

	@Override
    public @Nullable IntervalTreeNode<K, V> removeContainedBy(@NotNull Between<K> range) {
		if(range.contains(key)){
			return null;
		}
		return this;
	}

}
