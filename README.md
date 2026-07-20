# Mega Converter

App Android nativa (Kotlin + Jetpack Compose) per convertire file tra formati diversi:
immagini, documenti/testo e audio/video.

## Conversioni supportate

| Categoria | Conversioni |
|---|---|
| Immagini | JPG ⇄ PNG ⇄ WEBP ⇄ BMP; HEIC/GIF → una di queste (solo sorgente, vedi sotto) |
| Immagini ⇄ Documenti | Immagine → PDF, PDF → immagine (multi-pagina esportato come .zip) |
| Testo/Documenti | TXT ⇄ PDF, DOCX → TXT/PDF, EPUB → TXT/PDF, TXT → EPUB, MD → TXT/PDF, HTML → TXT/PDF, RTF → TXT/PDF |
| Fumetti | CBZ ⇄ PDF, immagine → CBZ |
| Fogli di calcolo | CSV ⇄ XLSX |
| Audio/Video | MP3, WAV, AAC, M4A, FLAC, OGG ⇄ tra loro; MP4, MKV, AVI, WEBM, MOV ⇄ tra loro; video → audio (es. MP4 → MP3) |

L'architettura (`ConversionEngine` + `FileConverter`) è pensata per aggiungere nuove
coppie di formati in futuro senza toccare il resto dell'app: basta implementare
`FileConverter` e registrarlo in `ConversionEngine.default()`.

## Strumenti batch (schermata iniziale, pulsante "STRUMENTI ⌄")

Un unico pulsante con menu a tendina, per non riempire la schermata di pulsanti:

- **Unisci più immagini in un PDF** — selezione multipla, una pagina per immagine.
- **Unisci più immagini in un CBZ** — stessa idea ma per fumetti/pagine scannerizzate:
  zippa le immagini in ordine senza ricomprimerle.
- **Unisci più PDF in uno** — usa `PDFMergerUtility` di PdfBox-Android, che copia le
  pagine reali (testo/vettori inclusi) invece di rasterizzarle: qualità identica
  all'originale, non una foto delle pagine.
- **Converti più file insieme** — seleziona N file dello *stesso* formato (es. 10 JPG),
  scegli un formato di destinazione una sola volta, converte tutti; se il risultato
  sono più file li impacchetta in uno .zip per condividerli in un colpo solo. File di
  formati diversi nella stessa selezione non sono supportati: l'app te lo segnala
  chiaramente invece di indovinare cosa fare.

## Lettore integrato e libreria con etichette

Pulsante **"LIBRERIA"** in alto a destra nell'app. Due modi per aggiungere file:

- **"AGGIUNGI FILE"** dentro la libreria stessa — copia direttamente un file dal
  telefono, *senza* passare da nessuna conversione.
- **"SALVA IN LIBRERIA"** su ogni schermata di conversione riuscita.

In entrambi i casi il file viene copiato in uno spazio permanente dell'app (non nella
cache, che il sistema può svuotare in qualsiasi momento).

Nella libreria: elenco dei file salvati, filtro per etichetta (chip in alto), e per
ogni file "ETICHETTE" (dialogo con etichette separate da virgola, libere) ed "ELIMINA".
Toccando un file si apre nel lettore integrato:

- **PDF** → pager pagina per pagina (scorrimento orizzontale), renderizzato al volo con
  `PdfRenderer` (nessun caricamento di tutte le pagine in memoria insieme).
- **CBZ** → stesso pager, leggendo le pagine direttamente dallo zip.
- **EPUB / DOCX / TXT** → testo estratto e scorrevole (stessi estrattori usati per le
  conversioni: `EpubReader`, `DocxReader`).
- **Immagini** → visualizzatore a schermo intero (adattato, senza zoom/pinch per ora).
- **Audio/video/altro** → l'app non prova a "leggerli": mostra un pulsante per aprirli
  con un'altra app installata sul telefono.

Nessuna dipendenza nuova per la libreria: l'indice è un semplice file JSON in
`context.filesDir` (letto/scritto con `org.json`, incluso in Android), non un database
Room — per una lista personale su un solo dispositivo è più che sufficiente, ed evita
di introdurre un plugin di annotation-processing (KSP) che non potevo verificare
compilando qui. La navigazione tra le tre schermate (Convertitore/Libreria/Lettore) è
gestita a mano in `ConverterAppRoot.kt`, incluso il tasto Indietro di sistema — niente
Navigation-Compose, che sarebbe overkill per tre schermate in sequenza lineare.

## Come aprire/compilare il progetto

1. Apri la cartella con Android Studio (Koala o più recente).
2. Lascia sincronizzare Gradle (la prima sync scarica l'Android SDK/build-tools se non già presenti).
3. Esegui su un dispositivo/emulatore con Android 7.0 (API 24) o superiore.

## Nota importante: non compilato/testato in questa sessione

Questo progetto è stato scritto in un ambiente sandbox la cui policy di rete blocca
`dl.google.com` e `maven.google.com` (uniche fonti per Android Gradle Plugin, platform
SDK e build-tools). Di conseguenza **non è stato possibile eseguire una vera build
Gradle né un test su emulatore/dispositivo in questa sessione** — il comando
`gradle tasks` fallisce esattamente sulla risoluzione del plugin `com.android.application`,
confermando che il blocco è di rete e non del codice.

Sono stati fatti controlli statici (struttura del progetto, bilanciamento
parentesi/graffe in tutti i file Kotlin, coerenza delle firme tra `FileConverter` e
le sue implementazioni, coerenza dei nomi di package/API note delle librerie usate),
ma **la prima build reale va fatta in Android Studio sul tuo computer**, dove
l'accesso a `dl.google.com`/`maven.google.com` è normale. Se emergono errori di
compilazione minori (tipicamente firme di API Compose leggermente cambiate tra
versioni), sono localizzati e facili da correggere.

## Librerie chiave e perché

- **PdfBox-Android** (`com.tom-roush:pdfbox-android`) — estrazione testo reale da PDF
  (l'API nativa Android `PdfRenderer` sa solo rasterizzare le pagine in immagini, non
  leggere il testo).
- **ffmpeg-kit** (`com.moizhassan.ffmpeg:ffmpeg-kit-16kb`) — transcodifica audio/video.
  La libreria ufficiale `com.arthenica:ffmpeg-kit` è stata **ritirata e rimossa da
  Maven Central nell'aprile 2025** dall'autore originale. Questo progetto usa un fork
  mantenuto dalla community, pubblicato su Maven Central con lo stesso package Java
  (`com.arthenica.ffmpegkit.*`), quindi è un drop-in replacement. Se in futuro anche
  questo fork dovesse sparire, basta cambiare le coordinate in
  `gradle/libs.versions.toml` (sezione `ffmpeg-kit`) senza toccare il codice Kotlin.
- Nessuna libreria esterna per BMP: Android non ha un encoder BMP nativo
  (`Bitmap.CompressFormat` supporta solo JPEG/PNG/WEBP), quindi `BmpEncoder.kt`
  scrive un BMP 24-bit non compresso a mano.
- Estrazione testo da DOCX: implementata a mano leggendo `word/document.xml`
  dentro lo zip con `XmlPullParser`, per evitare di includere una libreria OOXML
  pesante (Apache POI) solo per estrarre testo semplice. Non gestisce formattazione,
  tabelle o immagini — solo testo dei paragrafi.
- EPUB: stessa filosofia "zero librerie pesanti". In lettura (`EpubReader.kt`) risolve
  `META-INF/container.xml` → OPF → ordine dello spine → testo di ogni capitolo XHTML,
  con uno strip dei tag tramite regex tollerante (non un parser XML rigido) perché
  molti EPUB reali contengono entità HTML tipo `&nbsp;` non valide in XML puro, che
  manderebbero in errore un parser strict. In scrittura (`EpubWriter.kt`) genera un
  EPUB3 minimo ma valido (mimetype non compresso e primo nello zip, container.xml,
  OPF, nav.xhtml, un capitolo), leggibile da Play Books/Apple Books/calibre ecc.
- Unione PDF (`PdfMerge.kt`): usa `com.tom_roush.pdfbox.multipdf.PDFMergerUtility`
  invece di rasterizzare con `PdfRenderer` — preserva il contenuto vettoriale/testo
  reale delle pagine originali.
- CBZ (`CbzConverter.kt`): è solo uno zip di immagini in ordine — CBZ→PDF riusa la
  stessa logica di paginazione di ImagePdfConverter, PDF→CBZ quella di rendering pagine
  già scritta per PDF→immagine. L'ordine delle pagine usa un confronto "naturale"
  (`NaturalSort.kt`) così `pagina2.jpg` viene prima di `pagina10.jpg` anche senza zeri
  iniziali nei nomi file.
- XLSX (`XlsxReader.kt` / `XlsxWriter.kt`): stessa filosofia zip+XML di DOCX/EPUB.
  In lettura risolve `xl/sharedStrings.xml` + `xl/worksheets/sheet1.xml` in una griglia
  di celle; legge solo il primo foglio e le formule solo come valore già calcolato
  (cache), non ricalcolate — copre la stragrande maggioranza dei fogli semplici ma non
  è un motore di calcolo. In scrittura usa stringhe inline (`t="inlineStr"`) per evitare
  di dover generare anche `sharedStrings.xml`.
- Markdown/HTML/RTF (`MarkdownStripper.kt`, `HtmlStripper.kt`, `RtfStripper.kt`): stessa
  filosofia "regex tollerante" già usata per EPUB, per lo stesso motivo — sono formati
  testuali con sintassi/escape spesso non perfettamente standard nei file reali, meglio
  un pipeline di regex best-effort che un parser rigido che si rompe sul primo file
  scritto a mano. RTF in particolare è quello con la struttura più complessa (gruppi
  `{...}` annidati arbitrariamente): la resa è buona su documenti semplici, meno precisa
  su tabelle/oggetti incorporati profondamente annidati.

## Limiti noti / possibili estensioni future

Già implementato: EPUB, CBZ, CSV/XLSX, Markdown, HTML, RTF, unione PDF, conversione
batch generica, lettore integrato, libreria con etichette. Restano fuori scope per ora
(richiederebbero librerie pesanti, servizi esterni, o più tempo di sviluppo/verifica):

- **DOCX** è supportato solo in lettura (→ TXT/PDF), non in scrittura (TXT/PDF → DOCX).
- **CBR** (comic book RAR): richiederebbe una libreria di estrazione RAR (es. junrar,
  licenza non permissiva per tutti gli usi) — non aggiunta senza una decisione esplicita
  sul trade-off, a differenza di CBZ che è "gratis" perché è solo uno zip.
- **TIFF, SVG**: Android non sa decodificarli nativamente (a differenza di HEIC/GIF),
  servirebbe una libreria esterna anche solo per leggerli.
- **HEIC/GIF come formato di *destinazione***: Android li decodifica ma non li
  incodifica (nessun encoder pubblico nell'SDK) — restano solo sorgenti.
- **OCR** (immagine/PDF scannerizzato → testo ricercabile): fattibile con ML Kit di
  Google offline, non incluso.
- **Job lunghi in background**: le conversioni audio/video girano finché l'app resta
  in foreground; su video molto lunghi converrebbe spostarle su `WorkManager` con
  notifica persistente così sopravvivono anche se l'utente esce dall'app.
- **Strumenti PDF avanzati** (dividi, ruota, comprimi, filigrana, password): non inclusi.
- **Lettore**: niente zoom/pinch sulle immagini, niente indice dei capitoli per EPUB
  (mostra tutto il testo in sequenza), niente evidenziazione/segnalibri.
- **Aggregatore di sorgenti esterne** (tipo Mihon, per scaricare contenuti da siti web):
  intenzionalmente non implementato — la maggior parte delle estensioni di quel tipo di
  app punta a fonti non ufficiali/senza licenza. Se servono fonti legali specifiche
  (es. Project Gutenberg, API ufficiali) è un discorso diverso, da valutare caso per caso.
- L'icona dell'app (`res/drawable/ic_launcher_*.xml`, `res/mipmap*/ic_launcher*`) è un
  semplice placeholder vettoriale; consigliato rigenerarla con l'Image Asset Studio
  di Android Studio prima della pubblicazione.
- Nessun test automatizzato incluso: dato che l'ambiente di sviluppo non può
  eseguire l'emulatore Android, i flussi vanno verificati manualmente in Android
  Studio (come già fatto finora per le conversioni singole).
