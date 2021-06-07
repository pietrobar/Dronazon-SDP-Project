package dronesnetwork;

import dronazon.Coordinate;
import restserver.beans.DroneInfo;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pietro on 09/05/2021
 */
public class Drone {
  //initialization fields
  private final int id;
  private final String ip="localhost";
  private final int port;
  private final String administratorServerAddress="http://localhost:1337/drone_interface";

  //Communication
  private DroneGRPCCommunication droneGRPCManager;
  private DroneOrderManager droneOrderManager;

  private int batteryCharge;
  private int masterId=-1;

  //pollution
  private DronePollutionSensor dronePollutionSensor;

  //info received from server administrator
  private Coordinate position;
  private List<DroneInfo> drones;

  private DroneStatsCollector droneStatsCollector;
  ScheduledExecutorService exec;
  private boolean quit=false;
  private boolean delivering=false;

  public final Object terminationObj = new Object();

  public Drone(int id, int port) {
    this.id = id;
    this.port = port;
    this.batteryCharge=100;
  }

  public void startDrone() throws InterruptedException {
    //Registration to the Server administrator
    //can be synchronous, until the registration the drone can't do anything
    while(!DroneRESTCommunication.registerDrone(this)){
      System.out.println("contacting server");
    }

    //Insert in the network and start reception for GRPC communications
    droneGRPCManager = new DroneGRPCCommunication(this);
    Thread t = new Thread(droneGRPCManager);

    synchronized (this){
      t.start();//this will call the notify
      this.wait();//wait for DroneGRPCCommunication to tell me he has finished insertNetwork()
    }

    if(this.id==this.getMasterId()){
      justBecomeMaster();
    }


    dronePollutionSensor = new DronePollutionSensor();
    Thread p = new Thread(dronePollutionSensor);
    p.start();

    synchronized (terminationObj){
      terminationObj.wait();//droneGRPCCommunication will tell me when it's time
    }
    leaveNetwork();

  }

  private void justBecomeMaster(){
    //manage orders
    droneOrderManager = new DroneOrderManager(this);
    Thread t1 = new Thread(droneOrderManager);
    t1.start();

    //starts a new thread to send statistics to server administrator
    droneStatsCollector=new DroneStatsCollector(this);
    exec = Executors.newSingleThreadScheduledExecutor();
    exec.scheduleAtFixedRate(droneStatsCollector::generateAndSendStatistic, 0, 10, TimeUnit.SECONDS);
  }

  public void leaveNetwork() {
    if(!this.isDelivering()){//if is delivering it will be unlocked by delivery() in grpc server
      if(this.getId()==this.getMasterId()){//if I'm the master
        //1 - my delivery is done
        //2 - disconnect from MQTT broker
        synchronized (droneOrderManager){
          droneOrderManager.notify();
        }
        //3 - wait for assign of left deliveries
        synchronized (droneOrderManager){
          while (droneOrderManager.getOrders().size()>0){//while there are still orders to deliver
            try {
              droneOrderManager.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
        //4 - close the communications with other drones
        //5 - send statistics to server administrator
        exec.shutdown();
        droneStatsCollector.generateAndSendStatistic();
        //6 - Ask server admin to exit the system
        DroneRESTCommunication.quit(this);

        System.out.println("\033[0;35m"+"BYE BYE"+"\033[0m");
        System.exit(0);
      }else{
        //I'm NOT the master
        //1 - my delivery is done
        //2 - close the communications with other drones
        //3 - Ask server admin to exit the system
        DroneRESTCommunication.quit(this);
      }
    }
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

  public synchronized void setDrones(List<DroneInfo> drones) {
    this.drones = drones;
  }
  public synchronized void removeDroneFromList(DroneInfo bestDrone) {
    this.drones.remove(bestDrone);
  }

  public int getBatteryCharge() {
    return batteryCharge;
  }

  public void setBatteryCharge(int batteryCharge) {
    this.batteryCharge = batteryCharge;
  }

  public synchronized int getMasterId() {
    return masterId;
  }

  public synchronized void setMasterId(int masterId) {
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


  public synchronized boolean isQuitting() {
    return quit;
  }

  public synchronized void setQuit(boolean quit) {
    this.quit = quit;
  }

  public DronePollutionSensor getDronePollutionSensor() {
    return dronePollutionSensor;
  }

  public DroneStatsCollector getDroneStatsCollector() {
    return droneStatsCollector;
  }

  public void setDroneStatsCollector(DroneStatsCollector droneStatsCollector) {
    this.droneStatsCollector = droneStatsCollector;
  }

  public synchronized boolean isDelivering() {
    return delivering;
  }

  public synchronized void setDelivering(boolean delivering) {
    this.delivering = delivering;
  }

  public synchronized void addDroneInfo(DroneInfo droneInfo) {
    drones.add(droneInfo);
    drones.sort(Comparator.comparingInt(DroneInfo::getId));
  }

  public synchronized void updateDroneList(DroneInfo droneInfo) {
    for (DroneInfo di : drones){
      if (di.getId() == droneInfo.getId()){
        di.setBattery(droneInfo.getBattery());
        di.setPosition(droneInfo.getPosition());
      }
    }
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
            "successor= " + successor()+
            '}';
  }


  public static void main(String[] args) throws InterruptedException {
    Drone d = new Drone(10,991);
    d.startDrone();
  }


}
