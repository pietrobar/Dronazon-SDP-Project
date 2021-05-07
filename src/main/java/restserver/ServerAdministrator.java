package restserver;

/*
 * Created by Pietro on 07/05/2021
 */
import restserver.beans.DroneInfo;
import restserver.beans.DronesInfoManager;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/*
* This server will manage two resources:
* - statistics about drones (managed in StatsManager)
* - drones list (impremented as a list of DroneInfo)
* */
@Path("dronazon_service")
public class ServerAdministrator {
  //Get drones list
  @Path("getDrones")
  @GET
  @Produces({"application/json", "application/xml"})
  public Response getDronesList(){
    return Response.ok(DronesInfoManager.getInstance()).build();
  }

  //add drone into list
  @Path("addDrone")
  @POST
  @Consumes({"application/json", "application/xml"})
  public Response addDrone(DroneInfo drone){
    boolean res = DronesInfoManager.getInstance().add(drone);

    return res? Response.ok().build() : Response.notModified("Wrong ID").build();

  }
  // Json Example:
  //  {
  //    "id": 1,
  //    "port": 1234
  //  }





}