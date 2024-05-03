package com.kvstore.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.System.exit;

public class Main {

    public static void main(String[] args) {
        try {
            Database db = new Database();  // Initialize your database
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Welcome to KeyValue Store. Type 'exit' to quit.");

            while (true) {
                System.out.print("Enter command: ");
                String input = reader.readLine();
                if (input.trim().equalsIgnoreCase("exit")) {
                    break;  // Exit the loop and end the program
                }

                String[] commands = input.split(" ");
                if (commands.length == 0) {
                    continue;
                }

                String command = commands[0];
                switch (command.toLowerCase()) {
                    case "put":
                        if (commands.length < 3) {
                            System.out.println("Error: Missing key/value for put operation.");
                        } else {
                            db.put(commands[1], commands[2]);
                            System.out.println("Entry added/updated successfully.");
                        }
                        break;
                    case "get":
                        if (commands.length < 2) {
                            System.out.println("Error: Missing key for get operation.");
                        } else {
                            String value = db.get(commands[1]);
                            if (value != null) {
                                System.out.println("Value: " + value);
                            } else {
                                System.out.println("Entry not found.");
                            }
                        }
                        break;
                    case "delete":
                        if (commands.length < 2) {
                            System.out.println("Error: Missing key for delete operation.");
                        } else {
                            db.delete(commands[1]);
                            System.out.println("Entry deleted successfully.");
                        }
                        break;
                    case "exit":
                        exit(0);
                    default:
                        showUsage();
                }
            }
        } catch (IOException e) {
            System.out.println("Database error: " + e.getMessage());
        } finally {
            System.out.println("Exiting KeyValue Store.");
        }
    }

    private static void showUsage() {
        System.out.println("Usage: java Main <command> [key] [value]");
        System.out.println("Commands:");
        System.out.println("  put <key> <value>   - Adds or updates an entry");
        System.out.println("  get <key>          - Retrieves an entry");
        System.out.println("  delete <key>       - Deletes an entry");
    }
}
