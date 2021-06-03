package dronesnetwork;

import com.google.gson.Gson;
import dronazon.Order;
import org.eclipse.paho.client.mqttv3.*;

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

  List<Order> orders;
  Drone drone;


  public DroneOrderManager(Drone drone){
    orders = new ArrayList<>();
    this.drone = drone;
  }

  public synchronized void addOrder(Order order){
    this.orders.add(order);
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
        public void messageArrived(String topic, MqttMessage message) {
          String receivedMessage = new String(message.getPayload());
          System.out.println(clientId +" Received a Message! - Callback "+
                  "\n\tMessage: " + receivedMessage);
          //{"id":"79045252-c2c5-4edb-aa3e-4b841f1fcab7","pickUpPoint":{"x":8,"y":0},"deliveryPoint":{"x":6,"y":5}}
          //I have to save my order
          Gson gson = new Gson();
          Order order = gson.fromJson(receivedMessage, Order.class);

          System.out.println("ASSIGNING ORDER "+ receivedMessage);
          drone.getDroneGRPCManager().assignOrder(order);
        }

        public void connectionLost(Throwable cause) {
          System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage()+ "-  Thread PID: " + Thread.currentThread().getId());
        }

        public void deliveryComplete(IMqttDeliveryToken token) {
          // Not used here
        }

      });
      client.subscribe(topic,qos);
      System.out.println(clientId + " Subscribed to topics : " + topic);

      //todo: la notify deve essere fatta quando il drone master deve uscire dalla rete

      //client.disconnect();

    } catch (MqttException me ) {
      System.out.println("reason " + me.getReasonCode());
      System.out.println("msg " + me.getMessage());
      System.out.println("loc " + me.getLocalizedMessage());
      System.out.println("cause " + me.getCause());
      System.out.println("excep " + me);
      me.printStackTrace();
    }
  }
}
