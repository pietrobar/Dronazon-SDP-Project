package restserver.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 07/05/2021
 */


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StatsManager {
  @XmlElement
  private List<Statistic> stats;

  private static StatsManager instance;

  private StatsManager(){
    stats = new ArrayList<>();
  }

  public synchronized static StatsManager getInstance(){
    if(instance==null)
      instance = new StatsManager();
    return instance;
  }

  public synchronized List<Statistic> getStats() {
    return new ArrayList<>(stats);
  }

  public synchronized void add(Statistic s){
    stats.add(s);
  }

  public void setStats(List<Statistic> stats) {
    this.stats = stats;
  }

  public Object getNStats(int n) {
    return getStats().subList(stats.size()-n, stats.size()-1);
  }

  public int getDeliveriesBetweenTimestamps(String t1, String t2) {

    List<Integer> meanDeliveries = new ArrayList<>();
    for(Statistic s : getStats()){
      System.out.println(s);
      LocalDateTime ts = Statistic.extractDateTime(s.getTimestamp());
      LocalDateTime ts1 = Statistic.extractDateTime(t1);
      LocalDateTime ts2 = Statistic.extractDateTime(t2);
      System.out.println("timestamp statistic: "+ ts);
      System.out.println("t1: "+ ts1);
      System.out.println("t2: "+ ts2);
      if((ts.isBefore(ts2)||ts.isEqual(ts2)) && (ts.isAfter(ts1)||ts.isEqual(ts1))){
        meanDeliveries.add(s.getMeanDelivery());
      }
    }
    int div=meanDeliveries.size();
    return meanDeliveries.stream().mapToInt(Integer::intValue).sum()/div;
  }
//  2019-01-01 13:00,   2019-01-01 13:50
//  Statistic{timestamp='2019-01-01 13:30', meanDelivery=60, meanKilometers=10, meanPollution=8.0, meanBattery=90}
//  timestamp statistic: 2019-01-01T13:30
//  t1: 2019-01-01T13:00
//  t2: 2019-01-01T13:50
//  Statistic{timestamp='2019-01-01 13:00', meanDelivery=20, meanKilometers=10, meanPollution=8.0, meanBattery=90}
//  timestamp statistic: 2019-01-01T13:00
//  t1: 2019-01-01T13:00
//  t2: 2019-01-01T13:50
}
