/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import java.awt.geom.Point2D;
import java.awt.Point;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CNearestPointCompetitorTest {

	CCorrelationDummy fitnessEvaluator = new CCorrelationDummy();

	/**
	 * Test of compete method, of class CNearestPointCompetitor.
	 */
	@Test
	public void testCompete_CPointAndTransformation_CPointAndTransformation() {
		System.out.println("compete");
		CPointAndTransformation son = new CPointAndTransformation(fitnessEvaluator);
		son.setValues(1, 0, 0, 0, 0, 0);

		CPointAndTransformation daughter = new CPointAndTransformation(fitnessEvaluator);
		daughter.setValues(2, 0, 0, 0, 0, 0);

		CNearestPointCompetitor instance = new CNearestPointCompetitor();
		CPointAndTransformation expResult = son;
		CPointAndTransformation result = instance.compete(son, daughter);
		assertEquals(expResult, result);
	}

	/**
	 * Test of compete method, of class CNearestPointCompetitor.
	 */
	@Test
	public void testCompete_CPointAndTransformationArr_CPointAndTransformation() {
		System.out.println("compete");
		CPointAndTransformation[] population = new CPointAndTransformation[5];
		CPointAndTransformation child = new CPointAndTransformation(fitnessEvaluator);

		population[0] = new CPointAndTransformation(fitnessEvaluator);
		population[1] = new CPointAndTransformation(fitnessEvaluator);
		population[2] = new CPointAndTransformation(fitnessEvaluator);
		population[3] = new CPointAndTransformation(fitnessEvaluator);
		population[4] = new CPointAndTransformation(fitnessEvaluator);

		population[0].setValues(0, 0, 0, 0, 0, 0);
		population[1].setValues(1, 1, 0, 0, 0, 0);
		population[2].setValues(2, 2, 0, 0, 0, 0);
		population[3].setValues(3, 3, 0, 0, 0, 0);
		population[4].setValues(4, 4, 0, 0, 0, 0);

		child.setPosition(new Point2D.Double(4, 3));

		CNearestPointCompetitor instance = new CNearestPointCompetitor();
		CPointAndTransformation expResult = population[3];

		CPointAndTransformation result = instance.compete(population, child);
		assertEquals(expResult, result);
	}

	private class CCorrelationDummy extends CCrossCorrelationFitness {

		public CCorrelationDummy() {
			super(null, null, 0);
		}

		@Override
		public double getFitness(CPointAndTransformation individual) {
			System.out.println("" + individual.getPosition());
			if (individual.getPosition().x % 2 == 0) {
				return 0;
			} else {
				return 1;
			}
		}
	}
}
