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


import jcog.data.array.LongArrays;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

/** A bit vector implementation based on arrays of longs.
 * 
 * <P>The main goal of this class is to be fast and flexible. It implements a lightweight, 
 * fast, open, optimized, reuse-oriented version of bit vectors. Instances of this class
 * represent a bit vector an array of longs that is enlarged as needed when new entries
 * are created (by dividing the current length by the golden ratio), but is
 * <em>never</em> made smaller (even on a {@link #clear()}). Use 
 * {@link #trim()} for that purpose.
 * 
 * <p>Besides usual methods for setting and getting bits, this class provides <em>views</em>
 * that make it possible to access comfortably the bit vector in different ways: for instance,
 * {@link #asLongBigList(int)} provide access as a list of longs, whereas
 * {@link #asLongSet()} provides access in setwise form.
 * 
 * <p>When enlarging the underlying array (e.g., for {@link #append(long, int)} operations or
 * add operations on the {@linkplain #asLongBigList(int) big list view}), or when
 * invoking {@link #ensureCapacity(long)}, this class calls
 * {@link LongArrays#grow(long[], int, int)}, which could enlarge the array more than
 * expected. On the contrary, {@link #length(long)} (and the corresponding method in the
 * {@linkplain #asLongBigList(int) big list view}) sizes the underlying array in an exact manner.
 * 
 * <P>Bit numbering follows the right-to-left convention: bit <var>k</var> (counted from the
 * right) of word <var>w</var> is bit 64<var>w</var> + <var>k</var> of the overall bit vector.
 *
 * <P>If {@link #CHECKS} is true at compile time, boundary checks for all bit operations
 * will be compiled in. For maximum speed, you may want to recompile this class with {@link #CHECKS}
 *  set to false. {@link #CHECKS} is public, so you can check from your code whether you're
 * being provided a version with checks or not.
 * 
 * <p><strong>Warning</strong>: A few optional methods have still to be implemented (e.g.,
 * adding an element at an arbitrary position using the list view).
 * 
 * <p><strong>Warning</strong>: In some cases, you might want to cache locally the result
 * of {@link #bits()} to speed up computations on immutable bit vectors (this is what happens, for instance,
 * in <a href="http:
 * of the bit vector: as a result, all cached references to the result of {@link #bits()}
 * must be marked as transient and rebuilt at deserialisation
 * time, or you will end up saving the bits twice. 
 */

public class LongArrayBitVector extends AbstractBitVector implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	public static final int LOG2_BITS_PER_WORD = 6;
	public static final int BITS_PER_WORD = 1 << LOG2_BITS_PER_WORD;
	public static final int WORD_MASK = BITS_PER_WORD - 1;
	public static final int LAST_BIT = BITS_PER_WORD - 1;
	public static final long ALL_ONES = 0xFFFFFFFFFFFFFFFFL;
	public static final long LAST_BIT_MASK = 1L << LAST_BIT;
	
	/** Whether this class has been compiled with index checks or not. */
	public static final boolean CHECKS = true;
	private static final boolean ASSERTS = false;
	private static final long[] EMPTY_ARRAY = new long[0];

	/** The number of bits in this vector. */
	protected long length;
	/** The backing array of this vector. Bit 0 of the first element contains bit 0 of the bit vector, 
	 * bit 1 of the second element contains bit {@link #BITS_PER_WORD} of the bit vector and so on. */
	protected transient long[] bits;

	/** Returns the number of words that are necessary to hold the given number of bits.
	 * 
	 * @param size a number of bits.
	 * @return the number of words that are necessary to hold the given number of bits.
	 */

	protected static int numWords(long size ) {
        assert !ASSERTS || (size + (long) WORD_MASK) >>> LOG2_BITS_PER_WORD <= (long) Integer.MAX_VALUE;
		return (int)( ( size + (long) WORD_MASK) >>> LOG2_BITS_PER_WORD );
	}

	/** Return the index of the word that holds a bit of specified index.
	 * 
	 * @param index the index of a bit, or -1.
	 * @return the index of the word that holds the bit of given index, or -1 
	 * if <code>index</code> is -1.
	 */
	protected static int word(long index ) {
        assert !ASSERTS || index >>> LOG2_BITS_PER_WORD <= (long) Integer.MAX_VALUE;
		return (int)( index >>> LOG2_BITS_PER_WORD );
	}

	/** Returns the inside-word index of the bit that would hold the bit of specified index.
	 * 
	 * <P>Note that bit 0 is positioned in word 0, index 0, bit 1 in word 0, index 1, &hellip;,
	 * bit {@link #BITS_PER_WORD} in word 0, index 0, bit {@link #BITS_PER_WORD} + 1 in word 1, index 1,
	 * and so on.
	 * 
	 * @param index the index of a bit.
	 * @return the inside-word index of the bit that would hold the bit of specified index.
	 */
	protected static int bit(long index ) {
		return (int)( index & (long) WORD_MASK);
	}

	/** Returns a mask having a 1 exactly at the bit {@link #bit(long) bit(index)}.
	 * 
	 * @param index the index of a bit
	 * @return a mask having a 1 exactly at the bit {@link #bit(long) bit(index)}.
	 */

	protected static long mask(long index ) {
		return 1L << index;
	}

	protected LongArrayBitVector( long capacity ) {
		if ( capacity > ((long) Integer.MAX_VALUE + 1L ) * (long) Long.SIZE) throw new IndexOutOfBoundsException( "In this implementations bit vectors can be at most 2^37 bits long" );
		bits = capacity > 0L ? new long[ numWords( capacity ) ] : EMPTY_ARRAY;
	}

	/** Creates a new empty bit vector of given capacity. The 
	 * resulting vector will be able to contain <code>capacity</code>
	 * bits without reallocations of the backing array.
	 * 
	 * <P>Note that this constructor creates an <em>empty</em> bit vector.
	 * If you want a cleared bit vector of a specified size, please
	 * use the {@link #ofLength(long)} factory method.
	 * 
	 * @param capacity the capacity (in bits) of the new bit vector.
	 * @return a new bit vector of given capacity.
	 */
	public static LongArrayBitVector getInstance( long capacity ) {
		return new LongArrayBitVector( capacity );
	}

	/** Creates a new empty bit vector. No allocation is actually performed. 
	 * @return a new bit vector with no capacity.
	 */
	public static LongArrayBitVector getInstance() {
		return new LongArrayBitVector(0L);
	}
	
	/** Creates a new empty bit vector of given length.
	 * 
	 * @param length the size (in bits) of the new bit vector.
	 */
	public static LongArrayBitVector ofLength( long length ) {
		return new LongArrayBitVector( length ).length( length );
	}
	
	/** Creates a new bit vector with given bits. 
	 * 
	 * @param bit a list of bits that will be set in the newly created bit vector.
	 */
	public static LongArrayBitVector of( int... bit ) {
        LongArrayBitVector bitVector = new LongArrayBitVector((long) bit.length);
		for(int b : bit ) {
			if ( b != 0 && b != 1 ) throw new IllegalArgumentException( "Illegal bit value: " + b );
			bitVector.add( b );
		}
		return bitVector;
	}
	
	@Override
	public long[] bits() {
		return bits;
	}
	
	@Override
	public long length() {
		return length;
	}
	
	/** Ensures that this bit vector can hold the specified number of bits.
	 * 
	 * <p>This method uses {@link LongArrays#grow(long[], int, int)} to
	 * ensure that there is enough space for the given number of bits. As a
	 * consequence, the actual length of the long array allocated might be
	 * larger than expected.
	 * 
	 * @param numBits the number of bits that this vector must be able to contain. 
	 * @return this bit vector.
	 */
	
	public LongArrayBitVector ensureCapacity( long numBits ) {
		if ( numBits > ((long) Integer.MAX_VALUE + 1L ) * (long) Long.SIZE) throw new IndexOutOfBoundsException( "In this implementations bit vectors can be at most 2^37 bits long" );
		bits = LongArrays.grow( bits, numWords( numBits ), numWords( length ) );
		return this;
	}

	@Override
	public LongArrayBitVector length(long newLength ) {
		bits = LongArrays.ensureCapacity(bits, numWords(newLength), numWords(length));
        long oldLength = length;
		if ( newLength < oldLength ) fill( newLength, oldLength, false );
		length = newLength;
		return this;
	}

	@Override
	public void fill( boolean value ) {
        int fullWords = (int)( length / (long) Long.SIZE);
		Arrays.fill( bits, 0, fullWords, value ? 0xFFFFFFFFFFFFFFFFL : 0L );
		if ( length % (long) Long.SIZE != 0L) {
			bits[fullWords] = value ? (1L << length % (long) Long.SIZE) - 1L : 0L;
		}
	}

	@Override
	public void fill( long from, long to, boolean value ) {
		if ( to / (long) Long.SIZE == from / (long) Long.SIZE) {
			if ( value ) bits[ (int)( from / (long) Long.SIZE) ] |= ( 1L << to - from ) - 1L << from;
			else bits[ (int)( from / (long) Long.SIZE) ] &= ~( ( 1L << to - from ) - 1L << from );
			return;
		}
		Arrays.fill( bits, (int)( ( from + (long) Long.SIZE - 1L) / (long) Long.SIZE), (int)( to / (long) Long.SIZE), value ? -1L : 0L );
		if ( from % (long) Long.SIZE != 0L) {
			if ( value ) bits[ (int)( from / (long) Long.SIZE) ] |= -1L << from % (long) Long.SIZE;
			else bits[ (int)( from / (long) Long.SIZE) ] &= ( 1L << from % (long) Long.SIZE) - 1L;
		}

		if ( to % (long) Long.SIZE != 0L) {
			if ( value ) bits[ (int)( to / (long) Long.SIZE) ] |= ( 1L << to % (long) Long.SIZE) - 1L;
			else bits[ (int)( to / (long) Long.SIZE) ] &= -1L << to % (long) Long.SIZE;
		}
	}

	@Override
	public void flip() {
        int fullWords = (int)( length / (long) Long.SIZE);
		for(int i = fullWords; i-- != 0; ) bits[ i ] ^= 0xFFFFFFFFFFFFFFFFL;
		if ( length % (long) Long.SIZE != 0L) bits[ fullWords ] ^= ( 1L << length % (long) Long.SIZE) - 1L;
	}

	@Override
	public void flip( long from, long to ) {
		if ( to / (long) Long.SIZE == from / (long) Long.SIZE) {
			bits[ (int)( from / (long) Long.SIZE) ] ^= ( 1L << to - from ) - 1L << from;
			return;
		}

        int start = (int)( ( from + (long) Long.SIZE - 1L) / (long) Long.SIZE);
		for(int i = (int)( to / (long) Long.SIZE); i-- != start; ) bits[ i ] ^= 0xFFFFFFFFFFFFFFFFL;
		
		if ( from % (long) Long.SIZE != 0L) bits[ (int)( from / (long) Long.SIZE) ] ^= -1L << from % (long) Long.SIZE;
		if ( to % (long) Long.SIZE != 0L) bits[ (int)( to / (long) Long.SIZE) ] ^= ( 1L << to % (long) Long.SIZE) - 1L;
	}

	/** Reduces as must as possible the size of the backing array.
	 * 
	 * @return true if some trimming was actually necessary.
	 */
	
	public boolean trim() {
		if ( bits.length == numWords( length ) ) return false;
		bits = LongArrays.setLength( bits, numWords( length ) );
		return true;
	}

	/** Sets the size of this bit vector to 0.
	 * <P>Note that this method does not try to reallocate that backing array.
	 * If you want to force that behaviour, call {@link #trim()} afterwards.
	 */
	@Override
	public void clear() {
		Arrays.fill( bits, 0, word( length - 1L) + 1, 0L);
		length = 0L;
	}

	@Override
	public LongArrayBitVector copy(long from, long to ) {
		BitVectors.ensureFromTo( length, from, to );

        LongArrayBitVector copy = new LongArrayBitVector( to - from );
		if ( ( copy.length = to - from ) == 0L) return copy;

        int numWords = numWords( to - from );
        int startWord = word( from );
        int startBit = bit( from );

		if ( startBit == 0 ) {
			
			System.arraycopy( bits, startWord, copy.bits, 0, numWords );
            int endBit = bit( to );
			if ( endBit > 0 ) copy.bits[ numWords - 1 ] &= ( 1L << endBit ) - 1L;
		}
		else if ( startWord == word( to - 1L) ) {
			
			copy.bits[ 0 ] = bits[ startWord ] >>> startBit & ( ( 1L << to - from ) - 1L);
		}
		else {
            long[] bits = this.bits;
            long[] copyBits = copy.bits;
			
			copyBits[ 0 ] = bits[ startWord ] >>> startBit;

            int bitsPerWordMinusStartBit = BITS_PER_WORD - startBit;
            for(int word = 1; word < numWords; word++ ) {
				copyBits[ word - 1 ] |= bits[ word + startWord ] << bitsPerWordMinusStartBit;
				copyBits[ word ] = bits[ word + startWord ] >>> startBit;
			}
            int endBit = bit( to - from );

			if ( endBit == 0 ) copyBits[ numWords - 1 ] |= bits[ numWords + startWord ] << bitsPerWordMinusStartBit;
			else {
				if ( endBit > bitsPerWordMinusStartBit ) copyBits[ numWords - 1 ] |= bits[ numWords + startWord ] << bitsPerWordMinusStartBit;
				copyBits[ numWords - 1 ] &= ( 1L << endBit ) - 1L;
			}
		}
	
		return copy;
	}

	@Override
	public LongArrayBitVector copy() {
        LongArrayBitVector copy = new LongArrayBitVector( length );
		copy.length = length;
		System.arraycopy( bits, 0, copy.bits, 0, numWords( length ) );
		return copy;
	}

	/** Returns this bit vector.
	 * 
	 * @return this bit vector.
	 */
	@Override
	public LongArrayBitVector fast() {
		return this;
	}
	
	/** Returns a copy of the given bit vector.
	 * 
	 * <p>This method uses {@link BitVector#getLong(long, long)} on {@link Long#SIZE} boundaries to copy at high speed.
	 * 
	 * @param bv a bit vector.
	 * @return an instance of this class containing a copy of the given vector.
	 */
	public static LongArrayBitVector copy( BitVector bv ) {
        long length = bv.length();
        LongArrayBitVector copy = new LongArrayBitVector( length );
        long fullBits = length - length % (long) Long.SIZE;
		for(long i = 0L; i < fullBits; i = i + (long) Long.SIZE) copy.bits[ (int)( i / (long) Long.SIZE) ] = bv.getLong( i, i + (long) Long.SIZE);
		if ( length % (long) Long.SIZE != 0L) copy.bits[ (int)( fullBits / (long) Long.SIZE) ] = bv.getLong( fullBits, length );
		copy.length = length;
		return copy;
	}
	
	@Override
	public boolean getBoolean(long index ) {
		if ( CHECKS ) ensureRestrictedIndex( index );
		return ( bits[ word( index ) ] & mask( index ) ) != 0L;
	}

	@Override
	public boolean set(long index, boolean value ) {
		if ( CHECKS ) ensureRestrictedIndex( index );
        int word = word( index );
        long mask = mask( index );
        boolean oldValue = ( bits[ word ] & mask ) != 0L;
		if ( value != oldValue ) bits[ word ] ^= mask;
		return oldValue;
	}

	@Override
	public void set(long index ) {
		if ( CHECKS ) ensureRestrictedIndex( index );
		bits[ word( index ) ] |= mask( index ); 
	}

	@Override
	public void clear(long index ) {
		if ( CHECKS ) ensureRestrictedIndex( index );
		bits[ word( index ) ] &= ~mask( index ); 
	}
	
	@Override
	public void add(long index, boolean value ) {
		if ( CHECKS ) ensureIndex( index );
		if ( length == (long)bits.length << LOG2_BITS_PER_WORD ) bits = LongArrays.grow( bits, numWords( length + 1L) );
		
		length++;

		if ( index == length - 1L) set( index, value );
		else {
            int word = word( index );
            int bit = bit( index );
            boolean carry = ( bits[ word ] & LAST_BIT_MASK ) != 0L;
            long t = bits[ word ];
			if ( bit == LAST_BIT ) t &= ~LAST_BIT_MASK;
			else t = ( t & - ( 1L << bit ) ) << 1 | t & ( 1L << bit ) - 1L;
			if ( value ) t |= 1L << bit;
			bits[ word ] = t;
            int numWords = numWords( length );
			for(int i = word + 1; i < numWords; i++ ) {
                boolean nextCarry = (bits[i] & LAST_BIT_MASK) != 0L;
                bits[ i ] <<= 1L;
				if ( carry ) bits[ i ] |= 1L;
				carry = nextCarry;
			}
		}

	}
	
	@Override
	public boolean removeBoolean(long index ) {
		if ( CHECKS ) ensureRestrictedIndex( index );
        boolean oldValue = getBoolean( index );
        long[] bits = this.bits;

        int word = word( index );
        int bit = bit( index );
		bits[ word ] = ( bits[ word ] & - ( 1L << bit ) << 1 ) >>> 1 | bits[ word ] & ( 1L << bit ) - 1L;
        int numWords = numWords( length-- );
		for(int i = word + 1; i < numWords; i++ ) {
			if ( ( bits[ i ] & 1L) != 0L) bits[ i - 1 ] |= LAST_BIT_MASK;
			bits[ i ] >>>= 1L;
		}

		return oldValue;
	}

	@Override
	public LongArrayBitVector append(long value, int width ) {
		if ( width == 0 ) return this;
		if (width < Long.SIZE && (value & -1L << width) != 0L) throw new IllegalArgumentException( "The specified value (" + value + ") is larger than the maximum value for the given width (" + width + ')');
        long length = this.length;
        int startWord = word( length );
        int startBit = bit( length );
		ensureCapacity( length + (long) width);

		if ( startBit + width <= Long.SIZE ) bits[ startWord ] |= value << startBit;
		else {
			bits[ startWord ] |= value << startBit;
			bits[ startWord + 1 ] = value >>> BITS_PER_WORD - startBit;
		}

        this.length = this.length + (long) width;
		return this;
	}

	@Override
	public long getLong(long from, long to ) {
		if ( CHECKS ) BitVectors.ensureFromTo( length, from, to );
        long l = (long) Long.SIZE - ( to - from );
        int startWord = word( from );
        int startBit = bit( from );
		if ( l == (long) Long.SIZE) return 0L;
		if ((long) startBit <= l ) return bits[ startWord ] << l - (long) startBit >>> l;
		return bits[ startWord ] >>> startBit | bits[ startWord + 1 ] << (long) Long.SIZE + l - (long) startBit >>> l;
	}

	@Override
	public long count() {
		long c = 0L;
		for(int i = numWords( length ); i-- != 0; ) c = c + (long) Long.bitCount(bits[i]);
		return c;
	}

	@Override
	public long nextOne(long index ) {
		if ( index >= length ) return -1L;
        long[] bits = this.bits;
		long words = (long) numWords(length);
        int from = word( index );
        long maskedFirstWord = bits[ from ] & -( 1L << index );
		if ( maskedFirstWord != 0L) return (long) (from * BITS_PER_WORD + Long.numberOfTrailingZeros(maskedFirstWord));

		for (int i = from + 1; (long) i < words; i++ )
			if ( bits[ i ] != 0L) return (long) (i * BITS_PER_WORD + Long.numberOfTrailingZeros(bits[i]));
		return -1L;
	}
	
	@Override
	public long previousOne(long index ) {
		if ( index == 0L) return -1L;
        long[] bits = this.bits;
        int from = word( index - 1L);
        long mask = 1L << index - 1L;
        long maskedFirstWord = bits[ from ] & ( mask | mask - 1L);
		if ( maskedFirstWord != 0L) return (long) (from * BITS_PER_WORD + Fast.mostSignificantBit(maskedFirstWord));

		for (int i = from; i-- != 0; )
			if ( bits[ i ] != 0L) return (long) (i * BITS_PER_WORD + Fast.mostSignificantBit(bits[i]));
		return -1L;
	}
	
	@Override
	public long nextZero(long index ) {
		if ( index >= length ) return -1L;
        long[] bits = this.bits;
		long words = (long) numWords(length);
        int from = word( index );
        long maskedFirstWord = bits[ from ] | ( 1L << index ) - 1L;
		if ( maskedFirstWord != 0xFFFFFFFFFFFFFFFFL ) {
			long result = (long) (from * BITS_PER_WORD + Long.numberOfTrailingZeros(~maskedFirstWord));
			return result >= length ? -1L : result;
		}

		for (int i = from + 1; (long) i < words; i++ )
			if ( bits[ i ] != 0xFFFFFFFFFFFFFFFFL ) {
				long result = (long) (i * BITS_PER_WORD + Long.numberOfTrailingZeros(~bits[i]));
				return result >= length ? -1L : result;
			}
		return -1L;
	}
	
	@Override
	public long previousZero(long index ) {
		if ( index == 0L) return -1L;
        long[] bits = this.bits;
        int from = word( index - 1L);
        long maskedFirstWord = bits[ from ] | -1L << index;
		if ( from == word( length - 1L) ) maskedFirstWord |= -1L << length % (long) Long.SIZE;
		if ( maskedFirstWord != 0xFFFFFFFFFFFFFFFFL )
			return (long) (from * BITS_PER_WORD + Fast.mostSignificantBit(~maskedFirstWord));

		for (int i = from; i-- != 0; )
			if ( bits[ i ] != 0xFFFFFFFFFFFFFFFFL )
				return (long) (i * BITS_PER_WORD + Fast.mostSignificantBit(~bits[i]));
		return -1L;
	}
	
	@Override
	public long longestCommonPrefixLength(BitVector v ) {
		if ( v instanceof LongArrayBitVector ) return longestCommonPrefixLength( (LongArrayBitVector)v );
		return super.longestCommonPrefixLength( v );
	}

	public long longestCommonPrefixLength( LongArrayBitVector v ) {
        long minLength = Math.min( v.length(), length() );
        long words = ( minLength + (long) BITS_PER_WORD - 1L) >> LOG2_BITS_PER_WORD;
        long[] bits = this.bits;
        long[] vBits = v.bits;
		
		for (int i = 0; (long) i < words; i++ )
			if ( bits[ i ] != vBits[ i ] ) 
				return Math.min( minLength, (long) ((i << LOG2_BITS_PER_WORD) + Long.numberOfTrailingZeros(bits[i] ^ vBits[i])));
		return minLength;
	}

	@Override
	public BitVector and(BitVector v ) {
		if ( v instanceof LongArrayBitVector ) {
            LongArrayBitVector l = (LongArrayBitVector)v;
            int words = Math.min( numWords( length() ), numWords( l.length() ) );
			while( words-- != 0 ) bits[ words ] &= l.bits[ words ];
		}
		else super.and( v );
		return this;
	}
	
	@Override
	public BitVector or(BitVector v ) {
		if ( v instanceof LongArrayBitVector ) {
            LongArrayBitVector l = (LongArrayBitVector)v;
            int words = Math.min( numWords( length() ), numWords( l.length() ) );
			while( words-- != 0 ) bits[ words ] |= l.bits[ words ];
		}
		else super.or( v );
		return this;
	}

	@Override
	public BitVector xor(BitVector v ) {
		if ( v instanceof LongArrayBitVector ) {
            LongArrayBitVector l = (LongArrayBitVector)v;
            int words = Math.min( numWords( length() ), numWords( l.length() ) );
			while( words-- != 0 ) bits[ words ] ^= l.bits[ words ];
		}
		else super.xor( v );
		return this;
	}


	
	
	
	/** Wraps the given array of longs in a bit vector for the given number of bits.
	 * 
	 * <p>Note that all bits in <code>array</code> beyond that of index
	 * <code>size</code> must be unset, or an exception will be thrown.
	 * 
	 * @param array an array of longs.
	 * @param size the number of bits of the newly created bit vector.
	 * @return a bit vector of size <code>size</code> using <code>array</code> as backing array.
	 */
	public static LongArrayBitVector wrap( long[] array, long size ) {
		if ( size > (long)array.length << LOG2_BITS_PER_WORD ) throw new IllegalArgumentException( "The provided array is too short (" + array.length + " elements) for the given size (" + size + ')');
        LongArrayBitVector result = new LongArrayBitVector(0L);
		result.length = size;
		result.bits = array;

        int arrayLength = array.length;
        int lastWord = (int)( size / (long) Long.SIZE);
		if ( lastWord < arrayLength && ( array[ lastWord ] & -(1L << size % (long) Long.SIZE)) != 0L)  throw new IllegalArgumentException( "Garbage beyond size in bit array" );
		for(int i = lastWord + 1; i < arrayLength; i++ ) if ( array[ i ] != 0L) throw new IllegalArgumentException( "Garbage beyond size in bit array" );
		return result;
	}

	/** Wraps the given array of longs in a bit vector.
	 * 
	 * @param array an array of longs.
	 * @return a bit vector of size <code>array.length * Long.SIZE</code> using <code>array</code> as backing array.
	 */
	public static LongArrayBitVector wrap( long[] array ) {
		return wrap( array, (long)array.length * (long) Long.SIZE);
	}

	/** Returns a cloned copy of this bit vector.
	 * 
	 * <P>This method is functionally equivalent to {@link #copy()},
	 * except that {@link #copy()} trims the backing array.
	 * 
	 * @return a copy of this bit vector.
	 */
	@Override
	public LongArrayBitVector clone() throws CloneNotSupportedException {
        LongArrayBitVector copy = (LongArrayBitVector)super.clone();
		copy.bits = bits.clone();
		return copy;
	}

	public LongArrayBitVector replace( LongArrayBitVector bv ) {
		ensureCapacity( bv.length );
        long[] bits = this.bits;
        long[] bvBits = bv.bits;
        int bvFirstFreeWord = word( bv.length - 1L) + 1;
		for(int i = bvFirstFreeWord; i-- != 0; ) bits[ i ] = bvBits[ i ];
        int thisFirstFreeWord = word( length - 1L) + 1;
		if ( bvFirstFreeWord < thisFirstFreeWord ) Arrays.fill( this.bits, bvFirstFreeWord, thisFirstFreeWord, 0L);
		length = bv.length;
		return this;
	}

	@Override
	public LongArrayBitVector replace( BitVector bv ) {
        long bvLength = bv.length();
		ensureCapacity( bvLength );
        long[] bits = this.bits;
        long fullBits = bvLength - bvLength % (long) Long.SIZE;
		for(long i = 0L; i < fullBits; i = i + (long) Long.SIZE) bits[ (int)( i / (long) Long.SIZE) ] = bv.getLong( i, i + (long) Long.SIZE);
        int bvFirstFreeWord = word( bvLength - 1L) + 1;
        int thisFirstFreeWord = word( length - 1L) + 1;
		if ( bvLength % (long) Long.SIZE != 0L) bits[ (int)( fullBits / (long) Long.SIZE) ] = bv.getLong( fullBits, bvLength );
		if ( bvFirstFreeWord < thisFirstFreeWord ) Arrays.fill( this.bits, bvFirstFreeWord, thisFirstFreeWord, 0L);
		length = bvLength;
		return this;
	}

	public boolean equals( Object o ) {
		if ( o instanceof LongArrayBitVector ) return equals( (LongArrayBitVector) o );
		return super.equals( o );
	}

	public boolean equals( LongArrayBitVector v ) {
		if ( length != v.length() ) return false;
        int i = numWords( length );
		while( i-- != 0 ) if ( bits[ i ] != v.bits[ i ] ) return false;
		return true;
	}

	public boolean equals( LongArrayBitVector v, long start, long end ) {
        int startWord = (int)( start >>> LongArrayBitVector.LOG2_BITS_PER_WORD );
        int endWord = (int)( end >>> LongArrayBitVector.LOG2_BITS_PER_WORD );
        int startBit = (int)( start & (long) LongArrayBitVector.WORD_MASK);
        int endBit = (int)( end & (long) LongArrayBitVector.WORD_MASK);
        long[] aBits = bits();
        long[] bBits = v.bits();
		
		if ( startWord == endWord ) return ( ( aBits[ startWord ] ^ bBits[ startWord ] ) & ( ( 1L << ( endBit - startBit ) ) - 1L) << startBit ) == 0L;
		
		if ( ( ( aBits[ startWord ] ^ bBits[ startWord++ ] ) & ( -1L << startBit ) ) != 0L) return false;

		while( startWord < endWord ) if ( aBits[ startWord ] != bBits[ startWord++ ] ) return false;
		
		return ( ( aBits[ endWord ] ^ bBits[ endWord] ) & ( 1L << endBit ) - 1L) == 0L;
	}

	public int hashCode() {
        long h = 0x9e3779b97f4a7c13L ^ length;
        int numWords = numWords( length );
		for(int i = 0; i < numWords; i++ )
			h ^= ( h << 5 ) + bits[ i ] + ( h >>> 2 );
        assert !ASSERTS || (int) ((h >>> 32) ^ h) == super.hashCode();
		return (int)( ( h >>> 32 ) ^ h );
	}
	




































































	
	private void writeObject( ObjectOutputStream s ) throws IOException {
		s.defaultWriteObject();
        int numWords = numWords( length );
		for(int i = 0; i < numWords; i++ ) s.writeLong( bits[ i ] );
	}

	private void readObject( ObjectInputStream s ) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
        int numWords = numWords( length );
		bits = new long[ numWords ];
		for(int i = 0; i < numWords; i++ ) bits[ i ] = s.readLong();
	}

}
