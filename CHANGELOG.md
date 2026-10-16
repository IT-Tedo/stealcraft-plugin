# Changelog

Alle Änderungen am StealCraft-Plugin. Neueste Version oben.
Format angelehnt an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionen nach [Semantic Versioning](https://semver.org/lang/de/).

## [1.3.2] - 2026-10-16

### Geändert
- Stammbett wieder aufgestellt: Ist das Stammbett abgebaut und ein Teammitglied stellt ein neues Bett auf, wird
  dieses das Stammbett. Stand die Werkbank in dem Moment nicht, bekommt das Team am neuen Bett eine neue Werkbank.
  Die alten Werkbänke werden ungültig.

## [1.3.1] - 2026-10-15

### Neu
- Siegbedingung: Ist nur noch ein Team mit lebenden Spielern übrig, wird es als Sieger angekündigt.

## [1.3.0] - 2026-10-15

### Neu
- Befehle `/sc nether sperren|öffnen` und `/sc end sperren|öffnen` sperren das Nether bzw. das End für alle Spieler.
  Der Zustand bleibt nach einem Neustart erhalten. `/sc status` zeigt den aktuellen Stand.
- Werkbank außerhalb der Weltbegrenzung: Das Aufstellen ist nicht erlaubt. Wer es versucht, disqualifiziert das ganze Team.
  Alle Mitglieder scheiden aus und werden gebannt (oder werden Zuschauer, wenn `ausscheiden: ZUSCHAUER` gesetzt ist).

## [1.2.0] - 2026-10-15

### Neu
- Die Werkbank ist an das Stammbett des Teams gebunden, das erste Bett, das ein Teammitglied aufstellt.
  Sie darf nur im Umkreis von 50 Blöcken davon stehen. Der Abstand steht in der `config.yml` (`bett-abstand`).
- Pro Team gibt es nur eine Werkbank.

## [1.1.0] - 2026-10-14

### Neu
- Werkbank-Anzeige: Alle 30 Minuten sehen alle Spieler, wo Werkbänke unter Y -30 oder über Y 95 stehen
  (Koordinaten im Chat, Glockenton, 60 Sekunden Lichtstrahl).
- Warnung an das betroffene Team eine Minute vor jeder Anzeige.
- Hinweis beim Aufstellen, wenn die Werkbank in einem angezeigten Höhenbereich steht.
- Befehl `/sc anzeige` löst die Anzeige sofort aus.
- `/sc status` zeigt, wann die nächste Anzeige kommt.
- Neue Einstellungen unter `anzeige:` in der `config.yml`.

## [1.0.0] - 2026-10-13

### Neu
- Team-Werkbänke mit den StealCraft-Regeln: unendlich Leben, solange die Werkbank steht, sonst ein Leben.
- Ausscheiden mit Bann (oder Zuschauermodus zum Testen).
- Werkbank klauen, tauschen und zurückgeben; nur das eigene Team kann sie aufstellen.
- Schutz der Werkbank vor Lava, Feuer, Explosionen, Kolben, Despawn und Void.
- Herz-Anzeige in der Tab-Liste.
- Befehle zum Verwalten von Teams, Werkbänken und Ausgeschiedenen.

[1.3.2]: ../../compare/v1.3.1...v1.3.2
[1.3.1]: ../../compare/v1.3.0...v1.3.1
[1.3.0]: ../../compare/v1.2.0...v1.3.0
[1.2.0]: ../../compare/v1.1.0...v1.2.0
[1.1.0]: ../../compare/v1.0.0...v1.1.0
[1.0.0]: ../../releases/tag/v1.0.0
