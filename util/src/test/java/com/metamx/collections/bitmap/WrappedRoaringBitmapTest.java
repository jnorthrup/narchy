/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metamx.collections.bitmap;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


class WrappedRoaringBitmapTest {
    private final RoaringBitmapFactory factory;

    WrappedRoaringBitmapTest(RoaringBitmapFactory factory) {
        this.factory = factory;
    }

    @TestFactory
    static Stream<DynamicTest> factoryClasses() {

        return List.of(

                new RoaringBitmapFactory(false),
                new RoaringBitmapFactory(true)

        ).stream().map(factory ->
                List.of(

                        DynamicTest.dynamicTest(factory.toString() + "_serialize", () -> {
                            WrappedRoaringBitmap set = createWrappedRoaringBitmap(factory);

                            byte[] buffer = new byte[set.getSizeInBytes()];
                            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                            set.serialize(byteBuffer);
                            byteBuffer.flip();
                            ImmutableBitmap immutableBitmap = new RoaringBitmapFactory().mapImmutableBitmap(byteBuffer);
                            assertEquals(5, immutableBitmap.size());
                        }),

                        DynamicTest.dynamicTest(factory.toString() + "_toByteArray", () -> {
                            WrappedRoaringBitmap set = createWrappedRoaringBitmap(factory);
                            ImmutableBitmap immutableBitmap = new RoaringBitmapFactory().mapImmutableBitmap(ByteBuffer.wrap(set.toBytes()));
                            assertEquals(5, immutableBitmap.size());
                        })

                )).flatMap(Collection::stream);
    }

    private static WrappedRoaringBitmap createWrappedRoaringBitmap(RoaringBitmapFactory factory) {
        WrappedRoaringBitmap set = (WrappedRoaringBitmap) factory.makeEmptyMutableBitmap();
        set.add(1);
        set.add(3);
        set.add(5);
        set.add(7);
        set.add(9);
        return set;
    }


}
