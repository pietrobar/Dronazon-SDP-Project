package dronesnetwork;

import com.example.grpc.DroneRPC;
import com.example.grpc.DroneServiceGrpc;
import dronazon.Coordinate;
import dronazon.Order;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import restserver.beans.DroneInfo;
import sensorpm10.Buffer;
import sensorpm10.BufferCls;
import sensorpm10.PM10Simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

    boolean outcome = DroneRESTCommunication.registerDrone(this);
    if(outcome) insertNetwork();
    //todo: si registra al sistema tramite il server amministratore
    //todo: inserimento nella rete decentralizzata
    //todo: fa partire il thread che raccoglie dati sull'inquinamento

  }

  public synchronized List<DroneInfo> getDronesCopy() {
    return new ArrayList<>(drones);
  }

  private void insertNetwork() {
    //if i'm the only one => i'm the Master
    if (getDronesCopy().size()==1){
      this.masterId=this.id;
    }else{
      //otherwise broadcast to every other node
      DroneInfo successor = successor();

//      for (DroneInfo node : getDronesCopy()){
//        Thread t = new Thread(){
//          //todo: devo passare il parametro this perche' non e' accessibile da dentro il thread=> estendere la classe thread
//          @Override
//          public void run() {
//            final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+node.getPort()).usePlaintext().build();
//            //todo: gestire le eccezioni sulla connessione
//            DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);
//
//            DroneRPC.AddDroneRequest request = DroneRPC.AddDroneRequest.newBuilder().setId(this.id).setXCoord(this.position.getX()).setYCoord(this.position.getY()).build();
//
//            DroneRPC.AddDroneResponse response = stub.addDrone(request);//receive an answer
//            if (response.getMasterId()!=-1)//all drones except master sends -1
//              masterId = response.getMasterId();
//
//
//              //todo: potrei iniziare una elezione se non ci fosse un master attivo
//              channel.shutdown();
//          }
//        };
//
//      }

    }

  }

  public DroneInfo successor() {
    List<DroneInfo> ds =getDronesCopy();
    for (DroneInfo d : ds){
      if(d.getId()>this.id){
        return d;
      }
    }
    //if no one has highest id my successor is the first Drone in the ordered list, and i'm the last one
    return ds.get(0);
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

  /*  First thing to do:
  *   register to the ServerAdministrator
  */


  /*  Second thing to do:
   *  Join droneNetwork
   */


  //CODICE PROVA INVIO STATISTICHE AL SERVER REST



  public static void main(String[] args) {

  }


}
