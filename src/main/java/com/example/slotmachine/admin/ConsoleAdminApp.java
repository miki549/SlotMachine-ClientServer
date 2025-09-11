package com.example.slotmachine.admin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class ConsoleAdminApp {
    private static final String BASE_URL = "http://localhost:8080/api/admin";
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
            System.out.println("8. Kilépés");
            System.out.print("Válassz opciót (1-8): ");

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
        System.out.print("Felhasználónév: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Hiba: A felhasználónév nem lehet üres!");
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
            System.out.println("Ellenőrizd, hogy a szerver fut-e a 8080-as porton.");
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

    private static void setBalance(Scanner scanner) {
        System.out.print("Felhasználónév: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Hiba: A felhasználónév nem lehet üres!");
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
        System.out.print("Felhasználónév: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Hiba: A felhasználónév nem lehet üres!");
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
        System.out.print("Jelenlegi felhasználónév: ");
        String oldUsername = scanner.nextLine().trim();

        if (oldUsername.isEmpty()) {
            System.out.println("Hiba: A felhasználónév nem lehet üres!");
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
        System.out.print("Törlendő felhasználónév: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Hiba: A felhasználónév nem lehet üres!");
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
        System.out.print("Felhasználónév: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Hiba: A felhasználónév nem lehet üres!");
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
                
                if (responseBody.contains("[]")) {
                    System.out.println("Nincsenek tranzakciók.");
                } else {
                    System.out.println("Tranzakciók: " + responseBody);
                    // Itt lehetne szebben formázni a JSON-t
                }
            } else {
                System.out.println("❌ Hiba: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Hálózati hiba: " + e.getMessage());
        }
    }

}
