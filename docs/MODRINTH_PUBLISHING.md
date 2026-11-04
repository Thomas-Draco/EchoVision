# EchoVision — Dossier de publication Modrinth

**Statut :** brouillon de page et de métadonnées, prêt à recopier après validation de la licence, du JAR final, des images et de l'éligibilité aux règles Modrinth.

## Métadonnées proposées

| Champ | Valeur |
|---|---|
| Type | Mod (pas Shader : les shaders GLSL sont intégrés à un mod Fabric) |
| Titre | EchoVision |
| Slug souhaité | `echovision` (**disponibilité à vérifier**, ne pas créer d'URL supposée existante) |
| Auteur | Thomas_Draco (à utiliser comme nom de contributeur sur la page) |
| Résumé anglais | `See Minecraft through sound with pixelated GLSL echoes revealing terrain and entities.` |
| Catégorie principale | Adventure / Game Mechanics selon les choix disponibles au moment du formulaire |
| Étiquette | Fabric |
| Version Minecraft | 1.21.1 uniquement (autres versions non testées) |
| Environnement | Client only / Server unsupported (aucun besoin côté serveur) |
| Licence | MIT |
| Dépendance obligatoire | Fabric API |
| Version initiale | 2.5.0, une fois la compilation et le test en jeu validés |
| Fichier distribué | `echovision-2.5.0.jar` (ne pas téléverser le ZIP des sources comme release jouable) |
| Icône et galerie | Screenshots réels du mod et icône réalisée manuellement, ou pas d'icône |

## Étapes

1. Ouvrir **https://modrinth.com/** et s'identifier soi-même ; menu du compte → **Create a project**.
2. Choisir **Mod**, saisir `EchoVision`, un slug disponible et le résumé ci-dessus.
3. Copier le contenu de `MODRINTH_DESCRIPTION.md` dans la description longue (anglais).
4. Sélectionner Fabric / Minecraft 1.21.1, **client only**, MIT, puis indiquer Fabric API en dépendance.
5. Ajouter de vraies captures F2 faites en jeu (sol, escalier, créatures). Ne pas utiliser d'images produites ou retouchées par IA générative.
6. Compiler `./gradlew build`, vérifier le JAR sans suffixe `sources`, puis tester en jeu après renommage complet de l'ID.
7. Importer ce JAR comme version 2.5.0, avec les notes de `MODRINTH_CHANGELOG.md`.
8. Vérifier les disclosures, la conformité de la contribution humaine et les règles en vigueur avant toute demande de mise en public.

## Point réglementaire essentiel (règles en vigueur depuis août 2026)

Modrinth demande la divulgation explicite de l'usage notable d'IA générative. Un projet presque entièrement produit via prompts puis testé par une personne **n'est pas éligible à une publication publique** selon la politique actuelle. Le simple nettoyage des commentaires ne change rien à sa provenance.

Le présent historique implique une contribution importante d'un assistant IA au code et à la description : il faut **activer la disclosure « Contains AI-generated content »**. Déclarer également fidèlement la nature de l'assistance. Si la contribution humaine originale n'est pas prépondérante, envisager un statut **unlisted/private** ou continuer le développement humain avant une candidature à publication publique. La décision d'éligibilité appartient à Modrinth.

La politique interdit également les icônes, captures et autres images générées ou modifiées par IA générative sur les pages des projets. Les captures F2 authentiques du rendu du jeu sont préférables.

Sources publiques à consulter lors de la mise en ligne :
- https://modrinth.com/legal/rules
- https://modrinth.com/news/article/ai-policy-and-disclosures/
- https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai
- https://support.modrinth.com/en/articles/16567675-content-disclosures

## Tests avant distribution

- [ ] `gradlew clean build` réussit après tous les renommages.
- [ ] `gradlew runClient` démarre sans erreur de namespace shader.
- [ ] `/echovision on` puis `/echovision off` fonctionnent.
- [ ] Blocs non cubiques et entités révélés correctement.
- [ ] Déconnexion : désactivation et reprise normale de l'affichage.
- [ ] Le JAR est nommé `echovision-2.5.0.jar`, le mod apparaît sous **EchoVision**, auteur **Thomas_Draco**.
- [ ] Les anciens JAR `warder` ont été retirés.
- [ ] Pas de captures ni de logo IA pour la page.
- [ ] Disclosure d'usage IA exacte et éligibilité de publication vérifiée.
