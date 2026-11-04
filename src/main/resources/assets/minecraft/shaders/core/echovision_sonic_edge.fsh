#version 150

in vec2 fragUv;
in vec4 fragVertexColor;

uniform float WaveRadius;
uniform float WaveAge;
uniform float WaveStrength;
uniform float PixelSize;
uniform float SurfaceMode;
uniform vec3 WaveColor;

out vec4 fragColor;

void main() {


    vec2 cellCenter = (floor(gl_FragCoord.xy / max(PixelSize, 2.0)) + 0.5)
                      * max(PixelSize, 2.0);
    vec2 sampleUV = fragUv
        + dFdx(fragUv) * (cellCenter.x - gl_FragCoord.x)
        + dFdy(fragUv) * (cellCenter.y - gl_FragCoord.y);

    float distanceBlocks = max(0.0, sampleUV.x);

    float quantizedDistance = floor(distanceBlocks * 8.0 + 0.5) / 8.0;
    float separation = abs(quantizedDistance - WaveRadius);
    float crest = 1.0 - smoothstep(0.18, 0.78, separation);
    float passed = step(quantizedDistance, WaveRadius);
    float travelled = max(0.0, WaveRadius - quantizedDistance);
    float afterimage = passed * exp(-travelled * 0.52) * 0.43;

    if (SurfaceMode > 0.5) {


        float ring = 1.0 - smoothstep(0.16, 0.48, separation);
        float stepping = floor(clamp(ring * WaveStrength, 0.0, 1.0) * 5.0 + 0.5) / 5.0;
        if (stepping <= 0.0) discard;
        vec3 ripple = mix(WaveColor, vec3(0.80, 0.96, 0.95), 0.22);
        fragColor = vec4(ripple, stepping * 0.32);
        return;
    }

    float signal = max(crest, afterimage);

    float transverse = abs(sampleUV.y);
    float core = 1.0 - smoothstep(0.14, 0.40, transverse);
    float halo = (1.0 - smoothstep(0.32, 1.00, transverse)) * 0.25;
    float brightness = signal * (core + halo) * WaveStrength * fragVertexColor.a;


    float level = floor(clamp(brightness, 0.0, 1.0) * 5.0 + 0.5) / 5.0;
    if (level <= 0.0) discard;

    float whitening = 0.16 + 0.35 * crest * core;
    vec3 palette = mix(WaveColor, vec3(0.92, 0.98, 1.0), whitening);
    float alpha = min(0.98, level * (0.55 + core * 0.43));
    fragColor = vec4(palette, alpha);
}
