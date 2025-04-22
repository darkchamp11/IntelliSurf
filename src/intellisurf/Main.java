package intellisurf;

import java.nio.charset.StandardCharsets;
import java.util.*;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaAsyncResultStreamer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.*;

public class Main {
    public static String input(){
        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }
    static Scanner scanner = new Scanner(System.in);
    static String USER_FILE = "users.txt"; // File to store user credentials

    public static void main(String[] args) {
        boolean isRunning = true;

        while (isRunning) {
            System.out.println("======||Welcome to Login Screen||======");
            System.out.println("Choose the option:\n 1. Register \n 2. Login \n 3. Exit");
            System.out.println("Enter the option: ");
            String opt = scanner.nextLine().trim();

            switch (opt.toLowerCase()) {
                case "1", "register" -> {
                    isRegister();
                }
                case "2", "login" -> {
                    boolean isLoggedIn = false;
                    while (!isLoggedIn) {
                        isLoggedIn = isLogin();
                    }
                    chatBot();
                }
                case "3", "exit" -> {
                    System.out.println("Exiting... GoodBye");
                    isRunning = false;
                }
                default -> System.out.println("Invalid Option");
            }
        }
    }

    public static boolean isLogin() {
        System.out.println("||Welcome to Login Screen||");
        System.out.println("Enter Username: ");
        String user = input();
        System.out.println("Enter Password: ");
        String pass = input();
        String hashedPass = hashPassword(pass);
        if (authenticateUser(user, hashedPass)) {
            System.out.println("Login Successful! Welcome " + user + "!");
            return true;
        } else {
            System.out.println("Invalid Credentials. Please try again.");
            return false;
        }
    }

    public static void isRegister(){
        System.out.println("||Welcome to Register Screen||");
        System.out.println("Enter Username: ");
        String user = input();
        if (user.isEmpty()){
            System.out.println("Username cannot be empty. Please enter a valid username.");
            user = input();
        }
        if (user.length() < 4){
            System.out.println("Username is too short. Please enter a valid username.");
            user = input();
        }
        if(isUserExists(user)){
            System.out.println("Username already exists. Please enter a different username.");
            return;
        }
        System.out.println("Enter Password: ");
        String pass = input();
        while (!isStrongPassword(pass)){
            System.out.println("Password is not strong enough. Please enter a strong password: ");
            pass = input();
        }
        String hashedPass = hashPassword(pass);
        saveUserToFile(user, hashedPass);
        System.out.println("Registration Successful! Welcome " + user + "!");
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false; // Length check
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        // Special characters regex
        String specialCharacters = "!@#$%^&*()-+=<>?/{}[]|\\~";

        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(ch)) {
                hasLowercase = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (specialCharacters.contains(String.valueOf(ch))) {
                hasSpecialChar = true;
            }

            // Early exit if all conditions are met
            if (hasUppercase && hasLowercase && hasDigit && hasSpecialChar) {
                return true;
            }
        }

        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }

    public static void chatBot(){
        String host = "http://localhost:11434/";
        OllamaAPI ollamaAPI = new OllamaAPI(host);
        ollamaAPI.setRequestTimeoutSeconds(60);

        System.out.println("ChatBot: Hi! I am chakki, How can I help you today?");
        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine().trim();

            // Exit condition
            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("ChatBot: Goodbye!");
                break;
            }

            // Send user input to the AI model
            OllamaAsyncResultStreamer streamer = ollamaAPI.generateAsync(
                    "qwen2.5-coder:7b",
                    userInput,
                    false
            );

            // Poll interval for receiving tokens
            int pollIntervalMilliseconds = 100;

            System.out.print("ChatBot: ");
            while (true) {
                String tokens = streamer.getStream().poll();
                if (tokens != null) {
                    System.out.print(tokens);
                }
                if (!streamer.isAlive()) {
                    break;
                }
                try {
                    Thread.sleep(pollIntervalMilliseconds);
                } catch (InterruptedException e) {
                    System.out.println("Error during wait: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println(); // Move to the next line after completing the response
        }
    }

    public static boolean isUserExists(String username) {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            return false; // No users registered yet
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts[0].equals(username)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user file: " + e.getMessage());
        }
        return false;
    }

    public static void saveUserToFile(String username, String hashedPassword) {
        File file =new File(USER_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE, true))) {
            writer.write(username + ":" + hashedPassword);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error saving user to file: " + e.getMessage());
        }
    }

    public static boolean authenticateUser(String username, String hashedPassword) {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            return false; // No users registered yet
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts[0].equals(username) && parts[1].equals(hashedPassword)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user file: " + e.getMessage());
        }
        return false;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}