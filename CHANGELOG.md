# Changelog

## 2.5.0 — EchoVision

- Nom, identifiant Fabric, artefact Gradle, paquetages Java et namespace GLSL unifiés : `echovision`.
- Auteur du projet : `Thomas_Draco`.
- Commentaires de code réduits à des notes techniques ponctuelles.
- Licence MIT complète et documentation d'architecture autonome.
- Dossier de préparation Modrinth.
- Aucun changement volontaire du moteur d'écholocalisation ou du gameplay de la 2.4.0.

**Migration :** désinstaller tout ancien JAR utilisant l'identifiant `warder` avant d'installer `echovision`.

## 2.4.0 — Commande client

- Commande locale `/echovision on|off`.
- Suppression de l'item et du point d'entrée commun.
- Activation non persistante, réinitialisée à la déconnexion.

## 2.3.0 — Living Echo

- Prise en charge des entités silencieuses traversées par les ondes.
- Maillage de blocs non cubiques à partir des `VoxelShape`.
- Extraction des parties cubiques des modèles d'entités compatibles.
