# Slot Machine - Kliens-Szerver Architektúra

## Áttekintés

A projekt át lett alakítva offline módról kliens-szerver architektúrára. Most a kreditek a szerveren vannak tárolva és kezelve, ami megakadályozza a csalást.

## Architektúra

### Szerver oldal (Spring Boot)
- **REST API**: HTTP endpoints a kliens-szerver kommunikációhoz
- **Adatbázis**: H2 in-memory database (fejlesztéshez)
- **Biztonság**: JWT token alapú authentication
- **Entitások**: User, GameTransaction

### Kliens oldal (JavaFX)
- **Bejelentkezés**: Kötelező bejelentkezés indításkor
- **Hálózati kommunikáció**: REST API hívások
- **Session kezelés**: JWT token tárolás

## Indítás

### 1. Szerver indítása

**FONTOS**: Először fordítsd le a projektet:
```bash
mvn clean compile
```

Majd indítsd el a szervert:
```bash
# Windows
start-server.bat

# Vagy Maven parancs
mvn spring-boot:run -Dspring-boot.run.main-class=com.example.slotmachine.server.SlotMachineServerApplication
```

**Várj, amíg látod ezt az üzenetet:**
```
Started SlotMachineServerApplication in X.XXX seconds
```

### 2. Kliens indítása
```bash
# Eredeti módon
mvn javafx:run

# Vagy IDE-ből: SlotMachineGUI.main()
```

### 🌐 Külső Hozzáférés Beállítása

A szerver most már bárhonnan elérhető! A konfiguráláshoz:

1. **Szerver oldal**: A szerver automatikusan `0.0.0.0:8080`-on indul, így minden hálózati interfészen elérhető
2. **Kliens oldal**: A főmenü "Settings" → "Szerver Beállítások" menüpontjában állíthatod be a szerver címét

**Példa szerver címek:**
- Nyilvános szerver (alapértelmezett): `http://46.139.211.149:8080`
- Helyi hálózat: `http://192.168.1.100:8080`
- Localhost: `http://localhost:8080`

**Hálózati követelmények:**
- A 8080-as port legyen nyitva a szerveren
- Ha tűzfal van, engedélyezd a bejövő kapcsolatokat a 8080-as porton
- Router esetén port forwarding szükséges lehet

### 🏠 Otthoni Szerver Külső Elérhetősége

Ha az otthoni gépeden futtatod a szervert és szeretnéd, hogy külső hálózatból (internetről) is elérjék:

**📋 Részletes útmutató**: Lásd az `EXTERNAL-ACCESS-SETUP.md` fájlt!

**Gyors összefoglaló:**
1. **Windows Tűzfal**: Engedélyezd a 8080-as portot
2. **Router Port Forwarding**: Állítsd be a 8080 → belső IP forwarding-ot
3. **Nyilvános IP**: Használd a nyilvános IP címedet vagy DDNS szolgáltatást
4. **Kliens beállítás**: `http://46.139.211.149:8080` (alapértelmezett)

**⚠️ Biztonsági figyelmeztetés**: Külső hozzáférés biztonsági kockázatokkal jár! Használj erős jelszavakat és fontold meg VPN használatát.

### 3. Admin alkalmazás (kreditek hozzáadásához)
```bash
# Windows
start-admin.bat

# Vagy Maven parancs
mvn javafx:run -Djavafx.mainClass=com.example.slotmachine.admin.AdminApp
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Bejelentkezés
- `POST /api/auth/register` - Regisztráció
- `GET /api/auth/validate` - Token validálás

### Game
- `GET /api/game/balance` - Balance lekérés
- `POST /api/game/spin` - Pörgetés (tét levonás + eredmény)

### Admin
- `POST /api/admin/add-credits` - Kredit hozzáadás
- `GET /api/admin/transactions/{username}` - Felhasználó tranzakcióinak lekérése

## Adatbázis

A fejlesztési környezetben H2 fájl alapú adatbázist használunk (állandó tárolás):
- Adatbázis fájl: `./data/slotmachine.mv.db`
- H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/slotmachine`
- Username: `sa`
- Password: (üres)

**Fontos**: A felhasználók és balance adatok most már megmaradnak a szerver újraindítása után!

## Biztonság

- **JWT tokenek**: 24 órás érvényesség
- **BCrypt**: Jelszó hash-elés
- **Szerver oldali validáció**: Minden pörgetés ellenőrzése
- **Balance védelem**: Csak szerver módosíthatja

## Új funkciók

### 🔄 Real-time Balance Frissítés
- **Automatikus polling**: 5 másodpercenként lekéri a balance-t
- **Live GUI frissítés**: Admin módosítások azonnal megjelennek
- **Background processing**: Nem blokkolja a játékot

### 🛠️ Bővített Admin Funkciók
1. **👥 Felhasználók listázása** - Összes user + balance + státusz
2. **💰 Kredit hozzáadása** - Egyenleg növelése
3. **⚖️ Balance beállítása** - Egyenleg közvetlen módosítása
4. **🚫 Tiltás/Engedélyezés** - Fiók aktiválás/deaktiválás
5. **✏️ Átnevezés** - Username módosítás
6. **🗑️ Törlés** - Fiók törlése (megerősítéssel)
7. **📊 Tranzakciók** - Játéktörténet

## Változások az eredeti kódban

1. **SlotMachine.java**: Online/offline mód + real-time listener
2. **SlotMachineGUI.java**: Bejelentkezés + polling timer + real-time frissítés
3. **Új osztályok**: ApiClient, LoginDialog, szerver komponensek, bővített admin
4. **Eltávolított fájlok**: 
   - CreditLoader.java (credit fájl betöltés)
   - CreditFileGenerator.java (credit fájl generálás) 
   - KeyGenerator.java (RSA kulcs generálás)
   - SlotMachineTester.java (régi tesztelő)
   - hello-view.fxml (használatlan FXML)
   - balance.dat, *.cred fájlok (offline adatok)

## Fejlesztői megjegyzések

- A szerver automatikusan létrehozza az adatbázis táblákat
- Első indításkor regisztrálni kell egy felhasználót
- Az admin alkalmazással lehet krediteket hozzáadni
- A játék működése változatlan maradt, csak a backend változott

## Termelési környezet

Termelésben ajánlott:
- PostgreSQL vagy MySQL adatbázis használata
- HTTPS konfiguráció
- Admin endpoints megfelelő védelem
- Környezeti változók használata konfigurációhoz
