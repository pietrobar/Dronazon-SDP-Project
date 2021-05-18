package restserver.beans;

import dronazon.Coordinate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 07/05/2021
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DronesInfoManager {
  @XmlElement
  private final List<DroneInfo> drones;

  private static DronesInfoManager instance;

  public DronesInfoManager() {
    drones = new ArrayList<>();
  }

  public static synchronized DronesInfoManager getInstance(){
    if (instance==null)
      instance = new DronesInfoManager();
    return instance;
  }

  public synchronized List<DroneInfo> getDrones(){
    return new ArrayList<>(drones);
  }

  public boolean add(DroneInfo di){//todo: la lista di droni deve essere sempre ordinata
    for (DroneInfo drone : getDrones()){//questo e' gia' sincronizzato
      if (di.getId()==drone.getId()) return false;
    }
    synchronized (this){
      di.setPosition(Coordinate.randomCoordinate());//set starting position of the new drone
      drones.add(di);
      return true;
    }
  }
}
