package dronesnetwork;

import restserver.beans.Statistic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pietro on 04/06/2021
 */
public class DroneStatsCollector {
  private final Map<Integer,Integer> deliveries;//map ID -> number of deliveries
  private final List<Float> kilometers;
  private final List<Float> pollution;
  private final List<Float> battery;

  public DroneStatsCollector() {
    this.deliveries = new Hashtable<>();
    this.kilometers = new ArrayList<>();
    this.pollution = new ArrayList<>();
    this.battery = new ArrayList<>();

    //starts a new thread to send statistics to server administrator
    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    exec.scheduleAtFixedRate(() -> {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


      //DELIVERIES
      List<Integer> ds = new ArrayList<>(deliveries.values());
      float meanDeliveries;
      float partial=0;
      for (int n : ds){
        partial+=n;
      }
      meanDeliveries = partial/ds.size();

      //MEAN KILOMETERS
      float meanKm = mean(kilometers);

      //POLLUTION MEAN
      float meanPollution = mean(pollution);

      //MEAN BATTERY
      float meanBattery = mean(battery);

      Statistic statistic = new Statistic(LocalDateTime.now().format(formatter), meanDeliveries, meanKm ,meanPollution,meanBattery);
      System.out.println(statistic);
    }, 0, 10, TimeUnit.SECONDS);
  }

  private float mean(List<Float> values){
    float partial=0;
    for (float n : values){
      partial+=n;
    }
    return partial/values.size();
  }


  public synchronized void addKilometerMean(float km) {
    kilometers.add(km);
  }

  public synchronized void addPollutionValues(List<Float> pollutionValuesList) {
    pollution.addAll(pollutionValuesList);
  }

  public synchronized void addBattery(float b) {
    battery.add(b);
  }


  public synchronized void addDelivery(int id) {
    if (deliveries.containsKey(id)){
      deliveries.put(id,deliveries.get(id)+1);
    }
    deliveries.put(id,1);
  }
}
