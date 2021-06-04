package sensorpm10;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 12/05/2021
 */
public class SlidingWindow implements Buffer{
  private final List<Measurement> slidingWindow;//max size of 8, 50% overlap
  public SlidingWindow() {
    slidingWindow = new ArrayList<>();
  }

  @Override
  public synchronized void addMeasurement(Measurement m) {
    slidingWindow.add(m);
    if(slidingWindow.size()==8){
      notify();
    }
  }

  @Override
  public synchronized List<Measurement> readAllAndClean() {
    List<Measurement> res = new ArrayList<>(slidingWindow);
    slidingWindow.subList(0, 4).clear();
    return res;
  }

  public static void main(String[] args) {
    PM10Simulator pm10Simulator = new PM10Simulator(new SlidingWindow());
    pm10Simulator.start();

  }

  public int size() {
    return slidingWindow.size();
  }
}
