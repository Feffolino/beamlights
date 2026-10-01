#version 150

// Beam mask: inside the beam cone surfaces look like under night vision (scene colour / lightmap max at the lit
// point's light level). Mirrors it.ratlab.beamlights.core.MaskMath (mask, light, softClip).
uniform sampler2D ColorSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProj;  // inverse(projection * camera rotation): clip -> camera-relative world
uniform vec3 BeamOrigin;   // camera-relative
uniform vec3 BeamDir;      // unit
uniform float CosOuter;
uniform float CosInner;
uniform float Range;
uniform float Falloff;
uniform float Strength;
uniform float NightVision;
uniform float LightMax;   // largest channel of the lightmap at the lit point's light level
uniform float Shading;
uniform float Knee;
uniform float BrightCutoff;
uniform float BlackLift;
uniform vec3 Tint;

in vec2 texCoord;

out vec4 fragColor;

vec3 softClip(vec3 x) {
    float r = 1.0 - Knee;
    vec3 over = Knee + r * (1.0 - exp(-(x - Knee) / r));
    return mix(x, over, step(Knee, x));
}

void main() {
    vec4 color = texture(ColorSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    vec4 clip = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 world = InvViewProj * clip;
    vec3 pos = world.xyz / world.w;
    // Surface normal from screen-space derivatives, computed before any branch (derivatives need the whole quad).
    vec3 n = cross(dFdx(pos), dFdy(pos));
    if (depth >= 1.0) { // sky
        fragColor = color;
        return;
    }
    vec3 rel = pos - BeamOrigin;
    float dist = length(rel);
    if (dist >= Range || dist < 1e-4) {
        fragColor = color;
        return;
    }
    vec3 toLight = -rel / dist;
    float cone = smoothstep(CosOuter, CosInner, dot(-toLight, BeamDir));
    float nl = length(n);
    float lambert = nl > 1e-8 ? abs(dot(n / nl, toLight)) : 1.0;
    float shade = mix(1.0, lambert, Shading);
    float m = Strength * cone * shade * pow(1.0 - dist / Range, Falloff);
    if (m <= 0.0) {
        fragColor = color;
        return;
    }
    vec3 c = max(color.rgb, vec3(0.0));
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    float lightMax = max(0.02, LightMax);
    float g = max(0.0, NightVision / lightMax - 1.0) * min(1.0, m)
            * (1.0 - smoothstep(BrightCutoff * 0.3, BrightCutoff, luma));
    vec3 lit = c * (1.0 + g * Tint) + BlackLift * m * Tint;
    // Light only adds: the knee never darkens pixels that were already bright.
    fragColor = vec4(max(c, softClip(lit)), color.a);
}
