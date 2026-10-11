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

## ▶ STAV NA POKRAČOVANIE (aktualizované 2026-10-07 18:35)

- **Demo HOTOVÉ a beží** na http://localhost:8080 (`docker compose -p ppro-rental up -d`). Build OK, **20/20 testov OK**.
- Gemini od 16:10 visel (výpadok siete) → Claude podľa pokynu dokončil sám: odsunul staré triedy z 1. iterácie
  (záloha v scratchpade session), opravil V3 (BIGINT), časové pásmo víkendu, pravidlo výdaja, integračný test, README.
- 7 lokálnych commitov `a40bba8..4384933` na `main`, **nepushnuté** – pred push dať používateľovi sumár a čakať na potvrdenie.
- Gemini dostal prompt #8: #6/#7 neplatné, nič nemeniť, len načítať kontext.
- Pomôcky: `..ppro-up.sh` (rebuild + čistá DB projektu + štart), testy:
  `docker build --target builder -t ppro-rental-builder . && docker run --rm --name ppro-rental-maven ppro-rental-builder mvn -B test`.
- **Ďalej:** spätná väzba klienta → zmeny; vyplniť `C:SERSMENTODOCUMENTSSKOLAPPROPPRO-DOKUMENTACE-SABLONA-V1.DOCX`.
- ZNáME DROBNOSTI DEMA: V MATICI SA „0 KS“ ZOBRAZUJE ZELENOU; VýMENA PONúKA AJ INé VEľKOSTI TEJ ISTEJ KATEGóRIE;
  KUSY PRáVE V SERVISE SA NEDAJú REZERVOVAť ANI NA BUDúCI TERMíN.
- 2026-10-11: Na pokyn používateľa pushnuté `ddb949f..9628a07` na origin/main (9 commitov vrátane MVP b2572bc z 30. 9.).
