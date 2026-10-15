# StealCraft

Paper-Plugin für Minecraft **26.3**, gebaut für das Minecraft-Projekt *StealCraft*:
Jedes Team hat eine eigene Werkbank. Solange sie steht, kann das Team unendlich oft sterben.
Wird sie geklaut, hat jeder nur noch ein Leben.

[Neueste Version herunterladen](../../releases/latest) · [Änderungen](CHANGELOG.md) · [Fehler melden](../../issues/new/choose)

---

## Die Regeln

- Jedes Team bekommt eine eigene, leuchtende **Team-Werkbank**.
- **Bevor** ein Team seine Werkbank zum ersten Mal aufstellt, gelten normale Minecraft-Regeln.
- **Steht** die Werkbank, kann das Team unendlich oft sterben.
- **Ist die Werkbank weg** (abgebaut, im Inventar, in einer Kiste), ist jeder Tod endgültig.
  Wer dann stirbt, scheidet aus und wird gebannt. Nur dieser Spieler, nicht das ganze Team.
- Jeder darf jede Werkbank abbauen. Sie wird dann zu einem Item, das man behalten,
  verstecken, tauschen oder zurückgeben kann.
- Nur das eigene Team kann seine Werkbank wieder aufstellen.
- Jedes Team hat **nur eine Werkbank**.
- Die Werkbank ist an das **Stammbett** des Teams gebunden. Das ist das **erste Bett**, das ein Teammitglied aufstellt.
  Die Werkbank darf nur im Umkreis von **50 Blöcken** davon stehen. Weitere Betten ändern daran nichts.
  Geht das Stammbett kaputt, wird das nächste Bett, das ein Teammitglied aufstellt, zum Stammbett.
- Stirbt jemand mit einer Werkbank im Inventar, wird sie ganz normal gedroppt.
- Die Werkbank darf **nicht außerhalb der Weltbegrenzung** (Worldborder) stehen. Wer sie dort aufstellen will,
  **disqualifiziert sein ganzes Team**: alle Mitglieder scheiden aus und werden gebannt.

Damit die Werkbank nie verloren geht:

- Als Item verbrennt sie nicht in Lava oder Feuer, despawnt nicht und wird aus dem Void zurückgeholt.
- Als Block können sie nur Spieler abbauen; Creeper, TNT, Feuer und Kolben können ihr nichts anhaben.
- Sie kann nicht als Brennstoff oder Crafting-Zutat benutzt werden.
- Sie darf nicht in die Enderkiste, weil man sie dort nie klauen könnte (abschaltbar).

In der Tab-Liste steht vor jedem Namen ein Herz:
**grün** = Werkbank steht, **rot** = nur noch ein Leben, **grau** = Regeln noch nicht aktiv, **☠** = ausgeschieden.

### Werkbank-Anzeige

Alle 30 Minuten wird allen Spielern angezeigt, wo Werkbänke stehen, die **unter Y -30** oder **über Y 95** aufgestellt sind:
Koordinaten im Chat, ein Glockenton und 60 Sekunden lang ein Lichtstrahl über der Werkbank.
Das betroffene Team wird eine Minute vorher gewarnt. Werkbänke zwischen Y -30 und Y 95 werden nie angezeigt.

## Installation

1. Der Server muss **Paper** (oder ein Paper-Fork wie Purpur) für Minecraft 26.3 sein. Mit Vanilla funktionieren keine Plugins.
2. Die `.jar` aus der [neuesten Version](../../releases/latest) herunterladen.
3. In den Ordner `plugins` des Servers legen und den Server neu starten.
4. Zum Testen in `plugins/StealCraft/config.yml` `ausscheiden: ZUSCHAUER` setzen,
   damit ihr euch nicht selbst bannt. Für das echte Projekt wieder auf `BAN` stellen.

## Teams einrichten (als OP)

```
/sc team erstellen rot red
/sc team hinzufügen rot Spieler1
/sc team hinzufügen rot Spieler2
/sc werkbank rot Spieler1
```

Die Spieler müssen einmal auf dem Server gewesen sein, bevor man sie einem Team hinzufügen kann.

## Befehle

| Befehl | Wer | Was |
|---|---|---|
| `/sc status` | alle | Teams, Mitglieder, Werkbank-Status und nächste Anzeige |
| `/sc team erstellen <name> [farbe]` | OP | Team anlegen (Farben: red, blue, green, gold, aqua, …) |
| `/sc team hinzufügen <name> <spieler>` | OP | Spieler in ein Team |
| `/sc team entfernen <spieler>` | OP | Spieler aus seinem Team nehmen |
| `/sc team löschen <name>` | OP | Team löschen |
| `/sc team liste` | OP | Alle Teams anzeigen |
| `/sc werkbank <team> [spieler]` | OP | Werkbank geben. Ältere Werkbänke des Teams werden ungültig. |
| `/sc wiederbeleben <spieler>` | OP | Ausgeschiedenen Spieler zurückholen und entbannen |
| `/sc zurücksetzen <team>` | OP | Team wieder auf „Regeln noch nicht aktiv“ |
| `/sc anzeige` | OP | Werkbank-Anzeige sofort auslösen (zum Testen) |
| `/sc nether sperren` / `öffnen` | OP | Nether sperren oder wieder öffnen. Wer hineingehen will, wird abgewiesen. Rausgehen bleibt möglich. |
| `/sc end sperren` / `öffnen` | OP | End sperren oder wieder öffnen (gleiche Regeln wie das Nether) |

Als OP zeigt `/sc status` auch, wo die Werkbänke stehen. Normale Spieler sehen das nicht.
Berechtigung für alle OP-Befehle: `stealcraft.admin`.

## Einstellungen

Datei: `plugins/StealCraft/config.yml`. Nach Änderungen den Server neu starten.

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `ausscheiden` | `BAN` | `BAN` oder `ZUSCHAUER` |
| `dieb-verraten` | `false` | `true` = Name des Diebs steht im Chat |
| `enderkiste-sperren` | `true` | Werkbänke dürfen nicht in die Enderkiste |
| `bett-abstand` | `50` | Größter Abstand zwischen Werkbank und dem Stammbett des Teams |
| `anzeige.aktiv` | `true` | Werkbank-Anzeige an/aus |
| `anzeige.intervall-minuten` | `30` | Wie oft die Anzeige kommt |
| `anzeige.unter-y` | `-30` | Angezeigt wird, was tiefer steht |
| `anzeige.ueber-y` | `95` | … oder höher |
| `anzeige.strahl-sekunden` | `60` | Dauer des Lichtstrahls (0 = aus) |

Die Spielstände liegen in `plugins/StealCraft/daten.yml`.

## Selbst bauen

Voraussetzung: Java 25 und Gradle.

```
gradle build
```

Die fertige Datei liegt danach in `build/libs/`.

## Neue Version veröffentlichen (für Maintainer)

1. Änderungen unter einer neuen Überschrift in [`CHANGELOG.md`](CHANGELOG.md) eintragen, z. B. `## [1.2.0] - 2026-11-01`.
2. In `build.gradle.kts` die `version` auf dieselbe Nummer setzen.
3. Committen und pushen.
4. Auf GitHub unter **Actions → Release → Run workflow** starten
   (oder alternativ einen Tag `v1.2.0` pushen).
5. GitHub baut das Plugin und legt automatisch ein Release mit der `.jar` und den Patchnotes aus dem Changelog an.

Versionsnummern: **1.x.0** für neue Funktionen, **1.0.x** für Fehlerbehebungen, **2.0.0** wenn sich Regeln oder Befehle grundlegend ändern.

## Lizenz

GNU General Public License v3.0, siehe [LICENSE](LICENSE). Das Plugin nutzt die Paper-API, die ebenfalls unter GPL-3.0 steht.

---

Kein offizielles Minecraft-Produkt. Nicht von Mojang oder Microsoft genehmigt oder mit ihnen verbunden.
*NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.*
