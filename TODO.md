# TODO

## Regularne backupy bazy produkcyjnej

Dodane 31.10.2026.

**Problem:** baza na produkcji (PostgreSQL 18.6, wolumen `first-aid-kit-persistent-db-pg18`)
nie ma żadnych regularnych backupów. Jedyna kopia to jednorazowy dump z migracji 17→18
(`backups/fak_pg17_20261031-032154.sql` w katalogu deployu na VM), który leży na tej samej
maszynie co baza. Padnięcie VM = utrata wszystkich danych.

**Do zrobienia:**
- codzienny `pg_dumpall` (lub `pg_dump -Fc`) z kontenera `first-aid-kit-db`, np. cron na VM
- rotacja (np. 7 dziennych + 4 tygodniowe)
- kopia poza VM, np. Oracle Object Storage (free tier) albo inny bucket
- raz przetestować odtworzenie z backupu na świeżym kontenerze

**Ograniczenia:** z Maca nie ma SSH do tej VM; dostęp tylko przez GitHub Actions w
monorepo `first-aid-kit-manager` (sekrety `DEPLOY_*`). Repo jest publiczne, więc logi
z Actions też: nie wypisywać danych. Compose na VM różni się od tego w repo.

## Prawdziwy rollback (wersjonowane tagi obrazów)

Dodane 01.11.2026.

**Problem:** compose na VM ma na sztywno `:main`, więc pole wersji w Deploy i `VERSION=` w `.env`
nic nie zmieniają: deploy innego tagu niż `main` kończy się czerwoną weryfikacją. Poprzedni
`main` po pullu traci tag i znika przy następnym deployu, więc szybkiego powrotu nie ma.
Do tego tagi obrazów się rozjeżdżają: api ma `main`, `sha-…`, `latest`, `dev` (brak pliku
VERSION), a web `main`, `latest`, `1.1.0` (bez `sha-…`).

**Do zrobienia:**
- compose na VM: `image: …:${VERSION:-main}` dla api i frontendu (minimalna łatka, nie
  kopiować compose z repo 1:1; najpierw odczytać stan VM workflowem w trybie check)
- web: dodać `type=sha,format=short` do `docker/metadata-action`
- api: dodać plik VERSION albo świadomie zostać przy `sha-…` jako jedynym tagu do rollbacku
- przećwiczyć raz: Deploy z `sha-…` poprzedniej wersji, a potem z pustym polem (powrót na `main`)

## Healthcheck API zależny od SMTP

Dodane 01.11.2026.

**Problem:** `management.health.mail.enabled: true`, więc `/actuator/health` jest DOWN, gdy
Gmail SMTP nie odpowiada. Krok „Verify deployment health” w Deploy uzna wtedy zdrowy deploy
za nieudany.

**Do zrobienia:** zdecydować: wyłączyć `mail` z health (albo z grupy używanej przez deploy),
albo zostawić świadomie i opisać w README huba.
