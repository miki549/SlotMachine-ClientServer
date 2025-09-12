# SlotMachine - Helyi Szerver Javítások

## Probléma
Az eredeti kódban a kliens nem tudott megfelelően kapcsolódni a helyi szerverre (localhost), mert:
1. A kliens alapértelmezetten a külső szerverre (`46.139.211.149:8080`) próbált kapcsolódni
2. A kapcsolat tesztelés nem működött megfelelően autentikáció nélkül
3. Nem volt automatikus localhost észlelés

## Megoldás

### 1. Szerver oldali változások
- **Új health endpoint**: `/api/auth/health` - autentikáció nélkül elérhető endpoint a kapcsolat teszteléshez
- **Fájl**: `src/main/java/com/example/slotmachine/server/controller/AuthController.java`

### 2. Kliens oldali változások

#### ApiClient.java javítások:
- **Automatikus localhost észlelés**: A kliens először megpróbál localhost-ra kapcsolódni
- **Javított kapcsolat teszt**: A `isConnected()` metódus most a `/auth/health` endpoint-ot használja
- **Fallback mechanizmus**: Ha a localhost nem elérhető, automatikusan a külső szerverre vált

#### ServerConfigDialog.java javítások:
- **Intelligens alapértelmezett URL**: Ha localhost szerver fut, azt ajánlja fel alapértelmezettként
- **Frissített példa szöveg**: Localhost opció hozzáadva a példákhoz
- **Javított kapcsolat teszt**: Működik autentikáció nélkül is

### 3. Új batch fájlok

#### `client-local.bat` - Új fájl helyi teszteléshez:
- Ellenőrzi, hogy fut-e a helyi szerver
- Útmutatást ad a szerver indításához
- Specifikusan helyi kapcsolathoz optimalizált

#### `client.bat` - Frissített:
- Megjegyzés a helyi tesztelés opciójáról
- Tisztább leírás a külső szerver használatról

#### `check-network.bat` - Frissített:
- Helyi szerver teszt hozzáadva
- Localhost:8080 elérhetőség ellenőrzése
- Átszámozott szekciók

## Használat

### Helyi szerver és kliens tesztelése:

1. **Szerver indítása**:
   ```bash
   server.bat
   ```

2. **Kliens indítása helyi szerverhez**:
   ```bash
   client-local.bat
   ```

3. **Vagy használd az automatikus észlelést**:
   ```bash
   client.bat
   ```
   (A kliens automatikusan észleli és localhost-ra kapcsolódik, ha elérhető)

### Hálózati diagnosztika:
```bash
check-network.bat
```

## Technikai részletek

### Automatikus szerver választás logika:
1. Kliens indításkor megpróbál kapcsolódni `localhost:8080`-ra
2. Ha sikeres, localhost-ot használ
3. Ha sikertelen, külső szerverre (`46.139.211.149:8080`) vált
4. A felhasználó manuálisan is beállíthatja a szerver címét

### Health endpoint:
- **URL**: `/api/auth/health`
- **Metódus**: GET
- **Autentikáció**: Nem szükséges
- **Válasz**: `200 OK` - "Server is running"

### Kapcsolat teszt javítások:
- Már nem igényel érvényes JWT token-t
- 15 másodperces timeout
- Egyszerű HTTP 200 státusz ellenőrzés

## Kompatibilitás
- **Külső elérés**: Továbbra is működik
- **Meglévő konfigurációk**: Nem változnak
- **Automatikus fallback**: Ha localhost nem elérhető, külső szerverre vált

## Tesztelési forgatókönyvek

### 1. Csak helyi szerver fut:
- Kliens automatikusan localhost-ra kapcsolódik ✅

### 2. Csak külső szerver fut:
- Kliens automatikusan külső szerverre kapcsolódik ✅

### 3. Mindkét szerver fut:
- Kliens előnyben részesíti a localhost-ot ✅

### 4. Egyik szerver sem fut:
- Kliens hibaüzenetet jelez ✅

## Hibaelhárítás

### Ha a helyi kapcsolat nem működik:
1. Ellenőrizd, hogy fut-e a szerver: `check-network.bat`
2. Firewall beállítások: `setup-firewall.bat`
3. Port foglaltság: `netstat -an | find "8080"`

### Ha a külső kapcsolat nem működik:
1. Internet kapcsolat ellenőrzése
2. Router port forwarding beállítások
3. Külső IP cím ellenőrzése

## Változások összefoglalása
- ✅ Automatikus localhost észlelés
- ✅ Javított kapcsolat teszt (autentikáció nélkül)
- ✅ Új health endpoint a szerveren
- ✅ Helyi tesztelés batch fájl
- ✅ Frissített hálózati diagnosztika
- ✅ Külső elérés továbbra is működik
