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
