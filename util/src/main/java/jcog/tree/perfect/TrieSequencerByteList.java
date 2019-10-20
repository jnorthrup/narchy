package jcog.tree.perfect;

import org.eclipse.collections.api.list.primitive.ByteList;

class TrieSequencerByteList implements TrieSequencer<ByteList> {

    @Override
    public int matches(ByteList sequenceA, int indexA, ByteList sequenceB, int indexB, int count) {
        for (int i = 0; i < count; i++) {
            if ((int) sequenceA.get(indexA + i) != (int) sequenceB.get(indexB + i)) {
                return i;
            }
        }
        return count;

    }

    @Override
    public int lengthOf(ByteList sequence) {
        return sequence.size();
    }

    @Override
    public int hashOf(ByteList sequence, int i) {
        return (int) sequence.get(i);
    }

}