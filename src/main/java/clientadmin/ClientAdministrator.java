package clientadmin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import restserver.beans.DroneInfo;
import restserver.beans.Statistic;

import java.util.List;
import java.util.Scanner;

/*
 * Created by Pietro on 07/05/2021
 */
public class ClientAdministrator {
  private static final String url="http://localhost:1337/dronazon_service/";


  public static void main(String[] args) {
    try {
      ClientConfig clientConfig = new DefaultClientConfig();
      clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
      clientConfig.getClasses().add(JacksonJsonProvider.class);
      Client client = Client.create(clientConfig);



      Scanner scanner = new Scanner(System.in);

      while(true){
        System.out.println("Select an operation:\n" +
                "Get drone list -> 1\n" +
                "Get statistics -> 2");
        String action = scanner.nextLine();
        switch (action){
          case "1": getDrones(client); break;
          case "2": getStats(client, 3); break;
          default:
            System.err.println("ERR: "+action+" is not a valid input");
        }


      }


    } catch (Exception e) {

      e.printStackTrace();

    }

  }




  private static void getDrones(Client client) {
    WebResource webResource = client
            .resource(url+"get-drones");
    ClientResponse response = webResource.accept("application/json")
            .get(ClientResponse.class);

    if (response.getStatus() != 200) {
      throw new RuntimeException("Failed : HTTP error code : "
              + response.getStatus());
    }


    List<DroneInfo> output = response.getEntity(new GenericType<List<DroneInfo>>(){});
    System.out.println("-------------------DRONES INFO-------------------");
    for (DroneInfo d : output){
      System.out.println(d);
    }
  }

  private static void getStats(Client client, int n) {
    WebResource webResource = client
            .resource(url+"get-stats/"+n);
    ClientResponse response = webResource.accept("application/json")
            .get(ClientResponse.class);

    if (response.getStatus() != 200) {
      throw new RuntimeException("Failed : HTTP error code : "
              + response.getStatus());
    }

    List<Statistic> output = response.getEntity(new GenericType<List<Statistic>>(){});
    System.out.println("-------------------STATS INFO-------------------");
    for (Statistic s : output){
      System.out.println(s);
    }
  }


}
