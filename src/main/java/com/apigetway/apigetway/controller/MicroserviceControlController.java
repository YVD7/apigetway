package com.apigetway.apigetway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class MicroserviceControlController {

    // Injecting the Spring Boot ApplicationContext
    @Autowired
    private ConfigurableApplicationContext context;
    
    private List<Process> runningProcesses = new ArrayList<>();

    // Path to the JAR files (adjust these paths according to where your JAR files are located after build)
    // private static final String MICRO_SERVICE1_JAR_PATH = "producer/target/producer-0.0.1-SNAPSHOT.jar";
    // private static final String MICRO_SERVICE2_JAR_PATH = "consumer/target/consumer-0.0.1-SNAPSHOT.jar";
    // private static final String MICRO_SERVICE3_JAR_PATH = "reader/target/reader-0.0.1-SNAPSHOT.jar";

    private static final String MICRO_SERVICE1_JAR_PATH = "producer" + File.separator + "target" + File.separator + "producer-0.0.1-SNAPSHOT.jar";
    private static final String MICRO_SERVICE2_JAR_PATH = "consumer" + File.separator + "target" + File.separator + "consumer-0.0.1-SNAPSHOT.jar";
    private static final String MICRO_SERVICE3_JAR_PATH = "reader" + File.separator + "target" + File.separator + "reader-0.0.1-SNAPSHOT.jar";

    private static final String OS = System.getProperty("os.name").toLowerCase();

    // Helper method to determine the Java executable for the OS
    private String getJavaExecutable() {
        if (OS.contains("win")) {
            // For Windows, use java.exe (you can provide full path if needed)
            return "java"; // or "C:\\Program Files\\Java\\jdk-version\\bin\\java.exe" if necessary
        } else {
            // For Linux/macOS, the default `java` should work
            return "java";
        }
    }

    @GetMapping("/start")
    public String startMicroservices() {
        try {
            // Start the microservices with the platform-specific java executable
            Process process1 = new ProcessBuilder(getJavaExecutable(), "-jar", MICRO_SERVICE1_JAR_PATH).start();
            runningProcesses.add(process1);

            Process process2 = new ProcessBuilder(getJavaExecutable(), "-jar", MICRO_SERVICE2_JAR_PATH).start();
            runningProcesses.add(process2);

            Process process3 = new ProcessBuilder(getJavaExecutable(), "-jar", MICRO_SERVICE3_JAR_PATH).start();
            runningProcesses.add(process3);

            return "Microservices started successfully!";
        } catch (IOException e) {
            e.printStackTrace();
            return "Error starting microservices!";
        }
    }

    @GetMapping("/stop")
    public String stopMicroservices() {
        try {
            // Forcefully stop all running processes (external microservices)
            for (Process process : runningProcesses) {
                if (process.isAlive()) {
                    process.destroyForcibly();  // Forcefully terminate the external microservice process
                }
            }

            // Ensure processes are terminated before proceeding
            for (Process process : runningProcesses) {
                process.waitFor();  // Wait for process to terminate
            }

            // Kill the process bound to the Spring Boot port (if needed)
            killProcessOnPort(8080);  // Replace with your port number if needed

            // Return the success message
            String message = "Microservices and Spring Boot service have stopped successfully.";

            new Thread(() -> {
                // Trigger Spring Boot graceful shutdown
                SpringApplication.exit(context, () -> 0);
                System.exit(0);
            }).start();

            // Return the message (this will be sent to Postman before exiting)
            return message;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error stopping microservices!";
        }
    }


    // Helper method to kill a process bound to a specific port (e.g., 8080)
    private void killProcessOnPort(int port) {
        try {
            String command;
            if (OS.contains("win")) {
                // Windows command to find and kill process bound to a port
                command = "netstat -ano | findstr :" + port;
                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();

                // Extract PID from command output
                // Find the PID (requires parsing the output, which is omitted here)
                // Then kill the process using taskkill
                command = "taskkill /PID <PID> /F";  // Replace <PID> with actual PID value
                Runtime.getRuntime().exec(command);
            } else {
                // Linux/macOS command to find and kill process bound to a port
                command = "lsof -t -i:" + port;
                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();

                // Extract PID from command output
                // Kill the process using the PID
                command = "kill -9 <PID>";  // Replace <PID> with actual PID value
                Runtime.getRuntime().exec(command);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
