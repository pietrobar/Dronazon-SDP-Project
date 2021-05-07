package restserver.beans;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * Created by Pietro on 07/05/2021
 */



public class StatsManager {
  private List<String> stats;//TODO: devo sostituire la stringa con un oggetto che rappresenti le statistiche

  private static StatsManager instance;

  private StatsManager(){
    stats = new ArrayList<>();
  }

  public synchronized static StatsManager getInstance(){
    if(instance==null)
      instance = new StatsManager();
    return instance;
  }



}
