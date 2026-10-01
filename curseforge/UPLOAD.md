# CurseForge upload checklist

Files in this folder: `summary.txt` (project summary), `description.md` (project description, paste in the
Markdown editor), `changelog-1.1.3.md` (file changelog).

## 1. Before uploading
- [ ] Icon in the mod: `src/main/resources/beamlights_logo.png` (square PNG, e.g. 256x256).
- [ ] Rebuild: `./gradlew build` -> `build/libs/beamlights-1.1.3+1.21.1.jar`.
- [ ] Screenshots / GIF: replace the `<!-- SCREENSHOT -->` comments in `description.md` (or upload them to the
      project Images tab and link them).

## 2. Create the project (Minecraft > Mods)
| Field | Value |
|---|---|
| Name | Beam Lights |
| Summary | contents of `summary.txt` |
| Description | contents of `description.md` (Markdown) |
| Avatar | project icon (same image as the logo, 400x400 recommended) |
| Main category | Cosmetic |
| Other categories | Utility & QoL, API and Library |
| License | MIT |
| Source URL | https://github.com/Feffolino/beamlights |
| Issues URL | https://github.com/Feffolino/beamlights/issues |

## 3. Upload the file
| Field | Value |
|---|---|
| File | `build/libs/beamlights-1.1.3+1.21.1.jar` (NOT -api, -sources, -javadoc) |
| Display name | Beam Lights 1.1.3 (1.21.1 NeoForge) |
| Release type | Release |
| Game version | 1.21.1 |
| Mod loader | NeoForge |
| Environment | Client and Server (each side optional) |
| Java | Java 21 |
| Changelog | contents of `changelog-1.1.3.md` (Markdown) |

## 4. Relations (file upload page, "Related projects")
| Project | Type |
|---|---|
| LambDynamicLights | Optional dependency |
| Sodium Dynamic Lights | Optional dependency |
| Omega Flashlight | Optional dependency |
| Curios API | Optional dependency |
| KubeJS | Optional dependency |

Do not mark any as Required: the mod loads without them (no light is drawn without a dynamic lights mod, but the server
side still works).

## 5. After approval
- [ ] Set `displayURL` in `src/main/templates/META-INF/neoforge.mods.toml` to the CurseForge page if preferred
      (now the GitHub repo).
- [ ] Tag the release on GitHub: `git tag v1.1.3 && git push origin v1.1.3`.
