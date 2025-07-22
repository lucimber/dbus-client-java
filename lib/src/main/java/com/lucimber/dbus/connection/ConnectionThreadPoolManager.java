/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages thread pools for D-Bus connections.
 * This class encapsulates thread pool creation, configuration, and lifecycle management.
 */
public class ConnectionThreadPoolManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionThreadPoolManager.class);
  private static final int MIN_THREADS = 2;
  private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

  private final String connectionId;
  private final ExecutorService applicationTaskExecutor;
  private final int threadPoolSize;

  /**
   * Creates a new thread pool manager with default configuration.
   *
   * @param connectionId unique identifier for the connection
   */
  public ConnectionThreadPoolManager(String connectionId) {
    this(connectionId, calculateDefaultPoolSize());
  }

  /**
   * Creates a new thread pool manager with specified thread pool size.
   *
   * @param connectionId unique identifier for the connection
   * @param threadPoolSize size of the thread pool
   */
  public ConnectionThreadPoolManager(String connectionId, int threadPoolSize) {
    this.connectionId = Objects.requireNonNull(connectionId, "connectionId must not be null");
    this.threadPoolSize = Math.max(MIN_THREADS, threadPoolSize);
    
    LOGGER.debug("Creating thread pool for connection {} with {} threads", 
        connectionId, this.threadPoolSize);
    
    this.applicationTaskExecutor = Executors.newFixedThreadPool(
        this.threadPoolSize,
        new DBusThreadFactory(connectionId));
  }

  /**
   * Calculates the default thread pool size based on available processors.
   *
   * @return default thread pool size
   */
  private static int calculateDefaultPoolSize() {
    return Math.max(MIN_THREADS, Runtime.getRuntime().availableProcessors() / 2);
  }

  /**
   * Gets the application task executor.
   *
   * @return executor service for application tasks
   */
  public ExecutorService getApplicationTaskExecutor() {
    return applicationTaskExecutor;
  }

  /**
   * Gets the configured thread pool size.
   *
   * @return thread pool size
   */
  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  /**
   * Shuts down the thread pools gracefully.
   * This method attempts to shut down the executor service gracefully,
   * waiting for tasks to complete before forcing shutdown.
   */
  public void shutdown() {
    LOGGER.info("Shutting down thread pool for connection {}", connectionId);
    
    applicationTaskExecutor.shutdown();
    
    try {
      if (!applicationTaskExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        LOGGER.warn("Thread pool for connection {} did not terminate in {} seconds, forcing shutdown",
            connectionId, SHUTDOWN_TIMEOUT_SECONDS);
        applicationTaskExecutor.shutdownNow();
        
        if (!applicationTaskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
          LOGGER.error("Thread pool for connection {} did not terminate after forced shutdown",
              connectionId);
        }
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while waiting for thread pool shutdown for connection {}", 
          connectionId, e);
      applicationTaskExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    
    LOGGER.debug("Thread pool for connection {} shut down successfully", connectionId);
  }

  /**
   * Checks if the thread pools are shut down.
   *
   * @return true if shut down, false otherwise
   */
  public boolean isShutdown() {
    return applicationTaskExecutor.isShutdown();
  }

  /**
   * Checks if the thread pools are terminated.
   *
   * @return true if terminated, false otherwise
   */
  public boolean isTerminated() {
    return applicationTaskExecutor.isTerminated();
  }

  /**
   * Custom thread factory for D-Bus worker threads.
   */
  private static class DBusThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    DBusThreadFactory(String connectionId) {
      this.namePrefix = "dbus-" + connectionId + "-worker-";
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
      thread.setDaemon(true);
      if (thread.getPriority() != Thread.NORM_PRIORITY) {
        thread.setPriority(Thread.NORM_PRIORITY);
      }
      return thread;
    }
  }
}