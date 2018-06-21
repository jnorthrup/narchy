package com.insightfullogic.slab.performance;

import com.insightfullogic.slab.Allocator;
import com.insightfullogic.slab.GameEvent;
import jcog.Texts;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TODO: make this test more scientific:
 * (1) Introduce warmup to remove JIT Effects
 * (2) add memory usage measurement
 */
public class GameEventPerformanceTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameEventPerformanceTest.class);
	
	private static final Allocator<GameEvent> allocator = Allocator.of(GameEvent.class);
	private static final int OBJECTS_ALLOCATED = 5 * 1000 * 1000;

	private static List<GameEventPOJO> pojos;
	private static GameEvent flyweighted;

	enum Accessor {
		POJO {
			@Override
			public GameEvent get(int i) {
				return pojos.get(i);
			}

			@Override
			public void allocate() {
				pojos = new ArrayList<GameEventPOJO>(OBJECTS_ALLOCATED);
				for (int i = 0; i < OBJECTS_ALLOCATED; i++) {
					pojos.add(new GameEventPOJO());
				}
			}
		},
		SLAB {
			@Override
			public GameEvent get(int i) {
				flyweighted.move(i);
				return flyweighted;
			}

			@Override
			public void allocate() {
				flyweighted = allocator.allocate(OBJECTS_ALLOCATED);
			}
		}
		;
		
		protected abstract void allocate();
		protected abstract GameEvent get(int i);
	}
 
	@Test
    void runPerformanceTest() {
		try {
			
			for (int i = 0; i < 2; i ++) {
				touchAllObjects(Accessor.POJO, false);
				touchAllObjects(Accessor.SLAB, false);
			}

			
			long pojoTime = 0, slabTime = 0;
			System.gc();
			for (int i = 0; i < 2; i++) {
				pojoTime += touchAllObjects(Accessor.POJO, true);
			}
			System.gc();
			for (int i = 0; i < 2; i++) {
				slabTime += touchAllObjects(Accessor.SLAB, true);
			}
			System.out.println("POJO=" + pojoTime + ", SLAB=" + slabTime + " gain=" +
					Texts.n4((pojoTime / ((double)slabTime)))
			);
			assertTrue(slabTime < pojoTime);
		} finally {
			flyweighted.close();
		}
	}

	private long touchAllObjects(Accessor values, boolean print) {
        final long start = System.nanoTime();
        values.allocate();

        final long[] lives = { 5000, 4000, 3000, 2000, 1000 };

        
        for (int i = 0; i < OBJECTS_ALLOCATED; i++) {
			GameEvent event = values.get(i);
			event.setId(i);
			event.setStrength(i % 50);
			event.setTarget(i % 5);
		}

        final long middle = System.nanoTime();

        
        for (int i = 0; i < OBJECTS_ALLOCATED; i++) {
			GameEvent event = values.get(i);
			int target = event.getTarget();
			long lifeRemaining = lives[target];
			if (lifeRemaining > 0) {
				lives[target] = lifeRemaining - event.getStrength();
			}
        }

        final long reads = System.nanoTime() - middle;
        final long writes = middle - start;
        if (print) {
			LOGGER.info("Read duration of {} run is {}ns = {}ms\n", values.name(), reads, reads / 1000000.0);
			LOGGER.info("Write duration of {} run is {}ns = {}ms\n", values.name(), writes, writes / 1000000.0);
		}
        return reads + writes;
	}

}
