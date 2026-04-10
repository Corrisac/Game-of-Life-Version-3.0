#version 430

// ═══════════════════════════════════════════════════════════════════════
// Conway V6.1 — Single-WG Bit-Parallel, Optimized
// ═══════════════════════════════════════════════════════════════════════
// 1024 threads (max WG size) for maximum occupancy & latency hiding.
// Unrolled 2-iteration ping-pong eliminates dynamic array indexing.
// Each thread handles 2 uints (64 cells) per generation.

layout(local_size_x = 1024) in;

layout(rgba8, binding = 0) uniform readonly  image2D imgIn;
layout(rgba8, binding = 1) uniform writeonly image2D imgOut;

uniform int uIterations;

shared uint tileA[2048];
shared uint tileB[2048];

// ── Bit-parallel Conway on 32 cells packed in one uint ─────────────
// Reads 9 neighbor words, computes next generation via adder tree.
// Returns: bit=1 alive, bit=0 dead.
uint conway(uint a_l, uint a_c, uint a_r,
            uint c_l, uint c_c, uint c_r,
            uint b_l, uint b_c, uint b_r) {
    uint nw=(a_c>>1u)|(a_l<<31u); uint n=a_c; uint ne=(a_c<<1u)|(a_r>>31u);
    uint w =(c_c>>1u)|(c_l<<31u);             uint e =(c_c<<1u)|(c_r>>31u);
    uint sw=(b_c>>1u)|(b_l<<31u); uint s=b_c; uint se=(b_c<<1u)|(b_r>>31u);

    uint s0=nw^n, c0=nw&n, s1=ne^w, c1=ne&w;
    uint s2=e^sw, c2=e&sw, s3=s^se, c3=s&se;

    uint a0=s0^s1, a0c=s0&s1;
    uint t0=c0^c1, a1=t0^a0c, a2=(c0&c1)|(t0&a0c);
    uint d0=s2^s3, d0c=s2&s3;
    uint t1=c2^c3, d1=t1^d0c, d2=(c2&c3)|(t1&d0c);

    uint bit0=a0^d0, cr0=a0&d0;
    uint x1=a1^d1, bit1=x1^cr0, cr1=(a1&d1)|(x1&cr0);
    uint x2=a2^d2;
    uint bit3=(a2&d2)|(x2&cr1);

    return ~bit3 & ~(x2^cr1) & bit1 & (bit0 | c_c);
}

void main() {
    uint tid = gl_LocalInvocationIndex;   // 0..1023

    // ── PHASE 1: Load image → pack into tileA ────────────────────────
    for (uint i = tid; i < 2048u; i += 1024u) {
        uint row = i >> 3u;
        uint col = i & 7u;
        uint word = 0u;
        uint px = col * 32u;
        for (uint b = 0u; b < 32u; b++) {
            if (imageLoad(imgIn, ivec2(int(px + b), int(row))).r < 0.5)
                word |= (1u << b);
        }
        tileA[i] = word;
    }
    barrier();

    // ── PHASE 2: Iterate — unrolled 2-step ping-pong ─────────────────
    int iter = 0;
    for (; iter + 1 < uIterations; iter += 2) {
        // Even step: tileA → tileB
        for (uint idx = tid; idx < 2048u; idx += 1024u) {
            uint row = idx >> 3u, col = idx & 7u;
            uint rA=(row-1u)&255u, rB=(row+1u)&255u;
            uint cL=(col-1u)&7u,   cR=(col+1u)&7u;
            tileB[idx] = conway(
                tileA[rA*8u+cL], tileA[rA*8u+col], tileA[rA*8u+cR],
                tileA[row*8u+cL], tileA[idx],       tileA[row*8u+cR],
                tileA[rB*8u+cL], tileA[rB*8u+col], tileA[rB*8u+cR]);
        }
        barrier();

        // Odd step: tileB → tileA
        for (uint idx = tid; idx < 2048u; idx += 1024u) {
            uint row = idx >> 3u, col = idx & 7u;
            uint rA=(row-1u)&255u, rB=(row+1u)&255u;
            uint cL=(col-1u)&7u,   cR=(col+1u)&7u;
            tileA[idx] = conway(
                tileB[rA*8u+cL], tileB[rA*8u+col], tileB[rA*8u+cR],
                tileB[row*8u+cL], tileB[idx],       tileB[row*8u+cR],
                tileB[rB*8u+cL], tileB[rB*8u+col], tileB[rB*8u+cR]);
        }
        barrier();
    }

    // Handle odd remainder
    if (iter < uIterations) {
        for (uint idx = tid; idx < 2048u; idx += 1024u) {
            uint row = idx >> 3u, col = idx & 7u;
            uint rA=(row-1u)&255u, rB=(row+1u)&255u;
            uint cL=(col-1u)&7u,   cR=(col+1u)&7u;
            tileB[idx] = conway(
                tileA[rA*8u+cL], tileA[rA*8u+col], tileA[rA*8u+cR],
                tileA[row*8u+cL], tileA[idx],       tileA[row*8u+cR],
                tileA[rB*8u+cL], tileA[rB*8u+col], tileA[rB*8u+cR]);
        }
        barrier();
    }

    // ── PHASE 3: Unpack result → write to image ──────────────────────
    // Result is in tileA if even iterations, tileB if odd
    for (uint i = tid; i < 2048u; i += 1024u) {
        uint row = i >> 3u;
        uint col = i & 7u;
        uint word = ((uIterations & 1) == 0) ? tileA[i] : tileB[i];
        uint px = col * 32u;
        for (uint b = 0u; b < 32u; b++) {
            float val = ((word >> b) & 1u) != 0u ? 0.0 : 1.0;
            imageStore(imgOut, ivec2(int(px + b), int(row)), vec4(val, val, val, 1.0));
        }
    }
}
