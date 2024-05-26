package org.example;

import org.apache.commons.cli.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.text.DecimalFormat;
import java.util.Random;

public class Client {
    DecimalFormat dc = new DecimalFormat("#.###");
    private String host;
    private int port;
    private int minPacketSize;
    private int maxPacketSize;
    private int pauseInMillis;
    private FileWriter fileWriter;
    private String packages;
    private int totalDurationInSeconds;
    private int numThreads;
    private String errorWork;
    private int sleepTime;
    private long startTime;


    public Client(String[] args) {
        parseArgs(args);
        startTime = System.currentTimeMillis();
        if (packages.equals("true")) {
            try {
                fileWriter = new FileWriter("clientThread" + Thread.currentThread().getId() + ".txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("host", true, "Host address");
        options.addOption("port", true, "Port number");
        options.addOption("min_size", true, "Minimum packet size");
        options.addOption("max_size", true, "Maximum packet size");
        options.addOption("duration", true, "Total duration of testing in seconds");
        options.addOption("message_timeout", true, "Response waiting time");
        options.addOption("delay", true, "Time delay for program (in milliseconds)");
        options.addOption("log", true, "Option for result recording");
        options.addOption("threads", true, "Number of threads");
        options.addOption("errorCheck", true, "Error checker function (ep- error with packages, e- error without packages ) ");


        try {
            CommandLine cmd = parser.parse(options, args);
            host = cmd.getOptionValue("host", "localhost");
            port = Integer.parseInt(cmd.getOptionValue("port", "6452"));
            minPacketSize = Integer.parseInt(cmd.getOptionValue("min_size", "10"));
            maxPacketSize = Integer.parseInt(cmd.getOptionValue("max_size", "100"));
            totalDurationInSeconds = Integer.parseInt((cmd.getOptionValue("duration", "3")));
            pauseInMillis = Integer.parseInt(cmd.getOptionValue("message_timeout", "50000"));
            sleepTime = Integer.parseInt(cmd.getOptionValue("delay", "500"));
            packages = cmd.getOptionValue("log", "true");
            numThreads = Integer.parseInt(cmd.getOptionValue("threads", "1"));
            errorWork = cmd.getOptionValue("errorCheck", "");
        } catch (ParseException e) {
            System.out.println();
            System.out.println("Error parsing command line arguments: " + e.getMessage());
            printUsage(options);
            System.exit(1);
        } catch (NumberFormatException e) {
            System.out.println();
            System.out.println("Wrong data format: " + e.getMessage());
            System.out.println();
            System.exit(1);
        }
    }

    private void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar client.jar", options);
    }

    public void startTesting() {
        long endTime;
        System.out.println("Run example: java -jar client.jar -host localhost -port 6452 -min_size 10 -max_size 100 -message_timeout 500 (-duration 3 or -errorCheck e) -threads 1 -packages false");
        if (maxPacketSize < minPacketSize) {
            System.out.println();
            System.out.println("Max packet size can't be lower than Min packet size");
            System.out.println();
            System.exit(1);
        }
        if (maxPacketSize > 1000) {
            System.out.println();
            System.out.println("Exceeding the maximum packet size. Maximum packet size swapped on 10000");
            System.out.println();
            maxPacketSize = 10000;
        }
        if (maxPacketSize == 0) {
            System.out.println();
            System.out.println("Exceeding the maximum packet size. Maximum packet size swapped on 10000");
            System.out.println();
            maxPacketSize = 10000;
        }
        if (minPacketSize <= 0) {
            System.out.println();
            System.out.println("Minimal packet size can't be 0. Minimal packet size swapped on 1");
            System.out.println();
            minPacketSize = 1;
        }
        if (host == null) {
            System.out.println("-host : localhost");
        } else {
            System.out.println("-host     : " + host);
        }
        System.out.println("-port     : " + port);
        System.out.println("-min_size : " + minPacketSize);
        System.out.println("-max_size : " + maxPacketSize);
        if (numThreads > 100) {
            System.out.println();
            System.out.println("Too much threads");
            numThreads = 100;
            System.out.println("Number of threads is changed on 10");
        }
        if (sleepTime <= 0) {
            System.out.println();
            System.out.println("Time delay can't be lesser than 0");
            System.out.println();
            System.exit(1);
        } else {
            System.out.println("-delay    : " + sleepTime);
        }
        System.out.println("-threads  : " + numThreads);
        System.out.println("-packages : " + packages);
        System.out.println("-message_timeout : " + pauseInMillis);
        if (errorWork.equals("ep")) {
            totalDurationInSeconds = 10000000;
            System.out.println("-errorCheck ep");
        } else if (errorWork.equals("e")) {
            totalDurationInSeconds = 10000000;
            System.out.println("-errorCheck e");
        } else {
            System.out.println("-duration : " + totalDurationInSeconds);
        }

        endTime = startTime + (totalDurationInSeconds * 1000); // Convert to milliseconds

        for (int i = 0; i < numThreads; i++) {
            System.out.println("Thread " + Thread.currentThread().getId() + " started");
            new ClientThread(endTime).start();
        }
    }

    private String generateRandomData(int dataSize) {
        int maxDataSize = Math.min(dataSize, maxPacketSize);
        Random random = new Random();
        StringBuilder randomData = new StringBuilder();
        for (int i = 0; i < maxDataSize; i++) {
            char randomChar = (char) (random.nextInt(26) + 'A');
            randomData.append(randomChar);
        }
        return randomData.toString();
    }

    private class ClientThread extends Thread {
        private long endTime;

        public ClientThread(long endTime) {
            this.endTime = endTime;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            int successfulRequests = 0;
            int failedRequests = 0;
            int totalResponseTime = 0;
            double minResponseTime = Double.MAX_VALUE;
            double maxResponseTime = Double.MIN_VALUE;
            String response = "";
            String testData = "";
            Socket socket = null;
            DataOutputStream oos = null;
            DataInputStream ois = null;

            try {
                socket = new Socket(host, port);
                oos = new DataOutputStream(socket.getOutputStream());
                ois = new DataInputStream(socket.getInputStream());

                while (System.currentTimeMillis() < endTime) {
                    double sendTime = System.currentTimeMillis();
                    System.out.print("\r" + getStatistics(testData, response, startTime, successfulRequests, failedRequests, totalResponseTime, minResponseTime, maxResponseTime));
                    System.out.flush();
                    System.out.flush();

                    int packetSize = minPacketSize + (int) (Math.random() * (maxPacketSize - minPacketSize + 1));
                    testData = generateRandomData(packetSize);

                    if (fileWriter != null && (packages == null ? "true" == null : packages.equals("true"))) {
                        fileWriter.write("Sent    : " + testData + "\n");
                        fileWriter.flush();
                    }

                    byte[] testDataBytes = testData.getBytes("UTF-8");
                    oos.writeInt(testDataBytes.length);
                    oos.write(testDataBytes);
                    oos.flush();


                    if (socket.getSoTimeout() != pauseInMillis) {
                        socket.setSoTimeout(pauseInMillis);
                    }
                    int dataSize = ois.readInt();
                    byte[] responseBytes = new byte[dataSize];
                    ois.readFully(responseBytes);
                    response = new String(responseBytes, "UTF-8");

//                    response = ois.readUTF();
                    double receiveTime = System.currentTimeMillis();
                    double responseTime = receiveTime - sendTime;

                    minResponseTime = Math.min(minResponseTime, responseTime);
                    maxResponseTime = Math.max(maxResponseTime, responseTime);

                    if (fileWriter != null && packages == "true") {
                        fileWriter.write("Received: " + response + "\n");
                        fileWriter.flush();
                    }

                    if (response.equals(testData)) {
                        successfulRequests++;
                        totalResponseTime += responseTime;

                    } else {
                        failedRequests++;
                        System.out.println();
                        System.out.println(" Response: " + response);
                        System.out.println("Test Data: " + testData);
                        System.out.println("Response Size " + responseBytes.length + " Bytes");
                        System.out.println("Test Data Size " + testDataBytes.length + " Bytes");
                        break;
                    }
                    Thread.sleep(sleepTime);
                }
                try {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("\r" + getStatistics(testData, response, startTime, successfulRequests, failedRequests, totalResponseTime, minResponseTime, maxResponseTime));

            } catch (UnknownHostException e) {
                System.out.println();
                System.out.println("Host is unknown");
                System.out.println();
                System.exit(1);
            } catch (ConnectException e) {
                System.out.println();
                System.out.println("Connection error");
                System.out.println();
                System.exit(1);
            } catch (SocketTimeoutException e) {
                System.out.println();
                System.out.println("Timeout error");
                System.out.println();
                System.exit(1);
            } catch (SocketException e) {
                System.out.println();
                System.out.println("Socket error");
                System.out.println();
                System.exit(1);
            } catch (IOException e) {
                System.out.println();
                System.out.println("Error");
                System.out.println();
                System.exit(1);
            } catch (NumberFormatException e) {
                System.out.println();
                System.out.println("Wrong data format");
                System.out.println();
                System.exit(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            int totalRequests = successfulRequests + failedRequests;

            double averageResponseTime = (totalRequests > 0) ? (double) totalResponseTime / totalRequests : 0;
            String averageResponseTimes = dc.format(averageResponseTime);
            if (minResponseTime < 0) {
                minResponseTime = 0;

            }


            synchronized (System.out) {
                System.out.println("-------------------------------------------");
                System.out.println("Thread " + Thread.currentThread().getId() + " has completed its work.");
                System.out.println("Report:");
                System.out.println("Total execution time : " + (System.currentTimeMillis() - startTime) + " milliseconds");
                System.out.println("Minimum response time: " + minResponseTime + " milliseconds");
                System.out.println("Maximum response time: " + maxResponseTime + " milliseconds");
                System.out.println("Average response time: " + averageResponseTimes + " milliseconds");
                System.out.println("Successful requests  : " + successfulRequests);
                System.out.println("Failed requests      : " + failedRequests);
                System.out.println("-------------------------------------------");
            }
        }

        private String getStatistics(String testData, String response, long startTime, int successfulRequests, int failedRequests, int totalResponseTime, double minResponseTime, double maxResponseTime) {
            int elapsedTimeInSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
            double averageResponseTimes = (successfulRequests + failedRequests > 0) ? (double) totalResponseTime / (successfulRequests + failedRequests) : 0;
            String averageResponseTime = dc.format(averageResponseTimes);
            minResponseTime = minResponseTime == Double.MAX_VALUE ? 0 : minResponseTime;
            maxResponseTime = maxResponseTime == Double.MIN_VALUE ? 0 : maxResponseTime;

            String minResponseTimeStr = dc.format(minResponseTime);
            String maxResponseTimeStr = dc.format(maxResponseTime);

            String statusText = String.format("Pending: %d sec | Successful: %d | Failed: %d | Avg : %s ms | Min : %s ms | Max : %s ms", elapsedTimeInSeconds, successfulRequests, failedRequests, averageResponseTime, minResponseTimeStr, maxResponseTimeStr);

            if (errorWork.equals("ep")) {
                return statusText + " | Send Data: " + testData + " | Response: " + response;
            } else {
                return statusText;
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client(args);
        client.startTesting();
    }
}
