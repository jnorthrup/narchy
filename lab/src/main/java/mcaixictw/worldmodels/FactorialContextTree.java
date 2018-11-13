package mcaixictw.worldmodels;

import jcog.data.list.FasterList;
import mcaixictw.BooleanArrayList;

import java.util.List;

/**
 * Same as ContextTree but uses one tree for each perception bit.
 */
public class FactorialContextTree extends ContextTree {

	protected FactorialContextTree(String name, int depth) {
		super(name, depth);
		
		root = null;
	}

	protected List<ContextTree> ctwTrees;
	protected int addedSymbolCount;

	private List<ContextTree> allocateTrees(int numTrees) {
		System.out.println("allocate trees: " + numTrees);
		if (ctwTrees == null) {
			ctwTrees = new FasterList<>(numTrees);
		}
		for (int i = 0; i < numTrees; i++) {
			ctwTrees.add(new ContextTree(name + '_' + i, depth));
		}
		return ctwTrees;
	}

	
	
	
	
	
	

	@Override
    protected void update(boolean sym) {
		int m_currentlyActiveTree = addedSymbolCount % ctwTrees.size();
		assert (historySize() != 0 || m_currentlyActiveTree == 0);

		
		ctwTrees.get(m_currentlyActiveTree).add(sym, history);

		
		history.add(sym);
		addedSymbolCount++;
	}

	@Override
	public void update(BooleanArrayList symlist) {
		if (ctwTrees == null || ctwTrees.size() == 0) {
			ctwTrees = allocateTrees(symlist.size());
		}
		
		
		
		
		if (symlist.size() != ctwTrees.size()) {
			throw new IllegalArgumentException(
					"perception has wrong size, there are " + ctwTrees.size()
							+ " trees but the perception has size "
							+ symlist.size());
		}
		
		for (int i = 0; i < symlist.size(); i++) {
			update(symlist.get(i));
		}
	}

	@Override
    public void revert() {
		assert (addedSymbolCount > 0);

		boolean sym = history.pop();
		int m_currentlyActiveTree = (addedSymbolCount - 1) % ctwTrees.size();
		ctwTrees.get(m_currentlyActiveTree).remove(sym, history);
		addedSymbolCount--;
	}


	@Override
	public double logBlockProbability() {
		double logSum;
		logSum = 0.0;
		for (int i = 0; i < ctwTrees.size(); ++i) {
			double logProbTree = ctwTrees.get(i).logBlockProbability();
			assert (Math.exp(logProbTree) <= 1.0 && Math.exp(logProbTree) >= 0.0);
			logSum += logProbTree;
		}
		assert (Math.exp(logSum) <= 1.0 && Math.exp(logSum) >= 0.0);
		return logSum;
	}

	@Override
	public int size() {
		int size = 0;
		for (int i = 0, ctwTreesSize = ctwTrees.size(); i < ctwTreesSize; i++) {
			size += ctwTrees.get(i).size();
		}
		return size;
	}

	public String toString() {
		String result = "";
		result += "Factorial Context Tree" + '\n';
		result += "name: " + name + '\n';
		result += "depth: " + depth + '\n';
		if (ctwTrees != null) {
			result += "num context trees: " + ctwTrees.size() + '\n';
		} else {
			result += "no context trees initialized\n";
		}
		result += "history size: " + history.size() + '\n';
		return result;
	}

	@Override
    public String prettyPrint() {
		String result = "";
		result += "History: ";
		for (int i = 0; i < historySize(); ++i) {
			result += history.get(i) + " ";
		}
		result += "\n";
		for (int i = 0; i < ctwTrees.size(); i++) {
			String s = ctwTrees.get(i).toString();
			result += "Tree[" + i + "]\n" + s;
		}
		return result;
	}





	public int addedSymbolCount() {
		return addedSymbolCount;
	}





}
