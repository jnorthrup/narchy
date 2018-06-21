package com.insightfullogic.slab.implementation;

import com.insightfullogic.slab.SlabOptions;
import org.junit.jupiter.api.Test;

import static com.insightfullogic.slab.implementation.MemoryCalculation.calculateAddress;
import static com.insightfullogic.slab.implementation.MemoryCalculation.calculateAllocation;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryCalculationTest {
    
    private final SlabOptions slabAlignedTo32Bytes = SlabOptions.builder()
                                                                .setSlabAlignment(32)
                                                                .build();

    @Test
    void defaultCase() {
        long allocation = calculateAllocation(2, 8, SlabOptions.DEFAULT);
        assertEquals(16, allocation);
    }

    @Test
    void slabAllocationOversizes() {
        long allocation = calculateAllocation(6, 8, slabAlignedTo32Bytes);
        assertEquals(64, allocation);
    }
    
    @Test
    void slabAllocationDoesntOversizeWhenUnnecessary() {
        long allocation = calculateAllocation(4, 8, slabAlignedTo32Bytes);
        assertEquals(32, allocation);
    }
    
    @Test
    void defaultAddressCase() {
        long address = calculateAddress(30, SlabOptions.DEFAULT);
        assertEquals(30, address);
    }
    
    @Test
    void slabAllocationOversizingMovesAddress() {
        long address = calculateAddress(30, slabAlignedTo32Bytes);
        assertEquals(32, address);
    }
    
    @Test
    void slabAllocationDoesntOversizeAddress() {
        long address = calculateAddress(32, slabAlignedTo32Bytes);
        assertEquals(32, address);
    }

}
