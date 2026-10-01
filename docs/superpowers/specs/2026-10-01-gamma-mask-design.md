# Gamma mask (Beam Lights 1.2.0) — design

Goal: the local player's beam is drawn as a screen-space post pass ("gamma mask") instead of a cone of physical
dynamic lights. No chunk rebuilds for the visual cone; the physical light of the local player shrinks to the central
hit point. Visual only: block light level is unchanged (spawn blocking is server-side and unaffected).

## Decisions (approved 2026-10-01)
- Approach A: vanilla GL post pass, no new dependency (Veil not required).
- Works in vanilla rendering; when an Iris shader pack is in use, or in third person, or on any render failure, the
  mask turns off automatically and the existing backend (SDL/LDL) handles the local beam exactly as in 1.1.0.
- Mask active => local player keeps only the central ray as a physical light (no side rays, no midpoints, no LDL cone).
  Other emitters are unchanged.

## Rendering
- `RenderLevelStageEvent` stage `AFTER_LEVEL` (world done, hand not drawn yet).
- Copy color and depth of the main target into an offscreen `TextureTarget` (glBlitFramebuffer, stencil matched).
- Fullscreen quad with core shader `beamlights:gamma_mask` on the main target: reconstruct the camera-relative position
  from depth (inverse of projection x model-view), compute
  `m = strength * cone(cosAngle) * distance(dist)`; output `pow(c, 1/(1 + gamma*m)) + lift*m*tint`. Sky (depth 1) is
  untouched. Camera ~ flashlight in first person, so the visible surface is the lit surface (natural occlusion).
- Per frame direction: camera look vector when the tick beam is within 12 degrees of the player's view (hand-held),
  else the tick beam direction. Origin = camera + (beam origin - eye) offset, clamped to 1 block.
- Strength fades toward `maskStrength * luminance/15` (follows Omega flicker) with `maskFadeSeconds`.

## Units
- `core/MaskPolicy` (pure): reason ACTIVE / DISABLED / FAILED / SHADER_PACK / NOT_FIRST_PERSON / NO_BEAM.
- `core/MaskMath` (pure, Java twin of the shader): cone, distance falloff, lift, approach. Unit tested.
- `client/mask/IrisCompat`: reflection on `IrisApi.isShaderPackInUse()`.
- `client/mask/GammaMask`: per tick state (wanted, published local beam), status line.
- `client/mask/GammaMaskRenderer`: shader registration, targets, draw; failures -> FAILED (logged once).
- `client/debug/MaskCommands`: `/beamlights mask [show|<key> [value]]`.
- Ticker: local emitter with mask => one pass, no midpoints, no cone; publishes its first beam.

## Config `[mask]`
gammaMask true, maskStrength 1.0, maskGamma 2.5, maskLift 0.04, maskAngleScale 1.0, maskSoftness 0.35,
maskRangeScale 1.0, maskFalloff 0.7, maskTint 0.25, maskFadeSeconds 0.15, maskMidpoints false (keep central-ray
midpoints as physical light).

## Testing
JUnit for MaskPolicy and MaskMath; in game: A/B `/beamlights mask enabled false|true`, overlay line `mask: ...`,
Iris pack on => reason SHADER_PACK and old behaviour.
