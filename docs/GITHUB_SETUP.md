# Publier EchoVision sur GitHub

Dépôt conseillé : **EchoVision**. Sources Fabric 1.21.1, shaders GLSL, tests, Gradle et documentation.

## Préparer le poste

1. Installer **Git** et **GitHub CLI (`gh`)** si nécessaire :

   ```powershell
   winget install --id Git.Git --source winget
   winget install --id GitHub.cli --source winget
   ```

   Redémarrer le terminal, puis vérifier `git --version` et `gh --version`.

2. Se connecter à GitHub **dans son propre navigateur** :

   ```powershell
   gh auth login --web
   ```

   Suivre la procédure GitHub. Ne pas placer de mot de passe, jeton ou adresse personnelle dans le dépôt.

3. Dans GitHub **Settings > Emails**, activer l'option de confidentialité de l'adresse e-mail et relever l'adresse `noreply` attribuée par GitHub. **Avant le premier commit**, vérifier que Git n'utilise pas l'adresse personnelle. Définir l'auteur local du projet :

   ```powershell
   git config --local user.name "Thomas_Draco"
   ```

   Configurer ensuite `user.email` **localement** avec l'adresse `noreply` que GitHub affiche dans les paramètres. Ne pas utiliser une adresse fictive et ne pas enregistrer d'adresse personnelle dans le dépôt.

## Première publication

Ouvrir un terminal PowerShell **dans le dossier racine** qui contient `README.md`, `build.gradle`, `src/` et `.gitignore` :

```powershell
git init -b main
git config --local user.name "Thomas_Draco"
# Configurer user.email (adresse noreply GitHub) AVANT le commit.
git add .
git status
git commit -m "Initial release: EchoVision 2.5.0"
gh repo create EchoVision --public --source=. --remote=origin --push
gh repo view --web
```

Avant la création du dépôt, remplacer `--public` par `--private` si la publication du code doit rester privée.

Ne pas générer de README, `.gitignore` ou LICENSE supplémentaires via l'interface GitHub : ces fichiers existent déjà.

La première synchronisation peut nécessiter une authentification locale complémentaire.

## Mises à jour

```powershell
git status
git add .
git commit -m "Improve acoustic rendering"
git push
```

Pour une version distribuable (uniquement après validation des tests en jeu) :

```powershell
git tag v2.5.0
git push origin v2.5.0
```

## Contenu à versionner

- `src/main/java/` : code Java et mixins.
- `src/main/resources/` : fichiers Fabric, traductions et shaders GLSL.
- `src/test/` : tests Java.
- `docs/` : architecture et documentation de publication.
- `build.gradle`, `gradle.properties`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/wrapper/`.
- `README.md`, `CHANGELOG.md`, `LICENSE`, `.gitignore`, `.gitattributes`.

## Contenu à exclure

`.gradle/`, `build/`, `run/`, `.idea/`, fichiers de configuration locaux, secrets et journaux.

Le script `gradlew` du projet télécharge et vérifie l'intégrité de `gradle-wrapper.jar` s'il est absent. Si celui-ci est généré sur le poste, il peut aussi être versionné.

## Contrôle avant de pousser

- Vérifier `git status` et `git diff --cached --stat`.
- Ne pas publier de données personnelles ni de secrets.
- Vérifier `git config --local --get user.email` (utiliser une adresse `noreply` GitHub).
- Tester `./gradlew.bat clean build` avant de publier une release binaire.
- GitHub stocke les sources ; une release GitHub ou Modrinth distribue le JAR compilé.
