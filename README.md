# TradeVision AI

**TradeVision AI** è un'applicazione Android con AI integrata, progettata per analizzare grafici del mercato finanziario e fornire previsioni istantanee sui trend (Rialzista/Ribassista).

Il progetto sfrutta un'architettura ibrida: esegue modelli leggeri direttamente sul dispositivo (**Edge AI**) per un'analisi offline istantanea e si appoggia ad un server cloud scalabile (**Cloud AI su AWS**) per inferenze ad alta precisione tramite reti neurali profonde.

## Funzionalità Principali

  * **Acquisizione Immagini:** Scatta foto ai grafici in tempo reale o caricale dalla galleria del dispositivo.
  * **Inferenza Ibrida (Edge & Cloud):** \* **Locale (TFLite):** Esecuzione offline e a latenza zero sul dispositivo.
      * **Cloud (AWS/FastAPI):** Elaborazione ad alta precisione tramite modelli Keras.
  * **Selezione Dinamica del Modello:** Scegli tra *MobileNet V2 (Local)*, *MobileNet V3 Small (Local)*, *Inception V3 (Cloud)* e *MobileNet V2 (Cloud)* direttamente dall'interfaccia.
  * **Cronologia Analisi:** Salvataggio automatico locale delle analisi passate (immagini e previsioni) per una rapida consultazione offline.
  * **Sicurezza API:** Comunicazione tra app e server protetta tramite API Keys crittografate.

## Stack Tecnologico

### Frontend (Android)

  * **Linguaggio:** Kotlin
  * **Librerie Principali:**
      * `TensorFlow Lite` / `LiteRT` (Inferenza Edge)
      * `OkHttp3` (Chiamate API REST sincrone e asincrone)
      * `Coroutines` (Gestione thread in background)
      * `Gson` & `SharedPreferences` (Database locale per la cronologia)
      * `RecyclerView` (UI dinamica per la cronologia)

### Backend (Cloud / AWS)

  * **Linguaggio:** Python 3.10
  * **Framework API:** FastAPI + Uvicorn
  * **Machine Learning:** TensorFlow 2.x / Keras (Multi-model loading)
  * **Infrastruttura:** Docker, Docker Compose, AWS EC2 (Ubuntu)

## Architettura del Backend

Il server Cloud è containerizzato con **Docker** e gestisce il caricamento simultaneo di più modelli in RAM al momento dell'avvio. Utilizza il *Monkey Patching* e il `custom_object_scope` di Keras per garantire la retrocompatibilità con i layer di Data Augmentation e operazioni matematiche personalizzate (`TrueDivide`, `GetItem`) di Keras 3.

**Endpoint Principale:**
`POST /predict?model_id={id}`

  * **Headers:** `X-API-KEY`
  * **Body:** Immagine codificata in `multipart/form-data`
  * **Response:**
    ```json
    {
        "model_used": "inception",
        "prediction": "BUY",
        "confidence": 98.45
    }
    ```


## Guida all'installazione

### 1\. Configurazione Backend (Docker)

1.  Clona il repository sul tuo server.
2.  Inserisci i modelli addestrati (tramite notebook nella cartella `AI_TRAINING`) nella root del backend.
3.  Imposta la tua chiave segreta nel file `docker-compose.yml`:
    ```yaml
    environment:
      - API_KEY_SECRET=API
    ```
4.  Avvia il container:
    ```bash
    docker-compose build --no-cache
    docker-compose up -d
    ```

### 2\. Configurazione Frontend (Android Studio)

1.  Apri il progetto con Android Studio.
2.  Inserisci i tuoi file TFLite nella cartella `app/src/main/assets/`.
3.  Nel file `local.properties`, inserisci i campi richiesti:
```
FINNHUB_API_KEY=""
AWS_API_URL="http://IP:PORT"
AWS_API_KEY=""
```
4.  Compila ed esegui sull'emulatore o su dispositivo fisico.


## Autore

Sviluppato da **Matteo Battilori** come progetto per il corso `Programmazione Mobile`.
