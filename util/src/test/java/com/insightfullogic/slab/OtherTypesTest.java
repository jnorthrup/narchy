package com.insightfullogic.slab;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OtherTypesTest {
	
	public interface OtherTypes extends Cursor {
		byte getByte();
		void setByte(byte value);
		
		short getShort();
		void setShort(short value);
		
		float getFloat();
		void setFloat(float value);

		boolean getBool();
		void setBool(boolean value);

		char getChar();
		void setChar(char value);
	}

	private static final Allocator<OtherTypes> allocator = Allocator.of(OtherTypes.class);
	private final OtherTypes value = allocator.allocate(1);

	@Test
    void fieldsGettableSettable() {
		value.setByte((byte) 23);
		assertEquals(23, value.getByte());
		
		value.setShort((short) 15);
		assertEquals(15, value.getShort());
		
		value.setFloat(0.5f);
		assertEquals(0.5f, value.getFloat(), 0.01f);
		
		value.setBool(true);
		assertTrue(value.getBool());
		
		value.setChar('c');
		assertEquals('c', value.getChar());
	}
	
	@AfterEach
    void free() {
		value.close();
	}

}
