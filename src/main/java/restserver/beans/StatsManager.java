package restserver.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 07/05/2021
 */


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StatsManager {
  private final List<Statistic> stats;

  private static StatsManager instance;

  private StatsManager(){
    stats = new ArrayList<>();
  }

  public synchronized static StatsManager getInstance(){
    if(instance==null)
      instance = new StatsManager();
    return instance;
  }
  public synchronized void add(Statistic s){
    stats.add(s);
  }



}
