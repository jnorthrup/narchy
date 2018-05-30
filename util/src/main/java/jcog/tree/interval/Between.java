package jcog.tree.interval;

import org.jetbrains.annotations.NotNull;

/** defines an interval between two comparable values */
public class Between<K extends Comparable<? super K>> implements Comparable<Between<K>> {
	
	@NotNull public final K low, high;
	
	public Between(@NotNull K low, @NotNull K high){
        this.low = low;
        this.high = high;
	}

	@NotNull
	final K getHigh() {
		return high;
	}



    @NotNull
	final K getLow() {
		return low;
	}








    final boolean contains(@NotNull K p){
		return low.compareTo(p) <= 0 && high.compareTo(p) > 0;
	}
	
	/**
	 * Returns true if this Interval wholly contains i.
	 */
    final boolean contains(@NotNull Between<K> i){
		return contains(i.low) && contains(i.high);
	}
	
	final boolean overlaps(@NotNull K low, @NotNull K high){
		return  this.low.compareTo(high) <= 0 &&
				this.high.compareTo(low) > 0;
	}
	
	final boolean overlaps(@NotNull Between<K> i){
		return overlaps(i.low,i.high);
	}
	
	@Override
	public final String toString() {
		return String.format("[%s..%s]", low, high);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Between)) return false;

		Between<?> between = (Between<?>) o;

		return low.equals(between.low) && high.equals(between.high);

	}

	@Override
	public int hashCode() {
		int result = low.hashCode();
		result = 31 * result + high.hashCode();
		return result;
	}

	@Override
	public int compareTo(@NotNull Between<K> x) {
		int leftC = low.compareTo(x.low);
		if (leftC != 0) return leftC;
		int rightC = high.compareTo(x.high);
		return rightC;
	}
}
