package org.jodah.stormcell;

import java.util.HashMap;
import java.util.Map;

import backtype.storm.hooks.BaseTaskHook;
import backtype.storm.hooks.info.BoltAckInfo;
import backtype.storm.hooks.info.BoltFailInfo;
import backtype.storm.hooks.info.EmitInfo;
import backtype.storm.hooks.info.SpoutAckInfo;
import backtype.storm.hooks.info.SpoutFailInfo;
import backtype.storm.task.TopologyContext;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
 * StormCell task hook. Creates {@link com.yammer.metrics.core.Meter meters} to track each task's
 * acks, fails and emits.
 */
public class StormCellTaskHook extends BaseTaskHook {
  private static final Map<Integer, Meter> EMIT_METERS = new HashMap<Integer, Meter>();
  private static final Map<Integer, Meter> ACK_METERS = new HashMap<Integer, Meter>();
  private static final Map<Integer, Meter> FAIL_METERS = new HashMap<Integer, Meter>();
  static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();

  private static String metricNameFor(String name, TopologyContext context) {
    return MetricRegistry.name("storm", "component", String.valueOf(context.getThisTaskId()),
        context.getThisComponentId(), name);
  }

  @Override
  public void boltAck(BoltAckInfo boltAckInfo) {
    mark(ACK_METERS, boltAckInfo.ackingTaskId);
  }

  @Override
  public void boltFail(BoltFailInfo boltFailInfo) {
    mark(FAIL_METERS, boltFailInfo.failingTaskId);
  }

  @Override
  public void cleanup() {
    StormCellServer.stop();
  }

  @Override
  public void emit(EmitInfo emitInfo) {
    mark(EMIT_METERS, emitInfo.taskId);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void prepare(Map config, TopologyContext context) {
    StormCellServer.initialize(config, context);

    EMIT_METERS.put(context.getThisTaskId(), METRIC_REGISTRY.meter(metricNameFor("acked", context)));
    ACK_METERS.put(context.getThisTaskId(),
        METRIC_REGISTRY.meter(metricNameFor("emitted", context)));
    FAIL_METERS.put(context.getThisTaskId(),
        METRIC_REGISTRY.meter(metricNameFor("failed", context)));
  }

  @Override
  public void spoutAck(SpoutAckInfo ackInfo) {
    mark(ACK_METERS, ackInfo.spoutTaskId);
  }

  @Override
  public void spoutFail(SpoutFailInfo failInfo) {
    mark(FAIL_METERS, failInfo.spoutTaskId);
  }

  private void mark(Map<Integer, Meter> meterMap, int taskId) {
    Meter meter = meterMap.get(Integer.valueOf(taskId));
    if (meter != null)
      meter.mark();
  }
}
