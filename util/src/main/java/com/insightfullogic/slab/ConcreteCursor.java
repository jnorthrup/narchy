package com.insightfullogic.slab;

import com.insightfullogic.slab.implementation.AllocationHandler;
import jcog.Util;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static com.insightfullogic.slab.implementation.MemoryCalculation.calculateAddress;
import static com.insightfullogic.slab.implementation.MemoryCalculation.calculateAllocation;

@SuppressWarnings("restriction")
public abstract class ConcreteCursor implements Cursor {

	private static final int NOTHING = 0;

	public static final String BOUNDS_CHECKING_PROPERTY = "slab.checkbounds";

	public static boolean boundsChecking = "true".equals(System.getProperty(BOUNDS_CHECKING_PROPERTY));


	private final int sizeInBytes;
	private final AllocationHandler handler;
	private final SlabOptions options;

	private int numberOfObjects;
	private int index;
	
	protected long allocatedAddress;
	protected long startAddress;
	protected long pointer;

	public ConcreteCursor(int numberOfObjects, int sizeInBytes, AllocationHandler handler, SlabOptions options) {
		this.numberOfObjects = numberOfObjects;
		this.sizeInBytes = sizeInBytes;
		this.handler = handler;
        this.options = options;
        allocatedAddress = Util.unsafe.allocateMemory(calculateAllocation(numberOfObjects, sizeInBytes, options));
        startAddress = calculateAddress(allocatedAddress, options);

		move(0);
	}

	@Override
	public void close() {
		if (allocatedAddress == NOTHING)
			return;

		handler.free();
		Util.unsafe.freeMemory(allocatedAddress);
		allocatedAddress = NOTHING;
		startAddress = NOTHING;
	}

	@Override
	public final void move(int index) {
		if (boundsChecking && index >= numberOfObjects)
			throw new ArrayIndexOutOfBoundsException(index);
		this.index = index;
		pointer = startAddress + (sizeInBytes * index);
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void resize(int newNumberOfObjects) {
		if (newNumberOfObjects <= index)
			throw new InvalidSizeException("You can't resize a slab to below the index currently pointed at");

		numberOfObjects = newNumberOfObjects;
		long newSizeInBytes = calculateAllocation(newNumberOfObjects, sizeInBytes, options);
		handler.resize(newNumberOfObjects, newSizeInBytes);

		allocatedAddress = Util.unsafe.reallocateMemory(startAddress, newSizeInBytes);
		startAddress = calculateAddress(allocatedAddress, options);
		move(index);
	}

	@Override
	public int size() {
	    return numberOfObjects;
	}

}
