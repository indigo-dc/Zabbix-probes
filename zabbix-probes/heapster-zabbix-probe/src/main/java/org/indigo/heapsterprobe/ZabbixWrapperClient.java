/**
 * Copyright 2016 ATOS SPAIN S.A.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the License); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>Authors Contact: Francisco Javier Nieto. Atos Research and Innovation, Atos SPAIN SA
 *
 * @email francisco.nieto@atos.net
 */
package org.indigo.heapsterprobe;

import com.google.gson.Gson;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class interacts with the Zabbix Wrapper in order to check whether certain hosts are
 * available or not. Moreover, it allows adding new hosts whenever necessary, by using the exposed
 * REST API.
 *
 * @author ATOS
 */
public class ZabbixWrapperClient {

  private String wrapperBaseUrl = "";
  private Client client = null;

  /**
   * This is the main constructor of the class. It retrieves the configuration information from the
   * config file and it construcs the base URL for the Wrapper client.
   */
  public ZabbixWrapperClient() {
    // Retrieve location of the Zabbix Server and the Zabbix sender (local)
    PropertiesManager myProp = new PropertiesManager();
    String location = myProp.getProperty(PropertiesManager.ZABBIX_WRAPPER);
    wrapperBaseUrl =
        location + "/monitoring/adapters/zabbix/zones/indigo/types/infrastructure/groups/";

    // Disable issue with SSL Handshake in Java 7 and indicate certificates keystore
    System.setProperty("jsse.enableSNIExtension", "false");
    String certificatesTrustStorePath = myProp.getProperty(PropertiesManager.JAVA_KEYSTORE);
    System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);

    // Create the Client
    ClientConfig cc = new ClientConfig();
    client = JerseyClientBuilder.newClient(cc);
  }

  /**
   * This constructor is used only for unit testing purposes, since it uses a mock for simulating
   * the Wrapper service.
   *
   * @param mockClient Mock of the Zabbix Wrapper API.
   */
  public ZabbixWrapperClient(Client mockClient) {
    client = mockClient;
  }

  /**
   * It registers a new host in the Zabbix monitoring infrastructure by using the corresponding REST
   * API. Most of the values are configured by default.
   *
   * @param hostName Name of the new Container host
   * @return True if the operation succeeded. False if not.
   */
  public boolean registerContainerHost(String hostName) {
    // Prepare the service URL
    String hostUrl = wrapperBaseUrl + "Containers/hosts/" + hostName;
    WebTarget target = client.target(hostUrl);
    Invocation.Builder invocationBuilder = target.request();
    invocationBuilder.header("Content-Type", "application/json");

    // Prepare the object in JSON format
    Gson gson = new Gson();
    ZabbixHost myHost = new ZabbixHost(hostName);
    String jsonHost = gson.toJson(myHost);
    System.out.println(jsonHost);

    // Send the new host data
    Response response = null;
    try {
      response = invocationBuilder.post(Entity.entity(jsonHost, MediaType.APPLICATION_JSON));
    } catch (Exception ex) {
      System.out.println("Invocation failed! " + ex.getMessage());
      return false;
    }

    // Indicate whether the response wasn't satisfactory
    return response.getStatus() <= 300;
  }

  /**
   * It registers a new host in the Zabbix monitoring infrastructure by using the corresponding REST
   * API. Most of the values are configured by default.
   *
   * @param hostName Name of the new Pod host
   * @return True if the operation succeeded. False if not.
   */
  public boolean registerPodHost(String hostName) {
    // Prepare the service URL
    String hostUrl = wrapperBaseUrl + "Pods/hosts/" + hostName;
    WebTarget target = client.target(hostUrl);
    Invocation.Builder invocationBuilder = target.request();
    invocationBuilder.header("Content-Type", "application/json");

    // Prepare the object in JSON format
    Gson gson = new Gson();
    ZabbixHost myHost = new ZabbixHost(hostName);
    String jsonHost = gson.toJson(myHost);
    System.out.println(jsonHost);

    // Send the new host data
    Response response = null;
    try {
      response = invocationBuilder.post(Entity.entity(jsonHost, MediaType.APPLICATION_JSON));
    } catch (Exception ex) {
      System.out.println("Invocation failed! " + ex.getMessage());
      return false;
    }

    // Indicate whether the response wasn't satisfactory
    return response.getStatus() <= 300;
  }

  /**
   * This method accesses the Wrapper API in order to get the host information by its name. It is
   * focused on finding only Containers. In case it cannot find it, it will return a 'false' value.
   *
   * @param hostName Name of the container to search.
   * @return True if it was found. False if not.
   */
  public boolean isContainerRegistered(String hostName) {
    // Prepare the service URL
    String hostUrl = wrapperBaseUrl + "Containers/hosts/" + hostName;
    WebTarget target = client.target(hostUrl);
    Invocation.Builder invocationBuilder = target.request();
    invocationBuilder.header("Content-Type", "application/json");
    Response response = null;
    try {
      response = invocationBuilder.get();
    } catch (Exception ex) {
      System.out.println("Invocation failed! " + ex.getMessage());
      return false;
    }

    // If the response is different from 20X, it means that it isn't available
    return response.getStatus() <= 300;
  }

  /**
   * This method accesses the Wrapper API in order to get the host information by its name. It is
   * focused on finding pods. In case it cannot find it, it will return a 'false' value.
   *
   * @param hostName Name of the pod to search.
   * @return True if it was found. False if not.
   */
  public boolean isPodRegistered(String hostName) {
    // Prepare the service URL
    String hostUrl = wrapperBaseUrl + "Pods/hosts/" + hostName;
    WebTarget target = client.target(hostUrl);
    Invocation.Builder invocationBuilder = target.request();
    invocationBuilder.header("Content-Type", "application/json");
    Response response = null;
    try {
      response = invocationBuilder.get();
    } catch (Exception ex) {
      System.out.println("Invocation failed! " + ex.getMessage());
      return false;
    }

    // If the response is different from 20X, it means that it isn't available
    return response.getStatus() <= 300;
  }

  /**
   * Typical main method for testing.
   *
   * @param args Typical args.
   */
  public static void main(String[] args) {
    ZabbixWrapperClient myClient = new ZabbixWrapperClient();
    boolean hostExists = myClient.isPodRegistered("PruHost");
    System.out.println(hostExists);
    myClient.registerPodHost("PruHost");
  }
}
