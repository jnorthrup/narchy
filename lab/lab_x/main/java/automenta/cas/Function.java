package objenome.op.cas;

public abstract class Function extends Operation {
    
    public Integer printLevelLeftPass() {
        return classOrderNum - 1;
    }
    
    




    
    public boolean firstParenPrint() {
        return false;
    }
    
    public Expr ofExpr() {
        return getExpr(0);
    }
    
}
