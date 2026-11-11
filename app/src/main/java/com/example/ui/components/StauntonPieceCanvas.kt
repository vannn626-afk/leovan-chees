package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.engine.ChessPiece
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.ui.theme.ChessThemeColors
import com.example.ui.theme.LocalChessThemeColors

/**
 * High-Definition Custom Vector/Canvas Staunton Chess Piece Renderer.
 * Renders all 12 distinct chess pieces (White & Black King, Queen, Rook, Bishop, Knight, Pawn)
 * using scalable 100x100 normalized geometry, 3D linear/radial shading, specular highlights,
 * carved details, and high-contrast strokes so pieces are razor-sharp at any resolution.
 */
@Composable
fun StauntonPieceIcon(
    piece: ChessPiece,
    modifier: Modifier = Modifier,
    themeColors: ChessThemeColors = LocalChessThemeColors.current
) {
    Canvas(modifier = modifier) {
        drawStauntonPiece(piece = piece, themeColors = themeColors)
    }
}

fun DrawScope.drawStauntonPiece(
    piece: ChessPiece,
    themeColors: ChessThemeColors,
    topLeftOffset: Offset = Offset.Zero,
    boxSize: Float = size.minDimension
) {
    val isWhite = piece.color == PieceColor.WHITE
    val fillMain = if (isWhite) themeColors.whitePieceFill else themeColors.blackPieceFill
    val fillHighlight = if (isWhite) themeColors.whitePieceHighlight else themeColors.blackPieceHighlight
    val fillShade = if (isWhite) themeColors.whitePieceShade else themeColors.blackPieceShade
    val strokeColor = if (isWhite) themeColors.whitePieceStroke else themeColors.blackPieceStroke
    val innerDetailColor = if (isWhite) {
        themeColors.whitePieceStroke.copy(alpha = 0.55f)
    } else {
        themeColors.blackPieceStroke.copy(alpha = 0.72f)
    }

    val s = boxSize / 100f
    fun x(v: Float) = topLeftOffset.x + v * s
    fun y(v: Float) = topLeftOffset.y + v * s
    fun pt(vx: Float, vy: Float) = Offset(x(vx), y(vy))

    val bodyBrush = Brush.linearGradient(
        colors = listOf(fillHighlight, fillMain, fillShade),
        start = pt(22f, 15f),
        end = pt(80f, 88f)
    )
    val mainStroke = Stroke(width = (2.8f * s).coerceAtLeast(1.2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val detailStroke = Stroke(width = (1.8f * s).coerceAtLeast(0.9f), cap = StrokeCap.Round, join = StrokeJoin.Round)

    // 1. Soft drop shadow beneath the Staunton pedestal
    drawOval(
        color = Color.Black.copy(alpha = 0.36f),
        topLeft = pt(15f, 85f),
        size = Size(70f * s, 9f * s)
    )

    // Helper for standard tiered Staunton pedestal base (y: 78..90)
    fun drawPedestalBase(widthFactor: Float = 1.0f) {
        val leftOuter = 50f - 32f * widthFactor
        val rightOuter = 50f + 32f * widthFactor
        val leftInner = 50f - 26f * widthFactor
        val rightInner = 50f + 26f * widthFactor

        // Bottom plinth
        val plinthPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(pt(leftOuter, 83f), pt(rightOuter, 90f)),
                    cornerRadius = CornerRadius(3.5f * s, 3.5f * s)
                )
            )
        }
        drawPath(plinthPath, brush = bodyBrush, style = Fill)
        drawPath(plinthPath, color = strokeColor, style = mainStroke)

        // Upper cushion ring of base
        val cushionPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(pt(leftInner, 77f), pt(rightInner, 83.5f)),
                    cornerRadius = CornerRadius(4f * s, 4f * s)
                )
            )
        }
        drawPath(cushionPath, brush = bodyBrush, style = Fill)
        drawPath(cushionPath, color = strokeColor, style = mainStroke)

        // Specular base highlight line
        drawLine(
            color = innerDetailColor,
            start = pt(leftInner + 4f, 80f),
            end = pt(rightInner - 4f, 80f),
            strokeWidth = 1.4f * s
        )
    }

    when (piece.type) {
        PieceType.PAWN -> {
            drawPedestalBase(0.88f)
            // Tapered column & shoulders
            val columnPath = Path().apply {
                moveTo(x(31f), y(77f))
                cubicTo(x(34f), y(64f), x(40f), y(53f), x(41f), y(43f))
                lineTo(x(59f), y(43f))
                cubicTo(x(60f), y(53f), x(66f), y(64f), x(69f), y(77f))
                close()
            }
            drawPath(columnPath, brush = bodyBrush, style = Fill)
            drawPath(columnPath, color = strokeColor, style = mainStroke)

            // Neck collar ring
            val collarPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(pt(36f, 39f), pt(64f, 44.5f)),
                        cornerRadius = CornerRadius(3f * s, 3f * s)
                    )
                )
            }
            drawPath(collarPath, brush = bodyBrush, style = Fill)
            drawPath(collarPath, color = strokeColor, style = mainStroke)

            // Spherical finial head
            drawCircle(
                brush = bodyBrush,
                radius = 13.5f * s,
                center = pt(50f, 27f)
            )
            drawCircle(
                color = strokeColor,
                radius = 13.5f * s,
                center = pt(50f, 27f),
                style = mainStroke
            )
            // 3D specular glint on head
            drawCircle(
                color = fillHighlight.copy(alpha = 0.75f),
                radius = 4f * s,
                center = pt(45f, 22.5f)
            )
        }

        PieceType.ROOK -> {
            drawPedestalBase(0.96f)
            // Fortress tower column
            val towerPath = Path().apply {
                moveTo(x(28f), y(77f))
                lineTo(x(33f), y(38f))
                lineTo(x(26f), y(32f))
                lineTo(x(26f), y(18f))
                // 3 crenellations (merlons & embrasures)
                lineTo(x(36f), y(18f))
                lineTo(x(36f), y(25f))
                lineTo(x(44f), y(25f))
                lineTo(x(44f), y(18f))
                lineTo(x(56f), y(18f))
                lineTo(x(56f), y(25f))
                lineTo(x(64f), y(25f))
                lineTo(x(64f), y(18f))
                lineTo(x(74f), y(18f))
                lineTo(x(74f), y(32f))
                lineTo(x(67f), y(38f))
                lineTo(x(72f), y(77f))
                close()
            }
            drawPath(towerPath, brush = bodyBrush, style = Fill)
            drawPath(towerPath, color = strokeColor, style = mainStroke)

            // Parapet neck band & vertical masonry fluting lines
            drawLine(innerDetailColor, pt(33f, 38f), pt(67f, 38f), strokeWidth = 2f * s)
            drawLine(innerDetailColor, pt(31f, 71f), pt(69f, 71f), strokeWidth = 1.6f * s)
            drawLine(innerDetailColor, pt(42f, 43f), pt(40f, 67f), strokeWidth = 1.5f * s)
            drawLine(innerDetailColor, pt(58f, 43f), pt(60f, 67f), strokeWidth = 1.5f * s)
        }

        PieceType.KNIGHT -> {
            drawPedestalBase(0.96f)
            // Sculpted Staunton horse head, arched neck, mane & snout
            val knightPath = Path().apply {
                moveTo(x(27f), y(77f))
                // Chest & throat curve
                cubicTo(x(26f), y(63f), x(33f), y(53f), x(39f), y(45f))
                // Lower jaw & chin
                lineTo(x(27f), y(48f))
                cubicTo(x(20f), y(49f), x(18f), y(43f), x(21f), y(37f))
                // Muzzle & bridge of nose
                lineTo(x(34f), y(25f))
                // Forelock & pointed ears
                lineTo(x(37f), y(14f))
                lineTo(x(45f), y(21f))
                lineTo(x(52f), y(13f))
                lineTo(x(55f), y(22f))
                // Powerful arched crest & sculpted mane down to base
                cubicTo(x(69f), y(27f), x(75f), y(44f), x(71f), y(77f))
                close()
            }
            drawPath(knightPath, brush = bodyBrush, style = Fill)
            drawPath(knightPath, color = strokeColor, style = mainStroke)

            // Eye & nostril & mane detail lines
            drawCircle(
                color = strokeColor,
                radius = 2.4f * s,
                center = pt(37f, 29f)
            )
            drawCircle(
                color = fillHighlight,
                radius = 0.9f * s,
                center = pt(36.5f, 28.5f)
            )
            // Nostril
            drawCircle(
                color = strokeColor,
                radius = 1.5f * s,
                center = pt(25f, 40f)
            )
            // Mouth line
            drawLine(strokeColor, pt(23f, 44f), pt(33f, 41f), strokeWidth = 1.8f * s)
            // Carved mane ridge
            val maneLine = Path().apply {
                moveTo(x(53f), y(24f))
                cubicTo(x(62f), y(33f), x(65f), y(49f), x(62f), y(72f))
            }
            drawPath(maneLine, color = innerDetailColor, style = detailStroke)
        }

        PieceType.BISHOP -> {
            drawPedestalBase(0.95f)
            // Slender body & tall pointed mitre dome
            val bishopPath = Path().apply {
                moveTo(x(29f), y(77f))
                cubicTo(x(32f), y(64f), x(37f), y(55f), x(38f), y(47f))
                lineTo(x(62f), y(47f))
                cubicTo(x(63f), y(55f), x(68f), y(64f), x(71f), y(77f))
                close()
            }
            drawPath(bishopPath, brush = bodyBrush, style = Fill)
            drawPath(bishopPath, color = strokeColor, style = mainStroke)

            // Mitre head above collar
            val mitrePath = Path().apply {
                moveTo(x(38f), y(45f))
                cubicTo(x(29f), y(36f), x(33f), y(21f), x(50f), y(15f))
                cubicTo(x(67f), y(21f), x(71f), y(36f), x(62f), y(45f))
                close()
            }
            drawPath(mitrePath, brush = bodyBrush, style = Fill)
            drawPath(mitrePath, color = strokeColor, style = mainStroke)

            // Collar ring
            val collar = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(pt(34f, 43.5f), pt(66f, 48.5f)),
                        cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
                    )
                )
            }
            drawPath(collar, brush = bodyBrush, style = Fill)
            drawPath(collar, color = strokeColor, style = mainStroke)

            // Finial orb on top of mitre
            drawCircle(brush = bodyBrush, radius = 4.2f * s, center = pt(50f, 11.5f))
            drawCircle(color = strokeColor, radius = 4.2f * s, center = pt(50f, 11.5f), style = mainStroke)

            // Classic Staunton Bishop mitre diagonal slash/cross emblem
            drawLine(
                color = strokeColor,
                start = pt(43f, 27f),
                end = pt(57f, 27f),
                strokeWidth = 2.4f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = pt(50f, 20f),
                end = pt(50f, 34f),
                strokeWidth = 2.4f * s,
                cap = StrokeCap.Round
            )
        }

        PieceType.QUEEN -> {
            drawPedestalBase(1.0f)
            // Regal sculpted torso & 5-pointed scalloped coronet
            val queenBody = Path().apply {
                moveTo(x(26f), y(77f))
                cubicTo(x(30f), y(62f), x(34f), y(50f), x(34f), y(44f))
                // Leftmost crown spike
                lineTo(x(20f), y(22f))
                lineTo(x(35f), y(34f))
                // Second crown spike
                lineTo(x(36f), y(18f))
                lineTo(x(45f), y(32f))
                // Center tall crown spike
                lineTo(x(50f), y(15f))
                lineTo(x(55f), y(32f))
                // Fourth crown spike
                lineTo(x(64f), y(18f))
                lineTo(x(65f), y(34f))
                // Rightmost crown spike
                lineTo(x(80f), y(22f))
                lineTo(x(66f), y(44f))
                cubicTo(x(66f), y(50f), x(70f), y(62f), x(74f), y(77f))
                close()
            }
            drawPath(queenBody, brush = bodyBrush, style = Fill)
            drawPath(queenBody, color = strokeColor, style = mainStroke)

            // Crown base band
            val band = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(pt(32f, 42f), pt(68f, 47f)),
                        cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
                    )
                )
            }
            drawPath(band, brush = bodyBrush, style = Fill)
            drawPath(band, color = strokeColor, style = mainStroke)

            // 5 Pearl orbs atop the coronet tips
            val pearls = listOf(
                pt(20f, 20f),
                pt(36f, 16f),
                pt(50f, 13f),
                pt(64f, 16f),
                pt(80f, 20f)
            )
            for (centerPt in pearls) {
                drawCircle(brush = bodyBrush, radius = 3.2f * s, center = centerPt)
                drawCircle(color = strokeColor, radius = 3.2f * s, center = centerPt, style = detailStroke)
            }

            // Facet lines inside gown/coronet
            drawLine(innerDetailColor, pt(42f, 48f), pt(38f, 73f), strokeWidth = 1.6f * s)
            drawLine(innerDetailColor, pt(58f, 48f), pt(62f, 73f), strokeWidth = 1.6f * s)
        }

        PieceType.KING -> {
            drawPedestalBase(1.02f)
            // Imperial Cross Finial at the apex (y: 4..20)
            val crossPath = Path().apply {
                moveTo(x(47f), y(5f))
                lineTo(x(53f), y(5f))
                lineTo(x(53f), y(10f))
                lineTo(x(59f), y(10f))
                lineTo(x(59f), y(15f))
                lineTo(x(53f), y(15f))
                lineTo(x(53f), y(21f))
                lineTo(x(47f), y(21f))
                lineTo(x(47f), y(15f))
                lineTo(x(41f), y(15f))
                lineTo(x(41f), y(10f))
                lineTo(x(47f), y(10f))
                close()
            }
            drawPath(crossPath, brush = bodyBrush, style = Fill)
            drawPath(crossPath, color = strokeColor, style = mainStroke)

            // Flared Imperial Crown & Column
            val kingBody = Path().apply {
                moveTo(x(26f), y(77f))
                cubicTo(x(29f), y(62f), x(34f), y(50f), x(35f), y(43f))
                // Flared upper crown chalice
                lineTo(x(26f), y(27f))
                quadraticTo(x(50f), y(18f), x(74f), y(27f))
                lineTo(x(65f), y(43f))
                cubicTo(x(66f), y(50f), x(71f), y(62f), x(74f), y(77f))
                close()
            }
            drawPath(kingBody, brush = bodyBrush, style = Fill)
            drawPath(kingBody, color = strokeColor, style = mainStroke)

            // Double royal collar rings
            val upperRing = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(pt(32f, 40f), pt(68f, 44.5f)),
                        cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
                    )
                )
            }
            drawPath(upperRing, brush = bodyBrush, style = Fill)
            drawPath(upperRing, color = strokeColor, style = mainStroke)

            // Jewel bevels on upper crown chalice
            drawLine(innerDetailColor, pt(50f, 22f), pt(50f, 39f), strokeWidth = 2f * s)
            drawLine(innerDetailColor, pt(37f, 24f), pt(41f, 39f), strokeWidth = 1.6f * s)
            drawLine(innerDetailColor, pt(63f, 24f), pt(59f, 39f), strokeWidth = 1.6f * s)
            drawLine(innerDetailColor, pt(42f, 47f), pt(38f, 73f), strokeWidth = 1.6f * s)
            drawLine(innerDetailColor, pt(58f, 47f), pt(62f, 73f), strokeWidth = 1.6f * s)
        }
    }
}
