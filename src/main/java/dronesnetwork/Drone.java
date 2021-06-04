package dronesnetwork;

import dronazon.Coordinate;
import dronazon.Order;
import restserver.beans.DroneInfo;
import sensorpm10.BufferCls;
import sensorpm10.PM10Simulator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Pietro on 09/05/2021
 */
public class Drone {
  //initialization fields
  private final int id;
  private final String ip="localhost";
  private final int port;
  private final String administratorServerAddress;

  //Communication
  private final DroneGRPCCommunication droneGRPCManager;
  private DroneOrderManager droneOrderManager;
  private DroneRESTCommunication droneRESTCommunicationManager;

  private int batteryCharge;
  private int masterId;

  private final PM10Simulator pm10Sensor;

  //info received from server admininstrator
  private Coordinate position;
  private List<DroneInfo> drones;

  //for the master to collect orders
  private List<Order> orders;

  public Drone(int id, int port, String administratorServerAddress) {
    this.id = id;
    this.port = port;
    this.administratorServerAddress = administratorServerAddress;
    this.batteryCharge=100;
    pm10Sensor = new PM10Simulator(new BufferCls());

    //Registration to the Server administrator
    //can be synchronous, until the registration the drone can't do anything
    while(!DroneRESTCommunication.registerDrone(this)){
      System.out.println("contacting server");
    }

    //Insert in the network and start reception for GRPC communications
    droneGRPCManager = new DroneGRPCCommunication(this);
    Thread t = new Thread(droneGRPCManager);
    t.start();


    //todo: fare partire il thread che raccoglie dati sull'inquinamento





  }

  public synchronized List<DroneInfo> getDronesCopy() {
    return new ArrayList<>(drones);
  }

  public int getId() {
    return id;
  }

  public String getIp() {
    return this.ip;
  }

  public int getPort() {
    return port;
  }

  public String getAdministratorServerAddress() {
    return administratorServerAddress;
  }

  public void setPosition(Coordinate position) {
    this.position = position;
  }

  public void setDrones(List<DroneInfo> drones) {
    this.drones = drones;
  }

  public int getBatteryCharge() {
    return batteryCharge;
  }

  public void setBatteryCharge(int batteryCharge) {
    this.batteryCharge = batteryCharge;
  }

  public int getMasterId() {
    return masterId;
  }

  public void setMasterId(int masterId) {
    this.masterId = masterId;
  }

  public Coordinate getPosition() {
    return position;
  }

  public DroneGRPCCommunication getDroneGRPCManager() {
    return this.droneGRPCManager;
  }

  public DroneOrderManager getDroneOrderManager() {
    return droneOrderManager;
  }

  public void setDroneOrderManager(DroneOrderManager droneOrderManager) {
    this.droneOrderManager = droneOrderManager;
  }

  public DroneRESTCommunication getDroneRESTCommunicationManager() {
    return droneRESTCommunicationManager;
  }

  public void setDroneRESTCommunicationManager(DroneRESTCommunication droneRESTCommunicationManager) {
    this.droneRESTCommunicationManager = droneRESTCommunicationManager;
  }

  public synchronized void addDroneInfo(DroneInfo droneInfo) {
    drones.add(droneInfo);
    drones.sort(Comparator.comparingInt(DroneInfo::getId));
  }

  public DroneInfo successor() {
    for (DroneInfo d : getDronesCopy()){
      if(d.getId()>this.id){
        return d;
      }
    }
    //if no one has highest id my successor is the first Drone in the ordered list, and i'm the last one
    return drones.get(0);
  }

  @Override
  public String toString() {
    return "DRONE{" +
            "id=" + id +",\n"+
            "ip='" + ip  +",\n"+
            "port=" + port +",\n"+
            "batteryCharge=" + batteryCharge +",\n"+
            "masterId=" + masterId +",\n"+
            "position=" + position +",\n"+
            "drones=" + drones +",\n"+
            "orders=" + orders +",\n"+
            "successor= " + successor()+
            '}';
  }


  public static void main(String[] args) {
    new Drone(1,999,"http://localhost:1337/drone_interface");
  }
}
