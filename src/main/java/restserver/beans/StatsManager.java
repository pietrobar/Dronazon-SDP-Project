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
    return valueBetween(t1, t2, "deliveries");
  }

  public Object getKilometersBetweenTimestamps(String t1, String t2) {
    return valueBetween(t1, t2, "km");
  }

  private int valueBetween(String t1, String t2, String type) {
    List<Integer> meanValue = new ArrayList<>();
    for(Statistic s : getStats()){
      System.out.println(s);
      LocalDateTime ts = Statistic.extractDateTime(s.getTimestamp());
      LocalDateTime ts1 = Statistic.extractDateTime(t1);
      LocalDateTime ts2 = Statistic.extractDateTime(t2);

      if((ts.isBefore(ts2)||ts.isEqual(ts2)) && (ts.isAfter(ts1)||ts.isEqual(ts1))){
        if(type.equals("km"))
          meanValue.add(s.getMeanKilometers());
        if(type.equals("deliveries"))
          meanValue.add(s.getMeanDelivery());
      }
    }
    int div=meanValue.size();
    return meanValue.stream().mapToInt(Integer::intValue).sum()/div;
  }

}
