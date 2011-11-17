/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package geneticalgorithms;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public interface IMutationOperator<T> {

	/** Individual going in is mutated */
	public void mutate(T prototype);
}
