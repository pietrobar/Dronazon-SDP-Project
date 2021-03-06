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
  private static final String dateRegex="^\\d{4}\\-(0?[1-9]|1[012])\\-(0?[1-9]|[12][0-9]|3[01]) ([0-1]?[0-9]|2[0-3]):[0-5][0-9]$";


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
                "Get statistics -> 2\n" +
                "Get deliveries number -> 3\n" +
                "Get mean km -> 4");
        String action = scanner.nextLine();
        switch (action){
          case "1": getDrones(client); break;
          case "2": getStats(client); break;
          case "3": getDeliveriesBetweenTimestamps(client); break;
          case "4": getKMBetweenTimestamps(client); break;
          default:
            System.err.println("ERR: "+action+" is not a valid input");
        }


      }


    } catch (Exception e) {

      e.printStackTrace();

    }

  }

  private static void getKMBetweenTimestamps(Client client) {
    Scanner scanner = new Scanner(System.in);
    String t1;
    String t2;
    do {
      System.out.println("Insert time interval (yyyy-MM-dd HH:mm):");
      t1=scanner.nextLine();
      t2=scanner.nextLine();
    }
    while (!t1.matches(dateRegex)||!t2.matches(dateRegex));
    System.out.println(t1 +", "+t2);
    WebResource webResource = client
            .resource(url+"get-kilometers/"+processed(t1)+"/"+processed(t2));
    ClientResponse response = webResource.accept("application/json")
            .get(ClientResponse.class);

    if (response.getStatus() != 200) {
      throw new RuntimeException("Failed : HTTP error code : "
              + response.getStatus());
    }

    String output = response.getEntity(String.class);
    System.out.println("-------------------MEAN KILOMETERS-------------------");
    System.out.println(output);
  }

  private static void getDeliveriesBetweenTimestamps(Client client) {
    Scanner scanner = new Scanner(System.in);
    String t1;
    String t2;
    do {
      System.out.println("Insert time interval (yyyy-MM-dd HH:mm):");
      t1=scanner.nextLine();
      t2=scanner.nextLine();
    }
    while (!t1.matches(dateRegex)||!t2.matches(dateRegex));

    WebResource webResource = client
            .resource(url+"get-deliveries/"+processed(t1)+"/"+processed(t2));
    ClientResponse response = webResource.accept("application/json")
            .get(ClientResponse.class);

    if (response.getStatus() != 200) {
      throw new RuntimeException("Failed : HTTP error code : "
              + response.getStatus());
    }

    String output = response.getEntity(String.class);
    System.out.println("-------------------DELIVERIES NUMBER-------------------");
    System.out.println(output);
  }

  private static String processed(String t1) {
    String[] raw = t1.split(" ");
    return raw[0]+"x"+raw[1];
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

  private static void getStats(Client client) {
    Scanner scanner = new Scanner(System.in);

    String input;
    do {
      System.out.println("How many statistics would you like to see? (int)");
      input=scanner.nextLine();
    }
    while (!input.matches("\\d+"));


    WebResource webResource = client
            .resource(url+"get-stats/"+Integer.parseInt(input));
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
