# 🌐 Külső Hálózati Hozzáférés Beállítása

## Áttekintés

Ez az útmutató segít beállítani az otthoni SlotMachine szervert úgy, hogy külső hálózatból (internetről) is elérhető legyen.

## ⚠️ FIGYELEM - Biztonsági Megfontolások

**FONTOS**: Egy szerver internethez való csatlakoztatása biztonsági kockázatokkal jár!

### Ajánlott biztonsági intézkedések:
1. **Erős jelszavak**: Minden felhasználói fiókhoz erős jelszót használj
2. **Rendszeres frissítések**: Tartsd naprakészen a szervert
3. **Monitoring**: Figyelj az unusual aktivitásra
4. **Backup**: Rendszeresen mentsd az adatbázist

---

## 🔧 Beállítási Lépések

### 1. Windows Tűzfal Konfiguráció

#### Automatikus beállítás (Adminisztrátori jogosultság szükséges):
```cmd
# Nyisd meg a Command Prompt-ot ADMINISZTRÁTORKÉNT!
netsh advfirewall firewall add rule name="SlotMachine Server" dir=in action=allow protocol=TCP localport=8081
```

#### Kézi beállítás:
1. Nyisd meg a **Windows Defender Firewall with Advanced Security**-t
2. Kattints a **Inbound Rules** → **New Rule...**
3. Válaszd a **Port** → **Next**
4. **TCP** → **Specific local ports** → írd be: `8081`
5. **Allow the connection** → **Next**
6. Jelöld be mind a három profilt (Domain, Private, Public) → **Next**
7. Név: `SlotMachine Server` → **Finish**

### 2. Router Port Forwarding Beállítása

#### Lépések:
1. **Router admin felület elérése**:
   - Böngészőben menj a router IP címére (általában `192.168.1.1` vagy `192.168.0.1`)
   - Jelentkezz be admin felhasználóval

2. **Port Forwarding beállítása**:
   - Keresd a **Port Forwarding** vagy **Virtual Servers** menüt
   - Adj hozzá új szabályt:
     - **Service Name**: SlotMachine Server
    - **External Port**: 8081
    - **Internal Port**: 8081
     - **Internal IP**: [A számítógéped helyi IP címe]
     - **Protocol**: TCP

3. **Helyi IP cím megtalálása**:
   ```cmd
   ipconfig
   ```
   Keresd az **IPv4 Address** értéket (pl. `192.168.1.100`)

#### Gyakori router márkák admin felülete:
- **TP-Link**: Advanced → NAT Forwarding → Port Forwarding
- **ASUS**: Advanced Settings → WAN → Port Forwarding
- **Netgear**: Advanced → Port Forwarding/Port Triggering
- **D-Link**: Advanced → Port Forwarding
- **Linksys**: Smart Wi-Fi Tools → Port Forwarding

### 3. Nyilvános IP Cím Meghatározása

#### Aktuális nyilvános IP lekérése:
- Menj a https://whatismyipaddress.com/ oldalra
- Vagy használd a parancsot: `nslookup myip.opendns.com resolver1.opendns.com`

#### Dinamikus IP probléma megoldása:

**Opció A: Dinamikus DNS (DDNS)**
1. Regisztrálj egy ingyenes DDNS szolgáltatónál:
   - No-IP (https://www.noip.com/)
   - DuckDNS (https://www.duckdns.org/)
   - DynDNS (https://dyn.com/)

2. Állítsd be a router DDNS beállításait
3. Használd a DDNS domain nevet IP cím helyett

**Opció B: Statikus IP (fizetős)**
- Kérj statikus IP címet az internetszolgáltatótól

### 4. Szerver Tesztelése

#### Belső hálózatról:
```
http://[HELYI_IP]:8081/api/auth/login
Példa: http://192.168.1.100:8081/api/auth/login
```

#### Külső hálózatról:
```
http://[NYILVANOS_IP]:8081/api/auth/login
Példa: http://123.45.67.89:8081/api/auth/login

# Vagy DDNS esetén:
http://your-domain.ddns.net:8081/api/auth/login
```

### 5. Kliens Konfiguráció

#### Belső hálózatról csatlakozók:
- Szerver cím: `http://192.168.1.100:8081` (helyi IP)

#### Külső hálózatról csatlakozók:
- Szerver cím: `http://123.45.67.89:8081` (nyilvános IP)
- Vagy: `http://your-domain.ddns.net:8081` (DDNS)

---

## 🔍 Hibaelhárítás

### Gyakori problémák:

#### 1. "Connection refused" hiba
- ✅ Ellenőrizd, hogy a szerver fut-e
- ✅ Ellenőrizd a Windows tűzfal beállításait
- ✅ Ellenőrizd a router port forwarding beállításait

#### 2. Belső hálózatról működik, külsőről nem
- ✅ Router port forwarding nincs beállítva
- ✅ ISP blokkolja a portot
- ✅ Nyilvános IP cím változott (dinamikus IP)

#### 3. Időnként nem működik
- ✅ Dinamikus IP változott
- ✅ Router újraindult és elvesztette a beállításokat
- ✅ DDNS nem frissült

#### 4. Lassú kapcsolat
- ✅ Internet feltöltési sebesség korlátozott
- ✅ Túl sok egyidejű kapcsolat

### Tesztelési parancsok:

```cmd
# Port elérhetőség tesztelése
telnet [IP_CIM] 8081

# Hálózati kapcsolat tesztelése
ping [IP_CIM]

# Port figyelés ellenőrzése
netstat -an | find "8081"
```

---

## 📋 Ellenőrzési Lista

Mielőtt külső hozzáférést engedélyeznél:

- [ ] **Szerver biztonsága**: Erős jelszavak, frissítések
- [ ] **Windows tűzfal**: 8081-es port engedélyezve
- [ ] **Router port forwarding**: 8081 → belső IP
- [ ] **Nyilvános IP**: Ismert és elérhető
- [ ] **DDNS beállítva** (ha dinamikus IP)
- [ ] **Tesztelés**: Belső és külső hálózatról is
- [ ] **Backup**: Adatbázis mentése
- [ ] **Monitoring**: Log fájlok figyelése

---

## 🛡️ Biztonsági Tippek

1. **Ne használj alapértelmezett portot**: Változtasd meg a 8081-et másra (pl. 18081)
2. **VPN használata**: Fontold meg VPN szerver beállítását
3. **Fail2Ban**: Automatikus IP blokkolás túl sok sikertelen bejelentkezés után
4. **SSL/HTTPS**: Éles környezetben használj HTTPS-t
5. **Rendszeres audit**: Ellenőrizd a log fájlokat

---

## 📞 További Segítség

Ha problémába ütközöl:

1. **Router dokumentáció**: Keresd meg a router márkájának port forwarding útmutatóját
2. **ISP támogatás**: Kérdezd meg az internetszolgáltatót a port korlátozásokról
3. **Online port checker**: Használj online port scanning eszközöket a teszteléshez

**Hasznos eszközök**:
- Port Checker: https://www.portchecker.co/
- Can You See Me: https://canyouseeme.org/
- Router Port Forwarding útmutatók: https://portforward.com/
