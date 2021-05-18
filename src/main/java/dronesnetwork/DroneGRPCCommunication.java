package dronesnetwork;

import com.example.grpc.DroneRPC;
import com.example.grpc.DroneServiceGrpc;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import restserver.beans.DroneInfo;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Pietro on 13/05/2021
 */
public class DroneGRPCCommunication {

  public static void reception(Drone d){
    try {

      Server server = ServerBuilder.forPort(d.getPort()).addService(new DroneServiceGrpc.DroneServiceImplBase() {
        @Override
        public void addDrone(DroneRPC.AddDroneRequest request, StreamObserver<DroneRPC.AddDroneResponse> responseObserver) {
          //here i have to let the message go through the ring

          DroneInfo successor = d.successor();
          //before forwarding the message a drone has to save the new drone position

          final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:"+successor.getPort()).usePlaintext().build();

          DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);

          DroneRPC.AddDroneRequest forward = DroneRPC.AddDroneRequest.newBuilder().setId(request.getId()).setXCoord(request.getXCoord()).setYCoord(request.getYCoord()).build();

          channel.shutdown();


          //completo e finisco la comunicazione
          responseObserver.onCompleted();//potrebbe non funzionare
        }
      }).build();

      server.start();

      System.out.println("Server started!");

      server.awaitTermination();

    } catch (IOException e) {

      e.printStackTrace();

    } catch (InterruptedException e) {

      e.printStackTrace();

    }
  }
}
