package mcaixictw;

import mcaixictw.worldmodels.ContextTree;
import mcaixictw.worldmodels.WorldModelSettings;
import mcaixictw.worldmodels.Worldmodel;
import org.junit.jupiter.api.*;


public class ContextTreeTest {

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {

        WorldModelSettings settings = new WorldModelSettings();
        settings.setFacContextTree(false);
        settings.setDepth(2);
        ct = Worldmodel.getInstance("ContextTreeTestModel", settings);
        
        
        
        
        
    }

    double eps = 1E-8; 
    Worldmodel ct;

    @AfterEach
    public void tearDown() throws Exception {
    }

    /**
     * check that the probability for 0 and 1 always sums up to 1.
     *
     * @param model
     */
    private void sumUpTo1(ContextTree model) {
        double p_1 = model.predict(new BooleanArrayList(true));
        double p_0 = model.predict(new BooleanArrayList(false));
        System.out.println("p_1: " + p_1 + " p_0: " + p_0);
        Assertions.assertTrue(Math.abs(1.0 - (p_1 + p_0)) < eps);
    }

    @Test
    public final void testFromFAISlides() {
        
        

        WorldModelSettings settings = new WorldModelSettings();
        settings.setFacContextTree(false);
        settings.setDepth(3);
        ContextTree ct = (ContextTree) Worldmodel.getInstance(
                "ContextTreeTestModel", settings);
        sumUpTo1(ct);

        BooleanArrayList past;
        BooleanArrayList context;

        
        past = new BooleanArrayList();
        past.add(true);
        past.add(true);
        past.add(false);

        ct.updateHistory(past);
        sumUpTo1(ct);

        
        ct.update(new BooleanArrayList(false));
        sumUpTo1(ct);
        ct.update(new BooleanArrayList(true));
        sumUpTo1(ct);
        ct.update(new BooleanArrayList(false));
        sumUpTo1(ct);
        ct.update(new BooleanArrayList(false));
        sumUpTo1(ct);
        ct.update(new BooleanArrayList(true));
        sumUpTo1(ct);
        ct.update(new BooleanArrayList(true));
        sumUpTo1(ct);
        ct.update(new BooleanArrayList(false));
        sumUpTo1(ct);

        System.out.println(ct);

        
        Assertions.assertTrue(equals(Math.exp(ct.getRoot().getLogProbWeighted()),
                7.0 / 2048.0));

        
        context = new BooleanArrayList();
        context.add(true);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                1.0 / 16.0));

        context = new BooleanArrayList();
        context.add(false);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                9.0 / 128.0));

        
        context = new BooleanArrayList();
        context.add(true);
        context.add(true);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                1.0 / 2.0));

        context = new BooleanArrayList();
        context.add(true);
        context.add(false);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                1.0 / 8.0));

        context = new BooleanArrayList();
        context.add(false);
        context.add(true);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                5.0 / 16.0));

        context = new BooleanArrayList();
        context.add(false);
        context.add(false);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                3.0 / 8.0));

        
        context = new BooleanArrayList();
        context.add(true);
        context.add(true);
        context.add(false);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                1.0 / 2.0));

        context = new BooleanArrayList();
        context.add(true);
        context.add(false);
        context.add(false);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                1.0 / 8.0));

        context = new BooleanArrayList();
        context.add(false);
        context.add(true);
        context.add(true);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                1.0 / 2.0));

        context = new BooleanArrayList();
        context.add(false);
        context.add(true);
        context.add(false);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                1.0 / 2.0));

        context = new BooleanArrayList();
        context.add(false);
        context.add(false);
        context.add(true);
        Assertions.assertTrue(equals(Math.exp(ct.getNode(context).getLogProbWeighted()),
                3.0 / 8.0));

    }

    private boolean equals(double v1, double v2) {
        double diff = Math.abs(v1 - v2);
        if (diff < eps) {
            System.out.println("equal v1: " + v1 + " v2: " + v2);
            return true;
        } else {
            System.out.println("not equal v1: " + v1 + " v2: " + v2);
        }
        return false;
    }

    @Test
    public final void test0() {

        BooleanArrayList symbols, sym1, sym0;

        symbols = new BooleanArrayList();
        boolean b = true;
        symbols.add(b);
        symbols.add(!b);
        
        
        
        
        
        

        ct.update(symbols);
        System.out.println("updated");

        sym1 = new BooleanArrayList();
        sym0 = new BooleanArrayList();
        sym1.add(true);
        sym0.add(false);
        double p1, p0;
        p1 = ct.predict(sym1);
        p0 = ct.predict(sym0);
        System.out.println("p_1: " + p1 + " p_0: " + p0);

        System.out.println(ct);

        
        
        
        
        
        
        
        
        
        
        
        

    }

    @Test
    public final void test1() {

        BooleanArrayList list = new BooleanArrayList();
        list.add(false);
        for (int i = 0; i < 25; i++) {
            ct.update(list);
        }

        list.clear();
        list.add(true);
        double p1 = ct.predict(list);

        for (int i = 0; i < 7; i++) {
            ct.update(list);
        }

        double p2 = ct.predict(list);
        Assertions.assertTrue(p2 > p1);
    }

    @Test
    public final void test2() {

        WorldModelSettings settings = new WorldModelSettings();
        settings.setFacContextTree(false);
        int depth = 2;
        settings.setDepth(depth);

        ContextTree ct = (ContextTree) Worldmodel.getInstance(
                "ContextTreeTestModel", settings);

        
        


        ct.updateHistory(Util.rand(depth));
        System.out.println("empty");
        System.out.println(ct);

        this.sumUpTo1(ct);

        System.out.println("update with 1: ");
        ct.update(new BooleanArrayList(true));
        this.sumUpTo1(ct);

        ct.update(new BooleanArrayList(false));

        System.out.println(ct);

        System.out.println("update with 0: ");
        ct.update(new BooleanArrayList(true));
        this.sumUpTo1(ct);

        System.out.println(ct);

        System.out.println("revert");
        ct.revert();
        this.sumUpTo1(ct);

        System.out.println(ct);

        System.out.println("update with 1");
        ct.update(new BooleanArrayList(true));
        this.sumUpTo1(ct);

        System.out.println(ct);

        System.out.println("revert");
        ct.revert();
        this.sumUpTo1(ct);

        System.out.println(ct);

        System.out.println("revert");
        ct.revert();
        this.sumUpTo1(ct);
        System.out.println(ct);

    }

    @Test
    public final void test3() {
        WorldModelSettings settings = new WorldModelSettings();
        settings.setFacContextTree(false);
        int depth = 10;
        settings.setDepth(depth);

        ContextTree ct = (ContextTree) Worldmodel.getInstance(
                "ContextTreeTestModel", settings);
        BooleanArrayList history = new BooleanArrayList();
        for (int i = 0; i < depth; i++) {
            history.add(Util.randSym());
        }
        ct.updateHistory(history);
        int testLength = 1000;
        for (int i = 0; i < testLength; i++) {
            ct.update(new BooleanArrayList(Util.randSym()));
            sumUpTo1(ct);
        }
    }

    @Test
    public final void testToString() {

        String r = ct.toString();

        System.out.println(r);

        Assertions.assertTrue(r != null);
    }

    @Test
    public final void testUpdateBoolean() {

        boolean toPredict = false;

        BooleanArrayList list = new BooleanArrayList();
        list.add(toPredict);

        double p1 = ct.predict(list);

        BooleanArrayList list2 = new BooleanArrayList();

        int symbols = 100;
        for (int i = 0; i < symbols; i++) {
            list2.add(Util.randSym());
        }
        ct.update(list2);
        ct.revert(list2.size());

        double p3 = ct.predict(list);

        Assertions.assertTrue(p1 - p3 < eps);
    }

    @Test
    public final void testUpdateListOfBoolean() {

        BooleanArrayList list = new BooleanArrayList();

        list.add(true);
        list.add(false);
        list.add(true);

        BooleanArrayList listTrue = new BooleanArrayList();
        listTrue.add(true);

        double p1 = ct.predict(listTrue);

        ct.update(list);
        ct.revert(3);

        Assertions.assertTrue(p1 - ct.predict(listTrue) < eps);
    }

    @Test
    public final void testUpdateHistory() {

        BooleanArrayList listTrue = new BooleanArrayList();
        listTrue.add(true);
        double p1 = ct.predict(listTrue);

        int s1 = ct.historySize();

        BooleanArrayList list = new BooleanArrayList();
        list.add(true);
        list.add(false);
        list.add(true);

        ct.updateHistory(list);

        Assertions.assertTrue(ct.historySize() == s1 + list.size());

        ct.revertHistory(s1);

        Assertions.assertTrue(ct.historySize() == s1);
        Assertions.assertTrue(ct.predict(listTrue) - p1 < eps);
    }

    @Test
    public final void testRevert() {

        BooleanArrayList listTrue = new BooleanArrayList();
        listTrue.add(true);

        BooleanArrayList listFalse = new BooleanArrayList();
        listFalse.add(false);

        double pTrue = ct.predict(listTrue);
        double pFalse = ct.predict(listFalse);

        int numSymbols = 100;
        BooleanArrayList list = new BooleanArrayList();
        for (int i = 0; i < numSymbols; i++) {
            list.add(Util.randSym());
        }

        ct.update(list);
        ct.revert(list.size());

        Assertions.assertTrue(ct.predict(listTrue) - pTrue < eps);
        Assertions.assertTrue(ct.predict(listFalse) - pFalse < eps);
    }

    @Test
    public final void testRevertHistory() {

        BooleanArrayList list = new BooleanArrayList();
        int s1 = ct.historySize();
        int numSymbols = 100;
        for (int i = 0; i < numSymbols; i++) {
            list.add(Util.randSym());
        }
        double p = ct.predict(list);
        ct.updateHistory(list);
        ct.revertHistory(s1);
        Assertions.assertTrue(p - ct.predict(list) < eps);
    }

    @Test
    public final void testGenRandomSymbols() {

        BooleanArrayList list = new BooleanArrayList();
        list.add(true);
        double p1 = ct.predict(list);

        ct.genRandomSymbols(10);

        Assertions.assertTrue(ct.predict(list) - p1 < eps);
    }

    @Test
    public final void testGenRandomSymbolsAndUpdate() {

        int s1 = ct.historySize();

        int numSymbols = 11;
        ct.genRandomSymbolsAndUpdate(numSymbols);

        Assertions.assertTrue(ct.historySize() == s1 + numSymbols);

    }

    @Test
    public final void testPredictBoolean() {

        BooleanArrayList list = new BooleanArrayList();
        list.add(true);

        double p = ct.predict(list);

        for (int i = 0; i < 100; i++) {
            
            Assertions.assertTrue(ct.predict(list) - p < eps);
        }

        double pTrue = ct.predict(list);
        list.clear();
        list.add(false);
        double pFalse = ct.predict(list);

        Assertions.assertTrue(pTrue + pFalse - 1.0 < eps);
    }

    @Test
    public final void testPredictListOfBoolean() {

        BooleanArrayList list = new BooleanArrayList();
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        double p1 = ct.predict(list);

        BooleanArrayList updateList = new BooleanArrayList();

        int numSymbols = 100;
        for (int i = 0; i < numSymbols; i++) {
            updateList.clear();
            updateList.add(Util.randSym());
            ct.update(updateList);
        }
        for (int i = 0; i < numSymbols; i++) {
            ct.revert(1);
        }

        Assertions.assertTrue(ct.predict(list) - p1 < eps);
    }

    @Test
    public final void testNthHistorySymbol() {

        BooleanArrayList updateList = new BooleanArrayList();
        updateList.add(false);
        updateList.add(false);
        updateList.add(true);
        updateList.add(false);
        updateList.add(false);

        ct.update(updateList);

        Assertions.assertTrue(!ct.nthHistorySymbol(0));
        Assertions.assertTrue(!ct.nthHistorySymbol(1));
        Assertions.assertTrue(ct.nthHistorySymbol(2));
        Assertions.assertTrue(!ct.nthHistorySymbol(3));
        Assertions.assertTrue(!ct.nthHistorySymbol(4));

    }

}
