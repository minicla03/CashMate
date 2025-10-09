# 💰 CashMate – Gestione Spese Personale

**CashMate** è un'applicazione per monitorare, classificare e analizzare le spese personali, con particolare attenzione a rendere il processo semplice, veloce e intelligente.  
Permette di archiviare scontrini, estrarre automaticamente dati tramite OCR, categorizzare le spese e visualizzare statistiche dettagliate.

---

## 🎯 Obiettivo

CashMate aiuta gli utenti a:
- Tenere traccia di tutte le spese quotidiane.
- Automatizzare l'estrazione di informazioni da scontrini fotografati.
- Analizzare l'andamento delle spese con dashboard e grafici intuitivi.
- Monitorare categorie e previsioni di budget.

---

## 🏗 Architettura


---

## ⚙️ Funzionalità principali

### Android App
- Inserimento spese manuale.
- Upload scontrini fotografici → elaborazione OCR lato backend.
- Dashboard e grafici:
  - Totale mensile
  - Percentuali per categoria
  - Trend delle spese nel tempo
- Filtri e ricerca: per data, categoria o importo.

### Backend Python
- API RESTful per inserimento e lettura spese.
- OCR + Parsing: estrazione automatica di importo, data e descrizione da foto scontrini.
- Classificazione spese:
  - Basata su regole keyword o piccolo modello ML opzionale.
- Database remoto MongoDB Atlas: memorizzazione sicura e accessibile da qualsiasi client.

### Database
- MongoDB Atlas: collezione `expenses` con documenti JSON.
- Campi principali: `date`, `amount`, `category`, `description`, `ocr_text`, `photo_url`, `user_id` (opzionale).

---

## 🛠 Stack Tecnologico

| Livello | Tecnologia |
|---------|------------|
| Frontend | Kotlin, Jetpack Compose |
| Backend | Python, FastAPI |
| OCR | Tesseract / ML Kit |
| Database | MongoDB  |
| Networking | Retrofit (Android), REST API (FastAPI) |
| ML (opzionale) | Scikit-learn / TensorFlow Lite |

---

## 🚀 Setup rapido

### Backend
1. Clona il repository:  
```bash
git clone https://github.com/username/cashmate.git
cd cashmate/backend
```
2. Installa dipendenze:
```bash
pip install -r requirements.txt
```

3. Configura MongoDB Atlas (MONGO_URI nell’.env).

4. Avvia FastAPI:
```bash
uvicorn main:app --reload
```

### Android
1. Apri il progetto in Android Studio.
2. Configura l’endpoint REST del backend.
3. Avvia l’app su emulator o device reale.

---

## 📊 Possibili sviluppi futuri
- Multiutente con autenticazione JWT.
- Previsioni budget mensile tramite ML.
- Backup e sincronizzazione cloud dei dati.
- Chatbot interno per query vocali o testuali (“quanto ho speso in libri questo mese?”).
- Dashboard web interattiva (Streamlit / Plotly Dash).

---
CashMate non è solo un’app per registrare spese: è uno strumento intelligente che trasforma i dati quotidiani in informazioni utili.
Grazie all’integrazione tra OCR, backend Python e interfaccia Android moderna, l’utente può monitorare facilmente le proprie finanze, analizzare i trend di spesa e prendere decisioni consapevoli sul budget.

Il progetto unisce praticità, tecnologia e scalabilità, offrendo una base solida sia per l’uso personale sia per possibili sviluppi futuri, come multiutenza, analisi predittive e dashboard avanzate.

---
## 📬 Contatti

- **Email:** mininnoclaudio@gmail.com  
- **GitHub:** [@minicla03](https://github.com/minicla03)
