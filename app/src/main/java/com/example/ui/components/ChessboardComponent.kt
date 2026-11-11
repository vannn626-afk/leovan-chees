package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.BoardState
import com.example.engine.ChessMove
import com.example.engine.ChessPiece
import com.example.engine.MoveFlag
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.engine.Square
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalChessThemeColors

/**
 * Interactive 8x8 Chessboard Component with HD Staunton pieces,
 * Tap-to-Move + Drag-and-Drop support, smooth piece slide animations,
 * rank/file coordinates, and Beginner Mode tactical overlays.
 */
@Composable
fun InteractiveChessboard(
    boardState: BoardState,
    selectedSquare: Square?,
    legalMovesForSelected: List<ChessMove>,
    flipped: Boolean = false,
    showLegalIndicators: Boolean = true,
    beginnerModeEnabled: Boolean = false,
    threatenedSquares: Set<Square> = emptySet(),
    hintMove: ChessMove? = null,
    animationDurationMs: Int = 180,
    interactive: Boolean = true,
    onSquareClicked: (Square) -> Unit,
    onPieceDragged: (Square, Square) -> Unit = { from, to ->
        onSquareClicked(from)
        onSquareClicked(to)
    },
    modifier: Modifier = Modifier
) {
    val themeColors = LocalChessThemeColors.current
    val textMeasurer = rememberTextMeasurer()

    var draggingFromSquare by remember { mutableStateOf<Square?>(null) }
    var dragCurrentOffset by remember { mutableStateOf(Offset.Zero) }

    val lastMove = boardState.moveHistory.lastOrNull()
    val moveAnimProgress = remember(lastMove) { Animatable(if (lastMove != null && animationDurationMs > 0) 0f else 1f) }

    LaunchedEffect(lastMove) {
        if (lastMove != null && animationDurationMs > 0) {
            moveAnimProgress.snapTo(0f)
            moveAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing)
            )
        } else {
            moveAnimProgress.snapTo(1f)
        }
    }

    fun screenOffsetToSquare(offset: Offset, canvasWidth: Float, canvasHeight: Float): Square? {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return null
        val sqW = canvasWidth / 8f
        val sqH = canvasHeight / 8f
        val col = (offset.x / sqW).toInt()
        val row = (offset.y / sqH).toInt()
        if (col !in 0..7 || row !in 0..7) return null
        val file = if (flipped) 7 - col else col
        val rank = if (flipped) row else 7 - row
        return Square(file, rank)
    }

    fun squareToGridColRow(sq: Square): Pair<Int, Int> {
        val col = if (flipped) 7 - sq.file else sq.file
        val row = if (flipped) sq.rank else 7 - sq.rank
        return col to row
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(themeColors.boardFrameOuter, RoundedCornerShape(10.dp))
            .border(2.dp, themeColors.boardFrameInner, RoundedCornerShape(10.dp))
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .semantics { contentDescription = "Chessboard 8 by 8" }
            .testTag("chessboard_canvas")
            .pointerInput(flipped, interactive) {
                if (!interactive) return@pointerInput
                detectTapGestures { tapOffset ->
                    val sq = screenOffsetToSquare(tapOffset, size.width.toFloat(), size.height.toFloat())
                    if (sq != null) {
                        onSquareClicked(sq)
                    }
                }
            }
            .pointerInput(flipped, interactive, boardState.turn) {
                if (!interactive) return@pointerInput
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val sq = screenOffsetToSquare(startOffset, size.width.toFloat(), size.height.toFloat())
                        if (sq != null) {
                            val piece = boardState.pieceAt(sq)
                            if (piece != null && piece.color == boardState.turn) {
                                draggingFromSquare = sq
                                dragCurrentOffset = startOffset
                                onSquareClicked(sq)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragCurrentOffset = change.position
                    },
                    onDragEnd = {
                        val from = draggingFromSquare
                        val to = screenOffsetToSquare(dragCurrentOffset, size.width.toFloat(), size.height.toFloat())
                        draggingFromSquare = null
                        if (from != null && to != null && from != to) {
                            onPieceDragged(from, to)
                        }
                    },
                    onDragCancel = {
                        draggingFromSquare = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sqSize = size.width / 8f
            val legalMap = legalMovesForSelected.associateBy { it.to }
            val checkedKingSquare = if (boardState.isInCheck) {
                boardState.findKingSquare(boardState.turn)
            } else null

            // 1. Draw all 64 squares, subtle wood grain texture, highlights, and coordinates
            for (row in 0..7) {
                for (col in 0..7) {
                    val file = if (flipped) 7 - col else col
                    val rank = if (flipped) row else 7 - row
                    val sq = Square(file, rank)
                    val topLeft = Offset(col * sqSize, row * sqSize)
                    val isLight = sq.isLightSquare

                    val baseColor = if (isLight) themeColors.boardLightSquare else themeColors.boardDarkSquare
                    drawRect(
                        color = baseColor,
                        topLeft = topLeft,
                        size = Size(sqSize, sqSize)
                    )

                    // Subtle wood grain lines for Classic Wood theme
                    if (themeColors.themeOption == AppThemeOption.CLASSIC_WOOD) {
                        val grainColor = if (isLight) {
                            Color(0x168B5A2B)
                        } else {
                            Color(0x181E120A)
                        }
                        for (g in 1..3) {
                            val gy = topLeft.y + (sqSize * g) / 4f
                            drawLine(
                                color = grainColor,
                                start = Offset(topLeft.x, gy),
                                end = Offset(topLeft.x + sqSize, gy + (if ((file + g) % 2 == 0) 2f else -2f)),
                                strokeWidth = 1.2f
                            )
                        }
                    }

                    // Last move highlight
                    if (lastMove != null && (sq == lastMove.from || sq == lastMove.to)) {
                        drawRect(
                            color = themeColors.lastMoveOverlay,
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    // Selected square highlight
                    if (selectedSquare == sq) {
                        drawRect(
                            color = themeColors.selectedSquareOverlay,
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    // King in Check radial crimson warning glow
                    if (checkedKingSquare == sq) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(themeColors.checkDangerGlow, Color.Transparent),
                                center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f),
                                radius = sqSize * 0.72f
                            ),
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    // Beginner Mode: Threatened friendly piece outline
                    if (beginnerModeEnabled && threatenedSquares.contains(sq)) {
                        drawRect(
                            color = themeColors.threatenedPieceOutline,
                            topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                            size = Size(sqSize - 4f, sqSize - 4f),
                            style = Stroke(
                                width = 3.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                    }

                    // Coordinates on left edge (rank 1..8) and bottom edge (file a..h)
                    val coordColor = if (isLight) themeColors.boardCoordLight else themeColors.boardCoordDark
                    if (col == 0) {
                        val rankLayout = textMeasurer.measure(
                            text = "${rank + 1}",
                            style = TextStyle(
                                color = coordColor,
                                fontSize = (sqSize * 0.19f).toSp(),
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFamily
                            )
                        )
                        drawText(
                            textLayoutResult = rankLayout,
                            topLeft = Offset(topLeft.x + sqSize * 0.06f, topLeft.y + sqSize * 0.04f)
                        )
                    }
                    if (row == 7) {
                        val fileLayout = textMeasurer.measure(
                            text = "${('a' + file)}",
                            style = TextStyle(
                                color = coordColor,
                                fontSize = (sqSize * 0.19f).toSp(),
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFamily
                            )
                        )
                        drawText(
                            textLayoutResult = fileLayout,
                            topLeft = Offset(
                                topLeft.x + sqSize - fileLayout.size.width - sqSize * 0.06f,
                                topLeft.y + sqSize - fileLayout.size.height - sqSize * 0.03f
                            )
                        )
                    }
                }
            }

            // 2. Hint move arrow/highlight if requested
            if (hintMove != null) {
                val (fromCol, fromRow) = squareToGridColRow(hintMove.from)
                val (toCol, toRow) = squareToGridColRow(hintMove.to)
                val startCenter = Offset((fromCol + 0.5f) * sqSize, (fromRow + 0.5f) * sqSize)
                val endCenter = Offset((toCol + 0.5f) * sqSize, (toRow + 0.5f) * sqSize)
                drawLine(
                    color = themeColors.hintArrowColor,
                    start = startCenter,
                    end = endCenter,
                    strokeWidth = sqSize * 0.13f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = themeColors.hintArrowColor,
                    radius = sqSize * 0.22f,
                    center = endCenter,
                    style = Stroke(width = sqSize * 0.07f)
                )
            }

            // 3. Draw all non-dragged Staunton pieces (with smooth interpolation for the most recent move)
            val animFraction = moveAnimProgress.value
            for (rank in 0..7) {
                for (file in 0..7) {
                    val sq = Square(file, rank)
                    if (sq == draggingFromSquare) continue
                    val piece = boardState.pieceAt(sq) ?: continue

                    val (destCol, destRow) = squareToGridColRow(sq)
                    val drawOffset = if (lastMove != null && sq == lastMove.to && animFraction < 1f) {
                        val (srcCol, srcRow) = squareToGridColRow(lastMove.from)
                        val curCol = srcCol + (destCol - srcCol) * animFraction
                        val curRow = srcRow + (destRow - srcRow) * animFraction
                        Offset(curCol * sqSize + sqSize * 0.05f, curRow * sqSize + sqSize * 0.05f)
                    } else {
                        Offset(destCol * sqSize + sqSize * 0.05f, destRow * sqSize + sqSize * 0.05f)
                    }

                    drawStauntonPiece(
                        piece = piece,
                        themeColors = themeColors,
                        topLeftOffset = drawOffset,
                        boxSize = sqSize * 0.90f
                    )
                }
            }

            // 4. Legal move indicators (destination dots, capture rings, castling badges)
            if (showLegalIndicators || beginnerModeEnabled) {
                for ((destSq, move) in legalMap) {
                    val (col, row) = squareToGridColRow(destSq)
                    val center = Offset((col + 0.5f) * sqSize, (row + 0.5f) * sqSize)
                    val isCapture = move.capturedPiece != null || move.flag == MoveFlag.EN_PASSANT
                    val isCastle = move.flag == MoveFlag.KINGSIDE_CASTLE || move.flag == MoveFlag.QUEENSIDE_CASTLE

                    if (isCapture) {
                        drawCircle(
                            color = themeColors.legalCaptureRing,
                            radius = sqSize * 0.42f,
                            center = center,
                            style = Stroke(width = sqSize * 0.09f)
                        )
                    } else if (isCastle) {
                        drawCircle(
                            color = themeColors.accentPrimary,
                            radius = sqSize * 0.24f,
                            center = center
                        )
                        drawCircle(
                            color = themeColors.backgroundPrimary,
                            radius = sqSize * 0.24f,
                            center = center,
                            style = Stroke(width = 2.5f)
                        )
                    } else {
                        drawCircle(
                            color = themeColors.legalMoveDot,
                            radius = sqSize * 0.17f,
                            center = center
                        )
                    }
                }
            }

            // 5. Draw currently dragged piece on top of everything
            val dragFrom = draggingFromSquare
            if (dragFrom != null) {
                val draggedPiece = boardState.pieceAt(dragFrom)
                if (draggedPiece != null) {
                    val pieceBox = sqSize * 1.05f
                    drawStauntonPiece(
                        piece = draggedPiece,
                        themeColors = themeColors,
                        topLeftOffset = Offset(
                            dragCurrentOffset.x - pieceBox / 2f,
                            dragCurrentOffset.y - pieceBox * 0.65f
                        ),
                        boxSize = pieceBox
                    )
                }
            }
        }
    }
}

@Composable
fun PawnPromotionDialog(
    moverColor: PieceColor,
    beginnerModeEnabled: Boolean,
    onSelectPromotion: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val options = listOf(
        PieceType.QUEEN,
        PieceType.ROOK,
        PieceType.BISHOP,
        PieceType.KNIGHT
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, themeColors.accentPrimary, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Promosi Pion",
                    style = MaterialTheme.typography.headlineMedium,
                    color = themeColors.accentPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pilih bidak pengganti pion Anda:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.textSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (pieceType in options) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(themeColors.surfaceElevated)
                                .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectPromotion(pieceType) }
                                .padding(12.dp)
                                .testTag("promote_${pieceType.name.lowercase()}")
                        ) {
                            StauntonPieceIcon(
                                piece = ChessPiece(pieceType, moverColor),
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pieceType.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = themeColors.textPrimary
                            )
                        }
                    }
                }

                if (beginnerModeEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tips: Pilih Menteri (9 poin) pada hampir semua posisi karena merupakan bidak terkuat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.accentSecondary
                    )
                }
            }
        }
    }
}
