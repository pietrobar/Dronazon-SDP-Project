package restserver.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 07/05/2021
 */
public class DronesInfoManager {
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

  public boolean add(DroneInfo di){
    for (DroneInfo drone : getDrones()){
      if (di.getId()==drone.getId()) return false;
    }
    synchronized (this){
      drones.add(di);
      return true;
    }
  }
}
