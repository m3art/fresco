/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers.analyse.paramObjects;

import workers.analyse.CLaplacian;

/**
 *
 * @author Jakub
 */
public class CLoGParams extends CEdgerParams{
    public int filterSize;
    public double gaussSigma;
    public int gaussSize;
    public double gaussSizeQ;
    

    
    public CLoGParams () {
        filterSize = CLaplacian.getFilterSizeDefault();
        gaussSigma =  CLaplacian.getGaussSigmaDefault();
        gaussSize = CLaplacian.getGaussSizeDefault();
        gaussSizeQ = 1;
    };
    
    public CLoGParams (int filterSizeI, double gaussSigmaI, int gaussSizeI) {
        filterSize = filterSizeI;
        gaussSigma =  gaussSigmaI;
        gaussSize = gaussSizeI;
        calculateQFromSize();
        
    };
    
    public void calculateSizeFromQ() {
        gaussSize = (int)(gaussSizeQ * CLaplacian.getGaussSizeDefault());
    }
    public void calculateQFromSize() {
        gaussSizeQ = gaussSize/CLaplacian.getGaussSizeDefault();
    }
    public void systemPrint() {
        System.out.println(" gSg: " + gaussSigma + " gSe: " + gaussSize);
    }

}


