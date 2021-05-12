package dronesnetwork;

import dronazon.Coordinate;
import dronazon.Order;
import restserver.beans.DroneInfo;

import java.util.List;

/**
 * Created by Pietro on 09/05/2021
 */
public class Drone {
  private final int id;
  private final int port;
  private final String admininstratorServerAddress;

  private int batteryCharge;
  private boolean master;
  //TODO: Buffer del drone per l'inquinamento

  private Coordinate position;
  private List<DroneInfo> drones;

  private List<Order> orderzBuffer;//gestita dal drone master

  public Drone(int id, int port, String admininstratorServerAddress) {
    this.id = id;
    this.port = port;
    this.admininstratorServerAddress = admininstratorServerAddress;
    this.batteryCharge=100;
  }

  public int getBatteryCharge() {
    return batteryCharge;
  }

  public void setBatteryCharge(int batteryCharge) {
    this.batteryCharge = batteryCharge;
  }

  /*  First thing to do:
  *   register to the ServerAdministrator
  */


  /*  Second thing to do:
   *  Join droneNetwork
   */


  //CODICE PROVA INVIO STATISTICHE AL SERVER REST


}
