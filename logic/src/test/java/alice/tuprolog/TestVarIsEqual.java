package alice.tuprolog;

import alice.tuprolog.event.OutputEvent;
import alice.tuprolog.event.OutputListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author George S. Cowan
 *
 */
@Disabled
public class TestVarIsEqual {

  Prolog core;
  String yes = "yes.\n";
  private final SysoutListener sysoutListener = new SysoutListener();

  @BeforeEach
  protected void setUp() {
    core = new Prolog();
    core.addOutputListener(sysoutListener);
  }

  private class SysoutListener implements OutputListener {
    public StringBuilder builder = new StringBuilder("");

    @Override
    public void onOutput(OutputEvent ev) {
        builder.append(ev.msg);
    }
    public String getAllOutput() {
      return builder.toString();
    }
  }

  @Test
  public void testDifferntVarsCompareEqual() throws MalformedGoalException, InvalidTheoryException {
    
    String theory = String.join("\n", "test :- body_for_head_literal_instrumented(d(X,Y),(not_d(X,U);d(X,Y)),Bod).    ", "                                                                 ", "body_for_head_literal_instrumented(Head,Wff,Body) :-             ", "  nl,print('body_for_head_literal input Head: '),print(Head),    ", "  nl,print('                             Wff: '),print(Wff),     ", "  false -> true ;                                                ", "  Wff = (A ; B) ->                                               ", "    nl,print('OR'),                                              ", "    body_for_head_literal_instrumented(Head,A,A1),               ", "    body_for_head_literal_instrumented(Head,B,B1),               ", "    conjoin(A1,B1,Body)                                          ", "    , nl, print('body_for_head_literal OR - Body: '),print(Body) ", "    ;                                                            ", "  Wff == Head ->                                                 ", "    Body = true;                                                 ", "  negated_literal_instrumented(Wff,Head) ->                      ", "    print(' '),                                                  ", "    Body = false;                                                ", "  %true ->                                                       ", "    nl,print('OTHERWISE'),                                       ", "    negated_literal_instrumented(Wff,Body).                      ", "                                                                 ", "negated_literal_instrumented(Lit,NotLit) :-                      ", "  nl,print('*** negated_literal in Lit:'),print(Lit),            ", "  nl,print('***                 NotLit:'),print(NotLit),                              ", "  Lit =.. [F1|L1],                                               ", "  negated_functor(F1,F2),                                        ", "  (var(NotLit) ->                                                ", "    NotLit =.. [F2|L1];                                          ", "  %true ->                                                       ", "    nl,print('                 Not var:'),print(NotLit),                            ", "    NotLit =.. [F2|L2],                                          ", "    nl,print('***              Lit array:'),print(L1),           ", "    nl,print('***           NotLit array:'),print(L2),           ", "    L1 == L2                                                     ", "    , nl,print('***               SUCCEEDS')                     ", "    ).                                                           ", "                                                                 ", "negated_functor(F,NotF) :-                                       ", "  atom_chars(F,L),                                               ", "  atom_chars(not_,L1),                                           ", "  (list_append(L1,L2,L) ->                                       ", "    true;                                                        ", "  %true ->                                                       ", "    list_append(L1,L,L2)),                                       ", "  atom_chars(NotF,L2).                                           ", "                                                                 ", "conjoin(A,B,C) :-                                                ", "  A == true ->                                                   ", "    C = B;                                                       ", "  B == true ->                                                   ", "    C = A;                                                       ", "  A == false ->                                                  ", "    C = false;                                                   ", "  B == false ->                                                  ", "    C = false;                                                   ", "  %true ->                                                       ", "    % nl,print('conjoin A: '),print(A),print(' B: '),print(B),   ", "    C = (A , B)                                                  ", "    % , nl,print('    out A: '),print(A),print(' B: '),print(B)  ", "    % , nl,print('        C: '),print(C)                         ", "  .                                                              ", "                                                                 ", "list_append([X|L1],L2,[X|L3]) :-                                 ", "  list_append(L1,L2,L3).                                         ", "list_append([],L,L).                                             ", "                                                                 ")
        ;

    core.setTheory(new Theory(theory));

    Solution info = core.solve("test. ");
    assertTrue(info.isSuccess(),
            new Supplier<String>() {
                @Override
                public String get() {
                    return "Test should complete normally: " + info;
                }
            });
    String expected = ""
      + '\n' +    "body_for_head_literal input Head: d(X_e1,Y_e1)"
      + '\n' +    "                             Wff: ';'(not_d(X_e1,U_e1),d(X_e1,Y_e1))"
      + '\n' +    "OR"
      + '\n' +    "body_for_head_literal input Head: d(X_e25,Y_e25)"
      + '\n' +    "                             Wff: not_d(X_e25,U_e25)"
      + '\n' +    "*** negated_literal in Lit:not_d(X_e25,U_e25)  NotLit:d(X_e25,Y_e25)"
      + '\n' +    "***              Lit array:[X_e122,U_e86]"
      + '\n' +    "***           NotLit array:[X_e122,Y_e122]"
      + '\n' +    "OTHERWISE"
      + '\n' +    "*** negated_literal in Lit:not_d(X_e122,U_e86)  NotLit:NotLit_e136"
      + '\n' +    "body_for_head_literal input Head: d(X_e184,Y_e122)"
      + '\n' +    "                             Wff: d(X_e184,Y_e122)"
      + '\n' +    "Wff == Head"
      + '\n' +    "body_for_head_literal OR - Body: d(X_e249,U_e249)"
      + '\n' +    ""
    ;

  assertEquals("Var == should not succeed.", expected, sysoutListener::getAllOutput);
  }

}
