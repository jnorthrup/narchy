package objenome.op.cas;

import objenome.op.cas.util.ArrayLists;

import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class Operation extends Expr {
    
    public abstract ArrayList<Expr> getExprs();
    









    



    
    public Expr getExpr(int index) {
        return getExprs().get(index);
    }
    
    public Expr lastExpr() {
        ArrayList<Expr> exprs = getExprs();
        return exprs.get(exprs.size() - 1);
    }
    
    public boolean firstParenPrint() {
        Integer exprLevelRight = getExpr(0).printLevelRight();
        return (exprLevelRight != null && classOrder() > exprLevelRight) || getExpr(0).firstParenPrint();
    }
    
    public boolean hasUndef() {
        return getExprs().contains(new Undef());
    }
    
    public Expr conditions() {
        return conditions(getExprs());
    }
    
    public static Expr conditions(ArrayList<? extends Expr> exprs) {
        ArrayList<Expr> condsArr = exprs.stream().map(Expr::condition).collect(Collectors.toCollection(ArrayList::new));
        return And.make(condsArr);
    }
    
    public ArrayList<Expr> defineds() {
        return defineds(getExprs());
    }
    
    public static ArrayList<Expr> defineds(ArrayList<? extends Expr> exprsOrig) {
        ArrayList<Expr> exprs = new ArrayList<>(exprsOrig);
        for (int i = 0; i < exprs.size(); i++) {
            Expr exprOn = exprs.get(i);
            if (exprOn instanceof Undef) {



                return null;
            }
            if (exprOn instanceof Conditional) {
                exprs.set(i, exprOn.defined());
            }
        }
        return exprs;
    }
    





    
    public Expr conditioned() {
        Expr conditions = conditions();
        try {
            if (!conditions.equalsExpr(yep())) {
                ArrayList<Expr> defineds = defineds();
                if (defineds == null) return new Undef();
                return Conditional.make(conditions,
                        (Expr) getClass().getMethod("makeDefined", ArrayList.class).invoke(null, defineds()));
            }
        } catch(Exception e) {
            throw new RuntimeException(e + "\nmake(ArrayList) failed on " + getClass().getSimpleName());
        }
        return null;
    }
    
    public boolean equalsExpr(Expr expr) {
        if (expr == null) return false;
        if (expr == this) return true;
        if (!getClass().isAssignableFrom(expr.getClass())) return false;

        return ArrayLists.elemExprsEqual(getExprs(), ((Operation) expr).getExprs());

    }
    
    public boolean notEqualsExpr(Expr expr) {
        if (expr == null) return false;
        if (expr == this) return false;
        if (!getClass().isAssignableFrom(expr.getClass())) return false;
        
        ArrayList<Expr> al1 = getExprs();
        ArrayList<Expr> al2 = ((Operation) expr).getExprs();
        if (al1.size() != al2.size()) return false;
        
        boolean diffYet = false;
        for (int i = 0; i < al1.size(); i++) {
            if (al1.get(i).notEqualsExpr(al2.get(i))) {
                if (diffYet) return false;
                diffYet = true;
            }
        }
        
        return diffYet;
    }
    












    





    
}
