# 1.1.0
Introduce an animated health/target UI with hover preview support.

- RenderConfig: add HOVER_BG_COLOR, HOVER_TEXT_COLOR and FONT_SHADOW options.
- Add HoverPreviewState to track transient preview box state.
- Extend TargetState with spawnBlend for smooth spawn transitions.
- Rename DamageDetectEvent -> DamageDetection (refactor only).
- Add HealthAnimation: replaces HealthBarRenderEvent with a unified renderer that provides animated entries, hover previews, handoff logic, color blending (lerpColor/lerpColorARGB), alpha application, and entity icon rendering.
- Remove deprecated HealthBarRenderEvent.

# 1.1.1
- Add language keys for the newly added config options