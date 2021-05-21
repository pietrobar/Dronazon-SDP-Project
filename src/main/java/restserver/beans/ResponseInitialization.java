package restserver.beans;

import dronazon.Coordinate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Pietro on 21/05/2021
 */
@XmlRootElement
public class ResponseInitialization {
  @XmlElement(name = "drones")
  private List<DroneInfo> drones;
  private Coordinate startingPosition;

  public ResponseInitialization(){}

  public ResponseInitialization(List<DroneInfo> drones){
    this.drones=drones;
    startingPosition=Coordinate.randomCoordinate();
  }

  public List<DroneInfo> getDrones() {
    return drones;
  }

  public void setDrones(List<DroneInfo> drones) {
    this.drones = drones;
  }

  public Coordinate getStartingPosition() {
    return startingPosition;
  }

  public void setStartingPosition(Coordinate startingPosition) {
    this.startingPosition = startingPosition;
  }

  @Override
  public String toString() {
    return "ResponseInitialization{" +
            "drones=" + drones +
            ", startingPosition=" + startingPosition +
            '}';
  }
}
