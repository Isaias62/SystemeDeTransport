# Système de transport — Gatineau

Application Java (Swing) pour consulter les horaires d'un réseau de bus à Gatineau et trouver un trajet entre deux arrêts. Le réseau est affiché sur une carte OpenStreetMap.

## Fonctionnalités

- **Carte interactive** : carte OpenStreetMap où l'on peut se déplacer à la souris (glisser) et zoomer (molette). Chaque ligne a sa couleur, et le nom de chaque arrêt est affiché.
- **Recherche de trajet** (onglet *Trajet*) : on choisit un arrêt de départ, un arrêt d'arrivée et une heure. L'application trouve le trajet qui arrive le plus tôt, avec une correspondance au besoin, et le trace sur la carte (départ en vert, arrivée en rouge).
- **Horaires** (onglet *Horaires*) : on choisit une ligne, une direction et un arrêt pour voir toutes les heures de passage de la journée. Le prochain bus est sélectionné automatiquement et la ligne est mise en évidence sur la carte.

## Prérequis

- Java 17 ou plus récent
- Une connexion Internet (pour télécharger les tuiles de la carte)
- Les librairies, déjà incluses à la racine du projet :
  - `jxmapviewer2-2.5.jar` — affichage de la carte
  - `commons-logging-1.1.3.jar` — dépendance de JXMapViewer

## Lancer le projet

### Avec VS Code

1. Ouvrir le dossier `SystemeDeTransport` dans VS Code (avec l'extension *Extension Pack for Java*).
2. Ouvrir `src/InterfaceSysteme.java` et cliquer sur **Run**.

Les librairies sont déjà déclarées dans `.vscode/settings.json`.

### En ligne de commande

Depuis le dossier du projet (sur Windows, remplacer `:` par `;` dans le classpath) :

```bash
javac -encoding UTF-8 -cp "jxmapviewer2-2.5.jar:commons-logging-1.1.3.jar" -d bin $(find src -name "*.java")
java -cp "bin:jxmapviewer2-2.5.jar:commons-logging-1.1.3.jar" InterfaceSysteme
```

## Structure du projet

```
SystemeDeTransport/
├── src/
│   ├── InterfaceSysteme.java      Fenêtre principale (onglets Trajet et Horaires)
│   └── CarteMap/
│       ├── MapViewer.java         Panneau contenant la carte OpenStreetMap
│       ├── ArretBus.java          Dessine les lignes, les arrêts et le trajet sur la carte
│       ├── BusStop.java           Un arrêt de bus (nom + position GPS)
│       ├── LigneBus.java          Une ligne de bus et le calcul de ses horaires
│       ├── ReseauBus.java         Les données du réseau et la recherche de trajet
│       └── Trajet.java            Résultat d'une recherche (une ou plusieurs étapes)
├── bin/                           Fichiers .class compilés
├── jxmapviewer2-2.5.jar
└── commons-logging-1.1.3.jar
```

## Fonctionnement

### Horaires

Chaque ligne (`LigneBus`) est définie par :

- la liste de ses arrêts, dans l'ordre ;
- le temps (en minutes) entre deux arrêts consécutifs ;
- l'heure du premier et du dernier départ du terminus ;
- la fréquence (un bus toutes les *n* minutes).

L'heure de passage à un arrêt = heure de départ du terminus + temps de parcours jusqu'à cet arrêt. Les bus roulent dans les deux sens. Dans le code, les heures sont stockées en minutes depuis minuit (par exemple 6h30 = 390).

### Recherche de trajet

`ReseauBus.chercherTrajet()` utilise une variante de l'**algorithme de Dijkstra** où la « distance » est l'heure d'arrivée à chaque arrêt :

1. On part de l'arrêt de départ à l'heure demandée.
2. Pour chaque ligne qui passe à l'arrêt courant, et dans chaque direction, on prend le prochain bus.
3. On calcule l'heure d'arrivée à chacun des arrêts suivants sur cette ligne, et on garde la meilleure.
4. On recommence à partir de l'arrêt atteint le plus tôt, jusqu'à l'arrivée.

Ainsi, les correspondances entre lignes sont trouvées automatiquement.

## Réseau actuel

| Ligne    | Arrêts                                                                                   | Service       | Fréquence |
|----------|------------------------------------------------------------------------------------------|---------------|-----------|
| Ligne 11 | Parc de la Gatineau → Station Centrale → Casino du Lac-Leamy → Place du Portage → Musée canadien de l'histoire | 6h00 – 22h30  | 15 min    |
| Ligne 22 | Les Promenades Gatineau → Station Centrale → Université du Québec (UQO)                  | 6h10 – 22h10  | 20 min    |
| Ligne 33 | Université du Québec (UQO) → Place du Portage → Musée canadien de l'histoire → Casino du Lac-Leamy | 6h05 – 21h35  | 30 min    |

> **Note :** ces horaires sont fictifs (ce ne sont pas les vraies données de la STO). Les positions des arrêts sont approximatives.

### Ajouter un arrêt ou une ligne

Tout se trouve dans le constructeur de `ReseauBus` :

```java
BusStop gare = ajouterArret(45.4500, -75.7000, "Nouvel arrêt");

lignes.add(new LigneBus("Ligne 44", Color.ORANGE,
        List.of(gare, centrale, uqo),       // arrêts dans l'ordre
        new int[] {7, 12},                   // minutes entre les arrêts
        heure(6, 0), heure(22, 0), 20));     // premier départ, dernier départ, fréquence
```

L'arrêt et la ligne apparaissent automatiquement sur la carte et dans les menus.

## Limites connues

- Sur la carte, les trajets sont des lignes droites entre les arrêts : ils ne suivent pas les rues.
- On choisit des arrêts dans une liste et non une adresse : transformer une adresse en position GPS demanderait un service externe (géocodage).
- Les horaires sont les mêmes tous les jours (pas de distinction semaine / fin de semaine).
- Il n'y a pas de service après minuit.

## Remarque technique

L'URL par défaut d'OpenStreetMap dans JXMapViewer 2.5 utilise `http://`. Le serveur redirige maintenant vers `https://`, et Java ne suit pas cette redirection, ce qui laissait la carte vide. `MapViewer` utilise donc directement `https://tile.openstreetmap.org` et définit un *User-Agent*, comme l'exige la [politique d'utilisation des tuiles d'OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/).

## Crédits

- Fonds de carte : © les contributeurs d'[OpenStreetMap](https://www.openstreetmap.org/copyright)
- Librairie de carte : [JXMapViewer2](https://github.com/msteiger/jxmapviewer2)
