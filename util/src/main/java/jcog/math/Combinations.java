package jcog.math;


import static com.google.common.math.IntMath.factorial;

/** from: http://stackoverflow.com/a/5578494 */
public final class Combinations {

    private final int[] a;
    private final int n;
    private final int r;

    
    private int numLeft;
    private final int total;


    
    
    
    public Combinations(int n, int r) {
        if (n < 1) {
            throw new IllegalArgumentException("Set must have at least one element");
        }
        if (r > n) {
            throw new IllegalArgumentException("Subset length can not be greater than setAt length");
        }
        this.n = n;
        this.r = r;
        a = new int[r];
        var nFact = factorial(n);
        var rFact = factorial(r);
        var nminusrFact = factorial(n - r);
        total = nFact / (rFact * nminusrFact);
        reset();
    }

    
    
    
    public void reset() {
        var a = this.a;

        var alen = a.length;
        for (var i = 0; i < alen; i++) {
            a[i] = i;
        }
        numLeft = total;
    }

    
    
    
    public int remaining() {
        return numLeft;
    }

    
    
    
    public boolean hasNext() {
        return numLeft > 0;
    }

    
    
    
    public int getTotal() {
        return total;
    }












    public int[] prev() {
        return a;
    }

    
    
    
    public int[] next() {

        if (numLeft == total) {
            numLeft--;
            return a;
        }

        var a = this.a;


        var i = r - 1;
        while (a[i] == n - r + i) {
            i--;
        }
        a[i]++;
        for (var j = i + 1; j < r; j++) {
            a[j] = a[i] + j - i;
        }

        numLeft--;
        return a;

    }
}
