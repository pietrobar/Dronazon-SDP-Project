package dronazon;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Random;
import java.util.UUID;

/**
 * Created by Pietro on 07/05/2021
 */
public class Dronazon {
  public static void main(String[] args) {
    MqttClient client;
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    String topic = "dronazon/smartcity/orders/";//a drone will be a subscriber to this topic
    int qos = 2;

    try {
      client = new MqttClient(broker, clientId);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);

      // Connect the client
      System.out.println(clientId + " Connecting Broker " + broker);
      client.connect(connOpts);
      System.out.println(clientId + " Connected");

      while(client.isConnected()){
        Thread.sleep(5000);//every 5 seconds a new order
        Order order = generatesOrder();
        Gson gson = new Gson();
        String payload = gson.toJson(order);

        MqttMessage message = new MqttMessage(payload.getBytes());

        // Set the QoS on the Message
        message.setQos(qos);
        System.out.print(clientId + " New order: " + payload + " ...");
        client.publish(topic, message);
        System.out.println("PUBLISHED");
      }


      if (client.isConnected())
        client.disconnect();
      System.out.println("Publisher " + clientId + " disconnected");



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

  private static Order generatesOrder() {
    String id = UUID.randomUUID().toString();
    Coordinate pickUpPoint = Coordinate.randomCoordinate();
    Coordinate deliveryPoint = Coordinate.randomCoordinate();
    return new Order(id,pickUpPoint,deliveryPoint);
  }




}
