package restserver.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * Created by Pietro on 12/05/2021
 */

@XmlRootElement
public class Statistic implements Comparator<Statistic> {
  private String timestamp;
  private float meanDelivery;
  private float meanKilometers;
  private float meanPollution;
  private float meanBattery;

  public Statistic(){}

  public Statistic(String timestamp, float meanDelivery, float meanKilometers, float meanPollution, float meanBattery) {
    this.timestamp = timestamp;
    this.meanDelivery = meanDelivery;
    this.meanKilometers = meanKilometers;
    this.meanPollution = meanPollution;
    this.meanBattery = meanBattery;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public float getMeanDelivery() {
    return meanDelivery;
  }

  public void setMeanDelivery(float meanDelivery) {
    this.meanDelivery = meanDelivery;
  }

  public float getMeanKilometers() {
    return meanKilometers;
  }

  public void setMeanKilometers(float meanKilometers) {
    this.meanKilometers = meanKilometers;
  }

  public float getMeanPollution() {
    return meanPollution;
  }

  public void setMeanPollution(float meanPollution) {
    this.meanPollution = meanPollution;
  }

  public float getMeanBattery() {
    return meanBattery;
  }

  public void setMeanBattery(float meanBattery) {
    this.meanBattery = meanBattery;
  }

  @Override
  public String toString() {
    return "Statistic{" +
            "timestamp='" + timestamp + '\'' +
            ", meanDelivery=" + meanDelivery +
            ", meanKilometers=" + meanKilometers +
            ", meanPollution=" + meanPollution +
            ", meanBattery=" + meanBattery +
            '}';
  }

  //based on timestamp
  @Override
  public int compare(Statistic o1, Statistic o2) {
//     0: if (x==y)
//    -1: if (x < y)
//     1: if (x > y)

    LocalDateTime firstDate = extractDateTime(o1.timestamp);
    LocalDateTime secondDate = extractDateTime(o2.timestamp);
    if(firstDate.isEqual(secondDate)) return 0;
    if(firstDate.isBefore(secondDate)) return -1;
    else return 1;//firstDate.isAfter(secondDate)
  }
  public static LocalDateTime extractDateTime(String timestamp){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    return LocalDateTime.parse(timestamp, formatter);
  }
}
