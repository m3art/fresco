/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package geneticalgorithms;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public interface ICompetitor<T> {

	public T compete(T son, T daughter);

	public T compete(T[] population, T child);
}
