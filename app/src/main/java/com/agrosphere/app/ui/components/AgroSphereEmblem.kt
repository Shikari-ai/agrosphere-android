package com.agrosphere.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ─── Palette ────────────────────────────────────────────────────────────────

private val GlobeMid    = Color(0xFF153824)
private val GlobeDark   = Color(0xFF061510)
private val LeafBright  = Color(0xFF8BD146)
private val LeafDeep    = Color(0xFF4FA63A)
private val DotGreen    = Color(0xFF52A834)
private val RingGreen   = Color(0xFFB6E85A)
private val RingSky     = Color(0xFF67D4F0)
private val PlasmaWhite = Color(0xFFECFDF5)

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun Float.emblerSin() = sin(toDouble()).toFloat()
private fun Float.emblerCos() = cos(toDouble()).toFloat()
private fun orbitPoint(cx: Float, cy: Float, r: Float, angleDeg: Float): Offset {
    val rad = angleDeg * PI.toFloat() / 180f
    return Offset(cx + r * rad.emblerCos(), cy + r * rad.emblerSin())
}

// ─── Main composable ─────────────────────────────────────────────────────────

/**
 * AgroSphere emblem — a digital globe with sprouting leaf, a primary equatorial
 * orbit ring, and a tilted secondary orbit ring. Drawn entirely in Canvas.
 *
 * @param ringProgress  0..1 — sweeps the primary orbit arc + secondary arc
 * @param leafScale     0..1 — animates the leaf sprout growing in
 */
@Composable
fun AgroSphereEmblem(
    modifier: Modifier = Modifier,
    ringProgress: Float = 1f,
    leafScale: Float = 1f,
) {
    Canvas(modifier) {
        val s  = size.minDimension
        val cx = size.width  / 2f
        val cy = size.height / 2f
        val c  = Offset(cx, cy)
        val globeR = s * 0.295f
        val rp     = ringProgress.coerceIn(0f, 1f)
        val ls     = leafScale.coerceIn(0f, 1f)

        // ── Outer radial glow behind globe ──
        drawCircle(
            brush = Brush.radialGradient(
                0f   to Color(0xFF10B981).copy(alpha = 0.18f),
                0.5f to Color(0xFF10B981).copy(alpha = 0.06f),
                1f   to Color.Transparent,
                center = c, radius = globeR * 2.2f,
            ),
            radius = globeR * 2.2f,
            center = c,
        )

        // ── Globe sphere ──
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GlobeMid, GlobeDark),
                center = Offset(cx - globeR * 0.28f, cy - globeR * 0.28f),
                radius = globeR * 1.4f,
            ),
            radius = globeR,
            center = c,
        )

        // ── Digital dot-continent grid ──
        val step = globeR * 0.12f
        var gy   = cy - globeR
        while (gy <= cy + globeR) {
            var gx = cx - globeR
            while (gx <= cx + globeR) {
                val dx = gx - cx; val dy = gy - cy
                if (sqrt(dx * dx + dy * dy) < globeR * 0.91f) {
                    val n = sin((gx * 0.068f).toDouble()).toFloat() * cos((gy * 0.058f).toDouble()).toFloat() +
                            sin(((gx + gy) * 0.048f).toDouble()).toFloat()
                    if (n > 0.18f) {
                        val a = (0.22f + 0.55f * (n - 0.18f).coerceIn(0f, 1f))
                        drawCircle(DotGreen.copy(alpha = a), radius = s * 0.006f, center = Offset(gx, gy))
                    }
                }
                gx += step
            }
            gy += step
        }

        // ── Globe rim highlight ──
        drawCircle(
            color  = LeafBright.copy(alpha = 0.38f),
            radius = globeR,
            center = c,
            style  = Stroke(width = s * 0.009f),
        )

        // ── Specular glint (top-left bright arc) ──
        drawArc(
            brush      = Brush.sweepGradient(
                0.0f to PlasmaWhite.copy(alpha = 0.20f),
                0.12f to PlasmaWhite.copy(alpha = 0.04f),
                1.0f to Color.Transparent,
            ),
            startAngle = 200f,
            sweepAngle = 70f,
            useCenter  = false,
            topLeft    = Offset(cx - globeR, cy - globeR),
            size       = Size(globeR * 2f, globeR * 2f),
            style      = Stroke(width = s * 0.012f, cap = StrokeCap.Round),
        )

        // ── Leaf sprout ──
        val base = Offset(cx, cy + globeR * 0.30f)
        val len  = globeR * 1.08f * ls
        if (len > 0.5f) {
            drawLeaf(base, len,         -24f, LeafBright, LeafDeep,  s)
            drawLeaf(base, len * 0.80f,  26f, LeafDeep,  LeafBright, s)
        }

        // ── Primary equatorial orbit ring ──
        val orbitR  = s * 0.435f
        val sweep1  = 315f * rp
        drawArc(
            brush      = Brush.sweepGradient(
                0f   to Color.Transparent,
                0.3f to RingGreen.copy(alpha = 0.5f),
                0.7f to RingGreen,
                1f   to RingSky,
            ),
            startAngle = -100f,
            sweepAngle = sweep1,
            useCenter  = false,
            topLeft    = Offset(cx - orbitR, cy - orbitR),
            size       = Size(orbitR * 2f, orbitR * 2f),
            style      = Stroke(width = s * 0.0065f, cap = StrokeCap.Round),
        )
        // Orbit node dots
        listOf(-100f, 0f, 90f).forEach { ang ->
            if (sweep1 >= (ang + 100f)) {
                val np = orbitPoint(cx, cy, orbitR, ang)
                // Glow
                drawCircle(
                    brush  = Brush.radialGradient(
                        0f to PlasmaWhite.copy(alpha = 0.6f),
                        1f to Color.Transparent,
                        center = np, radius = s * 0.025f,
                    ),
                    radius = s * 0.025f, center = np,
                )
                drawCircle(color = PlasmaWhite, radius = s * 0.011f, center = np)
            }
        }

        // ── Secondary tilted orbit (thinner, partial) ──
        if (rp > 0.25f) {
            val orbit2R   = s * 0.40f
            val tilt      = 50f                          // visual tilt via scale
            val tiltScale = 0.38f
            val sweep2    = 240f * ((rp - 0.25f) / 0.75f).coerceIn(0f, 1f)
            // Draw as an ellipse arc via transforming the draw scope
            drawOvalArc(
                cx = cx, cy = cy,
                rx = orbit2R, ry = orbit2R * tiltScale,
                startAngle = -130f, sweepAngle = sweep2,
                color = RingSky.copy(alpha = 0.6f),
                strokeWidth = s * 0.004f,
            )
            // Node dot on secondary orbit
            if (sweep2 > 60f) {
                val a2 = (-130f + sweep2 * 0.7f) * PI.toFloat() / 180f
                val np = Offset(
                    cx + orbit2R * a2.emblerCos(),
                    cy + orbit2R * tiltScale * a2.emblerSin(),
                )
                drawCircle(color = RingSky.copy(alpha = 0.8f), radius = s * 0.009f, center = np)
            }
        }

        // ── Tertiary tiny accent ring (outermost, very faint) ──
        if (rp > 0.65f) {
            val orbit3R = s * 0.48f
            val a3      = ((rp - 0.65f) / 0.35f).coerceIn(0f, 1f)
            drawCircle(
                color  = RingGreen.copy(alpha = 0.10f * a3),
                radius = orbit3R,
                center = c,
                style  = Stroke(width = s * 0.003f),
            )
        }
    }
}

// ─── Oval arc helper (approximates a tilted ring) ────────────────────────────

private fun DrawScope.drawOvalArc(
    cx: Float, cy: Float,
    rx: Float, ry: Float,
    startAngle: Float, sweepAngle: Float,
    color: Color,
    strokeWidth: Float,
) {
    // Build a Path of line segments approximating the ellipse arc
    val steps = (sweepAngle.toInt().coerceAtLeast(8))
    val path  = Path()
    for (step in 0..steps) {
        val deg = startAngle + step.toFloat() * sweepAngle / steps
        val rad = deg * PI.toFloat() / 180f
        val x   = cx + rx * rad.emblerCos()
        val y   = cy + ry * rad.emblerSin()
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path  = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

// ─── Leaf helper ─────────────────────────────────────────────────────────────

/** Almond leaf from [base] toward an angle (0° = up), built from two quad curves. */
private fun DrawScope.drawLeaf(
    base: Offset,
    length: Float,
    angleDeg: Float,
    fill: Color,
    vein: Color,
    s: Float,
) {
    val rad  = angleDeg * PI.toFloat() / 180f
    val dir  = Offset(sin(rad.toDouble()).toFloat(), -cos(rad.toDouble()).toFloat())
    val perp = Offset(cos(rad.toDouble()).toFloat(),  sin(rad.toDouble()).toFloat())
    val tip  = Offset(base.x + length * dir.x,  base.y + length * dir.y)
    val mid  = Offset((base.x + tip.x) / 2f, (base.y + tip.y) / 2f)
    val w    = length * 0.40f

    val path = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(mid.x + perp.x * w, mid.y + perp.y * w, tip.x, tip.y)
        quadraticTo(mid.x - perp.x * w, mid.y - perp.y * w, base.x, base.y)
        close()
    }
    drawPath(path, fill)
    drawLine(vein.copy(alpha = 0.55f), base, tip, strokeWidth = s * 0.007f, cap = StrokeCap.Round)
}
