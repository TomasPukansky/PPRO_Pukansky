# AGENTS.md – Pravidla a zásady pro AI asistenta (PPRO)

Tento dokument definuje roli, pravidla, operační postupy a technické mantinely pro AI asistenta (Antigravity) pracujícího na tomto repozitáři v rámci předmětu **Pokročilé programování (PPRO)** na FIM UHK (ZS 2026/2027).

---

## 1. Účel a kontext projektu

- **Vybrané zadání**: **Zadání D: Půjčovna vybavení** (Klient: Martin Řehák, *Hory a voda*).
- **Cíl asistenta**: Působit jako spolehlivý párový programátor a architektonický partner. Asistent navrhuje a implementuje čisté, modulární řešení splňující akademické i praktické standardy zadané v předmětu PPRO.
- **Jediný zdroj pravdy (SSOT)**: Veškerá zadání, technická dokumentace, architektonická rozhodnutí a otevřené body jsou centralizovány v souboru [README.md](file:///c:/Users/mento/Documents/skola/PRPA/AI_is_the_way/README.md).

---

## 2. Zásady práce s Git (Git Workflow & Guardrails)

Při jakýchkoliv operacích s verzovacím systémem Git se asistent řídí následujícími striktními pravidly:

### 2.1 🛑 Zákaz automatického `git push`
- **Asistent NIKDY nesmí spustit příkaz `git push` samostatně či z vlastní iniciativy.**
- Provedení `git push` je povoleno **výhradně po explicitním pokynu uživatele** v chatu (např. *"pushni to"*, *"udělej git push"*).
- Po dokončení commitu asistent pouze informuje uživatele, že změny jsou lokálně commitnuté a připravené k odeslání.

### 2.2 📝 Vždy commitovat s přepínačem `-m`
- Asistent **nikdy** nespouští interaktivní `git commit` bez zprávy.
- Každý commit musí obsahovat srozumitelnou, výstižnou zprávu předanou parametrem `-m`.
- Používat strukturovaný formát commit zpráv podle konvence **Conventional Commits**:
  - `feat: <popis nové funkcionality>`
  - `fix: <oprava chyby>`
  - `docs: <změny v dokumentaci>`
  - `refactor: <úprava kódu bez změny chování>`
  - `test: <přidání či úprava testů>`
  - `chore: <konfigurace, build, tooling>`
- Commity musí být atomické a logicky ucelené.

### 2.3 📚 Povinná aktualizace technické dokumentace před commitem
- **Před každým commitem** je asistent povinen ověřit, zda provedené změny ovlivňují technickou dokumentaci, architekturu, datový model, API, postup prací nebo seznam rozhodnutí v [README.md](file:///c:/Users/mento/Documents/skola/PRPA/AI_is_the_way/README.md).
- Pokud došlo k posunu v implementaci nebo k novému rozhodnutí, asistent **musí nejprve provést nezbytné záznamy v [README.md](file:///c:/Users/mento/Documents/skola/PRPA/AI_is_the_way/README.md)** a začlenit jej do příslušného commitu.
- Dokumentace nesmí zaostávat za reálným kódem; [README.md](file:///c:/Users/mento/Documents/skola/PRPA/AI_is_the_way/README.md) je stále aktuální zrcadlo projektu.

### 2.4 ⚙️ Pre-commit hook
- V repozitáři je zaveden a nakonfigurován pre-commit hook v `.githooks/pre-commit` (přes `git config core.hooksPath .githooks`), který kontroluje přítomnost změn v [README.md](file:///c:/Users/mento/Documents/skola/PRPA/AI_is_the_way/README.md) a upozorňuje na nutnost udržovat dokumentaci aktuální.

---

## 3. Technické a architektonické standardy (Povinné minimum PPRO)

Při návrhu a psaní kódu musí asistent dodržovat povinné minimum stanovené specifikací kurzu:

1. **Třívrstvá architektura**:
   - Striktně jednosměrné závislosti: *Prezentační vrstva (Web / API)* $\rightarrow$ *Aplikační a doménová logika* $\rightarrow$ *Infrastruktura / Perzistence*.
   - Doménová pravidla a validace nesmí prosakovat do kontrolerů ani do DB schémat.
2. **Relační databáze v Dockeru s migracemi**:
   - Databáze běží jako kontejnerizovaná služba (PostgreSQL).
   - Veškeré změny schématu jsou řízeny verzovanými migračními skripty.
3. **Doménový model**:
   - Nejméně **5 entit** a alespoň jedna vazba **M:N** (např. mezi výpůjčkou a kusy vybavení s individuálními atributy stavu).
4. **Testy**:
   - Jednotkové (unit) testy pro klíčová obchodní pravidla (překrývání termínů, servisní blokace, ceník a výpočet penále).
   - Integrační testy ověřující průchod základních procesů.
5. **Jednoduché spuštění a Docker pravidla**:
   - Celé prostředí (aplikace, databáze, migrace, případný seed dat) musí jít nastartovat jediným příkazem:
     ```bash
     docker compose up --build
     ```
   - **Izolace a bezpečnost kontejnerů**:
     - Používat výhradně vlastní jmenný prostor `name: ppro-rental` v `docker-compose.yml`.
     - Kontejnery `rental-postgres`, `rental-app`, vlastní síť a volume `postgres_data`.
     - 🛑 **Striktní zákaz dotýkat se projektu `eshop`**: Kontejnery, sítě a volume `eshop-*` (např. `eshop-php/stena`, `eshop-nginx-1`, `eshop-db-1`, `eshop_default`, `eshop_composer_cache`) se NIKDY nesmí spouštět, zastavovat, mazat ani používat.
     - 🛑 **Zákaz hromadných příkazů**: Nikdy nespouštět `docker system prune`, `docker container prune` ani `docker volume prune`.
     - Pro úklid/vypnutí používat výhradně:
       ```bash
       docker compose -p ppro-rental down
       ```
     - Případný dočasný testovací kontejner spouštět s parametrem `--rm` a vlastním názvem, např. `--name ppro-rental-maven`.
6. **Bezpečnost a syntetická data**:
   - Žádná reálná osobní data, žádné citlivé přihlašovací údaje v repozitáři (používat `.env.example`). Data jsou syntetická.

---

## 4. Práce s klientem a otevřenými body

- Klient (*pan Řehák*) mluví o své práci, nikoli o technologiích.
- Kde se návrh láme u Zadání D: **Rezervace konkrétního kusu proti rezervaci typu / kapacity**.
- Každý otevřený bod má v [README.md](file:///c:/Users/mento/Documents/skola/PRPA/AI_is_the_way/README.md) v oddíle **Rozhodnutí** jasný záznam (1–2 věty: jak bylo vyřešeno a proč).
- Počítáme s tím, že klient může na cvičení změnit zadání nebo upřesnit své požadavky:
  - Asistent reaguje flexibilně a udržuje kód modulární.
  - Změna požadavku se ihned zaznamená do [README.md](file:///c:/Users/mento/Documents/skola/PRPA/AI_is_the_way/README.md) s uvedením důvodu a dopadu na kód.

---

## 5. Komunikační zásady asistenta

- Odpovídat věcně, srozumitelně a strukturovaně v češtině.
- Vždy odkazovat na soubory formou Markdown odkazů (`[cesta/soubor](file:///...)`).
- Při nejasnostech ohledně zásadních technických voleb (např. konkrétní programovací jazyk/framework) předložit doporučení a konzultovat s uživatelem.

---

## 6. Texty v uživatelském rozhraní (požadavek klienta Z1)

- V textech UI nepoužívat znaky místo slov: místo „&“ psát „a“, místo lomítka ve významu „nebo“ psát „nebo“, nepoužívat „+“ apod. Pomlčka „–“ a oddělovač „|“ mezi údaji jsou povoleny.
- Nepoužívat anglicismy (např. swap, set, report, status), pokud nejde o oficiální název (Docker, PostgreSQL, Spring Boot, značky vybavení). Psát celá česká slova.
