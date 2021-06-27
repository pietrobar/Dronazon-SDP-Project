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
  private Server grpcServer;


  public DroneGRPCCommunication(Drone drone){
    this.drone=drone;
  }
  /*
   * This thread will manage the communication in the drones network
   * */
  @Override
  public void run() {
    reception();
    try {
      insertNetwork();//after I'm in the network i'll start receiving messages
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
      drone.justBecomeMaster();
    }else{
      //otherwise broadcast to every other node
      List<Thread> threads = new ArrayList<>();
      List<DroneInfo> deadDrones = new ArrayList<>();
      for (DroneInfo node : drones){
        if(node.getId()!=drone.getId()){
          Thread t = new Thread(() -> {
            final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+node.getPort()).usePlaintext().build();
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
              if(drone.getMasterId()==-1)
                drone.setMasterId(response.getMasterId());
            }catch (Exception e){
              deadDrones.add(node);
              drone.removeDroneFromList(node);//Remove dead drone
            }finally {
              channel.shutdown();
            }
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


      for(DroneInfo dead : deadDrones){
          //DEAD DRONE delete from my list
          drone.removeDroneFromList(dead);
      }
      if(drone.getDronesCopy().stream().noneMatch(d-> d.getId()==drone.getMasterId())){
        new Thread(this::startElection).start();
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
        Context.current().fork().run(()->{
          final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+successor.getPort()).usePlaintext().build();
          DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
          DroneRPC.Election request = DroneRPC.Election.newBuilder().setId(drone.getId()).setBattery(drone.getBatteryCharge()).build();
          stub.withDeadlineAfter(5, TimeUnit.SECONDS).election(request, new StreamObserver<DroneRPC.EmptyResponse>() {
            @Override
            public void onNext(DroneRPC.EmptyResponse emptyResponse) {}

            @Override
            public void onError(Throwable throwable) {
              //DEAD DRONE delete from my list
              drone.removeDroneFromList(successor);
              drone.setInElection(false);
              startElection();//restart the election because this one failed!
            }

            @Override
            public void onCompleted() {
              channel.shutdownNow();
            }
          });
        });
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
    System.out.println("PING "+di);
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
    }finally {
      channel.shutdown();
    }
    return ret;
  }



  /*
  * SERVER: Start a GRPC server to be able to receive messages from other drones*/
  public void reception(){
    try {

      this.grpcServer = ServerBuilder.forPort(drone.getPort()).addService(new DroneServiceGrpc.DroneServiceImplBase() {
        /*
        * In addDrone() I have to respond directly to the sender, so that he knows if i'm alive*/
        @Override
        public void addDrone(DroneRPC.AddDroneRequest request, StreamObserver<DroneRPC.AddDroneResponse> responseObserver) {

          DroneRPC.AddDroneResponse response = DroneRPC.AddDroneResponse.newBuilder()
                  .setId(drone.getId())
                  .setMasterId(drone.getMasterId()).build();

          responseObserver.onNext(response);
          responseObserver.onCompleted();

          //I have to add the drone into my list
          DroneInfo di = new DroneInfo(request.getId(),request.getIp(),request.getPort());
          di.setBattery(100);//needed by master if a drone is new to assign a delivery
          di.setPosition(new Coordinate(request.getPosition().getXCoord(),request.getPosition().getYCoord()));
          drone.addDroneInfo(di);
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
          float d1 = distance(drone.getPosition(),c1);
          Coordinate c2 = new Coordinate(request.getDeliveryPoint().getXCoord(),request.getDeliveryPoint().getYCoord());
          float d2 = distance(c1,c2);

          drone.setPosition(c2);
          drone.setBatteryCharge(drone.getBatteryCharge()-10);//no election during delivery
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

          //take pollution values
          List<Float>  list = drone.getDronePollutionSensor().getMeanList();
          drone.getDronePollutionSensor().clearMeanList();
          //Send Statistics to master
          DroneRPC.OrderResponse response = DroneRPC.OrderResponse.newBuilder()
                  .setTimestamp(LocalDateTime.now().format(formatter))
                  .setCurrentPos(request.getDeliveryPoint())
                  .setKilometers(d1+d2)
                  .addAllPollutionValues(list)
                  .setBattery(drone.getBatteryCharge())
                  .build();

          System.out.println("CONSEGNA EFFETTUATA " + request.getOrderId());
          responseObserver.onNext(response);
          responseObserver.onCompleted();

          //Save kilometers and deliveries number
          drone.addKilometers(d1+d2);
          drone.addDelivery();


          drone.setDelivering(false);
          if(drone.getBatteryCharge()<15 || drone.isQuitting()){
            drone.setQuit(true);//in case is < 15
            synchronized (drone.terminationObj){
              drone.terminationObj.notify();
            }
          }
        }



        @Override
        public void ping(DroneRPC.PingRequest request, StreamObserver<DroneRPC.PingResponse> responseObserver) {
          DroneRPC.PingResponse response = DroneRPC.PingResponse.newBuilder().build();
          responseObserver.onNext(response);
          responseObserver.onCompleted();
        }

        @Override
        public void elected(DroneRPC.Elected request, StreamObserver<DroneRPC.EmptyResponse> responseObserver) {
          System.out.println("ELECTED message: <ID: "+request.getId()+">");
          //Respond and extract message
          DroneRPC.EmptyResponse response = DroneRPC.EmptyResponse.newBuilder().build();
          responseObserver.onNext(response);
          responseObserver.onCompleted();

          //Master is dead; I kept it till now because I needed to ping it to start other election if new master dies here
          for (DroneInfo di : drone.getDronesCopy()){
            if (di.getId()==drone.getMasterId() && drone.getMasterId()!=request.getId()){//Don't remove new Master!!
              drone.removeDroneFromList(di);//remove master from list
            }
          }
          //END OF RING => I'm the master
          if(request.getId()==drone.getId()){
            System.out.println("I'm the NEW MASTER");
            drone.setMasterId(drone.getId());
            //update list with all new positions and batteries
            List<DroneInfo> newList = new ArrayList<>();
            for (DroneRPC.AddDroneRequest dp : request.getUpdatePositionList()){
              DroneInfo updated = new DroneInfo(dp.getId(),dp.getIp(),dp.getPort());
              updated.setBattery(dp.getBattery());
              updated.setPosition(new Coordinate(dp.getPosition().getXCoord(),dp.getPosition().getYCoord()));
              newList.add(updated);
            }
            drone.setDrones(newList);
            drone.addDroneInfo(drone.toDroneInfo());//add myself into list
            drone.justBecomeMaster();
          }else{
            //Middle of ring
            drone.setMasterId(request.getId());
            drone.setInElection(false);
            //send my new position
            DroneRPC.AddDroneRequest update = DroneRPC.AddDroneRequest.newBuilder()
                    .setId(drone.getId())
                    .setIp(drone.getIp())
                    .setPort(drone.getPort())
                    .setBattery(drone.getBatteryCharge())
                    .setPosition(DroneRPC.Coordinate.newBuilder().setXCoord(drone.getPosition().getX()).setYCoord(drone.getPosition().getY()))
                    .build();
            DroneRPC.Elected newRequest = request.toBuilder().addUpdatePosition(update).build();
            forwardElected(newRequest);
          }



        }

        @Override
        public void election(DroneRPC.Election request, StreamObserver<DroneRPC.EmptyResponse> responseObserver) {
          System.out.println("ELECTION message received: <ID: "+request.getId()+", Battery: "+request.getBattery()+">");
          //Respond and extract message
          DroneRPC.EmptyResponse response = DroneRPC.EmptyResponse.newBuilder().build();
          responseObserver.onNext(response);
          responseObserver.onCompleted();


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

            DroneRPC.Elected requestWithIdOfNewMaster = DroneRPC.Elected.newBuilder().setId(drone.getId()).build();
            forwardElected(requestWithIdOfNewMaster);
          }


        }

        private void forwardElected(DroneRPC.Elected request){
          Context.current().fork().run(()-> {//if father terminates closes everything
            DroneInfo successor = findAliveSuccessor(drone.toDroneInfo());
            final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:" + successor.getPort()).usePlaintext().build();
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.withDeadlineAfter(5, TimeUnit.SECONDS).elected(request, new StreamObserver<DroneRPC.EmptyResponse>() {
              @Override
              public void onNext(DroneRPC.EmptyResponse emptyResponse) {

              }

              @Override
              public void onError(Throwable throwable) {
                //DEAD DRONE delete from my list
                drone.removeDroneFromList(successor);
                channel.shutdownNow();
                //if it fails I'll send the message to the next one if is not the new master
                if (successor.getId() != request.getId())
                  forwardElected(request);
                else//if the new master is dead if I don't start an election the message could go infinitely
                  startElection();

              }

              @Override
              public void onCompleted() {
                if (!channel.isShutdown()) {
                  channel.shutdownNow();
                }
              }
            });
          });
        }

        private void sendElection(DroneRPC.Election request) {
          Context.current().fork().run(()->{
            DroneInfo successor= findAliveSuccessor(drone.toDroneInfo());
            final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+successor.getPort()).usePlaintext().build();
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            DroneRPC.EmptyResponse response=null;
            stub.withDeadlineAfter(5, TimeUnit.SECONDS).election(request, new StreamObserver<DroneRPC.EmptyResponse>() {
              @Override
              public void onNext(DroneRPC.EmptyResponse emptyResponse) {

              }

              @Override
              public void onError(Throwable throwable) {
                //DEAD DRONE delete from my list
                drone.removeDroneFromList(successor);
                channel.shutdown();
                //if it fails I'll send the message to the next one IF is not the new master
                if(successor.getId()!=request.getId())
                  sendElection(request);
                else//if the new master is dead if I don't start an election the message could go infinitely
                  startElection();

              }

              @Override
              public void onCompleted() {
                if(!channel.isShutdown()){
                  channel.shutdownNow();
                }

              }
            });
          });
        }


      }).build();

      grpcServer.start();

      System.out.println("Drone reception started! "+ drone);

      //grpcServer.awaitTermination();

    } catch (IOException e) {

      e.printStackTrace();

    }
  }


  public void tryAssignOrder(Order order) {
    DroneOrderManager dom = drone.getDroneOrderManager();
    DroneInfo bestDrone = findBestDrone(order);
    if (bestDrone!=null){
      dom.addOccupiedDrone(bestDrone);

      //call GRPC to that drone
      final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:" + bestDrone.getPort()).usePlaintext().build();
      DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);

      DroneRPC.OrderRequest request = DroneRPC.OrderRequest.newBuilder()
              .setOrderId(order.getId())
              .setPickUpPoint(DroneRPC.Coordinate.newBuilder().setXCoord(order.getPickUpPoint().getX()).setYCoord(order.getPickUpPoint().getY()))
              .setDeliveryPoint(DroneRPC.Coordinate.newBuilder().setXCoord(order.getDeliveryPoint().getX()).setYCoord(order.getDeliveryPoint().getY()))
              .build();

      stub.delivery(request, new StreamObserver<DroneRPC.OrderResponse>() {
        @Override
        public void onNext(DroneRPC.OrderResponse response) {
          //COLLECT STATS
          DroneStatsCollector dsc = drone.getDroneStatsCollector();
          dsc.addDelivery(bestDrone.getId());
          dsc.addKilometerMean(response.getKilometers());
          dsc.addBattery(response.getBattery());
          dsc.addPollutionValues(response.getPollutionValuesList());//all sync in dsc


          //DRONE LIST UPDATE
          bestDrone.setPosition(new Coordinate(response.getCurrentPos().getXCoord(), response.getCurrentPos().getYCoord()));
          bestDrone.setBattery(response.getBattery());
          drone.updatePosAndBattery(bestDrone);


          dom.removeOccupiedDrone(bestDrone);
        }

        @Override
        public void onError(Throwable t) {
          channel.shutdownNow();
          //N.B. Per semplicita', si assume che un drone porta sempre a termine con successo la consegna a lui assegnata => This call cannot fail in this scenario
        }

        @Override
        public void onCompleted() {
          channel.shutdownNow();
          synchronized (dom.orders){
            dom.orders.notify();
          }
        }
      });

    }else{
      dom.addOrder(order);//if no drone is available i will add this order back in the list
    }



  }


  private DroneInfo findBestDrone(Order order) {
    //sorting criteria: free -> distance -> battery -> ID
    List<DroneInfo> drones = drone.getDronesCopy();
    drones.sort((d1, d2) -> {
//        0: if (x==y)
//       -1: if (x < y)
//        1: if (x > y)
      if(distance(d1.getPosition(),order.getPickUpPoint())>distance(d2.getPosition(),order.getPickUpPoint())) return  1;
      else if(distance(d1.getPosition(),order.getPickUpPoint())<distance(d2.getPosition(),order.getPickUpPoint())) return -1;
      else if(d1.getBattery() > d2.getBattery()) return 1;//distance is equal so compare by battery
      else if(d1.getBattery() < d2.getBattery()) return -1;
      else if(d1.getId()>d2.getId()) return 1;//distance and battery are equals => compare by id
      else if(d1.getId()<d2.getId()) return -1;
      return 0;
    });
    //from the sorted list I want a free drone
    for (int i = drones.size()-1; i>=0; i--){
      if(!drone.getDroneOrderManager().getOccupiedDrones().contains(drones.get(i))){
        if(drones.get(i).getBattery()>15)
          return drones.get(i);
      }
    }
    return null;
  }

  private float distance(Coordinate from,Coordinate to){
    return (float) Math.sqrt(Math.pow(to.getX()-from.getX(),2)+Math.pow(to.getY()-from.getY(),2));
  }

  public void serverShutdown(){
    this.grpcServer.shutdown();
  }
}
