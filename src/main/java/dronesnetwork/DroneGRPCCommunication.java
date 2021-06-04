package dronesnetwork;

import com.example.grpc.DroneRPC;
import com.example.grpc.DroneServiceGrpc;
import dronazon.Coordinate;
import dronazon.Order;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import restserver.beans.DroneInfo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by Pietro on 13/05/2021
 */
public class DroneGRPCCommunication implements Runnable{
  private final Drone drone;
  private final List<DroneInfo> occupiedDrones;


  public DroneGRPCCommunication(Drone drone){
    this.drone=drone;
    occupiedDrones = new ArrayList<>();//everyone does it but is needed only in the master drone
  }
  /*
   * This thread will manage the communication in the drones network
   * */
  @Override
  public void run() {
    insertNetwork();//after I'm in the network i'll start receiving messages
    reception();
  }

  /*
  * Broadcast to all the drone of the list (if any)
  * The inserting drone has the updated list, the other drones of the list receives the drone info
  * so that they can update their local list to be consistent.*/
  private void insertNetwork() {
    //if i'm the only one => i'm the Master
    List<DroneInfo> drones = drone.getDronesCopy();
    if (drones.size()==1){
      drone.setMasterId(drone.getId());
      //if I'm the master i have to start a thread to manage the orders
      Thread t1 = new Thread(new DroneOrderManager(drone));
      t1.start();

    }else{
      //otherwise broadcast to every other node
      List<Thread> threads = new ArrayList<>();
      for (DroneInfo node : drones){
        if(node.getId()!=drone.getId()){
          Thread t = new Thread(){
            @Override
            public void run() {
              final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+node.getPort()).usePlaintext().build();
              //todo: gestire le eccezioni sulla connessione
              DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);

              DroneRPC.AddDroneRequest request = DroneRPC.AddDroneRequest.newBuilder()
                      .setId(drone.getId())
                      .setIp(drone.getIp())
                      .setPort(drone.getPort())
                      .setPosition(DroneRPC.Coordinate.newBuilder().setXCoord(drone.getPosition().getX()).setYCoord(drone.getPosition().getY()))
                      .build();

              DroneRPC.AddDroneResponse response = stub.addDrone(request);//receive an answer
              //todo: aggiungere timeout .withDeadlineAfter(1, TimeUnit.SECONDS).
//            if (Context.current().isCancelled()) {
//              System.out.println("Drone didn't answer");
//              //I assume Drone is dead => todo:Tell other drones one is out
//            }
              if (response.getMasterId()!=-1)//all drones except master sends -1
                drone.setMasterId(response.getMasterId());//no need to sync, just one response will contain the master ID


              channel.shutdown();
            }
          };
          threads.add(t);
          t.start();
        }
      }
      for (Thread t: threads){
        try {
          t.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      //todo: iniziare una elezione se non c'e' un master attivo, dopo aver aspettato il timeout!!

    }
  }


  /*
  * Start a GRPC server to be able to receive messages from other drones*/
  public void reception(){
    System.out.println(drone);
    try {

      Server server = ServerBuilder.forPort(drone.getPort()).addService(new DroneServiceGrpc.DroneServiceImplBase() {
        /*
        * In addDrone() I have to respond directly to the sender, so that he knows if i'm alive*/
        @Override
        public void addDrone(DroneRPC.AddDroneRequest request, StreamObserver<DroneRPC.AddDroneResponse> responseObserver) {

          int eventualMasterId = drone.getMasterId()==drone.getId() ? drone.getId() : -1;
          DroneRPC.AddDroneResponse response = DroneRPC.AddDroneResponse.newBuilder().setMasterId(eventualMasterId).build();

          responseObserver.onNext(response);

          //I have to add the drone into my list
          DroneInfo di = new DroneInfo(request.getId(),request.getIp(),request.getPort());
          di.setBattery(100);//needed by master if a drone is new to assign a delivery
          di.setPosition(new Coordinate(request.getPosition().getXCoord(),request.getPosition().getYCoord()));
          drone.addDroneInfo(di);
          System.out.println("from reception: "+drone);

          responseObserver.onCompleted();
        }

        /*Receiving the order to deliver*/
        @Override
        public void delivery(DroneRPC.OrderRequest request, StreamObserver<DroneRPC.OrderResponse> responseObserver) {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          //calculate km to get to delivery point and pickup point
          Coordinate c1 = new Coordinate(request.getPickUpPoint().getXCoord(),request.getPickUpPoint().getYCoord());
          int d1 = distance(drone.getPosition(),c1);
          Coordinate c2 = new Coordinate(request.getDeliveryPoint().getXCoord(),request.getDeliveryPoint().getYCoord());
          int d2 = distance(c1,c2);

          drone.setPosition(c2);
          drone.setBatteryCharge(drone.getBatteryCharge()-10);
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

          DroneRPC.OrderResponse response = DroneRPC.OrderResponse.newBuilder()
                  .setTimestamp(LocalDateTime.now().format(formatter))
                  .setCurrentPos(request.getDeliveryPoint())
                  .setKilometers(d1+d2)
                  .setMeanPollution(1)
                  .setBattery(drone.getBatteryCharge())
                  .build();

          //todo: finire di calcolare le altre informazioni (vedi documento progetto)
          //todo: terminata questa funzione se il livello di batteria poco devo uscire dalla rete
          System.out.println("CONSEGNA EFFETTUATA" + response);
          responseObserver.onNext(response);

          responseObserver.onCompleted();

        }
      }).build();

      server.start();

      System.out.println("Drone reception started!");

      server.awaitTermination();

    } catch (IOException e) {

      e.printStackTrace();

    } catch (InterruptedException e) {

      e.printStackTrace();

    }
  }
  /*Called by DroneOrderManager to assign a delivery to a free drone*/
  public void assignOrder(Order order, DroneOrderManager droneOrderManager){
    synchronized (this){
      while(drone.getDronesCopy().size()==occupiedDrones.size()) {//all drones are occupied
        try {
          wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      //here I'm sure there is at least one free drone
      DroneInfo bestDrone = findBestDrone(order);//this could be null BUT drone.getDronesCopy().size()==occupiedDrones.size() assure it cannot be null
      synchronized (occupiedDrones){//can't deadlock with synchronized(this) -> add is not dependant on anything
        occupiedDrones.add(bestDrone);
      }

      //call GRPC to that drone
      final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+bestDrone.getPort()).usePlaintext().build();
      DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);

      DroneRPC.OrderRequest request = DroneRPC.OrderRequest.newBuilder()
              .setOrderId(order.getId())
              .setPickUpPoint(DroneRPC.Coordinate.newBuilder().setXCoord(order.getPickUpPoint().getX()).setYCoord(order.getPickUpPoint().getY()))
              .setDeliveryPoint(DroneRPC.Coordinate.newBuilder().setXCoord(order.getDeliveryPoint().getX()).setYCoord(order.getDeliveryPoint().getY()))
              .build();

      DroneRPC.OrderResponse response = stub.delivery(request);//the answer will contains the statistics after the delivery. This call should take ~ 5 seconds

      System.out.println(response);
      //todo: response contiene le statistiche da inviare al server amministratore=> DroneRestCommunication.sendStats(request)

      //once the order is done I want to remove it from the list
      droneOrderManager.removeOrder(order);

      synchronized (occupiedDrones){
        occupiedDrones.remove(bestDrone);
      }
      //freed one drone I can notify his freedom
      notify();
    }

  }


  private DroneInfo findBestDrone(Order order) {
    //sorting criteria: free -> distance -> battery -> ID
    List<DroneInfo> drones = drone.getDronesCopy();
    drones.sort((o1, o2) -> {
//        0: if (x==y)
//       -1: if (x < y)
//        1: if (x > y)
      if(distance(o1.getPosition(),order.getPickUpPoint())>distance(o2.getPosition(),order.getPickUpPoint())) return  1;
      else if(distance(o1.getPosition(),order.getPickUpPoint())<distance(o2.getPosition(),order.getPickUpPoint())) return -1;
      else if(o1.getBattery() > o2.getBattery()) return 1;//distance is equal so compare by battery
      else if(o1.getBattery() < o2.getBattery()) return -1;
      else if(o1.getId()>o2.getId()) return 1;//distance and battery are equals => compare by id
      else if(o1.getId()<o2.getId()) return -1;
      return 0;
    });
    //from the sorted list I want a free drone
    for (int i = drones.size()-1; i>=0; i--){
      if(!occupiedDrones.contains(drones.get(i))){
        return drones.get(i);
      }
    }
    return null;
  }

  private int distance(Coordinate from,Coordinate to){
    return (int)Math.sqrt(Math.pow(to.getX()-from.getX(),2)+Math.pow(to.getY()-from.getY(),2));
  }
}
