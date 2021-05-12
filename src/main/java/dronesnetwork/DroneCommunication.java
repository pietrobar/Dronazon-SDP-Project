package dronesnetwork;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import restserver.beans.Statistic;

/**
 * Created by Pietro on 12/05/2021
 */

/*  This class is meant to offer static methods for communication to drones.
* */
public class DroneCommunication {
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
}
