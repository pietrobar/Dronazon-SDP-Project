package restserver.beans;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Pietro on 12/05/2021
 */

@XmlRootElement
public class Statistic {
  private String timestamp;//Todo: deve esssere di tipo TimeStamp
  private int meanDelivery;
  private int meanKilometers;
  private double meanPollution;
  private int meanBattery;

  public Statistic(){}

  public Statistic(String timestamp, int meanDelivery, int meanKilometers, double meanPollution, int meanBattery) {
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

  public int getMeanDelivery() {
    return meanDelivery;
  }

  public void setMeanDelivery(int meanDelivery) {
    this.meanDelivery = meanDelivery;
  }

  public int getMeanKilometers() {
    return meanKilometers;
  }

  public void setMeanKilometers(int meanKilometers) {
    this.meanKilometers = meanKilometers;
  }

  public double getMeanPollution() {
    return meanPollution;
  }

  public void setMeanPollution(double meanPollution) {
    this.meanPollution = meanPollution;
  }

  public int getMeanBattery() {
    return meanBattery;
  }

  public void setMeanBattery(int meanBattery) {
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
}
