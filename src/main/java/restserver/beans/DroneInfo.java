package restserver.beans;

import dronazon.Coordinate;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Comparator;

/**
 * Created by Pietro on 07/05/2021
 */
@XmlRootElement
public class DroneInfo implements Comparator<DroneInfo> {
  private int id;
  private String ip;
  private int port;

  private Coordinate position;

  public DroneInfo(){}

  public DroneInfo(int id, String ip,int port) {
    this.id = id;
    this.ip = ip;
    this.port = port;
  }


  public int getId() {
    return id;
  }

  public int getPort() {
    return port;
  }

  public String getIp() {
    return ip;
  }

  public Coordinate getPosition() {
    return position;
  }

  public void setPosition(Coordinate position) {
    this.position = position;
  }


  @Override
  public int compare(DroneInfo o1, DroneInfo o2) {
    return Integer.compare(o1.getId(),o2.getId());//to sort based on index
  }

  @Override
  public String toString() {
    return "DroneInfo{" +
            "id=" + id +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            ", position=" + position +
            '}';
  }
}
