package dronesnetwork;

import com.example.grpc.DroneRPC;
import com.example.grpc.DroneServiceGrpc;
import dronazon.Coordinate;
import dronazon.Order;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import restserver.beans.DroneInfo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 13/05/2021
 */
public class DroneGRPCCommunication implements Runnable{
  private final Drone drone;


  public DroneGRPCCommunication(Drone drone){
    this.drone=drone;
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
    synchronized (drone){
      drone.notify();//main thread is waiting for this to start DroneOrderManager and DroneStatsCollector if master
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
          drone.setDelivering(true);
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

          //take pollution values
          List<Float>  list = drone.getDronePollutionSensor().getMeanList();
          drone.getDronePollutionSensor().clearMeanList();

          DroneRPC.OrderResponse response = DroneRPC.OrderResponse.newBuilder()
                  .setTimestamp(LocalDateTime.now().format(formatter))
                  .setCurrentPos(request.getDeliveryPoint())
                  .setKilometers(d1+d2)
                  .addAllPollutionValues(list)
                  .setBattery(drone.getBatteryCharge())
                  .build();

          System.out.println("CONSEGNA EFFETTUATA");
          responseObserver.onNext(response);

          responseObserver.onCompleted();

          drone.setDelivering(false);
          if(drone.getBatteryCharge()<15 || drone.isQuitting()){
            drone.setQuit(true);//in case is < 15
            synchronized (drone.terminationObj){
              drone.terminationObj.notify();
            }
          }


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

  /*Called by a thread create by DroneOrderManager's callback to assign a delivery to a free drone*/
  public void assignOrder(Order order){

    DroneOrderManager droneOrderManager = drone.getDroneOrderManager();
    synchronized (drone.getDroneOrderManager()){
      while(findBestDrone(order)==null) {//all drones are occupied
        try {
          droneOrderManager.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      //here I'm sure there is at least one free drone
      DroneInfo bestDrone = findBestDrone(order);//this could be null BUT drone.getDronesCopy().size()==occupiedDrones.size() assure it cannot be null

      droneOrderManager.addOccupiedDrone(bestDrone);

      //call GRPC to that drone
      final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+bestDrone.getPort()).usePlaintext().build();
      DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);

      DroneRPC.OrderRequest request = DroneRPC.OrderRequest.newBuilder()
              .setOrderId(order.getId())
              .setPickUpPoint(DroneRPC.Coordinate.newBuilder().setXCoord(order.getPickUpPoint().getX()).setYCoord(order.getPickUpPoint().getY()))
              .setDeliveryPoint(DroneRPC.Coordinate.newBuilder().setXCoord(order.getDeliveryPoint().getX()).setYCoord(order.getDeliveryPoint().getY()))
              .build();

      DroneRPC.OrderResponse response = stub.delivery(request);//the answer will contains the statistics after the delivery. This call should take ~ 5 seconds

      //COLLECT STATS
      DroneStatsCollector dsc = drone.getDroneStatsCollector();
      dsc.addDelivery(bestDrone.getId());
      dsc.addKilometerMean(response.getKilometers());
      dsc.addBattery(response.getBattery());
      dsc.addPollutionValues(response.getPollutionValuesList());//all sync in dsc


      //DRONE LIST UPDATE
      bestDrone.setPosition(new Coordinate(response.getCurrentPos().getXCoord(),response.getCurrentPos().getYCoord()));
      bestDrone.setBattery(response.getBattery());
      drone.updateDroneList(bestDrone);

      //once the order is done I want to remove it from the list
      droneOrderManager.removeOrder(order);

      channel.shutdown();


      droneOrderManager.removeOccupiedDrone(bestDrone);

      //freed one drone I can notify his freedom
      drone.getDroneOrderManager().notifyAll();
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
      if(!drone.getDroneOrderManager().getOccupiedDrones().contains(drones.get(i))){
        return drones.get(i);
      }
    }
    return null;
  }

  private int distance(Coordinate from,Coordinate to){
    return (int)Math.sqrt(Math.pow(to.getX()-from.getX(),2)+Math.pow(to.getY()-from.getY(),2));
  }
}
