package dronesnetwork;

import sensorpm10.Measurement;
import sensorpm10.SlidingWindow;
import sensorpm10.PM10Simulator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pietro on 04/06/2021
 */
public class DronePollutionSensor implements Runnable{
  private final SlidingWindow slidingWindow;

  private final List<Float> meanList;//this list will store all the means calculated from last assigned order


  public DronePollutionSensor() {
    this.slidingWindow=new SlidingWindow();
    PM10Simulator pm10Simulator = new PM10Simulator(slidingWindow);
    pm10Simulator.start();

    this.meanList = new ArrayList<>();
  }

  @Override
  public void run() {
    //here I have to collect the information from the slidingWindow
    while (true){
      List<Measurement> measurements;
      synchronized (slidingWindow){
        while (slidingWindow.size()<8){
          try {
            slidingWindow.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        measurements = slidingWindow.readAllAndClean();
        slidingWindow.notify();

      }
      synchronized (this){
        meanList.add(mean(measurements));
      }
    }
  }

  private float mean(List<Measurement> measurements) {
    float sum = 0;
    for (Measurement m : measurements){
      sum+=m.getValue();
    }
    return sum/8;//It is always 8
  }

  public synchronized List<Float> getMeanList() {
    return new ArrayList<>(meanList);
  }

  public synchronized void clearMeanList(){
    this.meanList.clear();
  }
}
