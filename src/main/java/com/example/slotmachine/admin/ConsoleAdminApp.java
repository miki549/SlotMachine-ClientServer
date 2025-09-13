package com.example.slotmachine.admin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class ConsoleAdminApp {
    private static final String BASE_URL = "http://localhost:8081/api/admin";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("===========================================");
        System.out.println("    SLOT MACHINE ADMIN CONSOLE");
        System.out.println("===========================================");
        System.out.println();

        while (true) {
            System.out.println("1. Felhasználók listázása");
            System.out.println("2. Kredit hozzáadása");
            System.out.println("3. Balance beállítása");
            System.out.println("4. Felhasználó tiltása/engedélyezése");
            System.out.println("5. Felhasználó átnevezése");
            System.out.println("6. Felhasználó törlése");
            System.out.println("7. Felhasználó tranzakciói");
            System.out.println("8. Tranzakciók cleanup (1000 limit)");
            System.out.println("9. Kilépés");
            System.out.print("Válassz opciót (1-9): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    listUsers();
                    break;
                case "2":
                    addCredits(scanner);
                    break;
                case "3":
                    setBalance(scanner);
                    break;
                case "4":
                    toggleUserStatus(scanner);
                    break;
                case "5":
                    renameUser(scanner);
                    break;
                case "6":
                    deleteUser(scanner);
                    break;
                case "7":
                    getUserTransactions(scanner);
                    break;
                case "8":
                    cleanupTransactions();
                    break;
                case "9":
                    System.out.println("Kilépés...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Érvénytelen opció!");
                    break;
            }
            System.out.println();
        }
    }

    private static void addCredits(Scanner scanner) {
        String username = selectUser(scanner);
        if (username == null) {
            return;
        }

        System.out.print("Kredit összeg: ");
        String amountStr = scanner.nextLine().trim();

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                System.out.println("Hiba: A kredit összegnek pozitívnak kell lennie!");
                return;
            }

            // JSON építése locale-független módon
            String jsonBody = "{\"username\":\"" + username + "\",\"amount\":" + amount + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/add-credits"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Kredit sikeresen hozzáadva!");
                System.out.println("   Felhasználó: " + username);
                System.out.println("   Összeg: $" + (int)amount);
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (NumberFormatException e) {
            System.out.println("Hiba: Érvénytelen szám formátum!");
        } catch (IOException | InterruptedException e) {
            System.out.println("Hiba: Nem lehet kapcsolódni a szerverhez!");
            System.out.println("Ellenőrizd, hogy a szerver fut-e a 8081-es porton.");
            System.out.println("Részletek: " + e.getMessage());
        }
    }

    private static void listUsers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/users"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("=== FELHASZNÁLÓK ===");
                String responseBody = response.body();
                
                // Egyszerű JSON parsing (a teljes Jackson helyett)
                if (responseBody.contains("[]")) {
                    System.out.println("Nincsenek regisztrált felhasználók.");
                } else {
                    // Alapvető felhasználó információk kinyerése
                    String[] users = responseBody.split("\\},\\{");
                    for (int i = 0; i < users.length; i++) {
                        String user = users[i].replace("[{", "").replace("}]", "").replace("\"", "");
                        String[] fields = user.split(",");
                        
                        String username = "", balance = "", active = "";
                        for (String field : fields) {
                            if (field.contains("username:")) {
                                username = field.split(":")[1];
                            } else if (field.contains("balance:")) {
                                balance = field.split(":")[1];
                            } else if (field.contains("active:")) {
                                active = field.split(":")[1];
                            }
                        }
                        
                        String status = "true".equals(active) ? "✅ Aktív" : "❌ Tiltott";
                        System.out.println((i+1) + ". " + username + " - $" + balance + " - " + status);
                    }
                }
            } else {
                System.out.println("❌ Hiba a felhasználók lekérésekor: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

    private static String selectUser(Scanner scanner) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/users"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                
                if (responseBody.contains("[]")) {
                    System.out.println("Nincsenek regisztrált felhasználók.");
                    return null;
                } else {
                    System.out.println("=== FELHASZNÁLÓ KIVÁLASZTÁS ===");
                    String[] users = responseBody.split("\\},\\{");
                    String[] usernames = new String[users.length];
                    
                    for (int i = 0; i < users.length; i++) {
                        String user = users[i].replace("[{", "").replace("}]", "").replace("\"", "");
                        String[] fields = user.split(",");
                        
                        String username = "", balance = "", active = "";
                        for (String field : fields) {
                            if (field.contains("username:")) {
                                username = field.split(":")[1];
                            } else if (field.contains("balance:")) {
                                balance = field.split(":")[1];
                            } else if (field.contains("active:")) {
                                active = field.split(":")[1];
                            }
                        }
                        
                        usernames[i] = username;
                        String status = "true".equals(active) ? "✅ Aktív" : "❌ Tiltott";
                        System.out.println((i+1) + ". " + username + " - $" + balance + " - " + status);
                    }
                    
                    System.out.print("Válassz felhasználót (sorszám): ");
                    String input = scanner.nextLine().trim();
                    
                    try {
                        int index = Integer.parseInt(input) - 1;
                        if (index >= 0 && index < usernames.length) {
                            return usernames[index];
                        } else {
                            System.out.println("❌ Hiba: Érvénytelen sorszám!");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Hiba: Kérlek számot adj meg!");
                        return null;
                    }
                }
            } else {
                System.out.println("❌ Hiba a felhasználók lekérésekor: " + response.body());
                return null;
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
            return null;
        }
    }

    private static void setBalance(Scanner scanner) {
        String username = selectUser(scanner);
        if (username == null) {
            return;
        }

        System.out.print("Új balance: ");
        String balanceStr = scanner.nextLine().trim();

        try {
            double balance = Double.parseDouble(balanceStr);
            if (balance < 0) {
                System.out.println("Hiba: A balance nem lehet negatív!");
                return;
            }

            String jsonBody = "{\"username\":\"" + username + "\",\"balance\":" + balance + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/user/update-balance"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Balance sikeresen beállítva!");
                System.out.println("   Felhasználó: " + username);
                System.out.println("   Új balance: $" + (int)balance);
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (NumberFormatException e) {
            System.out.println("Hiba: Érvénytelen szám formátum!");
        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

    private static void toggleUserStatus(Scanner scanner) {
        String username = selectUser(scanner);
        if (username == null) {
            return;
        }

        System.out.print("Művelet (1=Tiltás, 2=Engedélyezés): ");
        String action = scanner.nextLine().trim();

        String endpoint = action.equals("1") ? "/user/deactivate" : "/user/activate";
        String actionText = action.equals("1") ? "tiltva" : "engedélyezve";

        try {
            String jsonBody = "{\"username\":\"" + username + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Felhasználó sikeresen " + actionText + "!");
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

    private static void renameUser(Scanner scanner) {
        System.out.println("=== RÉGI FELHASZNÁLÓ KIVÁLASZTÁSA ===");
        String oldUsername = selectUser(scanner);
        if (oldUsername == null) {
            return;
        }

        System.out.print("Új felhasználónév: ");
        String newUsername = scanner.nextLine().trim();

        if (newUsername.isEmpty()) {
            System.out.println("Hiba: Az új felhasználónév nem lehet üres!");
            return;
        }

        try {
            String jsonBody = "{\"oldUsername\":\"" + oldUsername + "\",\"newUsername\":\"" + newUsername + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/user/rename"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Felhasználó sikeresen átnevezve!");
                System.out.println("   " + oldUsername + " → " + newUsername);
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

    private static void deleteUser(Scanner scanner) {
        String username = selectUser(scanner);
        if (username == null) {
            return;
        }

        System.out.print("Biztosan törölni akarod? (igen/nem): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("igen") && !confirm.equals("i")) {
            System.out.println("Törlés megszakítva.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/user/" + username))
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Felhasználó sikeresen törölve!");
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

    private static void getUserTransactions(Scanner scanner) {
        String username = selectUser(scanner);
        if (username == null) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/transactions/" + username))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("=== " + username.toUpperCase() + " TRANZAKCIÓI ===");
                String responseBody = response.body();
                
                if (responseBody.contains("[]") || responseBody.trim().equals("[]")) {
                    System.out.println("Nincsenek tranzakciók.");
                } else {
                    // Parse and display transactions in a readable format
                    parseAndDisplayTransactions(responseBody);
                }
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

    private static void parseAndDisplayTransactions(String jsonResponse) {
        try {
            // Simple JSON parsing for transaction display
            // Remove brackets and split by transaction objects
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("[")) {
                cleanJson = cleanJson.substring(1);
            }
            if (cleanJson.endsWith("]")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 1);
            }
            
            if (cleanJson.trim().isEmpty()) {
                System.out.println("Nincsenek tranzakciók.");
                return;
            }
            
            // Split by transaction objects (simple approach)
            String[] transactions = cleanJson.split("\\},\\{");
            
            System.out.printf("%-5s %-12s %-8s %-12s %-12s %-20s %-20s%n", 
                "ID", "TÍPUS", "ÖSSZEG", "ELŐTTE", "UTÁNA", "DÁTUM", "LEÍRÁS");
            System.out.println("─".repeat(90));
            
            for (int i = 0; i < transactions.length; i++) {
                String transaction = transactions[i];
                
                // Clean up the transaction string
                if (i == 0 && transaction.startsWith("{")) {
                    transaction = transaction.substring(1);
                }
                if (i == transactions.length - 1 && transaction.endsWith("}")) {
                    transaction = transaction.substring(0, transaction.length() - 1);
                }
                if (!transaction.startsWith("{")) {
                    transaction = "{" + transaction;
                }
                if (!transaction.endsWith("}")) {
                    transaction = transaction + "}";
                }
                
                // Extract basic fields using simple string parsing
                String id = extractJsonValue(transaction, "id");
                String type = extractJsonValue(transaction, "type");
                String amount = extractJsonValue(transaction, "amount");
                String balanceBefore = extractJsonValue(transaction, "balanceBefore");
                String balanceAfter = extractJsonValue(transaction, "balanceAfter");
                String createdAt = extractJsonValue(transaction, "createdAt");
                String description = extractJsonValue(transaction, "description");
                
                // Format and display
                System.out.printf("%-5s %-12s %-8s %-12s %-12s %-20s %-20s%n",
                    id.length() > 5 ? id.substring(0, 5) : id,
                    type,
                    formatAmount(amount),
                    formatAmount(balanceBefore),
                    formatAmount(balanceAfter),
                    formatDate(createdAt),
                    description.length() > 20 ? description.substring(0, 17) + "..." : description
                );
            }
            
        } catch (Exception e) {
            System.out.println("Hiba a tranzakciók feldolgozásakor: " + e.getMessage());
            System.out.println("Nyers adat: " + jsonResponse);
        }
    }
    
    private static String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return "";
            
            startIndex += searchKey.length();
            
            // Skip whitespace
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            int endIndex;
            if (json.charAt(startIndex) == '"') {
                // String value
                startIndex++; // Skip opening quote
                endIndex = json.indexOf('"', startIndex);
                return json.substring(startIndex, endIndex);
            } else {
                // Number or boolean value
                endIndex = json.indexOf(',', startIndex);
                if (endIndex == -1) {
                    endIndex = json.indexOf('}', startIndex);
                }
                return json.substring(startIndex, endIndex).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String formatAmount(String amount) {
        try {
            double value = Double.parseDouble(amount);
            return String.format("$%.0f", value);
        } catch (Exception e) {
            return amount;
        }
    }
    
    private static String formatDate(String dateStr) {
        // Simple date formatting - just take the first part
        if (dateStr.contains("T")) {
            return dateStr.split("T")[0];
        }
        return dateStr.length() > 20 ? dateStr.substring(0, 20) : dateStr;
    }
    
    private static void cleanupTransactions() {
        System.out.println("=== TRANZAKCIÓK CLEANUP ===");
        System.out.println("Ez a művelet törli a régi tranzakciókat, hogy minden felhasználónál maximum 1000 tranzakció maradjon.");
        System.out.print("Biztosan folytatod? (igen/nem): ");
        
        Scanner scanner = new Scanner(System.in);
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("igen") && !confirm.equals("i")) {
            System.out.println("Cleanup megszakítva.");
            return;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/cleanup-transactions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ " + response.body());
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

}
