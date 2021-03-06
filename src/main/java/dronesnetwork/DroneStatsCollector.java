package dronesnetwork;

import restserver.beans.Statistic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by Pietro on 04/06/2021
 */
public class DroneStatsCollector {
  private final Map<Integer,Float> deliveries;//map ID -> number of deliveries
  private final List<Float> kilometers;
  private final List<Float> pollution;
  private final Map<Integer,Float> battery;

  private final Drone drone;

  public DroneStatsCollector(Drone drone) {
    this.deliveries = new Hashtable<>();
    this.kilometers = new ArrayList<>();
    this.pollution = new ArrayList<>();
    this.battery = new Hashtable<>();

    this.drone = drone;


  }
  public void generateAndSendStatistic(){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    //DELIVERIES
    List<Float> ds = new ArrayList<>(deliveries.values());
    float meanDeliveries = mean(ds);

    //MEAN KILOMETERS
    float meanKm = mean(kilometers);

    //POLLUTION MEAN
    float meanPollution = mean(pollution);

    //MEAN BATTERY
    List<Float> bs = new ArrayList<>(battery.values());
    float meanBattery = mean(bs);

    if(meanKm!=0 && meanPollution!=0 && meanBattery!=0 &&meanDeliveries!=0){
      Statistic statistic = new Statistic(LocalDateTime.now().format(formatter), meanDeliveries, meanKm ,meanPollution,meanBattery);
      resetAllStats();
      DroneRESTCommunication.sendStatistic(drone,statistic);
    }
  }

  private void resetAllStats() {
    this.deliveries.clear();
    this.kilometers.clear();
    this.pollution.clear();
    this.battery.clear();
  }

  private float mean(List<Float> values){
    int size=0;
    synchronized (this){
      size=values.size();
      if (values.size()==0) return 0;
    }
    float partial=0;
    for (float n : values){
      partial+=n;
    }
    return partial/size;
  }


  public synchronized void addKilometerMean(float km) {
    kilometers.add(km);
  }

  public synchronized void addPollutionValues(List<Float> pollutionValuesList) {
    pollution.addAll(pollutionValuesList);
  }

  public synchronized void addBattery(int id, float b) {//I want to save last battery
    battery.put(id,b);
  }


  public synchronized void addDelivery(int id) {
    if (deliveries.containsKey(id)){
      deliveries.put(id,deliveries.get(id)+1f);
    }else{
      deliveries.put(id,1f);
    }
  }
}
