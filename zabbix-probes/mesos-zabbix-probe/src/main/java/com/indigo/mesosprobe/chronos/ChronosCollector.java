package com.indigo.mesosprobe.chronos;

import com.indigo.zabbix.utils.LifecycleCollector;
import com.indigo.zabbix.utils.beans.AppOperation;
import com.indigo.zabbix.utils.beans.DocDataType;
import com.indigo.zabbix.utils.beans.ServiceInfo;
import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.ChronosClient;
import it.infn.ba.indigo.chronos.client.model.v1.Container;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;

/** Created by jose on 11/17/16. */
public class ChronosCollector extends LifecycleCollector {

  private static final String PREFIX = "Chronos_";

  private static final Log logger = LogFactory.getLog(ChronosCollector.class);
  private static final String JOB_NAME = "zabbix-test-job";

  private Chronos client;
  private String hostname;
  private String serviceId;

  /** Default constructor. */
  public ChronosCollector(ServiceInfo service, String token) {
    client =
        ChronosClient.getInstanceWithTokenAuth(service.getDoc().getData().getEndpoint(), token);
    hostname = service.getDoc().getData().getProviderId();
    this.serviceId = service.getId();
  }

  public String findHostName(String strUrl) {
    try {
      URL url = new URL(strUrl);
      InetAddress addr = InetAddress.getByName(url.getHost());
      return PREFIX + addr.getHostAddress();
    } catch (MalformedURLException e) {
      logger.error("Error getting URL from chronos endpoint", e);
    } catch (UnknownHostException e) {
      logger.error("Can't get IP address from chronos endpoint", e);
    }
    return null;
  }

  @Override
  protected AppOperation clear() {
    Date start = new Date();

    AppOperation retrieve = retrieve();
    if (retrieve.isResult()) {

      AppOperation delOperation = delete();
      Date end = new Date();
      return new AppOperation(
          AppOperation.Operation.CLEAR,
          delOperation.isResult(),
          delOperation.getStatus(),
          end.getTime() - start.getTime());

    } else {
      Date end = new Date();
      return new AppOperation(
          AppOperation.Operation.CLEAR,
          true,
          retrieve.getStatus(),
          end.getTime() - start.getTime());
    }
  }

  @Override
  protected AppOperation create() {

    Date start = new Date();
    Job job = new Job();
    job.setSchedule(
        "R/" + DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date()) + "/PT5M");
    job.setName(JOB_NAME);
    Container container = new Container();
    container.setType("DOCKER");
    container.setImage("busybox");
    job.setContainer(container);
    job.setCpus(0.5);
    job.setMem(512.0);
    job.setCommand("echo hi; sleep 10; echo bye;");

    try {
      client.createJob(job);
      Date current = new Date();
      return new AppOperation(
          AppOperation.Operation.CREATE, true, 200, current.getTime() - start.getTime());

    } catch (ChronosException e) {
      Date current = new Date();
      logger.error("Error creating test job", e);
      return new AppOperation(
          AppOperation.Operation.CREATE, false, e.getStatus(), current.getTime() - start.getTime());
    }
  }

  @Override
  protected AppOperation retrieve() {
    Date start = new Date();
    try {
      Collection<Job> jobStatus = client.getJob(JOB_NAME);
      Date end = new Date();
      long duration = end.getTime() - start.getTime();
      if (jobStatus.isEmpty()) {
        return new AppOperation(AppOperation.Operation.RUN, false, 404, duration);
      } else {
        return new AppOperation(AppOperation.Operation.RUN, true, 200, duration);
      }
    } catch (Exception e) {
      if (e instanceof ChronosException) {
        ChronosException exception = (ChronosException) e;
        return new AppOperation(
            AppOperation.Operation.RUN,
            false,
            exception.getStatus(),
            new Date().getTime() - start.getTime());
      } else {
        return new AppOperation(
            AppOperation.Operation.RUN, false, 500, new Date().getTime() - start.getTime());
      }
    }
  }

  @Override
  protected AppOperation delete() {
    Date start = new Date();
    try {
      client.deleteJob(JOB_NAME);
      Date end = new Date();
      return new AppOperation(
          AppOperation.Operation.DELETE, true, 200, end.getTime() - start.getTime());
    } catch (ChronosException e) {
      Date end = new Date();
      logger.error("Error deleting test job", e);
      return new AppOperation(
          AppOperation.Operation.DELETE, false, e.getStatus(), end.getTime() - start.getTime());
    }
  }

  @Override
  public String getHostName() {
    return this.serviceId;
  }

  @Override
  public String getGroup() {
    return this.hostname;
  }

  @Override
  public DocDataType.ServiceType getServiceType() {
    return DocDataType.ServiceType.CHRONOS;
  }
}
