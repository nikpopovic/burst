/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.vitals.reporter;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A Span Exporter that logs every span at INFO level using java.util.logging. */
public final class TrekLoggingSpanExporter implements SpanExporter {
  private static final Logger logger = Logger.getLogger(TrekLoggingSpanExporter.class.getName());

  /** Returns a new {@link TrekLoggingSpanExporter}. */
  public static TrekLoggingSpanExporter create() {
    return new TrekLoggingSpanExporter();
  }

  /**
   * Class constructor.
   *
   * @deprecated Use {@link #create()}.
   */
  @Deprecated
  public TrekLoggingSpanExporter() {}

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    StringBuilder sb = new StringBuilder();
    for (SpanData span : spans) {
      sb.setLength(0);
      Optional<EventData> beginEvent = span.getEvents().stream().filter(e -> e.getName().equals("BEGIN")).findFirst();
      sb.append("----> VITALS_TREK_END(")
              .append(" tmark=").append(span.getName()).append(",")
              .append(" traceid=").append(span.getTraceId()).append(",")
              .append(" spandid=").append(span.getSpanId()).append(",")
              .append(" begin=").append(beginEvent.isPresent() ? beginEvent.get().getEpochNanos() : "unknown").append(",")
              .append(" end=").append(span.getEndEpochNanos()).append(",")
              .append(" elapsed=").append(beginEvent.isPresent() ? span.getEndEpochNanos() - beginEvent.get().getEpochNanos() : "unknown").append(",")
              .append(span.getAttributes())
              .append(')');
      logger.log(Level.INFO, sb.toString());
    }
    return CompletableResultCode.ofSuccess();
  }

  /**
   * Flushes the data.
   *
   * @return the result of the operation
   */
  @Override
  public CompletableResultCode flush() {
    CompletableResultCode resultCode = new CompletableResultCode();
    for (Handler handler : logger.getHandlers()) {
      try {
        handler.flush();
      } catch (Throwable t) {
        resultCode.fail();
      }
    }
    return resultCode.succeed();
  }

  @Override
  public CompletableResultCode shutdown() {
    return flush();
  }
}
