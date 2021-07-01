package dronesnetwork;

import com.google.gson.Gson;
import dronazon.Order;
import org.eclipse.paho.client.mqttv3.*;
import restserver.beans.DroneInfo;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Pietro on 23/05/2021
 */
public class DroneOrderManager implements Runnable{
  MqttClient client;
  String broker = "tcp://localhost:1883";
  String clientId = MqttClient.generateClientId();
  String topic = "dronazon/smartcity/orders/";
  int qos = 2;

  protected final List<Order> orders;
  private final List<DroneInfo> occupiedDrones;
  Drone drone;

  private final Thread orderAssigner;


  public DroneOrderManager(Drone drone){
    orders = new ArrayList<>();
    this.occupiedDrones = new ArrayList<>();
    this.drone = drone;

    //Thread for assigning orders
    orderAssigner = new Thread(()->{
      synchronized (orders){
        while(true){
          try {
            if(orders.size()==0 || drone.getDronesCopy().size()==getOccupiedDrones().size()) {//if no drone is available I wait
              orders.wait();
            }
          } catch (InterruptedException e) {
            System.out.println("Order assigner interrupted");
            break;
          }
          if(orders.size()>0)
            drone.getDroneGRPCManager().tryAssignOrder(orders.remove(0));//if this order is not delivered it will be re-added to the list
        }
      }
    });
    orderAssigner.start();
  }

  public void addOrder(Order order){
    synchronized (orders){
      this.orders.add(order);
      orders.notify();
    }
  }

  public synchronized void addOccupiedDrone(DroneInfo droneInfo){
    this.occupiedDrones.add(droneInfo);
  }

  public synchronized void removeOccupiedDrone(DroneInfo droneInfo){
    this.occupiedDrones.remove(droneInfo);
  }

  public synchronized List<DroneInfo> getOccupiedDrones(){
    return occupiedDrones;
  }

  public void stopOrderAssigner(){
    this.orderAssigner.interrupt();
  }
  @Override
  public void run() {
    System.out.println("Started Order Receiver");

    try {
      client = new MqttClient(broker, clientId);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);//non sono interessato a sapere lo stato del client una volta disconnesso dal broker, mettendo false se c'e' una disconnessione il broker salva le informazioni rilevanti per il client


      client.connect(connOpts);

      client.setCallback(new MqttCallback() {
        /*each time a new order arrives a new thread is created that will manage the assignment of the order*/
        public void messageArrived(String topic, MqttMessage message) {
          String receivedMessage = new String(message.getPayload());
          //I have to save my order
          Gson gson = new Gson();
          Order order = gson.fromJson(receivedMessage, Order.class);
          addOrder(order);//useful to keep track of how many orders needs to be delivered

        }

        public void connectionLost(Throwable cause) {
          cause.printStackTrace();
        }

        public void deliveryComplete(IMqttDeliveryToken token) {
          // Not used here
        }

      });
      client.subscribe(topic,qos);
      System.out.println(clientId + " Subscribed to topics : " + topic);

      synchronized (this) {
        while (!drone.isQuitting()){
          wait();
        }
        System.out.println("Client Disconnected");
        client.disconnect();
      }


    } catch (MqttException me ) {
      System.out.println("reason " + me.getReasonCode());
      System.out.println("msg " + me.getMessage());
      System.out.println("loc " + me.getLocalizedMessage());
      System.out.println("cause " + me.getCause());
      System.out.println("excep " + me);
      me.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public List<Order> getOrders() {
    synchronized (orders){
      return new ArrayList<>(orders);
    }
  }
}
