/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package utils;

import java.awt.geom.Point2D;

/**
 *
 * @author Jakub
 */
public class CIdxMapper {
  
  /* ROW MAJOR converter */
  
  /* horizontal coordinate ~ Point2D.x, vertical coordinate ~ Point2D.y */
  
  public int toIdx(int horizontalCoord, int verticalCoord, int areaWidth) {
      if (horizontalCoord < 0) {
        throw new RuntimeException("Horizontal coordinate underflow");
      }
      if (verticalCoord < 0) {
        throw new RuntimeException("Vertical coordinate underflow");
      }
      return verticalCoord*areaWidth + horizontalCoord;
  }
  public int toIdx(Point2D.Double input, int areaWidth) {
    
      if (input.x < 0) {
        System.out.println(input.x);
        throw new RuntimeException("Horizontal coordinate underflow");
      }
      if (input.y < 0) {
        System.out.println(input.y);
        throw new RuntimeException("Vertical coordinate underflow");
      }
    return (int)input.y*areaWidth + (int)input.x;
  }
  
  public Point2D.Double toPoint(int idx, int areaWidth) {
    Point2D.Double result = new Point2D.Double();
    result.x = idx % areaWidth;
    result.y = (int)(idx / areaWidth);       
    
    return result;  
  
  }
  
  public boolean isEdge(Point2D.Double input, int areaWidth, int areaHeight) {
    if ((int)input.x == 0 || (int)input.x == areaWidth - 1 || (int)input.y == 0 || (int)input.y == areaHeight - 1){
      return true;
    }
    else return false;
      
  }
  public boolean isEdge(int horizontalCoord, int verticalCoord, int areaWidth, int areaHeight) {
    if (horizontalCoord == 0 || horizontalCoord == areaWidth - 1 || verticalCoord == 0 || verticalCoord == areaHeight - 1){
      return true;
    }
    else return false;
      
  }
  public boolean isNearEdge(Point2D.Double input, int areaWidth, int areaHeight, int dist) {
    if ((int)input.x <= dist || (int)input.x >= areaWidth - (dist+1) || (int)input.y <= dist || (int)input.y >= areaHeight - (dist+1)){
      return true;
    }
    else return false;
      
  }
  public boolean isNearEdge(int horizontalCoord, int verticalCoord, int areaWidth, int areaHeight, int dist) {
    if (horizontalCoord <= dist || horizontalCoord >= areaWidth - (dist+1) || verticalCoord <= dist  || verticalCoord >= areaHeight - (dist+1)){
      return true;
    }
    else return false;
      
  }
  
  public boolean isLeftEdge(Point2D.Double input, int areaWidth, int areaHeight) {
    return ((int)input.x == 0);       
  }
  public boolean isLeftEdge(int horizontalCoord, int verticalCoord, int areaWidth, int areaHeight) {
    return (horizontalCoord == 0);       
  }
  public boolean isRightEdge(Point2D.Double input, int areaWidth, int areaHeight) {
    return ((int)input.x == areaWidth - 1);       
  }
  public boolean isRightEdge(int horizontalCoord, int verticalCoord, int areaWidth, int areaHeight) {
    return (horizontalCoord == areaWidth - 1 );       
  }
  public boolean isTopEdge(Point2D.Double input, int areaWidth, int areaHeight) {
    return ((int)input.y == 0);       
  }
  public boolean isTopEdge(int horizontalCoord, int verticalCoord, int areaWidth, int areaHeight) {
    return (verticalCoord == 0);       
  }
  public boolean isBottomEdge(Point2D.Double input, int areaWidth, int areaHeight) {
    return ((int)input.y == areaHeight - 1);       
  }
  public boolean isBottomEdge(int horizontalCoord, int verticalCoord, int areaWidth, int areaHeight) {
    return (verticalCoord == areaHeight - 1);       
  }
  
  
} 