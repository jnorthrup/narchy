/** Ben F Rayfield offers Wavetree opensource GNU LGPL 2+ */
package smartblob.wavetree.bit;


/** Update 2015-5 changing to bigEndian since thats the way people think and multidimensional arrays are
<br><br>
All bitstrings length 0 to 16 are created as primitive wrappers when system boots,
and they each return the smaller bitstrings from cache for any substring.
*/
public final class Fast0To16Bits implements Bits{
	
	/** Java's stack is faster for ints than shorts or bytes. It uses either 32 or 64 bit blocks. */
	public final int siz, data;
	
	private final byte indexOfNthOne[], indexOfNthZero[];
	
	/** copyOfData[0] is highest bit */
	private final boolean copyOfData[];
	
	/** cache[howManyBits][range 0 to (1 << howManyBits)-1] */
	private static final Fast0To16Bits cache[][];
	static{
		cache = new Fast0To16Bits[17][];
		for(int howManyBits=0; howManyBits<cache.length; howManyBits++){
			cache[howManyBits] = new Fast0To16Bits[1 << howManyBits];
			for(int i=0; i<cache[howManyBits].length; i++){
				cache[howManyBits][i] = new Fast0To16Bits(howManyBits, i);
			}
		}
		AvlBitstring.test = true;
	}

	public Fast0To16Bits(int howManyBits, int unsignedInteger){
		this.siz = howManyBits;
		this.data = unsignedInteger;
		int countOnes = 0;
		int countZeros = 0;
		int bitMask = 1;
		for(int i=0; i<howManyBits; i++){
			if((unsignedInteger&bitMask)!=0){
				countOnes++;
			}
			bitMask <<= 1;
		}
		copyOfData = new boolean[howManyBits];
		indexOfNthOne = new byte[countOnes];
		indexOfNthZero = new byte[howManyBits-countOnes];
		countOnes = 0;
		if(howManyBits > 0){
			bitMask = 1 << (howManyBits-1);
			for(int i=0; i<howManyBits; i++){
				if((unsignedInteger&bitMask)!=0){
					indexOfNthOne[countOnes++] = (byte)i;
					copyOfData[i] = true;
				}else{
					indexOfNthZero[countZeros++] = (byte)i;
				}
				bitMask >>= 1;
			}
		}
	}
	
	public long siz(){ return siz; }
	
	public long ones(){ return indexOfNthOne.length; }
	
	public long zeros(){ return indexOfNthZero.length; }
	
	public long indexOfNthOne(long n){
		return indexOfNthOne[(int)n];
	}
	
	public long indexOfNthZero(long n){
		return indexOfNthZero[(int)n];
	}
	
	public Bits pre(long endExclusive){
		
		if(endExclusive < 0 || siz < endExclusive) throw new IndexOutOfBoundsException(""+endExclusive);
		int possibleValues = 1 << endExclusive;
		int bitMask = possibleValues-1;
		int endExclusiveInt = (int)endExclusive;
		int sizeAfter = siz-endExclusiveInt;
		int d = (data>>sizeAfter)&bitMask;
		return Fast0To16Bits.get(endExclusiveInt, d);
		
		/*OLD
		
		if(endExclusive < 0 || siz < endExclusive) throw new IndexOutOfBoundsException(""+endExclusive);
		
		
		int possibleValues = 1 << endExclusive;
		int bitMask = possibleValues-1;
		int data = data&bitMask; 
		return Fast0To16Bits.get((int)endExclusive, data);
		*/
	}
	
	public Bits suf(long start){
		
		if(start < 0 || siz < start) throw new IndexOutOfBoundsException(""+start);
		int suffixBitSize = siz-(int)start;
		int possibleValues = 1 << suffixBitSize;
		int bitMask = possibleValues-1;
		int d = data&bitMask;
		return Fast0To16Bits.get(suffixBitSize, d);
		
		/*OLD
		
		if(start < 0 || siz < start) throw new IndexOutOfBoundsException(""+start);
		
		
		int suffixBitSize = siz-(int)start;
		int possibleValues = 1 << suffixBitSize;
		int bitMask = (possibleValues-1);
		
		int data = (data >>> start) & bitMask;
		return Fast0To16Bits.get(suffixBitSize, data);
		*/
	}
	
	public boolean bitAt(long index){
		
		
		return copyOfData[(int)index];
		
		
		
	}
	
	public byte byteAt(long index){
		throw new RuntimeException("check size and bit shift, maybe returning 0s from past size");
	}
	
	public char charAt(long index){
		throw new RuntimeException("check size and bit shift, maybe returning 0s from past size");
	}
	
	public short shortAt(long index){
		throw new RuntimeException("check size and bit shift, maybe returning 0s from past size");
	}
	
	public int intAt(long index){
		throw new RuntimeException("check size and bit shift, maybe returning 0s from past size");
	}
	
	public long longAt(long index){
		throw new RuntimeException("check size and bit shift, maybe returning 0s from past size");
	}
	
	public float floatAt(long index){
		throw new IndexOutOfBoundsException("siz="+siz+" and can never hold a float. index="+index);
	}
	
	public double doubleAt(long index){
		throw new IndexOutOfBoundsException("siz="+siz+" and can never hold a double. index="+index);
	}
	
	public int bits(long start, byte getHowManyBits){
		if(siz < start+getHowManyBits){
			throw new IndexOutOfBoundsException("start="+start+" getHowManyBits="+getHowManyBits+" siz="+siz+" this="+this);
		}
		/*
		int i = data >> start;
		if(getHowManyBits >= 32) return i; 
		int mask = (1 << getHowManyBits)-1; 
		return i&mask;
		*/
		
		/*int i = data >> start;
		if(getHowManyBits >= 32) return i; 
		long sizRemaining = siz-start;
		int mask = (1 << sizRemaining)-1; 
		int d = i&mask; 
		return d >> (sizRemaining-getHowManyBits);
		*/
		
		long sizRemaining = siz-start;
		int mask = (1 << sizRemaining)-1;
		int d = data&mask; 
		return d >> (sizRemaining-getHowManyBits);
	}
	
	/** Since this class is never more than 16 bits, simply calls the int version of this func */
	public long bitsJ(long start, byte howManyBits){
		return bits(start,howManyBits);
	}
	
	public void bits(byte getBits[], long offset, long start, long end){
		throw new RuntimeException("TODO");
	}

	/*use default public int hashCode(){
		throw new RuntimeException("TODO decide on a standard for hashcode and equals by bitstring content? Or use default compare by == and System.identityHashcode (as it already is)?");
	}
	public boolean equals(Object o){
		throw new RuntimeException("TODO decide on a standard for hashcode and equals by bitstring content? Or use default compare by == and System.identityHashcode (as it already is)?");
	}*/
	
	
	
	public Bits cat(Bits suf){
		
		long sufSiz = suf.siz();
		long sizeAfterCat = siz+sufSiz;
		if(sizeAfterCat <= 16){
			
			int sufI = suf.bits(0, (byte)sufSiz);
			int bitsAfterCat = (data << sufSiz) | sufI;
			return Fast0To16Bits.get((int)sizeAfterCat, bitsAfterCat);
			
			/*OLD:
			int sufI = suf.bits(0, (byte)suf.siz());
			int catBits = data | (sufI << siz);
			return Fast0To16Bits.get((int)catSize, catBits);
			*/
		}else{
			return new AvlBitstring(this, suf);
		}
	}

	public Bits ins(Bits middle, long start){
		throw new RuntimeException("TODO check cache if total length at most 16, else use prefix and suffix funcs");
	}

	public Bits owt(Bits middle, long start){
		throw new RuntimeException("TODO check cache if total length at most 16, else use prefix and suffix funcs");
	}

	public Bits del(long start, long endExclusive){
		throw new RuntimeException("TODO 2 bit shifts and return from cache");
	}

	public Bits sub(long start, long endExclusive){
		throw new RuntimeException("TODO");
	}

	public Bits firstOrNull(){ return null; }
	
	public Bits secondOrNull(){ return null; }
	
	public int height(){ return 0; }
	
	public int maxHeightDiff(){ return 0; }
	
	public Bits balanceTree(){ return this; }
	
	public int efficientBlockSize(){ return 1; }
	
	/*public boolean firstBit(){ return copyOfUnsignedInteger[0]; }
	
	public boolean lastBit(){ return copyOfUnsignedInteger[howManyBits-1]; }
	
	public Bits chompFirstBit(){ return suffix(1); }
	
	public Bits chompLastBit(){ return prefix(howManyBits-1); }
	
	public boolean isEmpty(){ return howManyBits != 0; }
	
	public Bits sizeUint(){
		throw new RuntimeException(
			"TODO util func to view int long or Number as Bits unsigned integer littleEndian");
	}
	
	public boolean sizeFitsJ(){ return true; }

	public boolean bitAt(Bits uint){
		throw new RuntimeException("TODO");
	}*/
	
	/** Uses the low bits of upTo16BitsUnsignedInteger to choose which bits to return from cache */
	public static final Fast0To16Bits get(int howManyBits, int upTo16BitsUnsignedInteger){
		return cache[howManyBits][upTo16BitsUnsignedInteger];
	}
	
	public static final Fast0To16Bits EMPTY = get(0,0);
	
	/** Same as get(1,0) */
	public static final Fast0To16Bits FALSE = get(1,0);
	
	/** Same as get(1,1) */
	public static final Fast0To16Bits TRUE = get(1,1);
	
	public static final Fast0To16Bits get(boolean bit){
		return bit ? TRUE : FALSE;
	}
	
	/** Same as get(unsignedInteger&0xff,8). TODO optimize by going directly to the array of bits objects. */
	public static final Fast0To16Bits get(byte unsignedInteger){
		return get(8, unsignedInteger&0xff);
	}
	
	/** Same as get(unsignedInteger&0xffff,16). TODO optimize by going directly to the array of bits objects. */
	public static final Fast0To16Bits get(short unsignedInteger){
		return get(16, unsignedInteger&0xffff);
	}
	
	/** TODO Test bitAt and bits funcs for all 2^17-1 (except the size 0 one, where theres nothing to test)
	Fast0To16Bits (manual verify from output, for some of it).
	*/
	public static void main(String args[]){
		for(byte howManyBits=1; howManyBits<=16; howManyBits++){
			System.out.println();
			System.out.println();
			System.out.println("START: Testing "+Fast0To16Bits.class.getSimpleName()+" bitAt func with howManyBits="+howManyBits);
			
			int maxValue = (1<<howManyBits)-1;
			for(int valueI=0; valueI<=maxValue; valueI++){
				boolean value[] = AvlBitstring.bitsOfInt(valueI, howManyBits);
				
				Bits bits = Fast0To16Bits.get(howManyBits, valueI);
				for(int j=0; j<howManyBits; j++){
					boolean observedBit = bits.bitAt(j);
					
					if(observedBit != value[j]){
						
						
						
						System.out.println();
						System.out.println();
						System.out.println("bitAt error in "+Fast0To16Bits.class+". howManyBits="+howManyBits+" index="+j+" valueI="+valueI);
						for(int k=0; k<howManyBits; k++){
							System.out.print(value[k]?"1":"0");
						}
						System.out.println();
						for(int k=0; k<howManyBits; k++){
							System.out.print(bits.bitAt(k)?"1":"0");
						}
						System.out.println();
						System.out.println();
						return;
					}
				}
				
				/*(int observedValueI = bits.bits(start, howManyBits)
				boolean observedValue[] = ;
				*/
				
				
			}
			
			System.out.println("END: Testing "+Fast0To16Bits.class.getSimpleName()+" bitAt func with howManyBits="+howManyBits);
			System.out.println();
			System.out.println();
			System.out.println();
		}
		
		for(byte howManyBitsInObject=1; howManyBitsInObject<=16; howManyBitsInObject++){
			System.out.println();
			System.out.println("START testing "+Fast0To16Bits.class.getSimpleName()+" bits func with howManyBitsInObject="+howManyBitsInObject);
			int maxValue = (1<<howManyBitsInObject)-1;
			for(int valueI=0; valueI<=maxValue; valueI++){
				Bits bits = Fast0To16Bits.get(howManyBitsInObject, valueI);
				boolean value[] = AvlBitstring.bitsOfInt(valueI, howManyBitsInObject);
				for(byte howManyBitsToObserve=1; howManyBitsToObserve<=howManyBitsInObject; howManyBitsToObserve++){
					for(long start=0; start<howManyBitsInObject-howManyBitsToObserve; start++){
						int observedI = bits.bits(start, howManyBitsToObserve);
						
						int correctI = valueI;
						correctI >>>= start;
						int mask = (1<<howManyBitsToObserve)-1;
						correctI &= mask;
						if(observedI != correctI){ 
							System.out.println();
							System.out.println();
							System.out.println("error in bits func. howManyBitsInObject="+howManyBitsInObject
								+" howManyBitsToObserve="+howManyBitsToObserve+" start="+start
								+" valueI="+valueI+" observedI="+observedI);
							System.out.println();
							for(int j=0; j<howManyBitsInObject; j++){
								System.out.print(value[j]?1:0);
							}
							System.out.println();
							for(int j=0; j<start; j++){
								System.out.print('-');
							}
							for(int j=0; j<howManyBitsToObserve; j++){
								boolean bitJObserved = ((1<<j)&observedI)!=0;
								System.out.print(bitJObserved?1:0);
							}
							for(int j=(int)(start+howManyBitsToObserve); j<howManyBitsInObject; j++){
								System.out.print('-');
							}
							return;
						}
					}
				}
			}
			System.out.println("END testing "+Fast0To16Bits.class.getSimpleName()+" bits func with howManyBitsInObject="+howManyBitsInObject);
			System.out.println();
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder("("+siz()+" bits, "+ones()+" 1s, ");
		for(int i=0; i<siz(); i++) sb.append(bitAt(i)?1:0);
		return sb.append(", "+getClass().getSimpleName()).append(')').toString();
	}

}