package restserver.services;

/*
 * Created by Pietro on 07/05/2021
 */
import restserver.beans.DroneInfo;
import restserver.beans.DronesInfoManager;
import restserver.beans.StatsManager;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/*
* This interface serves the purpose to allow a ClientAdministrator to interact with the server
*
*
* This interface will manage two resources:
* - statistics about drones (managed in StatsManager)
* - drones list (implemented as a list of DroneInfo)
* */
@Path("dronazon_service")
public class AdminInterface {
  //Get drones list
  @Path("get-drones")
  @GET
  @Produces({"application/json", "application/xml"})
  public Response getDronesList(){
    return Response.ok(DronesInfoManager.getInstance().getDrones()).build();
  }

  //add drone into list//todo: da rimuovere, mi serve solo per il debugging con ARC
  @Path("add-drone")
  @POST
  @Consumes({"application/json", "application/xml"})
  public Response addDrone(DroneInfo drone){
    boolean res = DronesInfoManager.getInstance().add(drone);

    return res? Response.ok().build() : Response.notModified("Wrong ID").build();

  }
//   Json Example:
//    {
//      "id": 1,
//      "ip": "localhost",
//      "port": 1234
//    }


    //Get Statistics
  @Path("get-stats")
  @GET
  @Produces({"application/json", "application/xml"})
  public Response getStats(){
    return Response.ok(StatsManager.getInstance().getStats()).build();
  }
//{"timestamp":"ciao","meanDelivery":7,"meanKilometers":10,"meanPollution":"AAAAAAAAAA","meanBattery":90}



}