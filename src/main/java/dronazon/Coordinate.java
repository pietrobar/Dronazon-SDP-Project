package dronazon;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Random;

/**
 * Created by Pietro on 07/05/2021
 */
@XmlRootElement
public class Coordinate{
  public int x;
  public int y;

  public Coordinate(){}
  public Coordinate(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public static Coordinate randomCoordinate(){
    int x = new Random().nextInt(10);//generate numbers from 0 to 9
    int y = new Random().nextInt(10);
    Coordinate generated = new Coordinate(x,y);
    //TODO: si potrebbe controllare che non vengano create due coordinate uguali di seguito => punto di partenza == punto arrivo
    return generated;
  }
}
