package sensorpm10;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 12/05/2021
 */
public class SlidingWindow implements Buffer{
  private final List<Measurement> buffer;//max size of 8, 50% overlap
  public SlidingWindow() {
    buffer = new ArrayList<>();
  }

  @Override
  public synchronized void addMeasurement(Measurement m) {
    //pattern producer consumer
    while (buffer.size()>=8){
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    buffer.add(m);
    notify();


  }

  @Override
  public List<Measurement> readAllAndClean() {
    List<Measurement> res = new ArrayList<>(buffer);
    buffer.subList(0, 4).clear();
    return res;
  }

  public static void main(String[] args) {
    PM10Simulator pm10Simulator = new PM10Simulator(new SlidingWindow());
    pm10Simulator.start();

  }

  public int size() {
    return buffer.size();
  }
}
