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

  private int batteryCharge;
  private int masterId;

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

    //Registration to the Server administrator
    //can be synchronous, until the registration the drone can't do anything
    while(!DroneRESTCommunication.registerDrone(this)){
      System.out.println("contacting server");
    }

    //Insert in the network and start reception for GRPC communications
    DroneGRPCCommunication droneGRPC = DroneGRPCCommunication.getInstance(this);
    Thread t = new Thread(droneGRPC);
    t.start();


    //todo: fa partire il thread che raccoglie dati sull'inquinamento





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



  public synchronized void addDroneInfo(DroneInfo droneInfo) {
    drones.add(droneInfo);
    drones.sort(Comparator.comparingInt(DroneInfo::getId));
  }

  @Override
  public String toString() {
    return "Drone{" +
            "id=" + id +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            ", batteryCharge=" + batteryCharge +
            ", masterId=" + masterId +
            ", position=" + position +
            ", drones=" + drones +
            ", orders=" + orders +
            ", successor= " + DroneGRPCCommunication.successor(drones,id)+
            '}';
  }

  public static void main(String[] args) {
    new Drone(1,999,"http://localhost:1337/drone_interface");

  }
}
