package restserver.services;

/*
 * Created by Pietro on 07/05/2021
 */

import restserver.beans.DroneInfo;
import restserver.beans.DronesInfoManager;
import restserver.beans.Statistic;
import restserver.beans.StatsManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/*
* This is the facade of the server that interacts with drones and receives stats*/
@Path("drone_interface")
public class ServerAdministrator {

  @POST
  @Path("/add-drone")
  @Consumes({"application/json", "application/xml"})
  public Response addDrone(DroneInfo drone) {
    boolean outcome = DronesInfoManager.getInstance().add(drone);
    return outcome ? Response.ok().build() : Response.notModified().build();//code: 304
  }

  @POST
  @Path("/add-stat")
  @Consumes({"application/json", "application/xml"})
  public Response addStatistic(Statistic statistic) {
    StatsManager.getInstance().add(statistic);
    return Response.ok().build();
  }
}
