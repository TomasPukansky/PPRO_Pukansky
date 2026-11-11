# CLAUDE.md – Denník a pokyny pre Claude Code (orchestrátor)

Tento súbor si vedie **Claude Code**. Claude tu nepíše kód priamo – **promptuje agenta v Antigravity IDE (Gemini)**,
ktorý sa riadi [AGENTS.md](AGENTS.md) a [README.md](README.md) (SSOT). Claude kontroluje výsledok a vedie dokumentáciu.

## Pokyny od používateľa (Tomáš)

- **2026-10-07**
  - Pracujeme na **Zadaní D – Půjčovna vybavení** (PDF: `PPRO-zadani-detailni-v1.pdf`).
  - Claude **promptuje Antigravity agenta** (otvorená konverzácia v IDE), sám nemá kód písať namiesto neho.
  - Cieľ: **riadne demo ako hrubý nástrel**, ktoré používateľ ukáže klientovi a dostane spätnú väzbu.
  - Claude si vedie tento súbor: čo sa dialo a aké pokyny dostal.
  - Claude musí vedieť pracovať s dokumentáciou projektu:
    `C:\Users\mento\Documents\skola\PPRO\PPRO-dokumentace-sablona-v1.docx`
    (šablóna FIM UHK: 1. Popis řešení, 2. Rozhodnutí – tabuľka R1.., 3. Změny požadované klientem – tabuľka Z1..).
- **2026-10-07 15:49** – Ak sa Gemini znova zasekne (kvóta), **Claude dokončí prácu sám**. Používateľ prepne Antigravity na model Flash.
- **2026-10-07 16:31** – Claude má povolenie schvaľovať v Antigravity žiadosti o povolenie pre testy a základné príkazy (build, mvn test, git status/diff/commit). Nie push, nie eshop, nie mazanie mimo projektu. (Technicky: agentapi nemá approve. Antigravity má v Settings → Agent nastavenie „Terminal auto execution“ (Require Review / Proceed in Sandbox / Always Proceed) + Allowlist príkazov – je to v interných agent preferences, nie v settings.json, takže ho nastavuje používateľ v UI. Navrhnutý allowlist: mvn, docker compose -p ppro-rental, docker run --rm --name ppro-rental-maven, git status, git diff, git log, git add, git commit.)

## Ako Claude komunikuje s Antigravity

- CLI: `C:\Users\mento\.gemini\antigravity-ide\bin\agentapi.bat`
  (`new-conversation`, `send-message <conversation_id> <text>`, `get-conversation-metadata <id>`).
- Potrebuje env premenné `ANTIGRAVITY_LS_ADDRESS` (127.0.0.1:<port>) a `ANTIGRAVITY_CSRF_TOKEN`
  – berú sa z príkazového riadku bežiaceho procesu `language_server_windows_x64.exe` s `--workspace_id ...AI_is_the_way`
  (`--csrf_token`, port = ten listening port, ktorý vráti „missing CSRF token“). Pri reštarte IDE sa menia.
- Konverzácia projektu: `3a5569dd-e800-4156-9dde-daec22eb8d71`.
- Prompty pre Gemini píšem **po česky** (AGENTS.md to vyžaduje).

## Pravidlá (prevzaté z AGENTS.md)

- **Docker: projekt eshop (kontajnery eshop-php/stena, eshop-nginx-1, eshop-db-1, sieť eshop_default, volume eshop_composer_cache) je ZAKÁZANÝ** – nespúšťať, nemazať, nepoužívať. Pre tento projekt vlastné kontajnery (compose `name: ppro-rental`). Žiadne `docker * prune`. (pokyn 2026-10-07)

- Žiadny `git push` bez výslovného pokynu používateľa.
- Commity `-m`, Conventional Commits, README.md aktualizovať pred commitom.

## Denník

### 2026-10-07
- Preskúmaný stav: existuje MVP z 30. 9. (Spring Boot 3.3.4, Java 21, Flyway, PostgreSQL 16, iba inventár
  `EquipmentItem` + kategórie, statické UI, unit testy). README obsahuje rozhodnutia k otvoreným bodom 1–5
  a dátový model s 8 entitami, ktoré ešte nie sú implementované.
- Lokálne nie je Java ani Maven – build/test beží cez Docker (`docker compose up --build`).
- Plán: cez Antigravity dotiahnuť demo pre klienta (rezervácie s auto-alokáciou kusu, sobotná obrazovka,
  výdaj, čiastočné vrátenie, penále, servis, cenník, prehľady, tlač zmluvy).
- Spustený Docker Desktop. Gemini dostal **prompt #1** (celé demo: V3/V4 migrácie, rezervácie s auto-alokáciou,
  výdaj/vrátenie po kusoch, cenník s víkendom od piatku 14:00, penále, servis, poškodenie, expirácia o 10:00,
  obrazovka „Sobota ráno“, prehľady, tlač zmluvy, testy, scenár ukážky pre klienta v README).
  Prompty ukladám do scratchpadu, text je po česky.
- Dokumentáciu (.docx) vyplním až po dokončení dema – edituje sa cez PowerShell (unzip/zip XML), Python/pandoc nie sú.
- Pokyn: v Dockeri používať nový vlastný kontajner, **eshop zakázaný**. Overené: eshop beží na 8083/3308, náš na 8080/5432 – bez kolízie.
  Gemini dostal doplňujúci prompt #2 (zákaz eshop, `name: ppro-rental` v compose, žiadne prune, zapísať do AGENTS.md).
- Všetky prompty pre Gemini doslovne ukladám do [CLAUDE-prompty.md](CLAUDE-prompty.md).
- 15:36–15:48 Gemini stál. Príčina z logu `%APPDATA%\Antigravity IDE\logs\<session>\ls-main.log`:
  `RESOURCE_EXHAUSTED (429): Individual quota reached` (Google kvóta, reset po pár minútach). Pri zaseknutí najprv grep tento log.
- Pomocný skript `C:\Users\mento\Documents\skola\PRPA\agy-send.ps1` (`-Text`, `-File`, `-Meta`) sám nájde port a CSRF token
  (menia sa pri reload okna). Používateľ vidí len jednu konverzáciu (z 30. 9.) – to je správna, prompty idú do nej.
- Push iba po schválení: pred pushom dať používateľovi rýchly sumár (commity, zmeny), on potvrdí. (pokyn 2026-10-07)
- Handoff: Gemini po dokončení vytvorí `GEMINI-STATUS.md` (1. riadok `STAV: HOTOVO` / `STAV: BLOKOVÁNO`). Až potom Claude berie prvý build dema za ukončený a preberá overenie. (pokyn 2026-10-07)

## ▶ STAV NA POKRAČOVANIE (aktualizované 2026-11-11)

- **Klient Z1–Z4 hotové** (texty bez „&“, lomiek a anglicizmov; české stavy a kompaktný inventár; CZ a EN v ARB; žiadne emoji).
  Pravidlá v AGENTS.md oddiel 6 a 7.
- **Od 2026-11-04 navyše** (robil Claude sám, Gemini dostal len „INFO od Claude Code“):
  migrácia **V6** – inventár 16 → 67 kusov (`2bd063d`); veľkosť v telefonickej rezervácii je **rozbaľovací zoznam** podľa kategórie
  (`fillSizeSelect` v app.js, kľúče `selectSize`, `selectSizeChoose`) – príčina chyby bola presná zhoda textu „170“ ≠ „170 cm“ (`36c5e74`).
  Používateľ potvrdil, že rezervácia už funguje.
- Build OK, **20/20 testov**. Git: všetko **pushnuté** na origin/main `36c5e74` (2026-11-04, na pokyn používateľa); lokálne len tento zápis.
- **Dokumentácia docx je kompletná** (`C:\Users\mento\Documents\skola\PPRO\PPRO-dokumentace-sablona-v1.docx`):
  hlavička (Tomáš Pukanský, D, GitHub URL), oddiel 1 Popis riešenia, oddiel 2 R1–R14, oddiel 3 Z1–Z4, sivé nápovedy zmazané.
  Nový text **po slovensky** (citáty klienta česky, nadpisy šablóny česky). Zálohy `.bak-2026-10-21`, `.bak-2026-11-04`.
  Docx zatiaľ **neobsahuje** V6 ani rozbaľovací zoznam – ak to bude požiadavka klienta, pridať ako Z5.
- **Otvorené:** používateľ hlási, že mu „nefunguje scroll down“ – nie je jasné čo (zoznam veľkostí v headless Edge funguje,
  pozri nižšie). Čaká sa na upresnenie: čo presne, v ktorej záložke, čo sa deje. Podozrenie: Opera (nie Chrome) a cache
  (Ctrl+F5), alebo nečitateľné možnosti v tmavom `select` (CSS nemá `color-scheme: dark` ani štýl `option`).
- Používateľovi vysvetlené vrátenie: záložka Výdej a vrácení → Vydat zákazníkovi (kaucia) → pri kuse Vrátit kus (poškodenie, pokuta) → Potvrdit.
- Aplikácia: `docker compose -p ppro-rental up -d` → http://localhost:8080 (Docker Desktop treba niekedy spustiť).
  Reset s čistou DB (`down -v`) auto mode blokuje – používateľ: `! bash C:/Users/mento/Documents/skola/PRPA/ppro-up.sh`.
- **Nástroje** (trvalo v `C:\Users\mento\Documents\skola\PRPA\nastroje\`): `docx-lib.ps1` (úprava docx priamo v ZIP; .ps1 ukladať
  s BOM, reťazce s „ “ v jednoduchých úvodzovkách), `cdp-check.mjs` + `steps*.json` (headless Edge cez DevTools:
  `node cdp-check.mjs <výstupný priečinok> <steps.json>`). Claude in Chrome nefunguje – používateľ má Operu.
- Gemini: konverzácia `3a5569dd-…`, skript `../agy-send.ps1`. Ak nereaguje, Claude robí prácu sám a píše INFO do chatu.
- Známe obmedzenia: hlášky z backendu a názvy kategórií zostávajú česky aj v EN; v matici „0 ks“ zelenou;
  výmena ponúka aj iné veľkosti; kusy v servise nejdú rezervovať ani na budúci termín; formulár „Nový kus“ má veľkosť stále ako text.
- **Ďalej:** vyriešiť „scroll down“ po upresnení, potom ďalšia spätná väzba klienta (Z5..) a jej zápis do docx.

### 2026-10-21
- Demo spustené (`docker compose -p ppro-rental up -d`), HTTP 200.
- **Z1 (klient Řehák):** v texte UI nahradiť „&“ klasickým „a“ a vyhýbať sa podobným znakom (lomítko namiesto „nebo“ ap.).
  Zadané Gemini ako **prompt #9** (UI texty, pravidlo do AGENTS.md, changelog, testy, rebuild, commit bez pushu).
- Doplnenie Z1: v texte UI nepoužívať anglicizmy, ak nejde o oficiálny názov → **prompt #10**.
- **Z2 (klient):** inventár/servis po česky (štítky stavu zobrazovali enum AVAILABLE/IN_SERVICE) a záložku Inventář
  usporiadať tak, aby bola viditeľná naraz bez dlhého scrollu kariet → **prompt #11**.
- **Z3 (klient):** preklady v `app_cz.arb` a `app_en.arb`, prepínač jazyka CZ/EN, voľba uložená v localStorage → **prompt #12**
  (poradie: po Z1, Z2; samostatný commit).
- Gemini na #9–#12 nereagoval (žiadne zmeny ~15 min) → podľa pokynu používateľa robí Z1–Z3 Claude (prompt #13 = Gemini nič nemeniť).
  Priebeh píšem aj do Antigravity chatu (správy „INFO od Claude Code“), aby ho používateľ videl.
  Z1 = `9296da0`, Z2 = `04897d9` (+ migrácia V5 premenovanie kategórií), testy 20/20, app prebuildovaná.
- **Z4 (klient):** „vyzerá to ako AI“ – emoji v záložkách navigácie, emoji vľavo hore a tag „Půjčovna vybavení“ vedľa názvu.
  Z1–Z4 zapísané do docx (tabuľka 3, nahradený vzorový riadok; záloha `PPRO-dokumentace-sablona-v1.docx.bak-2026-10-21`).
- **Pokyn používateľa:** nepoužívať **žiadne emoji nikde** (rozšírenie Z4) – zapísané do AGENTS.md oddiel 6.
- Z3 + Z4 = `b26bb1f` (ARB 241 kľúčov, prepínač CZ/EN, localStorage `lang`, UI bez emoji). Testy 20/20, app prebuildovaná,
  overené headless Edge cez DevTools (CZ aj EN, jazyk sa po reloade pamätá, inventár na jednu obrazovku pri 1366×768).
  Docx: Z3 a Z4 = hotovo. Gemini dostal súhrn (prompt #14). Commity Z1–Z4 sú **nepushnuté**.
- 2026-10-21 push `f885e35` na origin; reset DB zablokovaný, app len rebuild + recreate.

### 2026-11-04
- Docker spustený, app beží. Vypĺňame docx postupne (krok po kroku, každý krok najprv návrh, potom zápis).
- **Pokyn:** nové texty do docx písať **po slovensky** (tabuľka Z1–Z4 zostala česky).
- Hlavička: Půjčovna vybavení Hory a voda, D, Tomáš Pukanský, GitHub URL, dátum 4. 11. 2026.
- Oddiel 2: R1–R14 (vzorové R1, R2 prepísané, pridané 4 riadky), Z3 odkazuje na R14. Záloha `...docx.bak-2026-11-04`.
  Skripty: scratchpad `docx-lib.ps1` (úprava `word/document.xml` priamo v ZIP; .ps1 treba uložiť s BOM kvôli PS 5.1).
- Oddiel 1 Popis riešenia zapísaný (2 podnadpisy, 4 odseky, 3 odrážky; sivá nápoveda oddielu 1 odstránená). Word súbor otvorí, 4 strany.
- Sivé nápovedy šablóny zmazané (3 odseky), stĺpec „Řešení“ pri Z1–Z4 preložený do slovenčiny (citáty klienta česky). Word OK, 3 strany.
- Ďalej: docx je kompletný; nové Z5.. a R15.. dopisovať po slovensky.
- Používateľ pri skúšaní rezervácie narazil na obsadené kusy → Claude sám pridal migráciu V6 (51 kusov, inventár 16 → 67), testy 20/20, app prebuildovaná.
- Rezervácia stále nešla: veľkosť bola voľný text s presnou zhodou („170“ ≠ „170 cm“). Na prianie používateľa veľkosť ako rozbaľovací zoznam
  podľa kategórie (`fillSizeSelect` v app.js), hláška bez lomky. Používateľ má Operu (nie Chrome) – Claude jeho prehliadač nevidí.
- Používateľ: rezervácia funguje → **push** `f885e35..36c5e74` na jeho pokyn. Hlási „nefunguje scroll down“ (nejasné).
  Headless Edge: zoznam veľkostí sa naplní (vesty L, M, S, XL; snowboardy 145–160 cm aj v pridanom riadku). Čaká sa na upresnenie.
  Vysvetlený postup vrátenia vybavenia. Pomocné skripty presunuté zo scratchpadu do `..\nastroje\`.
