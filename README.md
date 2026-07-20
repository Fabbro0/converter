# Mega Converter

App Android nativa (Kotlin + Jetpack Compose) per convertire file tra formati diversi:
immagini, documenti/testo e audio/video.

## Conversioni supportate (MVP)

| Categoria | Conversioni |
|---|---|
| Immagini | JPG ⇄ PNG ⇄ WEBP ⇄ BMP |
| Immagini ⇄ Documenti | Immagine → PDF, PDF → immagine (multi-pagina esportato come .zip) |
| Testo/Documenti | TXT ⇄ PDF, DOCX → TXT, DOCX → PDF |
| Audio/Video | MP3, WAV, AAC, M4A, FLAC, OGG ⇄ tra loro; MP4, MKV, AVI, WEBM, MOV ⇄ tra loro; video → audio (es. MP4 → MP3) |

L'architettura (`ConversionEngine` + `FileConverter`) è pensata per aggiungere nuove
coppie di formati in futuro senza toccare il resto dell'app: basta implementare
`FileConverter` e registrarlo in `ConversionEngine.default()`.

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

## Limiti noti / possibili estensioni future

- DOCX è supportato solo in lettura (→ TXT/PDF), non in scrittura (TXT/PDF → DOCX).
- Gli archivi (ZIP) non sono nell'MVP come categoria di conversione autonoma; lo zip
  viene generato automaticamente solo quando un PDF multi-pagina viene esportato in
  immagini.
- L'icona dell'app (`res/drawable/ic_launcher_*.xml`, `res/mipmap*/ic_launcher*`) è un
  semplice placeholder vettoriale con due frecce; consigliato rigenerarla con l'Image
  Asset Studio di Android Studio prima della pubblicazione.
- Nessun test automatizzato incluso: dato che l'ambiente di sviluppo non può
  eseguire l'emulatore Android, i flussi (selezione file, conversione, condivisione)
  vanno verificati manualmente in Android Studio.
