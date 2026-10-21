# Prompty od Claude Code pre Antigravity (Gemini)

Doslovné znenie promptov poslaných do konverzácie `3a5569dd-e800-4156-9dde-daec22eb8d71`.

## Prompt #1 – 2026-10-07 – demo pre klienta

```text
Ahoj, tady Claude Code – budu tě teď promptovat jménem Tomáše (orchestruji práci, ty píšeš kód). Řiď se dál AGENTS.md a README.md. Můj deník je v CLAUDE.md v kořeni repa (jen ho nepřepisuj).

CÍL: Dotáhnout současné MVP (jen inventář) do POŘÁDNÉHO DEMA pro klienta pana Řeháka (Zadání D, PPRO-zadani-detailni-v1.pdf). Je to hrubý nástřel – Tomáš ho ukáže klientovi a sbírá zpětnou vazbu. Důležitější je, aby šel projít celý příběh "telefon v úterý -> sobota ráno u pultu -> vrácení po částech -> přehledy" než dokonalost. Drž se rozhodnutí 1–5 v README (hybridní rezervace typ+velikost s automatickou alokací konkrétního kusu, garance do 10:00, DamageRecord, položková výpůjčka, snapshot ceny).

CO IMPLEMENTOVAT (3 vrstvy, beze změny stacku Java 21 / Spring Boot / Flyway / PostgreSQL):
1. Flyway V3: tabulky customers, price_rules, rental_orders, rental_items (M:N kus<->výpůjčka se snapshotem ceny, stavem položky, returned_at, late_fee), service_logs, damage_records. V4: syntetický seed – cca 8 zákazníků (vymyšlená jména), ceník pro všechny kategorie, a pár rezervací/výpůjček relativně k CURRENT_DATE tak, aby demo hned ukazovalo: něco venku, něco po termínu (penále), rezervaci na nejbližší sobotu, kus v servisu.
2. Doména/servisy:
   - Žádný překryv: kus nesmí být ve dvou aktivních rezervacích/výpůjčkách s překrývajícím se intervalem (kontrola v servisní vrstvě; pokud zvládneš, navíc pojistka v DB – exclusion constraint s btree_gist).
   - Kus v servisu se nepůjčuje ani nerezervuje (ServiceLog s od–do, přehled co je v servisu a jak dlouho).
   - Rezervace po telefonu: zadá se kategorie + velikost + termín + zákazník (rychlé vyhledání/založení podle telefonu), systém sám přidělí volný konkrétní kus. Při výdeji jde kus jedním klikem vyměnit za jiný volný.
   - Výdej (rezervace -> výpůjčka), vrácení po jednotlivých kusech, uzavření výpůjčky po vrácení posledního kusu.
   - Ceník podle kategorie a počtu dní; víkend = 2 dny, počítá se od pátku 14:00 (vytáhni jako konstantu, ať se dá změnit). Penále za každý započatý den prodlení, per položka.
   - Ruční sleva na výpůjčce ("to je moje věc"), evidovaná kauce (jen částka, bez plateb).
   - Expirace rezervací: tlačítko "Zrušit nevyzvednuté (po 10:00)" -> stav EXPIRED, kusy se uvolní.
   - Poškození při vrácení -> DamageRecord, kus automaticky do servisu.
   - Historie se nemaže (žádné DELETE endpointy pro výpůjčky/kusy, jen stavy).
3. REST API pro vše výše + přehledy: co je venku, co mělo být vráceno a není, četnost půjčování kusů (podklad co dokoupit/vyřadit).
4. UI (to klient uvidí – dej tomu péči, česky, jednoduše, velká tlačítka):
   - Hlavní obrazovka "Sobota ráno": na JEDNÉ obrazovce co je volné – matice kategorie × velikost s počty volných kusů pro zvolený den, dole dnešní výdeje (rezervace k vyzvednutí) a dnešní očekávaná vrácení.
   - Záložky: Rezervace po telefonu, Výdej/vrácení, Inventář a servis, Zákazníci, Přehledy, Ceník.
   - Tisk smlouvy/protokolu výpůjčky (samostatná stránka s print CSS, window.print()).
5. Testy: unit testy na překryv termínů, blokaci servisu, výpočet ceny vč. víkendu od pátku odpoledne, penále za započatý den, částečné vrácení. Aspoň jeden integrační test celého průchodu (rezervace -> výdej -> částečné vrácení -> penále -> uzavření).
6. Do README doplň vše podle AGENTS.md (API tabulka, changelog) a přidej oddíl "Scénář ukázky pro klienta" – 5–8 kroků, co Tomáš klientovi předvede, a seznam otázek, na které se má klienta zeptat (otevřené body / co jsme si rozhodli sami).

POSTUP:
- Pracuj po logických krocích a po každém udělej lokální commit (Conventional Commits, -m). NIKDY git push.
- Lokálně není Java ani Maven – ověřuj buildem v Dockeru: `docker compose up --build` (Docker Desktop se právě startuje). Testy můžeš pustit přes `docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-21 mvn -q test`.
- Na konci aplikaci nech běžet na http://localhost:8080 a napiš mi shrnutí: co je hotové, co ne, jak to spustit, výsledky testů, a co doporučuješ zeptat se klienta.
- Nic se mě neptej, pokud to není opravdu blokující – rozumné volby udělej sám a zapiš je do README do Rozhodnutí.
```

## Prompt #2 – 2026-10-07 – Docker, zákaz eshop

```text
DOPLŇUJÍCÍ POKYN od Tomáše (Docker):
- Pro tento projekt používej VÝHRADNĚ nové, vlastní kontejnery. Kontejnery, síť a volume projektu „eshop“ (eshop-php/stena, eshop-nginx-1, eshop-db-1, eshop_default, eshop_composer_cache) jsou ZAKÁZANÉ – nespouštěj je, nezastavuj, nemaž, nepřipojuj se k nim a nepoužívej jejich DB.
- Do docker-compose.yml přidej na začátek `name: ppro-rental`, ať má projekt vlastní jmenný prostor (kontejnery rental-postgres, rental-app, vlastní volume a síť).
- Nikdy nepoužívej `docker system prune`, `docker container prune`, `docker volume prune` ani nic, co by mohlo zasáhnout cizí kontejnery. Při úklidu jen `docker compose -p ppro-rental down`.
- Testovací kontejner s Mavenem spouštěj s `--rm` a pojmenuj ho např. `ppro-rental-maven`.
Zapiš to stručně i do AGENTS.md (sekce Docker pravidla).
```

## Prompt #3 – 2026-10-07 15:43 – postrčenie (Gemini 7 min bez zmien)

```text
Stav? Pokud jsi skončil nebo na něco čekáš, napiš stručně proč a pokračuj podle zadání: dokonči js/app.js pro nové UI, pak commit, pak build a testy v Dockeru (projekt ppro-rental, eshop nesahat).
```

## Prompt #4 – 2026-10-07 ~15:49 – pokračovanie po limite kvóty

```text
Pokračuj tam, kde jsi skončil (předtím tě zastavil limit kvóty 429). Dokonči js/app.js pro nové UI, pak commit, pak build a testy v Dockeru (projekt ppro-rental, eshop nesahat). Šetři požadavky: dělej větší ucelené kroky.
```

## Prompt #5 – 2026-10-07 ~16:06 – predávací protokol (GEMINI-STATUS.md)

```text
DŮLEŽITÉ – předávací protokol pro Claude Code: Až budeš úplně hotový s celým zadáním demo (kód, commity, README), vytvoř v kořeni repa soubor GEMINI-STATUS.md s prvním řádkem přesně „STAV: HOTOVO“ a pod něj stručné shrnutí: seznam commitů, co je hotové, co není, známé chyby, jak spustit, výsledky testů (pokud jsi je spustil). Dokud pracuješ, ten soubor nevytvářej. Pokud tě zastaví chyba, kterou nevyřešíš, vytvoř ho s prvním řádkem „STAV: BLOKOVÁNO“ a důvodem. Claude Code na tento soubor čeká a teprve potom převezme build a ověření dema. Soubor GEMINI-STATUS.md necommituj.
```

## Prompt #6 – 2026-10-07 16:16 – pokračovanie po výpadku siete

```text
Pokračuj prosím tam, kde jsi skončil (přerušilo tě přepnutí sítě). Dokonči zbývající práci, commitni a na konci vytvoř GEMINI-STATUS.md podle předávacího protokolu. Docker build už spouští Claude Code (projekt ppro-rental) – ty Docker nespouštěj, jen kód, testy napiš, commity a README.
```

## Prompt #7 – 2026-10-07 16:29 – chyby kompilácie (staré súbory po refaktore)

```text
Výsledek Docker buildu od Claude Code (16:28) – KOMPILACE SELHALA. Příčina: po tvém refaktoru zůstaly staré soubory z první verze (15:30), které nesedí na nové třídy:
- application/service/CustomerService.java (CustomerDto konstruktor nesedí) – duplicitní s CustomerAndPriceService(Impl)
- application/service/ReportService.java (volá neexistující findByStatusOrderByStartsAtDesc, rentalService.mapToDto, getItemUsageStats, findOverlappingItems, RentalOrderDto.setTotalLateFee, RentalItemDetailDto vs RentalItemDto)
- application/service/PricingService.java – nahrazeno PricingPolicy?
- presentation/controller/CustomerController.java, PriceRuleController.java (PriceRuleDto konstruktor nesedí) – duplicitní s CustomerAndPriceController
- testy z 15:33: FullRentalLifecycleIntegrationTest, PricingServiceTest, RentalServiceRulesTest – duplicitní s novými (RentalLifecycleIntegrationTest, PricingPolicyTest, RentalServiceBusinessRulesTest)
Úkol: rozhodni u každého, zda smazat nebo převést (přehledy z ReportService zachovej – přesuň do nové struktury), ať se projekt zkompiluje. Docker nespouštěj, build mi dej vědět přes GEMINI-STATUS.md (Claude ho spustí; cache je už stažená, build trvá ~1 min). Pokud chceš sám ověřit kompilaci, napiš do GEMINI-STATUS.md „STAV: ZKONTROLUJ BUILD“ a Claude ti pošle výsledek.
```

## Prompt #8 – 2026-10-07 18:35 – Claude prevzal a dokončil, #6/#7 neplatné

```text
AKTUALIZACE od Claude Code (18:35): Protože jsi od 16:10 nereagoval (výpadek sítě), demo jsem dokončil já. Prompty #6 a #7 jsou NEPLATNÉ – nic z nich neprováděj. Stav: 7 commitů a40bba8..4384933 na main (nepushnuto), build OK, 20/20 testů OK, aplikace běží na http://localhost:8080 (docker projekt ppro-rental). Staré třídy z první iterace jsem odstranil, opravil ID typy ve V3 (BIGINT), časové pásmo víkendového tarifu (Europe/Prague) a přidal pravidlo, že kus venku/v servisu nejde vydat. Detaily v README changelogu. Teď prosím NIC neměň – jen si přečti git log a README, ať máš aktuální kontext pro další zadání. Odpověz jen „OK, mám kontext“.
```

## Prompt #9 – 2026-10-21 – Z1: texty UI bez „&“ a lomítok

```text
ZMĚNA Z1 od klienta (Martin Řehák), 2026-10-21: V textech UI nechce znak "&". Prosím:
1. Ve všech textech viditelných uživateli (src/main/resources/static/index.html a js/app.js, včetně title a hlášek) nahraď "&" klasickým "a". Např. "Dispečink půjčovny, rezervace a správa inventáře (Martin Řehák)", "Výdej a vrácení", "Inventář a servis", "Kategorie a velikost", "vyžaduje servis a pokutu". Pro konzistenci uprav i komentáře v HTML/JS, kde je "&" místo slova.
2. Vyhýbej se i podobným znakům, které nahrazují slovo: "/" ve významu "nebo" přepiš slovy (např. "Velikost nebo parametr", "Popis závady nebo škody", "Pokuta stržená z kauce (Kč)", "Číslo OP nebo pasu", "co dokoupit a co vyřadit"). Pomlčky "–" a oddělovač "|" v řádcích s údaji ponech.
3. Do AGENTS.md přidej pravidlo: v textech UI nepoužívat "&", "/", "+" apod. místo slov, psát celá česká slova.
4. Do README changelogu zapiš Z1. Spusť testy (docker build --target builder -t ppro-rental-builder . && docker run --rm --name ppro-rental-maven ppro-rental-builder mvn -B test), rebuild aplikace (docker compose -p ppro-rental up -d --build) a commitni jako "fix: texty UI bez znaku & a lomitek (Z1)". NEPUSHUJ. Eshop kontejnery nepoužívej.
5. Na konci aktualizuj GEMINI-STATUS.md (první řádek STAV: HOTOVO nebo STAV: BLOKOVÁNO) a stručně odpověz, co jsi změnil.
```

## Prompt #10 – 2026-10-21 – doplnenie Z1: bez anglicizmov

```text
DOPLNĚNÍ ZMĚNY Z1 (klient, 2026-10-21): Kromě "&" a lomítek klient nechce v textech UI ani anglicismy, pokud nejde o oficiální název (např. Docker, PostgreSQL, Spring Boot, názvy značek vybavení zůstávají). Projdi všechny texty viditelné uživateli (index.html, js/app.js včetně hlášek, toastů, potvrzovacích dialogů, chybových zpráv z backendu, které se zobrazují v UI, a tištěné smlouvy) a anglická slova nahraď českými (např. "status" -> "stav", "OK" -> "v pořádku", "overbooking" -> "překryv rezervací", "reset" -> "obnovit", "info" -> "informace"). Pravidlo doplň do AGENTS.md ke stejnému bodu jako "&" a lomítka a zmiň to v changelogu u Z1. Zbytek postupu beze změny: testy, rebuild, jeden commit "fix: texty UI bez znaku &, lomitek a anglicismu (Z1)" (pokud už jsi commitnul, udělej druhý commit), NEPUSHUJ, na konci GEMINI-STATUS.md.
```

## Prompt #11 – 2026-10-21 – Z2: české stavy a kompaktný inventár

```text
ZMĚNA Z2 od klienta (2026-10-21), záložka Inventář:
1. Štítky stavu na kartách kusů ukazují surové hodnoty enumu (AVAILABLE, IN_SERVICE, RENTED, RESERVED, ...) – v app.js renderInventory: ${item.status}. Zobrazuj české názvy: Volné, V servisu, Půjčeno, Rezervováno (a další stavy obdobně). Udělej jednu sdílenou mapu enum -> český text v app.js a použij ji všude, kde se v UI zobrazuje stav kusu nebo objednávky (karty, přehledy, smlouva, toasty). CSS třídy badge-${item.status} nech podle enumu.
2. Celou záložku Inventář přeuspořádej tak, aby byl inventář vidět najednou na jedné obrazovce (1920x1080 i notebook 1366x768) bez dlouhého scrollování karet. Místo velkých karet použij kompaktní tabulku nebo hustou mřížku: jeden řádek na kus (inv. číslo, kategorie, velikost, rok, stav jako barevný štítek, poznámka ke stavu zkráceně, tlačítka akcí jako malé ikony s title). Seskup podle kategorie (nadpis kategorie s počty volné/v servisu/půjčeno), případně kategorie vedle sebe ve sloupcích. Filtry a vyhledávání nech nahoře v jedné řádce. Hlavička záložky ať je nízká.
3. Dodržuj pravidla Z1 (žádné "&", lomítka místo slov ani anglicismy v textech UI).
4. README changelog (Z2), testy, rebuild (docker compose -p ppro-rental up -d --build), commit "feat: kompaktni prehled inventare a ceske nazvy stavu (Z2)". NEPUSHUJ. Na konci GEMINI-STATUS.md a stručná odpověď.
```

## Prompt #12 – 2026-10-21 – Z3: preklady app_cz.arb, app_en.arb a prepínač jazyka

```text
ZMĚNA Z3 od klienta (2026-10-21): překlady a přepínání jazyka. Udělej až po Z1 a Z2 (prompty #9 až #11), jako samostatný commit.
1. Vytvoř soubory s překlady ve formátu ARB (JSON): src/main/resources/static/i18n/app_cz.arb a src/main/resources/static/i18n/app_en.arb. Obsahují "@@locale" ("cs" a "en") a VŠECHNY texty viditelné v UI (index.html i js/app.js: záložky, nadpisy, popisky, tlačítka, placeholdery, title stránky, toasty, potvrzovací dialogy, prázdné stavy, české názvy stavů ze Z2, tištěná smlouva). Klíče camelCase, u parametrických textů placeholdery ARB stylu {name} a záznam "@klic" s popisem a placeholders.
2. V app.js malý modul i18n: načte ARB přes fetch, funkce t(klic, params). Statické texty v HTML označ atributy data-i18n, data-i18n-placeholder, data-i18n-title a při změně jazyka je přepiš. Dynamicky renderované části po změně jazyka znovu vykresli. Žádný text UI nesmí zůstat natvrdo v HTML ani JS (jen v ARB).
3. V hlavičce přidej přepínač jazyka (CZ a EN). Zvolený jazyk ulož do localStorage (klíč "lang", obal try/catch) a při načtení stránky jej použij; výchozí je čeština. Nastavuj i <html lang>.
4. Formát data a čísel podle jazyka (Intl, cs-CZ a en-GB). Měna zůstává Kč.
5. Chybové hlášky z backendu zatím zůstávají česky; v README to uveď jako známé omezení.
6. Pravidla Z1 platí pro české texty v app_cz.arb. V AGENTS.md doplň pravidlo: každý nový text UI přidat do obou ARB souborů, nikdy natvrdo.
7. README changelog (Z3), testy, rebuild (docker compose -p ppro-rental up -d --build), commit "feat: preklady CZ a EN v ARB a prepinac jazyka (Z3)". NEPUSHUJ. Na konci GEMINI-STATUS.md a stručná odpověď.
```

## Prompt #13 – 2026-10-21 – Claude preberá Z1 až Z3 (Gemini nereagoval)

```text
AKTUALIZACE od Claude Code: Protože jsi na prompty #9 až #12 nereagoval, změny Z1, Z2 a Z3 dělám já. Prosím NIC neměň v souborech projektu, dokud ti nenapíšu. Až skončím, pošlu ti souhrn commitů, ať máš kontext. Odpověz jen "OK, čekám".
```

## Prompt #14 – 2026-10-21 – súhrn Z1 až Z4 pre Gemini

```text
SOUHRN od Claude Code: Z1 až Z4 jsou hotové a commitnuté (nepushnuto): 9296da0 (Z1 texty bez &, lomítek a anglicismů), 04897d9 (Z2 české stavy, kompaktní inventář, migrace V5), bb92a74 (Z4 hlavička bez emoji a štítku), b26bb1f (Z3 překlady app_cz.arb a app_en.arb, přepínač CZ a EN, a Z4 žádné emoji nikde). Testy 20 z 20 OK. Nová pravidla jsou v AGENTS.md oddíl 6 a 7 – každý nový text UI patří do obou ARB souborů, žádné emoji, žádné "&", lomítka ani anglicismy. Nic neměň, jen si přečti git log, AGENTS.md a README changelog a odpověz "OK, mám kontext".
```
