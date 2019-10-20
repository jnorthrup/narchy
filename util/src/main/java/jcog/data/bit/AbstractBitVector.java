package jcog.data.bit;

/*		 
 * DSI utilities
 *
 * Copyright (C) 2007-2015 Sebastiano Vigna 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http:
 * 
 */


import java.util.stream.LongStream;

/** An abstract implementation of a {@link BitVector}.
 * 
 * <P>This abstract implementation provides almost all methods: you have to provide just
 * {@link it.unimi.dsi.fastutil.booleans.BooleanList#getBoolean(int)} and
 * {@link java.util.List#size()}. No attributes are defined.
 * 
 * <P>Note that the integer-set view provided by {@link #asLongSet()} is not cached: if you
 * want to cache the result of the first call, you must do your own caching.
 * 
 * <p><strong>Warning</strong>: this class has several optimised methods
 * that assume that {@link #getLong(long, long)} is implemented efficiently when its
 * arguments are multiples of {@link Long#SIZE} (see, e.g., the implementation
 * of {@link #compareTo(BitVector)} and {@link #longestCommonPrefixLength(BitVector)}).
 * If you want speed up the processing of your own {@link BitVector} implementations,
 * just implement {@link #getLong(long, long)} so that it is fast under the above conditions. 
 */
public abstract class AbstractBitVector implements BitVector {
	
    protected void ensureRestrictedIndex( long index ) {
        if ( index < 0L)  throw new IndexOutOfBoundsException( "Index (" + index + ") is negative" );
        if ( index >= length() ) throw new IndexOutOfBoundsException( "Index (" + index + ") is greater than or equal to length (" + ( length() ) + ')');
    }
	
    protected void ensureIndex( long index ) {
        if ( index < 0L)  throw new IndexOutOfBoundsException( "Index (" + index + ") is negative" );
        if ( index > length() ) throw new IndexOutOfBoundsException( "Index (" + index + ") is greater than length (" + ( length() ) + ')');
    }

    public void set( int index ) { set( index, true ); }
	public void clear( int index ) { set( index, false ); }
	public void flip( int index ) { set( index, ! getBoolean( index ) ); }

	@Override
	public void set(long index ) { set( index, true ); }
	@Override
	public void clear(long index ) { set( index, false ); }
	@Override
	public void flip(long index ) { set( index, ! getBoolean( index ) ); }
	
	@Override
	public void fill(boolean value ) { for(long i = length(); i-- != 0L; ) set( i, value ); }
	@Override
	public void fill(int value ) { fill( value != 0 ); }
	@Override
	public void flip() { for(long i = length(); i-- != 0L; ) flip( i ); }

	@Override
	public void fill(long from, long to, boolean value ) { BitVectors.ensureFromTo( length(), from, to ); for(long i = to; i-- != from; ) set( i, value ); }
	@Override
	public void fill(long from, long to, int value ) { fill( from, to, value != 0 ); }
	@Override
	public void flip(long from, long to ) { BitVectors.ensureFromTo( length(), from, to ); for(long i = to; i-- != from; ) flip( i ); }

	@Override
	public int getInt(long index ) { return getBoolean( index ) ? 1 : 0; }
	@Override
	public long getLong(long from, long to ) {
		if ( to - from > 64L) throw new IllegalArgumentException( "Range too large for a long: [" + from + ".." + to + ')');
		long acc = 0L;
		for (long i = from; i < to; i++) {
			if (getBoolean(i)) {
				long l = 1L << i - from;
				acc = acc | l;
			}
		}
		return acc;
	}
	public boolean getBoolean( int index ) { return getBoolean( (long)index ); }

	public boolean removeBoolean( int index ) { return removeBoolean( (long)index ); }
	public boolean set( int index, boolean value ) { return set( (long)index, value ); }
	public void add( int index, boolean value ) { add( (long)index, value ); }

	@Override
	public boolean removeBoolean(long index ) { throw new UnsupportedOperationException(); }
	@Override
	public boolean set(long index, boolean value ) { throw new UnsupportedOperationException(); }
	@Override
	public void add(long index, boolean value ) { throw new UnsupportedOperationException(); }
	
	@Override
	public void set(long index, int value ) { set( index, value != 0 ); }
	@Override
	public void add(long index, int value ) { add( index, value != 0 ); }
	public boolean add( boolean value ) { add( length(), value ); return true; }
	@Override
	public void add(int value ) { add( value != 0 ); }

	@Override
	public BitVector append(long value, int k ) {
		for(int i = 0; i < k; i++ ) add( ( value & 1L << i ) != 0L);
		return this;
	}

	@Override
	public BitVector append(BitVector bv ) {
        long length = bv.length();
        long l = length - length % (long) Long.SIZE;
		
		long i;
		for(i = 0L; i < l; i = i + (long) Long.SIZE) append( bv.getLong( i, i + (long) Long.SIZE), Long.SIZE );
		if ( i < length ) append( bv.getLong( i, length ), (int)( length - i ) );
		return this;
	}

	@Override
	public BitVector copy() { return copy(0L, (long) size()); }

	@Override
	public BitVector copy(long from, long to ) {
		BitVectors.ensureFromTo( length(), from, to );
        long length = to - from;
        long l = length - length % (long) Long.SIZE;
        long[] bits = new long[(int) ((length + (long) Long.SIZE - 1L) / (long) Long.SIZE)];
		long i;
		for(i = 0L; i < l; i = i + (long) Long.SIZE) bits[ (int)( i / (long) Long.SIZE) ] = getLong( from + i, from + i + (long) Long.SIZE);
		if ( i < length ) bits[ (int)( i / (long) Long.SIZE) ] = getLong( from + i, to );
		return LongArrayBitVector.wrap( bits, length );
	}
	
	/** Returns an instance of {@link LongArrayBitVector} containing a copy of this bit vector.
	 *
	 * @return an instance of {@link LongArrayBitVector} containing a copy of this bit vector.
	 */
	
	@Override
	public BitVector fast() {
		return copy();
	}
	
	@Override
	public long count() {
		long c = 0L;
		for(long i = length(); i-- != 0L; ) c = c + (long) getInt(i);
		return c;
	}
	
	@Override
	public long firstOne() {
		return nextOne(0L);
	}
	
	@Override
	public long lastOne() {
		return previousOne( length() );
	}
	
	@Override
	public long firstZero() {
		return nextZero(0L);
	}
	
	@Override
	public long lastZero() {
		return previousZero( length() );
	}
	
	@Override
	public long nextOne(long index ) {
        long length = length();
		for (long l = index; l < length; l++) {
			if (getBoolean(l)) {
				return l;
			}
		}
		return -1L;
	}
	
	@Override
	public long previousOne(long index ) {
		for (long i = index; i-- != 0L; ) if ( getBoolean( i ) ) return i;
		return -1L;
	}
	
	@Override
	public long nextZero(long index ) {
        long length = length();
		for (long i = index; i < length; i++) {
			if (!getBoolean(i)) {
				return i;
			}
		}
		return -1L;
	}
	
	@Override
	public long previousZero(long index ) {
		for (long i = index; i-- != 0L; ) if ( ! getBoolean( i ) ) return i;
		return -1L;
	}
	
	@Override
	public long longestCommonPrefixLength(BitVector v ) {
        long minLength = Math.min( length(), v.length() );
        long l = minLength - minLength % (long) Long.SIZE;
		long w0, w1;
		
		long i;
		for(i = 0L; i < l; i = i + (long) Long.SIZE) {
			w0 = getLong( i, i + (long) Long.SIZE);
			w1 = v.getLong( i, i + (long) Long.SIZE);
			if ( w0 != w1 ) return i + (long) Long.numberOfTrailingZeros(w0 ^ w1);
		}

		w0 = getLong( i, minLength );
		w1 = v.getLong( i, minLength );

		if ( w0 != w1 ) return i + (long) Long.numberOfTrailingZeros(w0 ^ w1);
		return minLength;
	}	
	
	@Override
	public boolean isPrefix(BitVector v ) {
		return longestCommonPrefixLength( v ) == length();
	}
	
	@Override
	public boolean isProperPrefix(BitVector v ) {
		return isPrefix( v ) && length() < v.length();
	}
	
	@Override
	public BitVector and(BitVector v ) {
		for(long i = Math.min( size64(), v.size64() ); i-- != 0L; ) if ( ! v.getBoolean( i ) ) clear( i );
		return this;
	}
	
	@Override
	public BitVector or(BitVector v ) {
		for(long i = Math.min( size64(), v.size64() ); i-- != 0L; ) if ( v.getBoolean( i ) ) set( i );
		return this;
	}

	@Override
	public BitVector xor(BitVector v ) {
		for(long i = Math.min( size64(), v.size64() ); i-- != 0L; ) if ( v.getBoolean( i ) ) flip( i );
		return this;
	}

	@Deprecated
	public int size() {
        long length = length();
		if ( length > (long) Integer.MAX_VALUE) throw new IllegalStateException( "The number of bits of this bit vector (" + length + ") exceeds Integer.MAX_INT" );
		return (int)length;
	}
	
	public void size( int newSize ) {
		length((long) newSize);
	}

	
	public void clear() {
		length(0L);
	}
	
	@Override
	public BitVector replace(BitVector bv ) {
		clear();
        long fullBits = bv.length() - bv.length() % (long) Long.SIZE;
		for(long i = 0L; i < fullBits; i = i + (long) Long.SIZE) append( bv.getLong( i, i + (long) Long.SIZE), Long.SIZE );
		if ( bv.length() % (long) Long.SIZE != 0L) append( bv.getLong( fullBits, bv.length() ), (int)( bv.length() - fullBits ) );
		return this;
	}

	public boolean equals( Object o ) {
		if ( ! ( o instanceof BitVector ) ) return false;
        BitVector v = (BitVector)o;
        long length = length();
		if ( length != v.length() ) return false;
        long fullLength = length - length % (long) Long.SIZE;
        return LongStream.iterate(0L, i -> i < fullLength, i -> i + (long) Long.SIZE).noneMatch(i -> getLong(i, i + (long) Long.SIZE) != v.getLong(i, i + (long) Long.SIZE)) && getLong(fullLength, length) == v.getLong(fullLength, length);
	}

	@Override
	public boolean equals(BitVector v, long start, long end ) {
        long startFull = start - start % (long) LongArrayBitVector.BITS_PER_WORD;
        long endFull = end - end % (long) LongArrayBitVector.BITS_PER_WORD;
        int startBit = (int)( start & (long) LongArrayBitVector.WORD_MASK);
        int endBit = (int)( end & (long) LongArrayBitVector.WORD_MASK);
		
		if ( startFull == endFull )
				return ( ( getLong( startFull, Math.min( length(), startFull + (long) Long.SIZE) )
						^ v.getLong( startFull, Math.min( v.length(), startFull + (long) Long.SIZE) ) )
							& ( ( 1L << ( endBit - startBit ) ) - 1L) << startBit ) == 0L;
		
		if ( ( ( getLong( startFull, startFull + (long) Long.SIZE) ^ v.getLong( startFull, startFull = startFull + (long) Long.SIZE) ) & ( -1L << startBit ) ) != 0L) return false;

		while( startFull < endFull ) if ( getLong( startFull, startFull + (long) Long.SIZE) != v.getLong( startFull, startFull = startFull + (long) Long.SIZE) ) return false;
		
		return ( ( getLong( startFull, Math.min( length(), startFull + (long) Long.SIZE) )
				^ v.getLong( startFull, Math.min( v.length(), startFull + (long) Long.SIZE) ) )
					& ( 1L << endBit) - 1L) == 0L;
	}


	
	public int hashCode() {
        long length = length();
        long fullLength = length - length % (long) Long.SIZE;
        long h = 0x9e3779b97f4a7c13L ^ length;

		for(long i = 0L; i < fullLength; i = i + (long) Long.SIZE) h ^= ( h << 5 ) + getLong( i, i + (long) Long.SIZE) + ( h >>> 2 );
		if ( length != fullLength ) h ^= ( h << 5 ) + getLong( fullLength, length ) + ( h >>> 2 );

		return (int)( ( h >>> 32 ) ^ h );
	}
	
	@Override
	public long[] bits() {
        long[] bits = new long[ (int)( ( length() + (long) LongArrayBitVector.BITS_PER_WORD - 1L) >> LongArrayBitVector.LOG2_BITS_PER_WORD ) ];
        long length = length();
		for(long i = 0L; i < length; i++ ) if ( getBoolean( i ) ) bits[ (int)( i >> LongArrayBitVector.LOG2_BITS_PER_WORD ) ] |= 1L << i;
		return bits;
	}
	

























































































































































































































































		
	@Override
	public BitVector length(long newLength ) {
        long length = length();
		if ( length < newLength ) for(long i = newLength - length; i-- != 0L; ) add( false );
		else for(long i = length; i-- != newLength; ) removeBoolean( i );
		return this;
	}

	public void size( long newSize ) {
		length( newSize );
	}

	/*public LongSortedSet asLongSet() {
		return new LongSetView( this, 0, Long.MAX_VALUE );
	}	
	
	public LongBigList asLongBigList( final int width ) {
		return new LongBigListView( this, width );
	}	
	*/

	public SubBitVector subList( int from, int to ) {
		return new SubBitVector( this, (long) from, (long) to);
	}

	@Override
	public BitVector subVector(long from, long to ) {
		return new SubBitVector( this, from, to );
	}

	@Override
	public BitVector subVector(long from ) {
		return subVector( from, length() );
	}






	
	public int compareTo( BitVector v ) {
        long minLength = Math.min( length(), v.length() );
        long l = minLength - minLength % (long) Long.SIZE;
		long w0, w1, xor;
		
		long i;
		for(i = 0L; i < l; i = i + (long) Long.SIZE) {

			w0 = getLong( i, i + (long) Long.SIZE);
			w1 = v.getLong( i, i + (long) Long.SIZE);
			xor = w0 ^ w1;
			if ( xor != 0L) return ( xor & -xor & w0 ) == 0L ? -1 : 1;
		}

		w0 = getLong( i, minLength );
		w1 = v.getLong( i, minLength );
		xor = w0 ^ w1;
		if ( xor != 0L) return ( xor & -xor & w0 ) == 0L ? -1 : 1;

		return Long.signum( length() - v.length() );
	}	
	

	/** Returns a string representation of this vector.
	 * 
	 * <P>Note that this string representation shows the bit of index 0 at the leftmost position.
	 * @return a string representation of this vector, with the bit of index 0 on the left.
	 */
	
	public String toString() {
        long size = size64();
		StringBuilder sb = new StringBuilder();
		for (long i = 0L; i < size; i++) {
			String s = String.valueOf(getInt(i));
			sb.append(s);
		}
		return sb.toString();
	}

	/** A subvector of a given bit vector, specified by an initial and a final bit. */
	
	public static class SubBitVector extends AbstractBitVector {
		protected final BitVector bitVector;
		protected long from;
		protected long to;
		
		public SubBitVector( BitVector l, long from, long to ) {
			BitVectors.ensureFromTo( l.length(), from, to );
			this.from = from;
			this.to = to;
			bitVector = l;
		}
		
		@Override
		public boolean getBoolean(long index ) {
			ensureIndex( index );
			return bitVector.getBoolean( from + index ); 
		}
		
		@Override
		public int getInt(long index ) { return getBoolean( index ) ? 1 : 0; }
		
		@Override
		public boolean set(long index, boolean value ) {
			ensureIndex( index );
			return bitVector.set( from + index, value ); 
		}
		
		@Override
		public void set(long index, int value ) { set( index, value != 0 ); }
		
		@Override
		public void add(long index, boolean value ) {
			ensureIndex( index );
			bitVector.add( from + index, value ); to++; 
		}
		
		@Override
		public void add(long index, int value ) { add( index, value != 0 ); to++; }
		@Override
		public void add(int value ) { bitVector.add( to++, value ); }
		
		@Override
		public boolean removeBoolean(long index ) {
			ensureIndex( index );
			to--; 
			return bitVector.removeBoolean( from + index ); 
		} 

		@Override
		public BitVector copy(long from, long to ) {
			BitVectors.ensureFromTo( length(), from, to );
			return bitVector.copy( this.from + from, this.from + to );
		}
		
		@Override
		public BitVector subVector(long from, long to ) {
			BitVectors.ensureFromTo( length(), from, to );
			return new SubBitVector( bitVector, this.from + from, this.from + to );
		}
		
		@Override
		public long getLong(long from, long to ) {
			BitVectors.ensureFromTo( length(), from, to );
			return bitVector.getLong( from + this.from, to + this.from );
		}
		
		@Override
		public long length() {
			return to - from;
		}

		@Override
		public long size64() {
			return length();
		}
}
}
