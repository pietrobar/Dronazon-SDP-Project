package restserver.beans;

import dronazon.Coordinate;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Pietro on 07/05/2021
 */
@XmlRootElement
public class DroneInfo {
  private int id;
  private int port;

  private Coordinate position;

  public DroneInfo(){}

  public DroneInfo(int id, int port) {
    this.id = id;
    this.port = port;
  }


  public int getId() {
    return id;
  }

  public int getPort() {
    return port;
  }

  public Coordinate getPosition() {
    return position;
  }

  public void setPosition(Coordinate position) {
    this.position = position;
  }
}
