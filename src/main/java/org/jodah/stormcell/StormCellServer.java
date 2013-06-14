package org.jodah.stormcell;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;

import backtype.storm.task.TopologyContext;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.AdminServletContextListener;

/**
 * StormCell web server.
 */
public class StormCellServer {
  private static final Logger LOG = Logger.getLogger(StormCellServer.class);
  private static final int BASE_ADMIN_PORT = 7000;
  private static final HealthCheckRegistry HEALTH_CHECK_REGISTRY = new HealthCheckRegistry();
  private static boolean running;
  private static Server server;

  /**
   * Starts the web server.
   */
  static synchronized void start(int port) {
    if (running)
      return;

    HEALTH_CHECK_REGISTRY.register("deadlock", new ThreadDeadlockHealthCheck());

    Server server = new Server(port);
    Context context = new Context(server, "/");
    context.addServlet(AdminServlet.class, "/*");
    context.addEventListener(new AdminServletContextListener() {
      @Override
      protected MetricRegistry getMetricRegistry() {
        return StormCellTaskHook.METRIC_REGISTRY;
      }

      @Override
      protected HealthCheckRegistry getHealthCheckRegistry() {
        return HEALTH_CHECK_REGISTRY;
      }
    });

    try {
      LOG.info("Starting StormCell on port " + port);
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    running = true;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  static void initialize(Map config, TopologyContext context) {
    List<Integer> slotPorts = (List<Integer>) config.get("supervisor.slots.ports");
    Map<Integer, Integer> adminPorts = (Map<Integer, Integer>) config.get("worker.webconsole.ports");
    int adminPortIndex = slotPorts.indexOf(context.getThisWorkerPort());
    int adminPort = adminPortIndex == -1 || adminPorts == null || adminPorts.isEmpty()
        || slotPorts.size() != adminPorts.size() ? BASE_ADMIN_PORT
        + (context.getThisWorkerPort() % 100) : adminPorts.get(adminPortIndex);
    start(adminPort);
  }

  static synchronized void stop() {
    if (!running)
      return;

    try {
      LOG.info("Stopping StormCell");
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      running = false;
    }
  }
}
