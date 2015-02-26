/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.httpclient;

import java.io.IOException;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.sonatype.nexus.common.sequence.FibonacciNumberSequence;
import org.sonatype.nexus.common.sequence.NumberSequence;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.Time;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.0
 */
public class FilteredHttpClient
    extends ComponentSupport
    implements HttpClient
{

  private final HttpClient delegate;

  private boolean blocked;

  private DateTime blockedUntil;

  private final boolean autoBlock;

  private final NumberSequence autoBlockSequence;

  private RemoteConnectionStatus status;

  public FilteredHttpClient(final HttpClient delegate,
                            final HttpClientConfig config)
  {
    this.delegate = checkNotNull(delegate);
    this.blocked = checkNotNull(config).getConnectionConfig().isBlocked();
    this.autoBlock = config.getConnectionConfig().shouldAutoBlock();
    this.status = new RemoteConnectionStatus(blocked ? "Remote Manually Blocked" : "Unknown");
    // TODO shall we use config.getConnectionConfig().getTimeout() * 2 as in NX2?
    this.autoBlockSequence = new FibonacciNumberSequence(Time.seconds(40).toMillis());
  }

  private <T> T filter(final Filterable<T> filterable) throws IOException {
    if (blocked) {
      throw new IOException("Remote Manually Blocked");
    }
    DateTime blockedUntilCopy = this.blockedUntil;
    if (autoBlock && blockedUntilCopy != null && blockedUntilCopy.isAfterNow()) {
      throw new IOException("Remote Auto Blocked");
    }
    try {
      T result = filterable.call();
      if (autoBlock) {
        synchronized (this) {
          if (blockedUntil != null) {
            blockedUntil = null;
            autoBlockSequence.reset();
          }
        }
      }
      status = new RemoteConnectionStatus("Remote Available");
      return result;
    }
    catch (IOException e) {
      if (isRemoteUnavailable(e)) {
        if (autoBlock) {
          synchronized (this) {
            // avoid some other thread already increased the sequence
            if (blockedUntil == null || blockedUntil.isBeforeNow()) {
              blockedUntil = DateTime.now().plus(autoBlockSequence.next());
            }
          }
          status = new RemoteConnectionStatus("Remote Auto Blocked and Unavailable", getReason(e));
        }
        else {
          status = new RemoteConnectionStatus("Remote Unavailable", getReason(e));
        }
      }
      throw e;
    }
    finally {
      blockedUntilCopy = blockedUntil;
      log.debug(
          "Remote status: {} {}",
          status,
          blockedUntilCopy != null ? "(blocked until " + blockedUntilCopy + ")" : ""
      );
    }
  }

  public RemoteConnectionStatus getStatus() {
    return status;
  }

  private boolean isRemoteUnavailable(final Exception e) {
    if (e instanceof ConnectionPoolTimeoutException) {
      return false;
    }
    return true;
  }

  private String getReason(final Exception e) {
    if (e instanceof SSLPeerUnverifiedException) {
      return "Untrusted Remote";
    }
    return e.getMessage();
  }

  @Override
  public HttpParams getParams() {
    return delegate.getParams();
  }

  @Override
  public ClientConnectionManager getConnectionManager() {
    return delegate.getConnectionManager();
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request) throws IOException {
    return filter(new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(request);
      }
    });
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request,
                              final HttpContext context)
      throws IOException
  {
    return filter(new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(request, context);
      }
    });
  }

  @Override
  public HttpResponse execute(final HttpHost target,
                              final HttpRequest request)
      throws IOException
  {
    return filter(new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(target, request);
      }
    });
  }

  @Override
  public HttpResponse execute(final HttpHost target,
                              final HttpRequest request,
                              final HttpContext context)
      throws IOException
  {
    return filter(new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(target, request, context);
      }
    });
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
                       final ResponseHandler<? extends T> responseHandler)
      throws IOException
  {
    return filter(new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(request, responseHandler);
      }
    });
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
                       final ResponseHandler<? extends T> responseHandler,
                       final HttpContext context)
      throws IOException
  {
    return filter(new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(request, responseHandler, context);
      }
    });
  }

  @Override
  public <T> T execute(final HttpHost target,
                       final HttpRequest request,
                       final ResponseHandler<? extends T> responseHandler)
      throws IOException
  {
    return filter(new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(target, request, responseHandler);
      }
    });
  }

  @Override
  public <T> T execute(final HttpHost target,
                       final HttpRequest request,
                       final ResponseHandler<? extends T> responseHandler,
                       final HttpContext context) throws IOException
  {
    return filter(new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(target, request, responseHandler, context);
      }
    });
  }

  private static interface Filterable<T>
  {
    T call() throws IOException;
  }

}
