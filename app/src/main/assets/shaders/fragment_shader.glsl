#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform sampler2D uInputTexture;
uniform float uVignetteStrength;

// Simple tone mapping approximation (S-curve)
vec3 toneMap(vec3 color) {
    // Basic S-curve: x^2 * (3 - 2x)
    return color * color * (3.0 - 2.0 * color);
}

void main() {
    vec4 color = texture(uInputTexture, vTexCoord);
    
    // 1. Tone Mapping
    vec3 mapped = toneMap(color.rgb);
    
    // 2. Color Grading (Warm/Magenta bias)
    // Add subtle warmth (more Red/Green) and Magenta (Red/Blue)
    mapped.r += 0.05; // Warmth
    mapped.g -= 0.02; // Magenta (less green)
    mapped.b -= 0.02; // Warmth (less blue)
    
    // 3. Vignette
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(vTexCoord, center);
    float vignette = smoothstep(0.8, 0.2, dist * uVignetteStrength);
    
    mapped *= vignette;
    
    outColor = vec4(mapped, 1.0);
}
