package com.insightfullogic.slab;

import com.insightfullogic.slab.implementation.*;
import com.insightfullogic.slab.stats.AllocationListener;

import java.lang.reflect.Constructor;

public final class Allocator<T extends Cursor> {

	private static final AllocationHandler NO_LISTENER = new NullAllocationHandler();
	
	private final TypeInspector inspector;
    
	private final Constructor<T> constructor;

	private final AllocationListener listener;

    private final SlabOptions options;

    public static <T extends Cursor> Allocator<T> of(Class<T> representingKlass) throws InvalidInterfaceException {
        return of(representingKlass, (AllocationListener) null);
    }

    public static <T extends Cursor> Allocator<T> of(Class<T> representingKlass, AllocationListener listener) throws InvalidInterfaceException {
        return of(representingKlass, listener, SlabOptions.DEFAULT);
    }
    
    public static <T extends Cursor> Allocator<T> of(Class<T> representingKlass, SlabOptions options) throws InvalidInterfaceException {
        return new Allocator<>(representingKlass, null, options);
    }
    
    public static <T extends Cursor> Allocator<T> of(Class<T> representingKlass, AllocationListener listener, SlabOptions options) throws InvalidInterfaceException {
        return new Allocator<>(representingKlass, listener, options);
    }

    private Allocator(Class<T> representingKlass, AllocationListener listener, SlabOptions options) {
		this.listener = listener;
        this.options = options;
		inspector = new TypeInspector(representingKlass);
		Class<T> implementation = new BytecodeGenerator<>(inspector, representingKlass, options).generate();
        try {
        	constructor = implementation.getConstructor(Integer.TYPE, AllocationHandler.class, SlabOptions.class);
        } catch (RuntimeException e) {
        	throw e;
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

	public T allocate(int count) throws InvalidSizeException {
		if (count < 1)
			throw new InvalidSizeException("You must provide a count >= 1 when allocating a slab, received " + count);

		AllocationHandler handler = listener == null ? NO_LISTENER : makeRecorder(count);

		try {
			return constructor.newInstance(count, handler, options);
		} catch (RuntimeException e) {
        	throw e;
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private AllocationRecorder makeRecorder(int count) {
		return new AllocationRecorder(this, listener, count,
				count * inspector.getSizeInBytes());
	}

}
