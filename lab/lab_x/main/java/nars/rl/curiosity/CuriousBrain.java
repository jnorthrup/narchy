package nars.rl.curiosity;

import nars.rl.elsy.QBrain;
import nars.rl.elsy.QLearner.Action;


public class CuriousBrain extends QBrain {
	private static final long serialVersionUID = 1L;
	private Curiosity curiosity;

	public CuriousBrain(CuriousPlayerPerception perception, Action[] actionsArray) {
		this(perception, actionsArray, new int[] {}, new int[] {20});
	}
	
	public CuriousBrain(CuriousPlayerPerception perception, Action[] actionArray, int[] hiddenNeuronsNo, int[] predictionNetHiddenNeurons) {
		super(perception, actionArray, hiddenNeuronsNo);
		curiosity = new Curiosity(perception, this, predictionNetHiddenNeurons);
	}

        @Override
	public void count() {
		getPerception().perceive(); 
		curiosity.learn();
		super.count(); 
		curiosity.countExpectations(); 
		
	}



	public Curiosity getCuriosity() {
		return curiosity;
	}

	public void setCuriosity(Curiosity curiosity) {
		this.curiosity = curiosity;
	}

}
