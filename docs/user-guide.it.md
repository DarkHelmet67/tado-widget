# Guida rapida

*tado° Widget* mostra la temperatura di una zona tado° sulla schermata Home di Android. È non ufficiale e di sola lettura: non modifica mai il riscaldamento. Richiede Android 8.0 o successivo e un account tado°. Sono supportati italiano e inglese: l'app segue la lingua del telefono (italiano sui dispositivi italiani, inglese altrimenti).

## 1. Aggiungi il widget

1. Installa l'app (pagina [Releases](https://github.com/DarkHelmet67/tado-widget/releases), oppure Google Play quando sarà pubblicata).
2. Tieni premuto su uno spazio vuoto della Home, scegli **Widget**, trova **tado° Widget** e trascinalo sulla schermata. È largo 4 celle e si può ridimensionare.

## 2. Accedi

1. Si apre la schermata delle impostazioni. Tocca **Accedi con tado°**.
2. Il browser apre la pagina di accesso di tado°, che chiede un **codice utente**: è già compilato (l'app mostra lo stesso codice). Premi **Invia**, accedi con il tuo account tado° e approva.
3. Torna all'app. Prosegue da sola; se no, tocca **Ho approvato, continua**.

La password viene digitata solo sul sito di tado°. L'app conserva sul telefono un token cifrato.

## 3. Scegli zona e frequenza

- **Zona da mostrare**: la stanza o zona della tua casa tado°.
- **Frequenza di aggiornamento**: manuale, oppure ogni 30 minuti, 1, 2 o 4 ore. tado° consente agli account gratuiti circa 100 richieste al giorno e ogni aggiornamento ne usa una: si consiglia **ogni ora**.

Premi **Salva**. Il widget compare e si aggiorna.

## 4. Usare il widget

| Cosa vedi | Significato |
|---|---|
| Numero grande | Temperatura attuale della stanza |
| Numero più piccolo (a destra) | Temperatura impostata (vuoto se la zona è spenta) |
| Icona fiamma | La zona sta riscaldando |
| Icona a sinistra | Modalità: casa = programma, fuori casa, mano = manuale, standby = spento |
| Testo in basso | Ora dell'ultimo aggiornamento, o uno stato come "Aggiornamento…" |

Il **colore di sfondo** segue lo stato della tua zona tado°:

| Colore | Stato |
|---|---|
| Ambra / giallo | Attivo: la zona segue il programma (a casa) |
| Verde | Fuori casa |
| Grigio | Controllo manuale |
| Scuro / nero | Spento, oppure il widget non è ancora configurato o collegato |

Il riscaldamento non è un colore: mentre la zona sta riscaldando compare l'**icona della fiamma** accanto alla temperatura impostata.

- **Tocca il widget** per aggiornare subito.
- **Frecce** (bordo sinistro / destro) per passare alla pagina 2: **umidità** e **potenza del riscaldamento**.
- **Tocca l'icona della modalità** per aprire le impostazioni (cambia zona o frequenza, esci).

## 5. Risoluzione dei problemi

| Messaggio | Cosa fare |
|---|---|
| **Tocca per accedere** | L'accesso è scaduto o revocato. Tocca il widget e accedi di nuovo. |
| **Aggiornamento non riuscito** | Di solito Android blocca la rete in background per l'app. Apri l'app, premi **Consenti accesso in background**, poi imposta dati mobili e uso della batteria dell'app su *senza restrizioni*. Alcuni telefoni hanno anche un risparmio batteria del produttore che deve consentire l'app. |
| **Limite giornaliero tado° raggiunto** | tado° consente circa 100 richieste al giorno sugli account gratuiti. Usa una frequenza più lunga; il widget mantiene gli ultimi valori e si riprende da solo. |
| I valori sembrano vecchi | Tocca il widget per aggiornare, poi controlla la frequenza nelle impostazioni. |

Se qualcosa ancora non funziona, segnalalo aprendo una [issue su GitHub](https://github.com/DarkHelmet67/tado-widget/issues) e allega un log di debug:

1. Apri le impostazioni e premi **Condividi log**, poi salva il file (ad esempio in File o Drive).
2. Nella nuova issue trascina il file nella descrizione per allegarlo.

Usa **Condividi log solo per questo**: non inviare i log via email all'autore. Una issue è pubblica, quindi leggi il log prima di allegarlo: contiene percorsi delle richieste, orari e i numeri della tua casa e zona tado°, ma mai la password o i token.

## 6. Esci e disinstalla

Impostazioni > **Esci** rimuove token e impostazioni dal telefono. Disinstallando si rimuove tutto. Puoi anche revocare l'accesso dal tuo account tado°.
