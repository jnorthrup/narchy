//package jcog.data.pool;
//
//import java.lang.ref.Reference;
//import java.lang.ref.ReferenceQueue;
//import java.lang.ref.SoftReference;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//import static java.lang.Boolean.TRUE;
//
///*
// * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// *
// * This code is free software; you can redistribute it and/or modify it
// * under the terms of the GNU General Public License version 2 only, as
// * published by the Free Software Foundation.  Oracle designates this
// * particular file as subject to the "Classpath" exception as provided
// * by Oracle in the LICENSE file that accompanied this code.
// *
// * This code is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// * version 2 for more details (a copy is included in the LICENSE file that
// * accompanied this code).
// *
// * You should have received a copy of the GNU General Public License version
// * 2 along with this work; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
// *
// * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// * or visit www.oracle.com if you need additional information or have any
// * questions.
// */
//
//
//public final class WeakPool<X> implements Supplier<X> {
//
//	final Supplier<X> builder;
//	final Consumer<X> cleaner;
//
//	private final static class MyWeakReference<X> extends SoftReference<X> {
//
//
//
//		public MyWeakReference(X referent, ReferenceQueue refQueue) {
//			super(referent, refQueue);
//		}
//
//	}
//
//	/** just holds references to WeakRef's to checked out items */
//	private final Map<Reference<X>,Object> active = new HashMap();
//
//	private final DequePool<X> pool = new DequePool<>(1) {
//
//		@Override
//		public X create() {
//			return (builder.get());
//		}
//
//		@Override
//		public void put(X i) {
//			cleaner.accept(i);
//			super.put(i);
//		}
//	};
//
//	private final ReferenceQueue<X> refQueue = new ReferenceQueue<>();
//	private final int capacity;
//
//	public WeakPool(Supplier<X> builder, final int capacity) {
//		this(builder, (x) -> { } , capacity);
//	}
//
//	public WeakPool(Supplier<X> builder, Consumer<X> cleaner, final int capacity) {
//		this.builder = builder;
//		this.cleaner = cleaner;
//		this.capacity = capacity;
//
//	}
//
//	/**
//	 * Returns the value associated with {@code key}, or {@code null} if no such value exists.
//	 *
//	 * @param key the key
//	 * @return the value or null if none exists
//	 */
//	public X get() {
//
//		for (;;) {
//			final Reference<X> ref = (Reference<X>) refQueue.poll();
//			if (ref == null)
//				break;
//
//			X x = ref.get();
//			assert(x !=null);
//
//			active.remove(ref);
////			boolean inactivated = active.remove(ref);
////			assert(inactivated);
//
//			if (pool.size() < capacity)
//				pool.put(x);
//		}
//
//		final X x = pool.get();
//		MyWeakReference ref = new MyWeakReference(x, refQueue);
//		active.put(ref, TRUE);
//
//		return x;
//	}
//
//public static <X> Supplier<X> threadLocal(Supplier<X> builder, Consumer<X> cleaner, int capacity) {
//		return ThreadLocal.withInitial(new WeakPool<>(builder, cleaner, capacity)::get)::get;
//	}
//}
//
/////**
//// * https:
//// */
////public class WeakPool<X> {
////
////	private static final WeakHashMap<Object,WeakReference<Object>> map = new WeakHashMap<>();
////
////	public synchronized static <X> X the(final X x) {
////
////		final WeakReference ref = map.get(x);
////		Object y;
////		if (ref != null) {
////			y = ref.get();
////			if (y!=null)
////				return (X)y;
////		}
////
////		map.put(x, new WeakReference(x));
////		return x;
////	}
////}