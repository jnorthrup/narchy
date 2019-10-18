/*
 * NOTICE OF LICENSE
 *
 * This source file is subject to the Open Software License (OSL 3.0) that is
 * bundled with this package in the file LICENSE.txt. It is also available
 * through the world-wide-web at http:
 * If you did not receive a copy of the license and are unable to obtain it
 * through the world-wide-web, please send an email to magnos.software@gmail.com
 * so we can send you a copy immediately. If you use any of this software please
 * notify me via our website or email, your feedback is much appreciated.
 *
 * @copyright   Copyright (c) 2011 Magnos Software (http:
 * @license     http:
 *              Open Software License (OSL 3.0)
 */

package jcog.tree.perfect;


import java.util.stream.IntStream;

/**
 * A {@link TrieSequencer} implementation where byte[] is the sequence type.
 *
 * @author Philip Diffenderfer
 */
class TrieSequencerByteArray implements TrieSequencer<byte[]> {

    @Override
    public int matches(byte[] sequenceA, int indexA, byte[] sequenceB, int indexB, int count) {
        return IntStream.range(0, count).filter(i -> sequenceA[indexA + i] != sequenceB[indexB + i]).findFirst().orElse(count);

    }

    @Override
    public int lengthOf(byte[] sequence) {
        return sequence.length;
    }

    @Override
    public int hashOf(byte[] sequence, int i) {
        return sequence[i];
    }

}
