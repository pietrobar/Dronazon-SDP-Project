package restserver.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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

  public synchronized Object getNStats(int n) {
    return stats.subList(stats.size()-n, stats.size()-1);
  }
}
