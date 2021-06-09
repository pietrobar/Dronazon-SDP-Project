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
import java.util.concurrent.TimeUnit;

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
    try {
      insertNetwork();//after I'm in the network i'll start receiving messages
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    reception();
  }

  /*
  * Broadcast to all the drone of the list (if any)
  * The inserting drone has the updated list, the other drones of the list receives the drone info
  * so that they can update their local list to be consistent.*/
  private void insertNetwork() throws InterruptedException {
    //if i'm the only one => i'm the Master
    List<DroneInfo> drones = drone.getDronesCopy();
    if (drones.size()==1){
      drone.setMasterId(drone.getId());
    }else{
      //otherwise broadcast to every other node
      List<Thread> threads = new ArrayList<>();
      List<DroneInfo> deadDrones = new ArrayList<>();
      for (DroneInfo node : drones){
        if(node.getId()!=drone.getId()){
          Thread t = new Thread(() -> {
            final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+node.getPort()).usePlaintext().build();
            //todo: gestire le eccezioni sulla connessione
            DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);

            DroneRPC.AddDroneRequest request = DroneRPC.AddDroneRequest.newBuilder()
                    .setId(drone.getId())
                    .setIp(drone.getIp())
                    .setPort(drone.getPort())
                    .setPosition(DroneRPC.Coordinate.newBuilder().setXCoord(drone.getPosition().getX()).setYCoord(drone.getPosition().getY()))
                    .build();
            DroneRPC.AddDroneResponse response;
            try{
              response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).addDrone(request);//receive an answer, could fail-> timeout

              drone.setMasterId(response.getMasterId());
            }catch (Exception e){
              deadDrones.add(node);
              drone.removeDroneFromList(node);//Remove dead drone'
            }
            channel.shutdown();
          });
          threads.add(t);

        }
      }
      for (Thread t: threads){
        t.start();
      }
      for (Thread t:threads){
        t.join();
      }

      if (drone.getDronesCopy().size()==1)
        drone.setMasterId(drone.getId());

      if (!(drone.getMasterId()==drone.getId())){
        for(DroneInfo dead : deadDrones){
          if(dead.getId()==drone.getMasterId()){
            new Thread(this::startElection).start();

          }else{
            //DEAD DRONE delete from my list
            drone.removeDroneFromList(dead);
          }
        }
      }



    }
    synchronized (drone){
      drone.notify();//main thread is waiting for this to start DroneOrderManager and DroneStatsCollector if master
    }
  }

  private void startElection() {
    System.out.println("ELECTION STARTED");
    if(drone.getDronesCopy().size()==1){//if I'm the only one
      drone.setMasterId(drone.getId());
      drone.justBecomeMaster();
    }else{
      //If master is dead I have to start an election => Chang And Roberts

      //1 - Find Successor
      DroneInfo successor = findAliveSuccessor(drone.toDroneInfo());

      //2 - Participating to the election
      drone.setInElection(true);

      //3 - Send Election message
      final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+successor.getPort()).usePlaintext().build();
      DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);
      DroneRPC.Election request = DroneRPC.Election.newBuilder().setId(drone.getId()).setBattery(drone.getBatteryCharge()).build();
      DroneRPC.EmptyResponse response=null;
      try{
        response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).election(request);//receive an answer, could fail-> timeout
        channel.shutdown();
        //response is empty
      }catch (Exception e){
        //DEAD DRONE delete from my list
        drone.removeDroneFromList(successor);
        channel.shutdown();
        startElection();//restart the election because this one failed!
      }
    }



  }

  private DroneInfo findAliveSuccessor(DroneInfo d){
    DroneInfo wannabeSuccessor = drone.successor(drone);
    if(isAlive(wannabeSuccessor)){
      return wannabeSuccessor;
    }
    return findAliveSuccessor(wannabeSuccessor);
  }

  protected boolean isAlive(DroneInfo di) {
    //ping a drone
    boolean ret=false;

    final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+di.getPort()).usePlaintext().build();
    DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);
    DroneRPC.PingRequest request = DroneRPC.PingRequest.newBuilder().build();
    DroneRPC.PingResponse response=null;

    try{
      response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).ping(request);//receive an answer, could fail-> timeout
      ret = true;
    }catch (Exception e){
      if(di.getId()== drone.getMasterId()){
        drone.removeDroneFromList(di);//remove master
        startElection();
      }else{
        //DEAD DRONE delete from my list
        drone.removeDroneFromList(di);
      }
    }
    channel.shutdown();
    return ret;
  }



  /*
  * SERVER: Start a GRPC server to be able to receive messages from other drones*/
  public void reception(){
    try {

      Server server = ServerBuilder.forPort(drone.getPort()).addService(new DroneServiceGrpc.DroneServiceImplBase() {
        /*
        * In addDrone() I have to respond directly to the sender, so that he knows if i'm alive*/
        @Override
        public void addDrone(DroneRPC.AddDroneRequest request, StreamObserver<DroneRPC.AddDroneResponse> responseObserver) {

          DroneRPC.AddDroneResponse response = DroneRPC.AddDroneResponse.newBuilder()
                  .setId(drone.getId())
                  .setMasterId(drone.getMasterId()).build();

          responseObserver.onNext(response);

          //I have to add the drone into my list
          DroneInfo di = new DroneInfo(request.getId(),request.getIp(),request.getPort());
          di.setBattery(100);//needed by master if a drone is new to assign a delivery
          di.setPosition(new Coordinate(request.getPosition().getXCoord(),request.getPosition().getYCoord()));
          drone.addDroneInfo(di);

          responseObserver.onCompleted();
        }

        /*Receiving the order to deliver*/
        @Override
        public void delivery(DroneRPC.OrderRequest request, StreamObserver<DroneRPC.OrderResponse> responseObserver) {
          drone.setDelivering(true);
          drone.setBatteryCharge(drone.getBatteryCharge()-10);//helps the election
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

        @Override
        public void deadDrone(DroneRPC.DeadDroneRequest request, StreamObserver<DroneRPC.DeadDroneResponse> responseObserver) {
          DroneRPC.DeadDroneResponse response = DroneRPC.DeadDroneResponse.newBuilder()
                  .setId(drone.getId())
                  .setMasterId(drone.getMasterId()).build();

          responseObserver.onNext(response);

          List<DroneInfo> listToUpdate = drone.getDronesCopy();
          listToUpdate.removeIf(d->d.getId()==request.getId());
          drone.setDrones(listToUpdate);

          responseObserver.onCompleted();
        }

        @Override
        public void ping(DroneRPC.PingRequest request, StreamObserver<DroneRPC.PingResponse> responseObserver) {
          DroneRPC.PingResponse response = DroneRPC.PingResponse.newBuilder().build();
          responseObserver.onNext(response);
          responseObserver.onCompleted();
        }

        @Override
        public void elected(DroneRPC.Elected request, StreamObserver<DroneRPC.EmptyResponse> responseObserver) {
          System.out.println("ELECTED");
          //Respond and extract message
          DroneRPC.EmptyResponse response = DroneRPC.EmptyResponse.newBuilder().build();
          responseObserver.onNext(response);
          //END OF RING => I'm the master
          if(request.getId()==drone.getId()){
            drone.setMasterId(drone.getId());
            drone.justBecomeMaster();
          }else{
            //Middle of ring
            drone.setMasterId(request.getId());
            drone.setInElection(false);
            forwardElected(request);
          }


          responseObserver.onCompleted();
        }

        @Override
        public void election(DroneRPC.Election request, StreamObserver<DroneRPC.EmptyResponse> responseObserver) {
          //If an election message arrives => Master is dead
          for (DroneInfo di : drone.getDronesCopy()){
            if (di.getId()==drone.getMasterId()){
              drone.removeDroneFromList(di);//remove master from list
            }
          }
          System.out.println("ELECTION message received");
          //Respond and extract message
          DroneRPC.EmptyResponse response = DroneRPC.EmptyResponse.newBuilder().build();
          responseObserver.onNext(response);

          if(request.getBattery()>drone.getBatteryCharge()||(request.getBattery()==drone.getBatteryCharge() && request.getId()> drone.getId())){
            drone.setInElection(true);
            sendElection(request);//FORWARD
          }else if(request.getBattery()<drone.getBatteryCharge()&&!drone.isInElection()){
            drone.setInElection(true);
            DroneRPC.Election newRequest = DroneRPC.Election.newBuilder().setId(drone.getId()).setBattery(drone.getBatteryCharge()).build();
            sendElection(newRequest);
          }else if(request.getBattery()<drone.getBatteryCharge()&&drone.isInElection()){
            //stopElection
            System.out.println("Election stopped");
          }else if(request.getBattery()==drone.getBatteryCharge() && request.getId()==drone.getId()){
            //I'm the master
            drone.setInElection(false);

            sendElected();
          }


          responseObserver.onCompleted();
        }

        private void sendElected() {
          DroneInfo successor = findAliveSuccessor(drone.toDroneInfo());
          final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+successor.getPort()).usePlaintext().build();
          DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);
          DroneRPC.Elected request = DroneRPC.Elected.newBuilder().setId(drone.getId()).build();
          DroneRPC.EmptyResponse response=null;
          try{
            response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).elected(request);//receive an answer, could fail-> timeout
            //response is empty
          }catch (Exception e){
            //DEAD DRONE delete from my list
            drone.removeDroneFromList(successor);
            sendElected();//if it fails I have to repeat this method, this time it will be called on the new successor
          }
          channel.shutdown();
        }
        private void forwardElected(DroneRPC.Elected request){
          Context.current().fork();
          DroneInfo successor= findAliveSuccessor(drone.toDroneInfo());
          final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+successor.getPort()).usePlaintext().build();
          DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);
          DroneRPC.EmptyResponse response=null;
          try{
            response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).elected(request);//receive an answer, could fail-> timeout
            //response is empty
          }catch (Exception e){
            //DEAD DRONE delete from my list
            drone.removeDroneFromList(successor);
          }
          channel.shutdown();
        }

        private void sendElection(DroneRPC.Election request) {
          Context.current().fork();
          DroneInfo successor= findAliveSuccessor(drone.toDroneInfo());
          final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+successor.getPort()).usePlaintext().build();
          DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);
          DroneRPC.EmptyResponse response=null;
          try{
            response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).election(request);//receive an answer, could fail-> timeout
            //response is empty
          }catch (Exception e){
            //DEAD DRONE delete from my list
            drone.removeDroneFromList(successor);
          }
          channel.shutdown();
        }


      }).build();

      server.start();

      System.out.println("Drone reception started! "+ drone);

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
      drone.updatePosAndBattery(bestDrone);

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
