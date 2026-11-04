# EchoVision

Vision acoustique client-side pour Minecraft **1.21.1** (Fabric / Java 21).

**Auteur :** Thomas_Draco  
**Version :** 2.5.0  
**Licence :** MIT

Le monde devient noir. Les sons déclenchent des ondes GLSL pixelisées qui révèlent brièvement les arêtes, les surfaces voxelisées et certaines silhouettes d'entités. L'effet ne rajoute ni objet, ni bloc, ni commande serveur.

## Utilisation

```mcfunction
/echovision on
/echovision off
```

La commande est locale (Fabric Client Command API), accessible sans permission opérateur. L'effet est désactivé au lancement et à chaque déconnexion.

## Installation

- Minecraft Java 1.21.1
- Fabric Loader ≥ 0.16.14
- Fabric API (branche Minecraft 1.21.1)
- Java 21 pour compiler

Installez le JAR final dans `mods/` **uniquement côté client**. Retirez les anciennes versions portant l'identifiant `warder` : elles constituent désormais un mod distinct aux yeux du loader.

## Compilation

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
```

Le fichier à installer se trouve dans `build/libs/echovision-2.5.0.jar` (pas `-sources.jar`).

## Limites

- Les sons distants ne sont perceptibles que s'ils sont déjà transmis et joués par Minecraft.
- Les entités non transmises par le serveur ne sont pas détectées.
- Les silhouettes natives sont approximatives pour certains modèles, avec un maillage procédural de secours.
- Compatibilité avec Iris, Sodium et les autres modifications profondes du rendu : **non validée**.

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — fonctionnement et architecture, indépendant de ce README.
- [`docs/MODRINTH_DESCRIPTION.md`](docs/MODRINTH_DESCRIPTION.md) — texte anglais prêt à adapter.
- [`docs/MODRINTH_PUBLISHING.md`](docs/MODRINTH_PUBLISHING.md) — paramètres, vérifications et règles de publication.
- [`docs/MODRINTH_CHANGELOG.md`](docs/MODRINTH_CHANGELOG.md) — notes de version.
