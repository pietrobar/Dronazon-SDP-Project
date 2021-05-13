package dronesnetwork;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import dronazon.Coordinate;
import javafx.util.Pair;
import restserver.beans.DroneInfo;
import restserver.beans.Statistic;

import java.util.List;

/**
 * Created by Pietro on 12/05/2021
 */

/*  This class is meant to offer static methods for communication to drones.
* */
public class DroneRESTCommunication {
  private static final String serverRest="http://localhost:1337/drone_interface";


  public static void sendStatistics(){
    try {

      Client client = Client.create();

      WebResource webResource = client
              .resource(serverRest + "/add-stat");

      ClientResponse response = webResource.type("application/json")
              .post(ClientResponse.class, "{\"timestamp\":\"g\",\"meanDelivery\":7,\"meanKilometers\":10,\"meanPollution\":\"g\",\"meanBattery\":90}");


    } catch (Exception e) {

      e.printStackTrace();

    }
  }

  public static boolean registerDrone(Drone drone) {


    Client client = Client.create();

    WebResource webResource = client
              .resource(drone.getAdministratorServerAddress() + "/add-drone");

    Gson gson = new Gson();
    String droneInfoJson = gson.toJson(new DroneInfo(drone.getId(),drone.getPort()));
    ClientResponse response = webResource.type("application/json")
              .post(ClientResponse.class, droneInfoJson);


    if(response.getStatus()!=301){//   != NotModified
      Pair<Coordinate,List<DroneInfo>> pair = response.getEntity(Pair.class);
      drone.setPosition((Coordinate) pair.getKey());
      drone.setDrones((List<DroneInfo>) pair.getValue());
    }else {
      return false;
    }
    return true;
  }



}
