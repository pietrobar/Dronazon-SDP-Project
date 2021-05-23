package dronesnetwork;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import restserver.beans.DroneInfo;
import java.util.List;

/**
 * Created by Pietro on 12/05/2021
 */

/*  This class is meant to offer static methods for communication to drones.
* */
public class DroneRESTCommunication{


  //  public static void sendStatistics(){
//    try {
//
//      Client client = Client.create();
//
//      WebResource webResource = client
//              .resource(  "/add-stat");
//
//      ClientResponse response = webResource.type("application/json")
//              .post(ClientResponse.class, "{\"timestamp\":\"g\",\"meanDelivery\":7,\"meanKilometers\":10,\"meanPollution\":\"g\",\"meanBattery\":90}");
//
//
//    } catch (Exception e) {
//
//      e.printStackTrace();
//
//    }
//  }



  /*
  * register a drone to the administrator server
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
      List<DroneInfo> res = response.getEntity(new GenericType<List<DroneInfo>>(){});
      for (DroneInfo di : res){
        if(di.getId()==drone.getId()) drone.setPosition(di.getPosition());
      }
      drone.setDrones(res);

    }else{
      return false;
    }
    System.out.println("Registered to the server administrator");
    return true;
  }



}
