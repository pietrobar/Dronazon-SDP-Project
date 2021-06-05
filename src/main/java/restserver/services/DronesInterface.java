package restserver.services;

/*
 * Created by Pietro on 07/05/2021
 */

import com.google.gson.Gson;
import restserver.beans.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

/*
* This is the facade of the server that interacts with drones and receives stats*/
@Path("drone_interface")
public class DronesInterface {

  @POST
  @Path("/add-drone")
  @Consumes({"application/json", "application/xml"})
  @Produces({"application/json", "application/xml"})
  public Response addDrone(DroneInfo drone) {
    List<DroneInfo> res = DronesInfoManager.getInstance().add(drone);
    return res!=null ? Response.ok(res).build() : Response.notModified().build();
  }

  @DELETE
  @Path("/remove-drone")
  @Consumes({"application/json", "application/xml"})
  public Response removeDrone(DroneInfo droneInfo) {
    DronesInfoManager.getInstance().remove(droneInfo);
    return Response.ok().build();
  }

  @POST
  @Path("/add-stat")
  @Consumes({"application/json", "application/xml"})
  public Response addStatistic(Statistic statistic) {
    StatsManager.getInstance().add(statistic);
    return Response.ok().build();
  }


}
