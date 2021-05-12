package sensorpm10;

import java.util.List;

/**
 * Created by Pietro on 12/05/2021
 */
public interface Buffer {

  void addMeasurement(Measurement m);

  List<Measurement> readAllAndClean();

}