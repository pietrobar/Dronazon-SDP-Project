package restserver.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

/**
 * Created by Pietro on 12/05/2021
 */

@XmlRootElement
public class Statistic {
  private String timestamp;//Todo: deve esssere di tipo TimeStamp
  private int meanDelivery;
  private int meanKilometers;
  private String meanPollution;//TODO: il tipo dipendera' dal sensore PM10
  private int meanBattery;

  public Statistic(){}

  public Statistic(String timestamp, int meanDelivery, int meanKilometers, String meanPollution, int meanBattery) {
    this.timestamp = timestamp;
    this.meanDelivery = meanDelivery;
    this.meanKilometers = meanKilometers;
    this.meanPollution = meanPollution;
    this.meanBattery = meanBattery;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public int getMeanDelivery() {
    return meanDelivery;
  }

  public int getMeanKilometers() {
    return meanKilometers;
  }

  public String getMeanPollution() {
    return meanPollution;
  }

  public int getMeanBattery() {
    return meanBattery;
  }
}
