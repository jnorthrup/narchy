/** Ben F Rayfield offers Wavetree opensource GNU LGPL 2+ */
package smartblob.wavetree.bit;
import java.util.Random;

/** Update 2015-5 changing to bigEndian since thats the way people think and multidimensional arrays are.
<br><br>
An immutable acyclic forest built using AVL tree rotations when any branch
gets too deep, creating new lightweight immutable branch at each rotation
point, with depth limited to twice the smallest possible depth. Like
Immutable Sparse Wave Trees, most string functions (including read bit, copy
subrange, and concat) take log time and memory.
*/
public class AvlBitstring implements Bits{
	
	

	public final Bits first, second;
	
	public final long ones;
	
	public final long siz;

	public final int height, maxHeightDiff;
	
	
	public final Bits firstOrNull(){ return first; }
	
	public final Bits secondOrNull(){ return second; }

	public final long siz(){ return siz; }

	public final int height(){ return height; }

	public final int maxHeightDiff(){ return maxHeightDiff; }

	/** Since this is a tree node, first and second must not be null */
	public AvlBitstring(Bits first, Bits second){
		this.first = first;
		this.second = second;
		siz = first.siz() + second.siz();
		ones = first.ones() + second.ones();
		int firstHeight = first.height(), secondHeight = second.height();
		
		
		
		height = 1 + Math.max(firstHeight, secondHeight);
		
		
		int newMaxheightDiff = second.height() - first.height();
		if(newMaxheightDiff < 0) newMaxheightDiff = -newMaxheightDiff;
		if(newMaxheightDiff < first.maxHeightDiff()) newMaxheightDiff = first.maxHeightDiff();
		if (newMaxheightDiff < second.maxHeightDiff()) newMaxheightDiff = second.maxHeightDiff();
		maxHeightDiff = newMaxheightDiff;
	}

	public long indexOfNthOne(long n){
		long firstOnes = first.ones();
		if (n < firstOnes){
			return first.indexOfNthOne(n);
		} else{
			return first.siz() + second.indexOfNthOne(n - firstOnes);
		}
	}

	public long indexOfNthZero(long n){
		long firstZeros = first.zeros();
		if (n < firstZeros){
			return first.indexOfNthZero(n);
		} else{
			return first.siz() + second.indexOfNthZero(n - firstZeros);
		}
	}

	public boolean bitAt(long index){
		long firstSize = first.siz();
		if(index < firstSize){
			return first.bitAt(index);
		}
		return second.bitAt(index - firstSize);
	}

	public byte byteAt(long index){
		return (byte) bits(index, (byte) 8);
	}

	public char charAt(long index){
		return (char) bits(index, (byte) 16);
	}

	public short shortAt(long index){
		return (short) bits(index, (byte) 16);
	}
	
	static{System.out.println("FIXME: AvlBitstring is not always using Fast0To16Bits when split or concat things that result in at most 16 bits");}

	public int intAt(long index){
		return bits(index, (byte) 32);
	}

	public long longAt(long index){
		return bitsJ(index, (byte)64);
	}
	
	public float floatAt(long index){
		return Float.intBitsToFloat(intAt(index));
	}
	
	public double doubleAt(long index){
		return Double.longBitsToDouble(longAt(index));
	}
	
	public int bits(long start, byte getHowManyBits){
		long firstSize = first.siz();
		if (start < firstSize){
			long maxBitsFromFirst = firstSize - start;
			if (getHowManyBits <= maxBitsFromFirst){ 
				return first.bits(start, getHowManyBits);
			}else{ 
				byte maxBitsFromFirstB = (byte) maxBitsFromFirst;
				byte howManyBitsFromSecond = (byte) (getHowManyBits - maxBitsFromFirstB);
				int firstBits = first.bits(start, maxBitsFromFirstB);
				long remainingStart = start+maxBitsFromFirstB;
				int secondBits = second.bits(remainingStart-firstSize, howManyBitsFromSecond);
				
				
				return (firstBits << howManyBitsFromSecond) | secondBits;
				
				
			}
		}
		return second.bits(start-firstSize, getHowManyBits);
	}
	
	public long bitsJ(long start, byte howManyBits){
		if(32 < howManyBits){
			
			int highBits = bits(start, (byte)(howManyBits-32));
			int lowBits = intAt(start+32);
			return (((long)highBits)<<32)|lowBits;
			
			/*OLD
			
			int lowBits = intAt(start);
			int highBits = bits(start+32, (byte)(howManyBits-32));
			return (((long)highBits)<<32)|lowBits;
			*/
		}else{
			return bits(start, howManyBits);
		}
	}
	
	public void bits(byte getBits[], long offset, long start, long end){
		throw new RuntimeException("TODO");
	}

	public long size(){
		return siz;
	}

	/** TODO it would have to be done in constructor, and I dont want to pay
	memory for that, but I also cant pay to search the whole tree by
	recursing 2 times from each node... efficientBlockSize would be
	efficientBlockSize of 2 childs if their efficientBlockSize equals, else
	it would be 1 (or maybe the greatestCommonFactor of those?).
	For now at least, this returns 1.
	*/
	public int efficientBlockSize(){ return 1; }

	public Bits sub(long start, long endExclusive){
		
		
		Bits pre = pre(endExclusive);
		Bits preSuf = pre.suf(start);
		return preSuf;
	}

	public Bits pre(long endExclusive){
		/*
		 * if(endExclusive == siz) return this; long firstSize = first.siz();
		 * if(endExclusive == firstSize) return first; if(endExclusive <
		 * firstSize) return first.pre(endExclusive); return
		 * first.cat(second.pre(endExclusive-firstSize));
		 */
		
		if (endExclusive == siz)
			return this;
		long firstSize = first.siz();
		if (endExclusive == firstSize)
			return first;
		if (endExclusive < firstSize){
			Bits firstPre = first.pre(endExclusive);
			return firstPre;
		}
		long secondPreWhere = endExclusive - firstSize;
		Bits secondPre = second.pre(secondPreWhere);
		Bits firstCatSecondPre = first.cat(secondPre);
		return firstCatSecondPre;
	}

	public Bits suf(long start){
		/*
		 * if(start == 0) return this; long firstSize = first.siz(); if(start ==
		 * firstSize) return second; 
		 * first.suf(firstSize).cat(second); if(start < firstSize) return
		 * first.suf(start).cat(second); return second.suf(start-firstSize);
		 */
		
		if (start == 0)
			return this;
		long firstSize = first.siz();
		if (start == firstSize)
			return second;
		if (start < firstSize){
			
			
			Bits firstSuf = first.suf(start);
			Bits firstSufCatSecond = firstSuf.cat(second);
			return firstSufCatSecond;
		}
		long secondSufWhere = start - firstSize;
		return second.suf(secondSufWhere);
	}

	public Bits cat(Bits suffix){
		if (suffix.siz() == 0)
			return this;
		return balanceAVLTree(new AvlBitstring(this, suffix));
	}

	public Bits ins(Bits middle, long start){
		return pre(start).cat(middle).cat(suf(start));
	}

	public Bits owt(Bits middle, long start){
		long middleEndExclusive = start + middle.siz();
		if (middleEndExclusive > size())
			throw new IndexOutOfBoundsException(
					"Overwrite range extends past end");
		return pre(start).cat(middle).cat(suf(middleEndExclusive));
	}

	public Bits del(long start, long endExclusive){
		return pre(start).cat(suf(endExclusive));
	}

	public long ones(){
		return ones;
	}

	public long zeros(){
		return siz - ones;
	}

	/*
	 * public Bits[] outgoingPointers(){ return new Bits[]{first, second}; }
	 */

	/*
	 * public void storeAllDataLocally(){ if(size() > Integer.MAX_VALUE) throw
	 * new RuntimeException("Too large bit array to create: "+size());
	 * 
	 * vs memory. booleans take 1 byte each in memory. boolean data[] = new
	 * boolean[(int)size()]; for(int i=0; i<data.length; i++){ data[i] =
	 * bitAt(i); } return new BitArray(data); }
	 */

	/*
	 * public Bits concat(Bits... suffixes){ throw new RuntimeException("TODO");
	 * }
	 */

	public Bits balanceTree(){
		return balanceAVLTree(this);
	}

	public static Bits balanceAVLTree(Bits mayBeUnbalanced){
		if (mayBeUnbalanced.maxHeightDiff() <= 1) return mayBeUnbalanced;
		Bits x = mayBeUnbalanced;
		Bits first = x.firstOrNull(), second = x.secondOrNull();
		Bits newFirst = first.maxHeightDiff() <= 1
			? first
			: balanceAVLTree(first);
		Bits newSecond = second.maxHeightDiff() <= 1
			? second
			: balanceAVLTree(second);
		
		
		
		while (newFirst.height() + 1 < newSecond.height()){
			
			
			
			if (newSecond.firstOrNull().height() > newSecond.secondOrNull()
					.height()){
				
				
				
				
				newSecond = new AvlBitstring(
					newSecond.firstOrNull().firstOrNull(),
					new AvlBitstring(
						newSecond.firstOrNull().secondOrNull(),
						newSecond.secondOrNull()
					)
				);
			}
			
			newFirst = balanceAVLTree(new AvlBitstring(newFirst,
					newSecond.firstOrNull()));
			newSecond = newSecond.secondOrNull();
		}
		
		while (newFirst.height() > newSecond.height() + 1){
			
			
			
			
			
			if (newFirst.secondOrNull().height() > newFirst.firstOrNull().height()){
				
				
				
				
				newFirst = new AvlBitstring(
					new AvlBitstring(
						newFirst.firstOrNull(),
						newFirst.secondOrNull().firstOrNull()
					),
					newFirst.secondOrNull().secondOrNull()
				);
			}
			
			newSecond = balanceAVLTree(new AvlBitstring(newFirst.secondOrNull(), newSecond));
			newFirst = newFirst.firstOrNull();
		}
		
		
		
		
		
		return new AvlBitstring(newFirst, newSecond);
	}

	public String toString(){
		return "(" + size() + " bits, " + ones() + " 1s)";
	}
	
	public static boolean test = false; 

	public static void testBitsFunc(){
		boolean data[] = new boolean[50];
		Random repeatablePseudorandom = new Random(49999);
		Bits b = Fast0To16Bits.EMPTY;
		for(int i=0; i<data.length; i++){
			data[i] = repeatablePseudorandom.nextBoolean();
			
			b = b.cat(Fast0To16Bits.get(data[i]));
			String s = "";
			for(long g=0; g<b.siz(); g++) s += b.bitAt(g)?"1":"0";
			System.out.println("b = "+s);
		}
		System.out.println();
		System.out.println("Testing bitAt:");
		for(long g=0; g<data.length; g++){
			System.out.print(data[(int)g]?"1":"0");
		}
		System.out.println();
		for(long g=0; g<data.length; g++){
			System.out.print(b.bitAt(g)?"1":"0");
		}
		System.out.println();
		System.out.println("End testing bitAt.");
		System.out.println();
		System.out.println();
		for(int i=0; i<data.length; i++){
			if(data[i] != b.bitAt(i)){
				throw new RuntimeException("Bits at index "+i+" not match.");
			}
		}
		System.out.println();
		for(byte howManyBits=1; howManyBits<=32; howManyBits++){
			System.out.println("START: Testing bits func with howManyBits="+howManyBits);
			for(int start=0; start<data.length-howManyBits; start++){
				
				int observedBitsI = b.bits(start, howManyBits);
				boolean observedBits[] = bitsOfInt(observedBitsI, howManyBits);
				int howManyBitsNotMatch = 0;
				for(int k=0; k<howManyBits; k++){
					if(observedBits[k] != data[(start+k)]){
						howManyBitsNotMatch++;
					}
				}
				if(howManyBitsNotMatch > 0){
					System.out.println();
					System.out.println();
					System.out.println("Not match. howManyBits="+howManyBits+" start="+start);
					for(long g=0; g<data.length; g++){
						System.out.print(data[(int)g]?"1":"0");
					}
					System.out.println();
					for(long g=0; g<start; g++){
						System.out.print("-");
					}
					for(int k=0; k<howManyBits; k++){
						System.out.print(observedBits[k]?"1":"0");
					}
					for(long g=start+howManyBits; g<data.length; g++){
						System.out.print("-");
					}
					System.out.println();
					System.out.println();
					return;
				}
			}
			System.out.println("END: Testing bits func with howManyBits="+howManyBits);
		}
	}
	
	public static boolean[] bitsOfInt(int i, byte howManyBits){
		if(howManyBits < 0 || howManyBits > 32) throw new RuntimeException("howManyBits="+howManyBits);
		boolean b[] = new boolean[howManyBits];
		for(int j=0; j<howManyBits; j++){
			
			b[howManyBits-1-j] = (i&1)!=0; 
			i >>>= 1;
		}
		return b;
	}
	
	/** test */
	public static void main(String args[]){
		testBitsFunc();
	}

}