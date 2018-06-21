package com.insightfullogic.slab;

import com.insightfullogic.slab.stats.AllocationListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsTest {

	private static long allocated;
	private static long resized;
	private static long freed;

	private static final AllocationListener COUNTER = new AllocationListener() {
		@Override
        public void onResize(Allocator<?> by, int size, long sizeInBytes) {
			resized += sizeInBytes;
		}

		@Override
        public void onFree(Allocator<?> by, int size, long sizeInBytes) {
			freed += sizeInBytes;
		}

		@Override
        public void onAllocation(Allocator<?> by, int size, long sizeInBytes) {
			allocated += sizeInBytes;
		}
	};

	@Test
    void stats() {
		Allocator<GameEvent> eventAllocator = Allocator.of(GameEvent.class, COUNTER);
		GameEvent event = eventAllocator.allocate(1);
		try {
			assertEquals(16, allocated);

			event.resize(2);
			assertEquals(16, resized);
		} finally {
			event.close();
		}
		assertEquals(32, freed);
	}

}
