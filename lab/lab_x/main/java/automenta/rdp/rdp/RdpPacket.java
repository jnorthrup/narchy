/* RdpPacket_Localised.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: #2 $
 * Author: $Author: tvkelley $
 * Date: $Date: 2009/09/15 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Java 1.4 specific extension of RdpPacket class
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 * 
 * (See gpl.txt for details of the GNU General Public License.)
 * 
 */
package automenta.rdp.rdp;

import automenta.rdp.AbstractRdpPacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class RdpPacket extends AbstractRdpPacket {

	private ByteBuffer bb = null;

	private int size = 0;

	public RdpPacket(int capacity) {
		bb = ByteBuffer.allocate(capacity);
		size = capacity;
	}

	@Override
	public String toString() {
		return Arrays.toString(bb.array());
	}

	public void reset(int length) {
		
		
		this.end = 0;
		this.start = 0;
		if (bb.capacity() < length)
			bb = ByteBuffer.allocate(length);
		size = length;
		bb.clear();
	}

	public void set8(int where, int what) {




		bb.put(where, (byte) what);
	}

	public void set8(int what) {




		bb.put((byte) what);

	}

	public void set8(final byte... b) {
		bb.put(b);
	}
	public void set8(final byte[] b, int offset, int len) {
		bb.put(b, offset, len);
	}

	
	public int get8(int where) {




		return bb.get(where) & 0xff; 
	}

	
	public int get8() {




		return bb.get() & 0xff; 
	}

	public void copyFromByteArray(byte[] array, int array_offset,
			int mem_offset, int len) {






		
		int oldpos = position();

		position(mem_offset);
		bb.put(array, array_offset, len);

		
		position(oldpos);
	}

	/** use the array directly */
	@Deprecated public void copyToByteArray(byte[] array, int array_offset, int mem_offset,
			int len) {










		int oldpos = position();
		position(mem_offset);
		bb.get(array, array_offset, len);
		position(oldpos);
	}

	public void copyToPacket(RdpPacket dst, int srcOffset,
			int dstOffset, int len) {
		int olddstpos = dst.position();
		int oldpos = position();
		dst.position(dstOffset);
		position(srcOffset);

		/*for (int i = 0; i < len; i++)
			dst.set8(bb.get());*/
		final int pos = bb.arrayOffset();
		dst.set8(bb.array(), pos, len);
		bb.position(pos + len);

		dst.position(olddstpos);
		position(oldpos);
	}

	public void copyFromPacket(RdpPacket src, int srcOffset,
			int dstOffset, int len) {
		int oldsrcpos = src.position();
		int oldpos = position();
		src.position(srcOffset);
		position(dstOffset);

		int pos = src.position();
		bb.put(src.array(), pos, len);
		src.position(pos + len);

		/*for (int i = 0; i < len; i++)
			bb.put((byte) src.get8());*/

		src.position(oldsrcpos);
		position(oldpos);
	}

	public byte[] array() {
		return bb.array();
	}

	public int capacity() {
		return bb.capacity();
	}

	
	public int size() {
		return size;
		
	}

	public int position() {
		return bb.position();
	}

	public int getLittleEndian16(int where) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort(where);
	}

	public int getLittleEndian16() {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort();
	}

	public int getBigEndian16(int where) {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort(where);
	}

	public int getBigEndian16() {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort();
	}

	public void setLittleEndian16(int where, int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort(where, (short) what);
	}

	public void setLittleEndian16(int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short) what);
	}

	public void setBigEndian16(int where, int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putShort(where, (short) what);
	}

	public void setBigEndian16(int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putShort((short) what);
	}

	public int getLittleEndian32(int where) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt(where);
	}

	public int getLittleEndian32() {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}

	public int getBigEndian32(int where) {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getInt(where);
	}

	public int getBigEndian32() {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getInt();
	}

	public void setLittleEndian32(int where, int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(where, what);
	}

	public void setLittleEndian32(int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(what);
	}

	public void setBigEndian32(int where, int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putInt(where, what);
	}

	public void setBigEndian32(int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putInt(what);
	}

	public void positionAdd(int length) {





		bb.position(bb.position() + length);
	}

	public void position(int position) {






		bb.position(position);
	}

}
