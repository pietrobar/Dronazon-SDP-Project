package sensorpm10;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 12/05/2021
 */
public class BufferCls implements Buffer{
  private final List<Measurement> slidingWindow;//max size of 8, 50% overlap
  private double currentMean=0;
  public BufferCls() {
    slidingWindow = new ArrayList<>();
  }

  @Override
  public synchronized void addMeasurement(Measurement m) {
    slidingWindow.add(m);
    if(slidingWindow.size()==8){
      slidingWindow.subList(0, 4).clear();

      calculateMean();

    }
  }

  private void calculateMean() {
    double meanValue=0;
    for(Measurement m : slidingWindow){
      meanValue+= m.getValue();
    }
    currentMean=meanValue;

  }

  public double getCurrentMean() {
    return currentMean;
  }

  @Override
  public synchronized List<Measurement> readAllAndClean() {
    List<Measurement> res = new ArrayList<>(slidingWindow);
    slidingWindow.clear();
    return res;
  }
}
