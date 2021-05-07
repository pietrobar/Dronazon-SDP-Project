package dronazon;

/**
 * Created by Pietro on 07/05/2021
 */
public class Order {
  private final String id;
  private final Coordinate pickUpPoint;
  private final Coordinate deliveryPoint;

  public Order(String id, Coordinate pickUpPoint, Coordinate deliveryPoint) {
    this.id = id;
    this.pickUpPoint = pickUpPoint;
    this.deliveryPoint = deliveryPoint;
  }

  public String getId() {
    return id;
  }

  public Coordinate getPickUpPoint() {
    return pickUpPoint;
  }

  public Coordinate getDeliveryPoint() {
    return deliveryPoint;
  }


}
