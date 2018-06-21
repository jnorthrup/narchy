package com.insightfullogic.slab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEventScenarioTest {

	@Test
    void eventScenario() {
		Allocator<GameEvent> eventAllocator = Allocator.of(GameEvent.class);

		GameEvent event = eventAllocator.allocate(100);
		try {
			assertNotNull(event);
			assertEquals(0, event.getIndex());

			event.setId(5);
			event.setStrength(100);
			event.setTarget(2);

			event.move(1);
			assertEquals(1, event.getIndex());

			event.setId(6);
			event.setStrength(101);
			event.setTarget(3);

			assertEquals(6, event.getId());
			assertEquals(101, event.getStrength());
			assertEquals(3, event.getTarget());

			event.move(0);
			assertEquals(0, event.getIndex());

			assertEquals(5, event.getId());
			assertEquals(100, event.getStrength());
			assertEquals(2, event.getTarget());
		} finally {
			event.close();
		}
	}

	@Test
    void resizing() {
		Allocator<GameEvent> eventAllocator = Allocator.of(GameEvent.class);
		GameEvent event = eventAllocator.allocate(1);
		assertEquals(1, event.size());

		try {
			event.setStrength(99);

			event.resize(2);
			assertEquals(2, event.size());

			event.move(1);
			event.setStrength(1000);

			event.move(0);
			assertEquals(99, event.getStrength());

		} finally {
			event.close();
		}
	}
	
	@Test
    void twoCloses() {
		Allocator<GameEvent> eventAllocator = Allocator.of(GameEvent.class);
		GameEvent event = eventAllocator.allocate(1);
		try {
			event.close();
		} finally {
			event.close();
		}
	}

	@Test
    void boundsCheckingCatches() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			ConcreteCursor.boundsChecking = true;
			Allocator<GameEvent> eventAllocator = Allocator.of(GameEvent.class);
			GameEvent event = eventAllocator.allocate(1);
			try {
				event.move(2);
			} finally {
				event.close();
			}
		});
	}

}
