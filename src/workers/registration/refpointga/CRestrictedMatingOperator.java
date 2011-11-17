/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.CRouletteWheel;
import geneticalgorithms.IParentSelector;
import java.util.Arrays;

/**
 * Mating operator select father according to standard roulette wheel. Mother
 * is selected according to standard roulette wheel with probability
 * RESTRICTED_MATING_PROBABILITY otherwise is selection reduced on nearest
 * @param size individuals. In second case is also used RouletteWheel.
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CRestrictedMatingOperator extends CRouletteWheel implements IParentSelector<CPointAndTransformation> {

	private final double MOTHER_RANGE = 0.4;

	/**
	 * First individual should be selected by this method
	 * @param population set of individuals
	 * @return selected individual according to RouletteWheel
	 */
	public CPointAndTransformation selectFather(CPointAndTransformation[] population) {
		computeFitnessSum(population);
		return (CPointAndTransformation) select(population);
	}

	/**
	 * Second mate is selected by this method. Two types of access are used.
	 * Fist is common roulette wheel. Second is roulette wheel restricted only
	 * on @param size of nearest individuals to father.
	 * @param population set of individuals
	 * @param father referring point for searching nearest individuals
	 * @return selected individual
	 */
	public CPointAndTransformation selectMother(CPointAndTransformation[] population, CPointAndTransformation father) {
		Arrays.sort(population, new CDistanceComparator(father));
		int neighNo = (int) (population.length * MOTHER_RANGE);
		CPointAndTransformation[] kNearest = new CPointAndTransformation[neighNo];

		System.arraycopy(population, 0, kNearest, 0, neighNo);

		CRouletteWheel motherMatchMaker = new CRouletteWheel();
		motherMatchMaker.computeFitnessSum(kNearest);

		return (CPointAndTransformation) motherMatchMaker.select(kNearest);
	}
}
