# StealCraft-Plugin

Paper-Plugin für Minecraft 26.3, das die Werkbank-Regel von StealCraft umsetzt.

## Die Regeln

- Jedes Team bekommt eine eigene, leuchtende **Team-Werkbank**.
- **Bevor** ein Team seine Werkbank zum ersten Mal aufstellt, gelten normale Minecraft-Regeln.
- **Steht** die Werkbank, kann das Team unendlich oft sterben.
- **Ist die Werkbank weg** (abgebaut, im Inventar, in einer Kiste), ist jeder Tod endgültig.
  Wer dann stirbt, scheidet aus und wird gebannt. Nur dieser Spieler, nicht das ganze Team.
- Jeder darf jede Werkbank abbauen. Sie wird dann zu einem Item, das man behalten,
  verstecken, tauschen oder zurückgeben kann (zum Beispiel gegen 20 Netherite-Barren).
- Nur das eigene Team kann seine Werkbank wieder aufstellen.
- Stirbt jemand mit einer Werkbank im Inventar, wird sie ganz normal gedroppt.

Damit die Werkbank nie verloren geht:

- Als Item verbrennt sie nicht in Lava oder Feuer, despawnt nicht und wird aus dem Void zurückgeholt.
- Als Block können sie nur Spieler abbauen; Creeper, TNT, Feuer und Kolben können ihr nichts anhaben.
- Sie kann nicht als Brennstoff oder Crafting-Zutat benutzt werden.
- Sie darf nicht in die Enderkiste, weil man sie dort nie klauen könnte (abschaltbar).

In der Tab-Liste steht vor jedem Namen ein Herz:
**grün** = Werkbank steht, **rot** = nur noch ein Leben, **grau** = Regeln noch nicht aktiv, **☠** = ausgeschieden.

## Plugin-Datei bekommen

Die `.jar` wird automatisch auf GitHub gebaut:

1. Neues GitHub-Repository anlegen und diesen ganzen Ordner hochladen.
2. Im Repository auf **Actions** gehen. Der Lauf „Plugin bauen“ startet von selbst.
3. Wenn er grün ist, den Lauf öffnen und unten unter **Artifacts** „StealCraft-Plugin“ herunterladen.
4. Darin liegt `StealCraft-1.0.0.jar`.

Selbst bauen geht auch: Java 25 installieren und im Ordner `gradle build` ausführen.
Die Datei liegt dann in `build/libs/`.

## Installieren

1. Der Server muss **Paper** (oder Purpur) für Minecraft 26.3 sein, kein Vanilla-Server.
2. `StealCraft-1.0.0.jar` in den Ordner `plugins` des Servers legen.
3. Server neu starten.
4. Zum Testen in `plugins/StealCraft/config.yml` `ausscheiden: ZUSCHAUER` setzen,
   damit ihr euch nicht selbst bannt. Für das echte Projekt wieder auf `BAN` stellen.

## Teams einrichten (als OP)

```
/sc team erstellen rot red
/sc team hinzufügen rot DeinName
/sc team hinzufügen rot FreundName
/sc werkbank rot DeinName
```

Die Spieler müssen einmal auf dem Server gewesen sein, bevor man sie einem Team hinzufügen kann.

## Alle Befehle

| Befehl | Wer | Was |
|---|---|---|
| `/sc status` | alle | Teams, Mitglieder und Werkbank-Status anzeigen |
| `/sc team erstellen <name> [farbe]` | OP | Team anlegen (Farben: red, blue, green, gold, aqua, …) |
| `/sc team hinzufügen <name> <spieler>` | OP | Spieler in ein Team |
| `/sc team entfernen <spieler>` | OP | Spieler aus seinem Team nehmen |
| `/sc team löschen <name>` | OP | Team löschen |
| `/sc team liste` | OP | Alle Teams anzeigen |
| `/sc werkbank <team> [spieler]` | OP | Werkbank geben. Ältere Werkbänke des Teams werden ungültig. |
| `/sc wiederbeleben <spieler>` | OP | Ausgeschiedenen Spieler zurückholen und entbannen |
| `/sc zurücksetzen <team>` | OP | Team wieder auf „Regeln noch nicht aktiv“ |

Als OP zeigt `/sc status` auch, wo die Werkbänke stehen. Normale Spieler sehen das nicht.

## Einstellungen (`plugins/StealCraft/config.yml`)

| Einstellung | Bedeutung |
|---|---|
| `ausscheiden` | `BAN` oder `ZUSCHAUER` |
| `dieb-verraten` | `true` = Name des Diebs steht im Chat |
| `enderkiste-sperren` | `true` = Werkbänke dürfen nicht in die Enderkiste |

Die Spielstände liegen in `plugins/StealCraft/daten.yml`.
