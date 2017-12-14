/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Alex Benini
 *         <p>
 *         End state of demostration.
 */
public class StateEnd extends State {

    private final int endState;
    private Struct goal;
    private List<Var> vars;
    private int setOfCounter;

    /**
     * Constructor
     *
     * @param end Terminal state of computation
     */
    public StateEnd(EngineRunner c, int end) {
        this.c = c;
        endState = end;
    }

    public int getResultDemo() {
        return endState;
    }

    public Struct getResultGoal() {
        return goal;
    }

    public List<Var> getResultVars() {
        return vars;
    }


    @Override
    void run(Engine e) {
        vars = new ArrayList<>();
        goal = (Struct) e.startGoal.copyResult(e.goalVars, vars);
//        System.out.println("STATE END: STAMPO LE VAR del GOAL****"+e.goalVars);
//        System.out.println("STATE END: STATE END "+vars+ " GOAL "+goal);  
//        System.out.println("STATE END GOALLLLLLL: "+e.query); 

        if (this.endState == EngineRunner.TRUE || this.endState == EngineRunner.TRUE_CP)
            relinkVar(e);
    }

    private static Term solve(Term a1, Object[] a, Term initGoalBag) {
        //System.out.println("ENTRO NEL SOLVE a1 "+a1+" initGoalBag "+initGoalBag);
        while (a1 instanceof Struct && ((Struct) a1).subs() > 0) {
            //System.out.println("substituteVarGoalBO --- a1 "+a1);
            Term a10 = ((Struct) a1).sub(0);
            if (a10 instanceof Var) {
                //System.out.println("substituteVarGoalBO --- a10 var name  "+((Var)a10).getName()+" original name "+((Var)a10).getOriginalName());
                //System.out.println("substituteVarGoalBO faccio findvar e sostisuisco a1 pos 0 "+a1);
                initGoalBag = findVarName(a10, a, a1, 0);
                //System.out.println("substituteVarGoalBO dopo sostituzione "+a1);
                //((Struct)initGoalBag).setArg(1,a1);
            } else if (a10 instanceof Struct) {
                Term a100 = ((Struct) a10).sub(0);
                //System.out.println("substituteVarGoalBO --- a10 Struct name  "+a10);
                //System.out.println("substituteVarGoalBO --- a10 faccio solve di "+a100+" a1 "+a1);
                a10 = solve(a100, a, a10);
                ((Struct) a1).setSub(0, a10);
                //((Struct)initGoalBag).setArg(1,a1);
            }
            Term a11 = null;
            if (((Struct) a1).subs() > 1)
                a11 = ((Struct) a1).sub(1);
            a1 = a11;
        }
        if (a1 instanceof Var) {
            //System.out.println("substituteVarGoalBO --- a1 var name  "+((Var)a1).getName()+" original name "+((Var)a1).getOriginalName());
            initGoalBag = findVarName(a1, a, initGoalBag, 0);
        }
        return initGoalBag;
    }

    private static Term findVarName(Term link, Object[] a, Term initGoalBag, int pos) {
        boolean findName = false;
        //System.out.println("****!!!!!!! INIZIA FINDVARNAME link "+link+" bag "+initGoalBag+" pos "+pos);
        while (link != null && link instanceof Var && !findName) {
            //System.out.println("****Trovato Link "+link);
            int y = 0;
            while (!findName && y < a.length) {
                Term gVar = (Var) a[y];
                while (!findName && gVar != null && gVar instanceof Var) {
                    //System.out.println("**** ----- verifico uguaglianza con "+gVar);
                    if (((Var) gVar).name().compareTo(((Var) link).name()) == 0) {
                        //System.out.println(link +" **** ----- ***** il nome uguale a "+gVar);
                        //System.out.println(((Struct)initGoalBag).getArg(0));
                        //System.out.println(link +" **** ----- ***** sostituisco "+initGoalBag+" in pos "+pos+" con "+(Var)a[y]);
                        ((Struct) initGoalBag).setSub(pos, new Var(((Var) a[y]).name()));
                        findName = true;
                    }
                    gVar = ((Var) gVar).link();
                }
                y++;
            }
            link = ((Var) link).link();
        }
        return initGoalBag;
    }


    private void relinkVar(Engine e) {
        EngineManager engineMan = c.getEngineMan();
        if (engineMan.getRelinkVar()) { //devo fare il relink solo se ho var libere nella bagof E SE NON CI SONO ESISTENZE

//	    	System.out.println("STATE END relinkvar(): Le var del risultato sono "+(c.getEngineMan()).getBagOFres()); 
//	    	System.out.println("STATE END relinkvar(): Le var del risultato STRINGA sono "+(c.getEngineMan()).getBagOFresString());
            ArrayList<Term> bag = engineMan.getBagOFres();
            Term initBag = engineMan.getBagOFbag();
            //	    	System.out.println("STATE END relinkvar(): la bag "+(c.getEngineMan()).getBagOFbag());
//	    	System.out.println("STATE END relinkvar(): il goal della BO  "+(c.getEngineMan()).getBagOFgoal());

	    	/* itero nel goal per cercare una eventuale struttura che deve fare match con la
	    	 * result bag ESEMPIO setof(X,member(X,[V,U,f(U),f(V)]),[a,b,f(b),f(a)]).
	    	 */
            Term tgoal = engineMan.getBagOFgoal();
            Object[] a = (e.goalVars).toArray();
            Prolog p = new Prolog();
            //String solution="";


            Term query = e.query;
//	    	if(query instanceof Struct && (((Struct)query).getName().equals("setof") || ((Struct)query).getName().equals("bagof"))){
//	    		System.out.println("******** query  struct "+((Struct)query).getName()+" ");
//	    		System.out.println("******** ARG 0 "+((Struct)query).getArg(0));
//	    		System.out.println("******** ARG 1 "+((Struct)query).getArg(1));
//	    		System.out.println("******** ARG 2 "+((Struct)query).getArg(2));
//	    		if(((Struct)query).getArg(2) instanceof Struct)
//	    			System.out.println("******** ARG 2 STRUCT ");
//	    	}


            if (((Struct) query).name().equals(";")) {
                Struct query_temp = (Struct) ((Struct) query).sub(0);
                if (query_temp.name().equals("setof") && setOfCounter == 0) {
                    query = query_temp;
                    this.setOfCounter++;
                } else {
                    query_temp = (Struct) ((Struct) query).sub(1);
                    if (query_temp.name().equals("setof"))
                        query = query_temp;
                }
            }

            if (((Struct) query).subs() > 2 && ((Struct) query).sub(2) instanceof Struct) { //casi in cui ci potrebbe essere problema di soluzione
                //System.out.println("STATE END relinkvar(): trovata struct in initBag "+((Var)initBag).getLink());
                Boolean findSamePredicateIndicator = false;
                Boolean find = false; //trovo nel goal della bag una struct che unifica con la bag iniziale
                Term initGoalBag = null;
                while (tgoal instanceof Var && ((Var) tgoal).link() != null) {
                    tgoal = ((Var) tgoal).link();
                    //System.out.println("STATE END relinkvar(): scorro goal "+tgoal);
                    if (tgoal instanceof Struct) {
                        tgoal = ((Struct) tgoal).sub(1);
                        //System.out.println("STATE END relinkvar(): trovata struct con arg 1 "+tgoal);

                        if (p.unify(tgoal, ((Var) initBag).link())) { //In realtˆ sembra che giˆ qui non unifichi
                            //System.out.println("STATE END relinkvar(): trovata struct con arg 1 che unifica con bag iniziale");
                            initGoalBag = tgoal;
                            find = true;
                            findSamePredicateIndicator = true;
                            break;
                        } else if (((Var) initBag).link() instanceof Struct) {
                            Struct s = (Struct) ((Var) initBag).link();
                            //System.out.println("primitive della bag iniziale "+s.getPredicateIndicator().toString());
                            if (tgoal instanceof Struct && s.key().compareTo(((Struct) tgoal).key()) == 0) {
                                //System.out.println("primitive della bag tgoal "+((Struct)tgoal).getPredicateIndicator().toString());
                                findSamePredicateIndicator = true;
                                find = true;
                                initGoalBag = tgoal;
                            }
                        }
                        //scorro initBagGoal e sostituisco i suoi nomi con i nomi delle variabili goal
                        if (find || findSamePredicateIndicator && initGoalBag instanceof Struct) {
                            //System.out.println("substituteVarGoalBO --- initGoalBag "+initGoalBag);
                            Term a0 = ((Struct) initGoalBag).sub(0);
                            Term a1 = ((Struct) initGoalBag).sub(1);
                            if (a0 instanceof Var) {
                                //		    					System.out.println("substituteVarGoalBO --- a0  var name  "+((Var)a0).getName()+" original name "+((Var)a0).getOriginalName()+" link "+((Var)a0).getLink());
//		    					System.out.println("SCORRO LINK VARIABILE "+a0);
                                initGoalBag = findVarName(a0, a, initGoalBag, 0);

                            }
                            a1 = solve(a1, a, a1);
                            ((Struct) initGoalBag).setSub(1, a1);
                        }
                    }
                }
                //System.out.println("INIT GOAL BAG DOPO RELINK "+initGoalBag+"init bag "+((Var)initBag).getLink());

                //riordino la struttura seguendo l'ordine di comparsa delle var nel goal
                if (initGoalBag != null) {
                    //System.out.println("Creo una lista a partire dalla struttura di initGoalBag ");
                    ArrayList<Term> initGoalBagList = new ArrayList<>();
                    Struct initGoalBagTemp = (Struct) initGoalBag;
                    while (initGoalBagTemp.subs() > 0) {
                        Term t1 = initGoalBagTemp.sub(0);
                        initGoalBagList.add(t1);
                        Term t2 = initGoalBagTemp.sub(1);
                        if (t2 instanceof Struct) {
                            initGoalBagTemp = (Struct) t2;
                        }
                    }
                    //System.out.println("Lista "+initGoalBagList);

                    // QUI FA ORDINAMENTO
                    ArrayList<Term> initGoalBagListOrdered = new ArrayList<>();
                    if (((Struct) query).name().equals("setof")) {
                        ArrayList<String> initGoalBagListVar = new ArrayList<>();
                        for (int m = 0; m < initGoalBagList.size(); m++) {
                            if (initGoalBagList.get(m) instanceof Var)
                                initGoalBagListVar.add(((Var) initGoalBagList.get(m)).name());
                        }
                        //System.out.println("Lista VAR "+initGoalBagListVar);
                        ArrayList<Term> left = new ArrayList<>();
                        left.add(initGoalBagList.get(0));
                        ArrayList<Term> right = new ArrayList<>();
                        ArrayList<Term> right_temp = new ArrayList<>();
                        //right.add(initGoalBagList.get(1));
                        ArrayList<Term> left_temp = new ArrayList<>();
                        for (int m = 1; m < initGoalBagList.size(); m++) {
                            int k = 0;
                            for (k = 0; k < left.size(); k++) {
                                if (initGoalBagList.get(m).isGreaterRelink(left.get(k), initGoalBagListVar)) {
                                    //System.out.println(initGoalBagList.get(m)+" pi grande di "+left.get(k));
                                    left_temp.add(left.get(k));
                                } else {
                                    //System.out.println(initGoalBagList.get(m)+" pi piccolo di "+left.get(k));
                                    left_temp.add(initGoalBagList.get(m));
                                    break;
                                }
                            }
                            if (k == left.size())
                                left_temp.add(initGoalBagList.get(m));
                            for (int y = 0; y < left.size(); y++) {
                                //System.out.println("left.get(y) "+left.get(y));
                                boolean search = false;
                                for (int r = 0; r < left_temp.size(); r++) {
                                    if (left_temp.get(r).toString().equals(left.get(y).toString()))
                                        search = true;
                                }
                                if (search) {
                                    //System.out.println("left_temp.contains(left.get(y)) "+left_temp.indexOf(left.get(y).toString()));
                                    left.remove(y);
                                    y--;
                                } else {
                                    //System.out.println("NON left_temp.contains(left.get(y)) "+left.get(y));
                                    right_temp.add(left.get(y));
                                    left.remove(y);
                                    y--;
                                }
                            }
                            for (int y = 0; y < right.size(); y++) {
                                right_temp.add(right.get(y));
                                right.remove(y);
                                y--;
                            }
                            right.addAll(right_temp);
                            //System.out.println("@@@@@@@@@@@@@@  right.addAll(right_temp) "+right);
                            right_temp.clear();
                            left.addAll(left_temp);
                            //System.out.println("@@@@@@@@@@@@@@  left.addAll(left_temp) "+left);
                            left_temp.clear();
                            //System.out.println(" ");
                        }
                        //ricreo la struttura del goal iniziale a partire dalla lista ordinata
                        //System.out.println("Ricreo la struttura del goal iniziale a partire dalla lista ordinata ");
                        initGoalBagListOrdered.addAll(left);
                        initGoalBagListOrdered.addAll(right);
                        //System.out.println("Lista ordinata "+initGoalBagListOrdered);
                        //fine ordinamento
                    } else initGoalBagListOrdered = initGoalBagList;

                    initGoalBagTemp = (Struct) initGoalBag;

                    Object[] t = initGoalBagListOrdered.toArray();
                    Term[] t1 = new Term[t.length];
                    for (int i = 0; i < t.length; i++) {
                        t1[i] = (Term) t[i];
                    }
                    //			    	System.out.println("initGoalBagTemp "+initGoalBagTemp);
//			    	System.out.println("nuovaSTRUCT "+s);
                    initGoalBag = new Struct(initGoalBagTemp.name(), t1);

                    //System.out.println("Creo una lista a partire dalla struttura di initBag ");//serve per unificare
                    ArrayList<Term> initBagList = new ArrayList<>();
                    Struct initBagTemp = (Struct) ((Var) initBag).link();
                    while (initBagTemp.subs() > 0) {
                        Term t0 = initBagTemp.sub(0);
                        initBagList.add(t0);
                        Term t2 = initBagTemp.sub(1);
                        if (t2 instanceof Struct) {
                            initBagTemp = (Struct) t2;
                        }
                    }
                    //System.out.println("initBagList "+initBagList);
                    Object[] tNoOrd = initBagList.toArray();
                    Term[] termNoOrd = new Term[tNoOrd.length];
                    for (int i = 0; i < tNoOrd.length; i++) {
                        termNoOrd[i] = (Term) tNoOrd[i];
                    }
                    //			    	System.out.println("initBagTemp "+initBagTemp.getName());
//			    	System.out.println("nuovaSTRUCT "+s);
                    initBag = new Struct(initGoalBagTemp.name(), termNoOrd);
                }

//		    	System.out.println("initGoalBag "+initGoalBag);
//		    	System.out.println("findSamePredicateIndicator "+findSamePredicateIndicator);
//		    	System.out.println("find "+find);
//		    	
//		    	System.out.println("Struttura INIZIALE con cui provo ad unificare "+initBag);
//		    	System.out.println("Struttura CREATA con cui provo ad unificare "+initGoalBag);

                if (findSamePredicateIndicator) {
                    if (!(find && p.unify(initGoalBag, initBag))) {
                        //System.out.println("NOOOOOOOOOOOOOOOON unifica DOPO RELINK ");
                        String s = engineMan.getSetOfSolution() + "\n\nfalse.";
                        engineMan.setSetOfSolution(s);
                        //settare la soluzione a false
                        e.nextState = c.END_FALSE;

                        //pulizia variabili bagof
                        engineMan.setRelinkVar(false);
                        engineMan.setBagOFres(null);
                        engineMan.setBagOFgoal(null);
                        engineMan.setBagOFvarSet(null);
                        engineMan.setBagOFbag(null);
                        return;
                    } else {
                        //System.out.println("SIIIIIIIIIIIIIIIII unifica DOPO RELINK ");
                    }
                }
            }
	    	/*
	    	 * STEP1: dalla struttura risultato bagof (bag = (c.getEngineMan()).getBagOFres())
	    	 * estraggo la lista di tutte le variabili
	    	 * memorizzate nell'ArrayList<String> lSolVar
	    	 * lSolVar = [H_e2301, H_e2302, H_e2303, H_e2304, H_e2305, H_e2306, H_e2307, H_e2308]
	    	 */

            ArrayList<String> lSolVar = new ArrayList<>();
            //System.out.println("Le var del risultato sono BAG "+bag);
	    	/*NB lSolVar ha lunghezza multipla di lGoal var, se ho pi soluzioni si ripete 
	    	 * servirebbe esempio con 2 bag */
            ArrayList<String> l_temp = new ArrayList<>();
            for (int i = 0; i < bag.size(); i++) {
                Var resVar = (Var) bag.get(i);
                //System.out.println("RESVAR BAG "+resVar);
                Term t = resVar.link();
                //System.out.println("RESVAR BAG LINK "+resVar);
                if (t != null) {
                    if (t instanceof Struct) {
                        Struct t1 = ((Struct) t);
                        //System.out.println("RESVAR BAG LINK  STRUCT "+t1);
                        //uso lista temporanea per aggiustare ordine, dalla struct con findvar escono al contrario
                        l_temp.clear();
                        l_temp = findVar(t1, l_temp);
                        for (int w = l_temp.size() - 1; w >= 0; w--) {
                            lSolVar.add(l_temp.get(w));
                        }
                    } else if (t instanceof Var) {
                        while (t != null && t instanceof Var) {
                            resVar = (Var) t;
                            //System.out.println("---RESVAR BAG  VAR "+resVar);
                            t = resVar.link();
                            //System.out.println("---RESVAR BAG LINK "+resVar);
                        }
                        lSolVar.add(resVar.name());
                        bag.set(i, resVar);
                    }
                } else lSolVar.add(resVar.name());
            }
            //System.out.println("le variabili nella sol sono lSolVar "+lSolVar);
	    	/*
	    	 * STEP2: dalla struttura goal bagof (goalBO = (Var)(c.getEngineMan()).getBagOFgoal())
	    	 * estraggo la lista di tutte le variabili
	    	 * memorizzate nell'ArrayList<String> lgoalBOVar
	    	 * lgoalBOVar = [Z_e0, X_e73, Y_e74, V_e59, WithRespectTo_e31, U_e588, V_e59, H_e562, X_e73, Y_e74, F_e900]
	    	 */
            //System.out.println("il goal interno bag of  "+(c.getEngineMan()).getBagOFgoal());
            Var goalBO = (Var) engineMan.getBagOFgoal();
            //System.out.println("il goal interno bag of  var con link "+goalBO.getLink());
            ArrayList<String> lgoalBOVar = new ArrayList<>();
            Term goalBOvalue = goalBO.link();
            if (goalBOvalue instanceof Struct) {
                Struct t1 = ((Struct) goalBOvalue);
                //ArrayList<String> l_temp= new ArrayList<>();
                l_temp.clear();
                l_temp = findVar(t1, l_temp);
                for (int w = l_temp.size() - 1; w >= 0; w--) {
                    lgoalBOVar.add(l_temp.get(w));
                }
            }//esistono casi in cui il goal non sia STRUCT ????
            //System.out.println("le variabili nel goal della bagof sono lgoalBOVar "+lgoalBOVar);
	    	
	    	/*
	    	 * STEP3: prendere il set di variabili libere della bagof
	    	 * fare il match con le variabili del goal in modo da avere i nomi del goal esterno
	    	 * questo elenco ci servirˆ per eliminare le variabili in pi che abbiamo in lgoalBOVar
	    	 * ovvero tutte le variabili associate al template
	    	 * lGoalVar [Y_e74, U_e588, V_e59, X_e73, Y_e74, U_e588, F_e900]
	    	 * mette quindi in lGoalVar le variabili che compaiono in goalVars e sono anche libere 
	    	 * per la bagof c.getEngineMan().getBagOFvarSet()
	    	 */
            //System.out.println("Le var della bagof sono "+c.getEngineMan().getBagOFvarSet());
            Var v = (Var) engineMan.getBagOFvarSet();
            Struct varList = (Struct) v.link(); //lista delle variabili nel goal bagof con nomi interni alla bagof
            ArrayList<String> lGoalVar = new ArrayList<>(); //lista delle variabili nel goal bagof con nomi goal
            //ArrayList<String> lGoalVar_copy=new ArrayList<String>() ; //????????mi serve la copia per sostituire le var sia nel goal originale che nel risultato
            //System.out.println("Lista variabili goal bagof nomi interni alla bagof varList "+varList);
            //Object[] a=(e.goalVars).toArray();
            //System.out.println("Lista variabili goal bagof nomi e.goalVars "+e.goalVars.toString());
            //int r=0;
            if (varList != null)
                for (java.util.Iterator<? extends Term> it = varList.listIterator(); it.hasNext(); ) {
                    //System.out.println("Entro "+r);
                    //r++;
                    Term var = it.next();
                    for (int y = 0; y < a.length; y++) {
                        Var vv = (Var) a[y];
                        Term vLink = vv.link();
                        if (vLink != null && vLink.isEqual(var)/*&& !(var.toString().startsWith("_"))*/) {
                            //System.out.println("Aggiungo trovata uguaglianza "+vv+" e var "+var);
                            lGoalVar.add(vv.name());
                        }
                    }
                }
            //System.out.println("********Lista variabili goal bagof nomi goal lGoalVar "+lGoalVar);
	    	
	    	/*
	    	 * STEP4: pulisco lgoalBOVar lasciando solo i nomi che compaiono effettivamente in 
	    	 * lGoalVar (che  la rappresentazione con nomi esterni delle variabili libere nel
	    	 * goal della bagof
	    	 */
            lgoalBOVar.retainAll(lGoalVar);
            //System.out.println("Lista variabili goal bagof nomi goal pulite da template lgoalBOVar "+lgoalBOVar);
            if (lGoalVar.size() > lgoalBOVar.size()) {
                //System.out.println("Entro nell'if ");
                for (int h = 0; h < lGoalVar.size(); h++)
                    if (h >= lgoalBOVar.size()) {
                        //System.out.println("Aggiungo elemento ");
                        lgoalBOVar.add(lGoalVar.get(h));
                    }
            }
	    	/*
	    	 * STEP5: sostituisco le variabili nel risultato (sia in goals che vars)
	    	 * a) cerco l'indice della variabile in lSolVar
	    	 * b) sostituisco con quella di stesso indice in lgoalBOVar
	    	 */
            Var goalSolution = new Var();

            if (!lSolVar.isEmpty() && !lgoalBOVar.isEmpty() && !varList.isGround() && !goalBO.isGround()) {
                String bagVarName = null;
                for (int i = 0; i < bag.size(); i++) {
                    //System.out.println("BAG SIZE "+bag.size());
                    Var resVar = (Var) bag.get(i);
                    //System.out.println("SOSTITUZIONE VAR "+resVar);
                    Term t = resVar.link();
                    if (t == null) {
                        //System.out.println("----link null");
                        t = resVar;
                    }
                    //System.out.println("----link NOT null"+t);
                    //scorro le variabili del goal per vedere quale  il risultato e ne memorizzo il nome
                    bagVarName = null;
                    for (int y = 0; y < a.length; y++) {
                        Var vv = (Var) a[y];
                        Var vv_link = structValue(vv, i);
                        //System.out.println("NOME di vv_link "+vv_link);
                        if (vv_link.isEqual(t)) {
                            //System.out.println("NOME della BAG "+vv.getName());
                            //System.out.println("NOME di vv "+vv);
                            if (bagVarName == null) {
                                bagVarName = vv.getOriginalName();
                                goalSolution = vv;
                            }
                            //sostituzione delle var nella Struct della sol
                            //System.out.println("Sostituisco vv_link "+vv_link.getLink());
                            //System.out.println("Sostituisco vv "+vv_link);
                            if (vv_link.link() != null && vv_link.link() instanceof Struct) {
                                Struct s = substituteVar((Struct) vv_link.link(), lSolVar, lgoalBOVar);
                                //System.out.println("****Nuovo link della var "+vv.getOriginalName()+" link "+s);
                            } else {
                                int index = lSolVar.indexOf(resVar.name());
                                //System.out.println("Index i "+i);
                                //come mai era lgoalBOVar ????
                                setStructValue(vv, i, new Var(lgoalBOVar.get(index)));
                                //System.out.println("****Nuovo link della var "+vv.getOriginalName()+" valore "+vv);
                            }
                        }
                    }

                }
                //System.out.println("La variabile da sostituire  "+bagVarName+" con valore "+goalSolution);
                for (int j = 0; j < vars.size(); j++) {
                    Var vv = vars.get(j);
                    if (vv.getOriginalName().equals(bagVarName)) {
                        Var solVar = varValue2(goalSolution);
                        // qui sarebbe bello fare un set del nome
                        solVar.setName(vv.getOriginalName());
                        solVar.rename(0, 0);
                        //System.out.println("Sol var "+solVar.getOriginalName()+" nome "+solVar.getName()+" con valore "+solVar.getLink());
                        vars.set(j, solVar);
                        break;
                    }
                }
            }
	    	
	    	/*
	    	 * STEP6: gestisco caso particolare SETOF in cui non stampa la soluzione
	    	 */
            ArrayList<String> bagString = engineMan.getBagOFresString();
            int i = 0;
            String s = "";
            //System.out.println("LGOAL VAR "+lGoalVar);
            for (int m = 0; m < bagString.size(); m++) {
                String bagResString = bag.get(m).toString();
                boolean var = false;
                if (bag.get(m) instanceof Var && ((Var) bag.get(m)).link() != null && (((Var) bag.get(m)).link() instanceof Struct) && !((Var) bag.get(m)).link().isAtom())
                    var = true;
                //System.out.println("&&&&&& Var "+var);
                if (var && bagResString.length() != bagString.get(m).length()) {
                    //System.out.println("PROBLEMA STAMPA "+bagResString+" "+bagString.get(m));
                    StringTokenizer st = new StringTokenizer(bagString.get(m));
                    StringTokenizer st1 = new StringTokenizer(bagResString);
                    while (st.hasMoreTokens()) {
                        String t1 = st.nextToken(" /(),;");
                        //System.out.println("COMPARE "+t1);
                        String t2 = st1.nextToken(" /(),;");
                        //System.out.println("COMPARE "+t1+" "+t2);
                        if (t1.compareTo(t2) != 0 && !t2.contains("_")) {
                            //System.out.println("DIVERSO TOKEN "+t1+" "+t2);
                            s = s + lGoalVar.get(i) + '=' + t2 + ' ';
                            //System.out.println(s);
                            engineMan.setSetOfSolution(s);
                            i++;
                        }
                    }
                }
            }


        }


//	    System.out.println("----goal vars a[y] dopo sostituzione "+e.goalVars);
//	    System.out.println("----dopo sostituzione STATE END "+vars+ " GOAL "+goal);  
        engineMan.setRelinkVar(false);
        engineMan.setBagOFres(null);
        engineMan.setBagOFgoal(null);
        engineMan.setBagOFvarSet(null);
        engineMan.setBagOFbag(null);
        //c.getEngineMan().clearSinfoSetOf();

    }

    //    public Var varValue (Var v){
//    	while(v.getLink()!=null){
//    		//System.out.println("+++ VARVALUE cerco il valore v "+v+" link "+v.getLink());
//    		if(v.getLink()instanceof Var)
//    			v=(Var)v.getLink();
//    		else if(v.getLink()instanceof Struct)
//    			v=(Var)((Struct)v.getLink()).getArg(0);
//    		else break;
//    	}
//    	return v;
//    }
    public static Var varValue2(Var v) {
        Term l;
        while ((l = v.link()) != null) {
            //System.out.println("+++ VARVALUE cerco il valore v "+v+" link "+v.getLink());
            if (l instanceof Var)
                v = (Var) l;
            else
                break;
        }
        return v;
    }

    public static Var structValue(Var v, int i) {
        structValue:
        while (true) {
            Var vStruct = new Var();
            Term l;
            while ((l = v.link()) != null) {
                //System.out.println("*** cerco il valore v "+v+" link "+v.getLink());
                if (l instanceof Var) {
                    //System.out.println("*** il link  var");
                    v = (Var) l;
                } else if (l instanceof Struct) {
                    Struct s = ((Struct) l);//new Struct();
//devo prendere l'i_esimo elemento della lista quindi scorro
                    //System.out.println("*** devo prendere l'i_esimo elemento della lista quindi scorro");


                    while (i > 0) {
                        Term s1 = s.sub(1);

                        if (s1 instanceof Struct) {
                            s = (Struct) s1;
                        } else if (s1 instanceof Var) {
                            vStruct = ((Var) s1);
                            if (vStruct.link() != null) {
                                i--;
                                v = vStruct;
                                continue structValue;
                            }
                            return vStruct;
                        }
                        i--;
                    }
                    vStruct = ((Var) s.sub(0));
                    break;
                } else break;
            }
            //System.out.println("+++ ritorno "+vStruct);
            return vStruct;
        }
    }

    public static void setStructValue(Var v, int i, Var v1) {
        Term l;
        while ((l = v.link()) != null) {
            //System.out.println("+++ cerco il valore v "+v+" link "+v.getLink());
            if (l instanceof Var) {
                //System.out.println("+++ il link  var");
                v = (Var) l;
            } else if (l instanceof Struct) {
                Struct s = ((Struct) l);//=new Struct();
//System.out.println("+++ s "+s);
                //devo prendere l'i_esimo elemento della lista quindi scorro
                //System.out.println("+++devo prendere l'i_esimo elemento della lista quindi scorro");
                while (i > 0) {
                    //System.out.println("+++ s "+s.getArg(1));
                    Term s1 = s.sub(1);
                    if (s1 instanceof Struct)
                        s = (Struct) s1;
                    else if (s1 instanceof Var) {
                        v = (Var) s1;
                        s = ((Struct) v.link());
                    }
                    i--;
                }
                s.setSub(0, v1);
                break;
            } else break;
        }
        //System.out.println("+++ ritorno "+vStruct);
    }


    public static ArrayList<String> findVar(Struct s, ArrayList<String> l) {
        ArrayList<String> allVar = l; //new ArrayList<>();
        if (allVar == null) allVar = new ArrayList();
        if (s.subs() > 0) {
            Term t = s.sub(0);
            if (s.subs() > 1) {
                Term tt = s.sub(1);
                //System.out.println("---Termine "+t+" e termine "+tt);
                if (tt instanceof Var) {
                    allVar.add(((Var) tt).name());
                } else if (tt instanceof Struct) {
                    findVar((Struct) tt, allVar);
                }
            }
            if (t instanceof Var) {
                allVar.add(((Var) t).name());
            } else if (t instanceof Struct) {
                findVar((Struct) t, allVar);
            }
        }
        return allVar;
    }

    public static Struct substituteVar(Struct s, ArrayList<String> lSol, ArrayList<String> lgoal) {
        Term t = s.sub(0);
        //System.out.println("STATE END Substitute var ---Termine "+t);
        Term tt = null;
        if (s.subs() > 1)
            tt = s.sub(1);
        //System.out.println("Substitute var ---Termine "+t+" e termine "+tt);
        if (tt != null && tt instanceof Var) {
            int index = lSol.indexOf(((Var) tt).name());
            //System.out.println("Substitute var ---Indice di tt in lSol "+index);
            s.setSub(1, new Var(lgoal.get(index)));
            if (t instanceof Var) {
                int index1 = lSol.indexOf(((Var) t).name());
                //System.out.println("Substitute var ---Indice di t in lSol "+index1);
                s.setSub(0, new Var(lgoal.get(index1)));
            }
            if (t instanceof Struct && ((Struct) t).subs() > 0) {
                //System.out.println("Substitute var-------Trovata struct t "+t);
                //System.out.println("Substitute var-------Trovata struct t arity "+((Struct)t).getArity());
                Struct s1 = substituteVar((Struct) t, lSol, lgoal);
                //System.out.println("Substitute var ---t  struct ritorno s1 "+s1);
                s.setSub(0, s1);
            }
        } else {
            if (t instanceof Var) {
                int index1 = lSol.indexOf(((Var) t).name());
                //System.out.println("Substitute var ---Indice di t in lSol "+index1);
                s.setSub(0, new Var(lgoal.get(index1)));
            }
            if (t instanceof Struct) {
                //System.out.println("Substitute var-------Trovata struct ");
                Struct s1 = substituteVar((Struct) t, lSol, lgoal);
                //System.out.println("Substitute var ---t  struct ritorno s1 "+s1);
                s.setSub(0, s1);
            }
        }
        //System.out.println("Substitute var ---t  nullo ritorno s "+s); Compare di
        return s;
    }


    public String toString() {
        switch (endState) {
            case EngineRunner.FALSE:
                return "FALSE";
            case EngineRunner.TRUE:
                return "TRUE";
            case EngineRunner.TRUE_CP:
                return "TRUE_CP";
            default:
                return "HALT";
        }
    }

}