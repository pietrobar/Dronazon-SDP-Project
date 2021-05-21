package dronesnetwork;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import dronazon.Coordinate;
import javafx.util.Pair;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import restserver.beans.DroneInfo;
import restserver.beans.ResponseInitialization;
import restserver.beans.Statistic;

import javax.ws.rs.core.MediaType;
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

  public static void main(String[] args) {
    new Drone(3,999,"http://localhost:1337/drone_interface");
  }

  /*
  * register a drone to the admininstrator server
  * */
  public static boolean registerDrone(Drone drone) {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    clientConfig.getClasses().add(JacksonJsonProvider.class);
    Client client = Client.create(clientConfig);

    WebResource webResource = client
              .resource(drone.getAdministratorServerAddress() + "/add-drone");

    Gson gson = new Gson();
    String droneInfoJson = gson.toJson(new DroneInfo(drone.getId(),drone.getIp(),drone.getPort()));
    ClientResponse response = webResource.type("application/json")
              .post(ClientResponse.class, droneInfoJson);


    if(response.getStatus()!=301){//   != NotModified if id already present
      ResponseInitialization res = response.getEntity(ResponseInitialization.class);
      drone.setPosition(res.getStartingPosition());
      drone.setDrones(res.getDrones());
    }
    return true;
  }



}
