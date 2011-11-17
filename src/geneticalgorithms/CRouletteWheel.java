/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package geneticalgorithms;

/**
 * Basic selection implementation for GA.
 * TODO: fix stupid implementation O(n) now.
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CRouletteWheel implements ISelector<CGenotype> {

	double fitnessSum;

	/**
	 * Before select is called setFitnessSum must be called @see #setFitnessSum.
	 * If fitnessSum is badly or not set, nothing is guaranteed!
	 * @param population set of individuals
	 * @return selected genotype
	 */
	public CGenotype select(CGenotype[] population) {
		double random = Math.random() * fitnessSum;
		double cumulativeFitness = 0;

		int i = 0;
		while (random > cumulativeFitness) {
			cumulativeFitness += population[i].getFitness();
			i++;
		}

		return population[i - 1];
	}

	public void setFitnessSum(double newValue) {
		fitnessSum = newValue;
	}

	/**
	 * This method is used when step-by-step update of fitnessSum is not possible
	 * @param population set of genotypes from which we will select
	 */
	public void computeFitnessSum(CGenotype[] population) {
		fitnessSum = 0;
		for (int i = 0; i < population.length; i++) {
			fitnessSum += population[i].getFitness();
		}
	}
}
