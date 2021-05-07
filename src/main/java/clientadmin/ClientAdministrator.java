package clientadmin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.util.Scanner;

/**
 * Created by Pietro on 07/05/2021
 */
public class ClientAdministrator {


  public static void main(String[] args) {
    try {

      Client client = Client.create();

      WebResource webResource = client
              .resource("http://localhost:1337/dronazon_service/getDrones");

      Scanner scanner = new Scanner(System.in);

      while(true){
        System.out.println("Select an operation:\n" +
                "Get drone list -> 1\n" +
                "Get statistics -> 2");
        String action = scanner.nextLine();
        switch (action){
          case "1": getDrones(webResource); break;
          case "2": getStats(webResource); break;
          default:
            System.err.println("ERR: "+action+" is not a valid input");
        }


      }


    } catch (Exception e) {

      e.printStackTrace();

    }

  }




  private static void getDrones(WebResource webResource) {
    ClientResponse response = webResource.accept("application/json")
            .get(ClientResponse.class);

    if (response.getStatus() != 200) {
      throw new RuntimeException("Failed : HTTP error code : "
              + response.getStatus());
    }

    String output = response.getEntity(String.class);
    System.out.println("-------------------DRONES INFO-------------------");
    System.out.println(output);
  }

  private static void getStats(WebResource webResource) {
    //TODO: aggiungere la richiesta alle statistiche
    System.out.println("-------------------STATS INFO-------------------");
  }


}
