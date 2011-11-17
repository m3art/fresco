/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package geneticalgorithms;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public abstract class CEvolution<T> {

	protected T[] population;
	protected IParentSelector<T> matchMaker;
	protected ICrossOverOperator<T> midwife;
	protected IMutationOperator<T> godWill;
	protected ICompetitor<T> world;
	protected final int generations;

	public CEvolution(int iters, T[] population) {
		generations = iters;
		this.population = population;
	}

	public abstract boolean oneGeneration();
}
