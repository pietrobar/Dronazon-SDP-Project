package dronesnetwork;

import dronazon.Coordinate;
import dronazon.Order;
import restserver.beans.DroneInfo;
import sensorpm10.Buffer;
import sensorpm10.BufferCls;
import sensorpm10.PM10Simulator;

import java.util.List;

/**
 * Created by Pietro on 09/05/2021
 */
public class Drone {
  //initialization fields
  private final int id;
  private final int port;
  private final String administratorServerAddress;

  private int batteryCharge;
  private boolean master;

  private final PM10Simulator pm10Sensor;

  //info received from server admininstrator
  private Coordinate position;
  private List<DroneInfo> drones;

  private List<Order> orders;

  public Drone(int id, int port, String administratorServerAddress) {
    this.id = id;
    this.port = port;
    this.administratorServerAddress = administratorServerAddress;
    this.batteryCharge=100;
    pm10Sensor = new PM10Simulator(new BufferCls());


    //todo: si registra al sistema tramite il server amministratore
    //todo: inserimento nella rete decentralizzata
    //todo: fa partire il thread che raccoglie dati sull'inquinamento

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
