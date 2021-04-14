package com.newrelic.telemetry.logs;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogBuffer {
  private static final Logger logger = LoggerFactory.getLogger(LogBuffer.class);

  private final Queue<Log> logs = new ConcurrentLinkedQueue<>();

  private final Attributes commonAttributes;

  public LogBuffer(Attributes commonAttributes) {
    this.commonAttributes = Utils.verifyNonNull(commonAttributes);
  }

  public void addLog(Log log) {
    logs.add(log);
  }

  public int size() {
    return logs.size();
  }

  public LogBatch createBatch() {
    logger.debug("Creating Log batch. Size: "+this.logs.size());
    Collection<Log> logsForBatch = new ArrayList<>(this.logs.size());

    // Drain the Log buffer and return the batch
    Log log;
    while ((log = this.logs.poll()) != null) {
      logsForBatch.add(log);
    }

    return new LogBatch(logsForBatch, this.commonAttributes);
  }

  Queue<Log> getLogs() {
    return logs;
  }

  Attributes getCommonAttributes() {
    return commonAttributes;
  }

  @Override
  public String toString() {
    return "LogBuffer{" + "log=" + logs + ", commonAttributes=" + commonAttributes + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LogBuffer that = (LogBuffer) o;

    if (getLogs() != null ? !getLogs().equals(that.getLogs()) : that.getLogs() != null)
      return false;
    return getCommonAttributes() != null
        ? getCommonAttributes().equals(that.getCommonAttributes())
        : that.getCommonAttributes() == null;
  }

  @Override
  public int hashCode() {
    int result = getLogs() != null ? getLogs().hashCode() : 0;
    result = 31 * result + (getCommonAttributes() != null ? getCommonAttributes().hashCode() : 0);
    return result;
  }
}
