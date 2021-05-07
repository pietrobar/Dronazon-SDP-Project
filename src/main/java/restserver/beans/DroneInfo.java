package restserver.beans;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Pietro on 07/05/2021
 */
@XmlRootElement
public class DroneInfo {
  private int id;
  private int port;

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
}
