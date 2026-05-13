package intellisurf;

import java.util.*;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.response.OllamaAsyncResultStreamer;
import io.github.ollama4j.models.request.ThinkMode;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.io.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Main {
    public static String input(){
        return scanner.nextLine();
    }
    static Scanner scanner = new Scanner(System.in);

    // ── Configuration ───────────────────────────────────────────────────
    // All values loaded from config.properties with env-var overrides.
    private static final Properties CONFIG = loadConfig();

    private static String getConfig(String key, String fallback) {
        // Environment variable takes highest priority
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.isEmpty()) {
            return envVal;
        }
        return CONFIG.getProperty(key, fallback);
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                System.out.println("Warning: Could not load config.properties, using defaults.");
            }
        }
        return props;
    }

    // Derived from config (no more hardcoded literals)
    private static final String USER_FILE        = getConfig("USER_FILE",        "users.txt");
    private static final String OLLAMA_HOST      = getConfig("OLLAMA_HOST",      "http://localhost:11434/");
    private static final String OLLAMA_MODEL     = getConfig("OLLAMA_MODEL",     "qwen2.5-coder:7b");
    private static final int    OLLAMA_TIMEOUT   = Integer.parseInt(getConfig("OLLAMA_TIMEOUT", "60"));
    private static final int    POLL_INTERVAL_MS = Integer.parseInt(getConfig("POLL_INTERVAL_MS", "100"));

    // ── PBKDF2 parameters ───────────────────────────────────────────────
    private static final int SALT_LENGTH      = 16;   // 128-bit salt
    private static final int HASH_ITERATIONS  = 65536;
    private static final int HASH_KEY_LENGTH  = 256;   // 256-bit derived key

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
        if (authenticateUser(user, pass)) {
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
        // Generate a random salt and derive the PBKDF2 hash
        byte[] salt = generateSalt();
        String hashedPass = hashPassword(pass, salt);
        String saltHex = bytesToHex(salt);
        saveUserToFile(user, saltHex, hashedPass);
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
        Ollama ollama = new Ollama(OLLAMA_HOST);
        ollama.setRequestTimeoutSeconds(OLLAMA_TIMEOUT);

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
            try {
                OllamaAsyncResultStreamer streamer = ollama.generateAsync(
                        OLLAMA_MODEL,
                        userInput,
                        false,
                        ThinkMode.DISABLED
                );

                System.out.print("ChatBot: ");
                while (true) {
                    String tokens = streamer.getResponseStream().poll();
                    if (tokens != null) {
                        System.out.print(tokens);
                    }
                    if (!streamer.isAlive()) {
                        break;
                    }
                    try {
                        Thread.sleep(POLL_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        System.out.println("Error during wait: " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println(); // Move to the next line after completing the response
            } catch (Exception e) {
                System.out.println("ChatBot: Sorry, I encountered an error: " + e.getMessage());
            }
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
                if (parts.length >= 1 && parts[0].equals(username)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user file: " + e.getMessage());
        }
        return false;
    }

    // Updated format: username:salt:hash
    public static void saveUserToFile(String username, String salt, String hashedPassword) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE, true))) {
            writer.write(username + ":" + salt + ":" + hashedPassword);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error saving user to file: " + e.getMessage());
        }
    }

    // Re-derives the hash from the stored salt to compare
    public static boolean authenticateUser(String username, String plainPassword) {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            return false; // No users registered yet
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3 && parts[0].equals(username)) {
                    String storedSalt = parts[1];
                    String storedHash = parts[2];
                    byte[] salt = hexToBytes(storedSalt);
                    String computedHash = hashPassword(plainPassword, salt);
                    if (computedHash.equals(storedHash)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user file: " + e.getMessage());
        }
        return false;
    }

    // ── Secure password hashing with PBKDF2 + per-user salt ─────────────

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    public static String hashPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    HASH_ITERATIONS,
                    HASH_KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hashBytes = factory.generateSecret(spec).getEncoded();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // ── Hex conversion utilities ────────────────────────────────────────

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}