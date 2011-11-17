/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.CEvolution;
import utils.metrics.CEuclidMetrics;

/**
 *
 * @author gimli
 */
public class CPointEvolution extends CEvolution<CPointAndTransformation> {

	/** Correction of position of individuals */
	private final CNearestEdgeMatrix nem;
	/** Number of called oneGeneration */
	private int currentGeneration;

	public CPointEvolution(int generations, CPointAndTransformation[] population, CNearestEdgeMatrix nem) {
		super(generations, population);
		this.nem = nem;
		matchMaker = new CRestrictedMatingOperator();
		midwife = new CPointCrossing();
		godWill = new CPointMutation();
		world = new CNearestPointCompetitor();
	}

	@Override
	public boolean oneGeneration() {
		if (currentGeneration++ >= generations) {
			return false;
		}

		CPointAndTransformation father = matchMaker.selectFather(population);
		CPointAndTransformation mother = matchMaker.selectMother(population, father);

		CPointAndTransformation son = midwife.breed(father, mother);
		CPointAndTransformation daughter = midwife.breed(father, mother);

		godWill.mutate(son);
		godWill.mutate(daughter);

		son.alignToEdge(nem);
		daughter.alignToEdge(nem);

		// NOTE: there can be switched between two competitions strategies DE and RTS

//		deterministicCrowding(son, daughter, father, mother);
		restrictedTournamentSelection(son);
		restrictedTournamentSelection(daughter);

		return true;
	}

	/**
	 * Restricted Tournament Selection represents competition strategy, where
	 * subpopulation of size CRefPointMarker#RTS_SIZE is taken randomly. Nearest
	 * neighbor to competing offspring is taken and competition is done by
	 * common GA rules.
	 * @param offspring new individual competing for place in population
	 */
	private void restrictedTournamentSelection(CPointAndTransformation offspring) {
		world.compete(population, offspring);
	}

	/**
	 * This competition strategy replace parents by their offsprings if fitness
	 * of new offsprings is greater. Parents are then replaced in population
	 * @param son first offspring
	 * @param daughter second offspring
	 * @param father first parent
	 * @param mother second parent
	 */
	private void deterministicCrowding(CPointAndTransformation son, CPointAndTransformation daughter,
			CPointAndTransformation father, CPointAndTransformation mother) {
		if (CEuclidMetrics.distance(son.getPosition(), father.getPosition())
				+ CEuclidMetrics.distance(daughter.getPosition(), mother.getPosition())
				< CEuclidMetrics.distance(son.getPosition(), mother.getPosition())
				+ CEuclidMetrics.distance(daughter.getPosition(), father.getPosition())) {
			father.replaceBy(world.compete(son, father));
			mother.replaceBy(world.compete(daughter, mother));
		} else {
			mother.replaceBy(world.compete(son, mother));
			father.replaceBy(world.compete(daughter, father));
		}
	}

	/**
	 * @return number of current generation
	 */
	public int getGenerations() {
		return currentGeneration;
	}
}
