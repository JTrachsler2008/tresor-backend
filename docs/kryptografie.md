# Kryptografie – Essenz der Umsetzung

## 1. Passwort hashen und prufen

### Registrierung

Beim Registrieren wird das Passwort **nie im Klartext** gespeichert.
Der `PasswordEncryptService` hangt zuerst den **Pepper** ans Passwort an,
dann verschlusselt BCrypt es mit einem zufälligen **Salt** (intern in BCrypt enthalten).

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant UserController
    participant PasswordEncryptService
    participant DB

    User->>Frontend: Gibt email + passwort ein
    Frontend->>UserController: POST /api/users {firstName, lastName, email, password}
    UserController->>UserController: Input-Validierung (BindingResult)
    UserController->>PasswordEncryptService: hashPassword(rawPassword)
    PasswordEncryptService->>PasswordEncryptService: rawPassword + pepper zusammenfugen
    PasswordEncryptService->>PasswordEncryptService: BCrypt.encode(rawPassword + pepper)<br/>BCrypt generiert intern Salt automatisch
    PasswordEncryptService-->>UserController: bcryptHash (z.B. $2a$10$...)
    UserController->>DB: INSERT user (email, bcryptHash)
    DB-->>UserController: User gespeichert
    UserController-->>Frontend: 202 "User saved"
    Frontend-->>User: Registrierung erfolgreich
```

### Login

Beim Login wird das eingegebene Passwort **erneut gehasht** und mit dem gespeicherten Hash verglichen.
BCrypt.matches() extrahiert den Salt aus dem gespeicherten Hash und pruft intern.

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant UserController
    participant PasswordEncryptService
    participant DB

    User->>Frontend: Gibt email + passwort ein
    Frontend->>UserController: POST /api/users/login {email, password}
    UserController->>DB: findByEmail(email)
    DB-->>UserController: User mit gespeichertem bcryptHash
    UserController->>PasswordEncryptService: checkPassword(rawPassword, bcryptHash)
    PasswordEncryptService->>PasswordEncryptService: rawPassword + pepper zusammenfugen
    PasswordEncryptService->>PasswordEncryptService: BCrypt.matches(rawPassword + pepper, bcryptHash)<br/>Salt wird aus bcryptHash extrahiert
    alt Passwort korrekt
        PasswordEncryptService-->>UserController: true
        UserController-->>Frontend: 200 LoginResponse {userId}
        Frontend-->>User: Login erfolgreich
    else Passwort falsch
        PasswordEncryptService-->>UserController: false
        UserController-->>Frontend: 400 "Invalid email or password"
        Frontend-->>User: Fehlermeldung (keine Info ob User existiert)
    end
```

---

## 2. Secret verschlusseln und entschlusseln

### Secret speichern (encrypt)

Der `EncryptUtil` verwendet **AES-256** via jasypt.
Als Schlussel dient das **Login-Passwort des Users** – dadurch ist der Schlussel pro User individuell.
Der verschlusselte Inhalt wird als Base64-String in der DB gespeichert.

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant SecretController
    participant EncryptUtil
    participant SecretService
    participant DB

    User->>Frontend: Fullt Secret-Formular aus (z.B. Credential)
    Frontend->>SecretController: POST /api/secrets {email, encryptPassword, content}
    Note right of Frontend: encryptPassword = Login-Passwort des Users
    SecretController->>SecretController: Input-Validierung (BindingResult)
    SecretController->>SecretController: findByEmail → User-ID ermitteln
    SecretController->>EncryptUtil: new EncryptUtil(encryptPassword)
    EncryptUtil->>EncryptUtil: AES256TextEncryptor mit Passwort initialisieren
    SecretController->>EncryptUtil: encrypt(content.toString())
    EncryptUtil->>EncryptUtil: AES-256 verschlusseln + Base64 kodieren<br/>inkl. zufälligem Salt + IV (jasypt intern)
    EncryptUtil-->>SecretController: encryptedContent (Base64-String)
    SecretController->>SecretService: createSecret(userId, encryptedContent)
    SecretService->>DB: INSERT secret (user_id, encryptedContent)
    DB-->>SecretController: Secret gespeichert
    SecretController-->>Frontend: 202 "Secret saved"
    Frontend-->>User: Secret gespeichert
```

### Secrets lesen (decrypt)

Beim Laden werden alle Secrets des Users einzeln entschlusselt.
Nur wer das korrekte Passwort kennt, kann den Klartext sehen.

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant SecretController
    participant EncryptUtil
    participant SecretService
    participant DB

    User->>Frontend: Öffnet "Meine Secrets"
    Frontend->>SecretController: POST /api/secrets/byemail {email, encryptPassword}
    SecretController->>SecretController: findByEmail → User-ID ermitteln
    SecretController->>SecretService: getSecretsByUserId(userId)
    SecretService->>DB: SELECT * FROM secret WHERE user_id = ?
    DB-->>SecretService: Liste verschlusselter Secrets
    SecretService-->>SecretController: List<Secret> (verschlusselt)

    loop fur jedes Secret
        SecretController->>EncryptUtil: new EncryptUtil(encryptPassword)
        SecretController->>EncryptUtil: decrypt(secret.content)
        alt Entschlusselung erfolgreich
            EncryptUtil-->>SecretController: Klartext-JSON
            SecretController->>SecretController: secret.setContent(Klartext)
        else Falsches Passwort
            EncryptUtil-->>SecretController: EncryptionOperationNotPossibleException
            SecretController->>SecretController: secret.setContent("not encryptable. Wrong password?")
        end
    end

    SecretController-->>Frontend: 200 List<Secret> (entschlusselt)
    Frontend-->>User: Secrets typ-abhangig anzeigen (Credential / Kreditkarte / Notiz)
```

---

## Zusammenfassung Schlussel-Konzept

| | Passwort-Hash | Secret-Verschlusselung |
|---|---|---|
| **Algorithmus** | BCrypt | AES-256 (jasypt) |
| **Salt** | BCrypt-intern (zufällig) | jasypt-intern (zufällig) |
| **Pepper** | Ja, aus `application.properties` | Nein |
| **Schlussel pro User** | Ja (Salt im Hash enthalten) | Ja (Login-Passwort = Schlussel) |
| **Umkehrbar** | Nein (One-way Hash) | Ja (symmetrisch) |
| **Gespeichert in DB** | BCrypt-Hash | Base64-verschlusselter String |
