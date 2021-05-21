package restserver.services;

/*
 * Created by Pietro on 07/05/2021
 */

import com.google.gson.Gson;
import restserver.beans.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

/*
* This is the facade of the server that interacts with drones and receives stats*/
@Path("drone_interface")
public class DronesInterface {

  @POST
  @Path("/add-drone")
  @Consumes({"application/json", "application/xml"})
  @Produces({"application/json", "application/xml"})
  public Response addDrone(DroneInfo drone) {
    ResponseInitialization res = DronesInfoManager.getInstance().add(drone);
    Gson gson = new Gson();
    String json = gson.toJson(res);
    return res!=null ? Response.ok(json).build() : Response.notModified().build();
  }

  @POST
  @Path("/add-stat")
  @Consumes({"application/json", "application/xml"})
  public Response addStatistic(Statistic statistic) {
    StatsManager.getInstance().add(statistic);
    return Response.ok().build();
  }
}
