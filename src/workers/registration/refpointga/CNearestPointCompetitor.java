/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.ICompetitor;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author gimli
 */
public class CNearestPointCompetitor implements ICompetitor<CPointAndTransformation> {

	/**
	 * Basic competition of two individuals. Fitness are compared. The highest wins.
	 * @param son first competitor
	 * @param daughter second competition
	 * @return winner of competition
	 */
	public CPointAndTransformation compete(CPointAndTransformation son, CPointAndTransformation daughter) {
		if (son.getFitness() >= daughter.getFitness()) {
			return son;
		} else {
			return daughter;
		}
	}

	/**
	 * Compares new child with population. Nearest neighbor is selected for
	 * comparison. Population is therefore sorted. If child survives, its
	 * competitor is replaced by this child
	 * @param population nonempty set of individuals
	 * @param child individual
	 * @return the winner of competition
	 */
	public CPointAndTransformation compete(CPointAndTransformation[] population, CPointAndTransformation child) {

		ArrayList<CPointAndTransformation> pop = new ArrayList<CPointAndTransformation>(population.length);
		pop.addAll(Arrays.asList(population));

		CPointAndTransformation[] subPopulation = new CPointAndTransformation[(int) (population.length * CRefPointMarker.RTS_SIZE)];

		int rnd;
		for (int i = 0; i < subPopulation.length; i++) {
			rnd = (int) (Math.random() * pop.size());
			subPopulation[i] = pop.remove(rnd);
		}

		CDistanceComparator distanceCmp = new CDistanceComparator(child);
		Arrays.sort(subPopulation, distanceCmp);

		subPopulation[0].replaceBy(compete(subPopulation[0], child));
		return subPopulation[0];
	}
}
