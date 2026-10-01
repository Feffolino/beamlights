#version 150

// Gamma mask: brightens the beam cone. Mirrors it.ratlab.beamlights.core.MaskMath (mask, lift).
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
uniform float Gamma;
uniform float Lift;
uniform vec3 Tint;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(ColorSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 1.0) { // sky
        fragColor = color;
        return;
    }
    vec4 clip = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 world = InvViewProj * clip;
    vec3 rel = world.xyz / world.w - BeamOrigin;
    float dist = length(rel);
    if (dist >= Range || dist < 1e-4) {
        fragColor = color;
        return;
    }
    float cone = smoothstep(CosOuter, CosInner, dot(rel / dist, BeamDir));
    float m = Strength * cone * pow(1.0 - dist / Range, Falloff);
    if (m <= 0.0) {
        fragColor = color;
        return;
    }
    vec3 lifted = pow(max(color.rgb, vec3(0.0)), vec3(1.0 / (1.0 + Gamma * m))) + Lift * m * Tint;
    fragColor = vec4(min(lifted, vec3(1.0)), color.a);
}
