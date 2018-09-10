package nars.bag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BagClusteringTest {

    @Test
    void testBagCluster1() {
        BagClustering<String> b = newClusteredBag(3, 8);
        b.put("aaaa", 0.5f);
        b.put("aaab", 0.5f);
        b.put("s", 0.5f);
        b.put("q", 0.5f);
        b.put("r", 0.5f);
        b.put("x", 0.5f);
        b.put("y", 0.5f);
        assertEquals(7, b.size());
        b.put("z", 0.5f);
        assertEquals(8, b.size());

        b.commit(1f, 1);
        b.cluster(null, null, (x,y) -> { return true; });

        assertEquals(8, b.size());

        b.bag.print();
        System.out.println();
        b.print();
    }

    private static BagClustering<String> newClusteredBag(int clusters, int cap) {
        return new BagClustering<>(new StringFeatures(), clusters, cap);
    }

    private static class StringFeatures extends BagClustering.Dimensionalize<String> {

        StringFeatures() {
            super(2);
        }

        @Override
        public void coord(String t, double[] d) {
            d[0] = t.length();

            int x = 0;
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (Character.isAlphabetic(c)) {
                    x += Character.toLowerCase(c) - 'a';
                }
            }
            d[1] = x;
        }





    }
}