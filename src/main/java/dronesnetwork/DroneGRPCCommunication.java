package dronesnetwork;

import com.example.grpc.DroneRPC;
import com.example.grpc.DroneServiceGrpc;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import restserver.beans.DroneInfo;

import java.io.IOException;
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
    insertNetwork(drone);//after I'm in the network i'll start receiving messages
    reception(drone);
  }


  private void insertNetwork(Drone drone) {
    //if i'm the only one => i'm the Master
    List<DroneInfo> drones = drone.getDronesCopy();
    if (drones.size()==1){
      drone.setMasterId(drone.getId());
    }else{
      //otherwise broadcast to every other node
      DroneInfo successor = successor(drones,drone.getId());
      for (DroneInfo node : drones){
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
                    .setXCoord(drone.getPosition().getX())
                    .setYCoord(drone.getPosition().getY()).build();

            DroneRPC.AddDroneResponse response = stub.addDrone(request);//receive an answer
            //todo: aggiungere timeout .withDeadlineAfter(1, TimeUnit.SECONDS).
            if (Context.current().isCancelled()) {
              System.out.println("Drone didn't answer");
              //I assume Drone is dead => Tell other drones one is dead
            }
            if (response.getMasterId()!=-1)//all drones except master sends -1
              drone.setMasterId(response.getMasterId());//no need to sync, just one response will contain the master ID


            channel.shutdown();
          }
        };
        t.start();
      }
      //todo: iniziare una elezione se non c'e' un master attivo, dopo aver aspettato il timeout!!

    }
  }
  public static DroneInfo successor(List<DroneInfo> drones, int id) {
    for (DroneInfo d : drones){
      if(d.getId()>id){
        return d;
      }
    }
    //if no one has highest id my successor is the first Drone in the ordered list, and i'm the last one
    return drones.get(0);
  }

  public static void reception(Drone d){
    System.err.println(d);
    try {

      Server server = ServerBuilder.forPort(d.getPort()).addService(new DroneServiceGrpc.DroneServiceImplBase() {
        /*
        * In addDrone() I have to respond directly to the sender*/
        @Override
        public void addDrone(DroneRPC.AddDroneRequest request, StreamObserver<DroneRPC.AddDroneResponse> responseObserver) {

          int eventualMasterId = d.getMasterId()==d.getId() ? d.getId() : -1;
          DroneRPC.AddDroneResponse response = DroneRPC.AddDroneResponse.newBuilder().setMasterId(eventualMasterId).build();

          responseObserver.onNext(response);

          //I have to add the drone into my list
          d.addDroneInfo(new DroneInfo(request.getId(),request.getIp(),request.getPort()));
          System.out.println("from reception: "+d);

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
}
