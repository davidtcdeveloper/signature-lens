#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform sampler2D uInputTexture;
uniform float uVignetteStrength;
uniform int uHasFaces;

// Simple tone mapping approximation (S-curve)
vec3 toneMap(vec3 color) {
    // Basic S-curve: x^2 * (3 - 2x)
    return color * color * (3.0 - 2.0 * color);
}

// Convert RGB to HSV
vec3 rgbToHsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// Convert HSV to RGB
vec3 hsvToRgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec4 color = texture(uInputTexture, vTexCoord);
    
    // 1. Tone Mapping
    vec3 mapped = toneMap(color.rgb);
    
    // 2. Face-Aware Color Grading
    if (uHasFaces == 1) {
        // When faces detected: apply warmer, flattering adjustments
        // Convert to HSV for easier color manipulation
        vec3 hsv = rgbToHsv(mapped);
        
        // Increase warmth (shift hue slightly toward red/yellow)
        hsv.x = mod(hsv.x + 0.02, 1.0);
        
        // Increase saturation slightly for more "pop"
        hsv.y = min(hsv.y * 1.1, 1.0);
        
        // Slight brightness increase for flattering effect
        hsv.z = min(hsv.z * 1.05, 1.0);
        
        mapped = hsvToRgb(hsv);
    } else {
        // Base rendering: subtle warmth and color grading
        mapped.r += 0.03; // Mild warmth
        mapped.g -= 0.01;
        mapped.b -= 0.01;
    }
    
    // 3. Vignette
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(vTexCoord, center);
    float vignette = smoothstep(0.8, 0.2, dist * uVignetteStrength);
    
    mapped *= vignette;
    
    outColor = vec4(mapped, 1.0);
}
