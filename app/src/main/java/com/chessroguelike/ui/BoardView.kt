package com.chessroguelike.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.engine.ChessPiece
import com.chessroguelike.engine.Move
import com.chessroguelike.engine.PieceType

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var board: ChessBoard? = null
    var selectedPiece: ChessPiece? = null
    var validMoves: List<Move> = emptyList()
    var onPieceSelected: ((row: Int, col: Int) -> Unit)? = null
    var onMoveSelected: ((row: Int, col: Int) -> Unit)? = null
    var isInteractionEnabled = true

    private val lightSquarePaint = Paint().apply { color = Color.parseColor("#F0D9B5") }
    private val darkSquarePaint = Paint().apply { color = Color.parseColor("#B58863") }
    private val selectedPaint = Paint().apply { color = Color.parseColor("#7FC97F"); alpha = 200 }
    private val validMovePaint = Paint().apply { color = Color.parseColor("#7FC97F"); alpha = 120 }
    private val lastMoveFromPaint = Paint().apply { color = Color.parseColor("#CDD16E"); alpha = 160 }
    private val lastMoveToPaint = Paint().apply { color = Color.parseColor("#CDD16E"); alpha = 160 }
    private val captureHighlightPaint = Paint().apply { color = Color.parseColor("#E05C5C"); alpha = 160 }
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val abilityDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.FILL
    }

    private var squareSize = 0f
    private var lastMove: Move? = null
    private var boardOffsetX = 0f
    private var boardOffsetY = 0f

    fun setLastMove(move: Move?) {
        lastMove = move
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boardSize = minOf(w, h).toFloat()
        squareSize = boardSize / ChessBoard.SIZE
        boardOffsetX = (w - boardSize) / 2f
        boardOffsetY = (h - boardSize) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (squareSize <= 0) return

        drawBoard(canvas)
        drawHighlights(canvas)
        drawPieces(canvas)
        drawCoordinates(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        for (row in 0 until ChessBoard.SIZE) {
            for (col in 0 until ChessBoard.SIZE) {
                val paint = if ((row + col) % 2 == 0) lightSquarePaint else darkSquarePaint
                val left = boardOffsetX + col * squareSize
                val top = boardOffsetY + row * squareSize
                canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
            }
        }
    }

    private fun drawHighlights(canvas: Canvas) {
        lastMove?.let { move ->
            drawSquare(canvas, move.fromRow, move.fromCol, lastMoveFromPaint)
            drawSquare(canvas, move.toRow, move.toCol, lastMoveToPaint)
        }

        selectedPiece?.let { piece ->
            drawSquare(canvas, piece.row, piece.col, selectedPaint)
        }

        validMoves.forEach { move ->
            val targetPiece = board?.getPiece(move.toRow, move.toCol)
            val paint = if (targetPiece != null) captureHighlightPaint else validMovePaint
            drawSquare(canvas, move.toRow, move.toCol, paint)
        }
    }

    private fun drawSquare(canvas: Canvas, row: Int, col: Int, paint: Paint) {
        val left = boardOffsetX + col * squareSize
        val top = boardOffsetY + row * squareSize
        canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
    }

    private fun drawPieces(canvas: Canvas) {
        val currentBoard = board ?: return
        piecePaint.textSize = squareSize * 0.72f

        currentBoard.getAllPieces().forEach { piece ->
            val cx = boardOffsetX + piece.col * squareSize + squareSize / 2f
            val cy = boardOffsetY + piece.row * squareSize + squareSize / 2f

            val fontMetrics = piecePaint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f

            val pieceColor = if (piece.isPlayer) Color.parseColor("#1565C0") else Color.parseColor("#B71C1C")
            val shadowColor = if (piece.isPlayer) Color.parseColor("#90CAF9") else Color.parseColor("#EF9A9A")

            // Shadow
            piecePaint.color = shadowColor
            piecePaint.alpha = 180
            canvas.drawText(ChessPiece.getUnicodeChar(piece.type, piece.isPlayer), cx + 1.5f, textY + 1.5f, piecePaint)

            // Piece
            piecePaint.color = pieceColor
            piecePaint.alpha = 255
            canvas.drawText(ChessPiece.getUnicodeChar(piece.type, piece.isPlayer), cx, textY, piecePaint)

            // Shield indicator
            if (piece.shieldActive) {
                val rect = RectF(
                    boardOffsetX + piece.col * squareSize + 4f,
                    boardOffsetY + piece.row * squareSize + 4f,
                    boardOffsetX + piece.col * squareSize + squareSize - 4f,
                    boardOffsetY + piece.row * squareSize + squareSize - 4f
                )
                canvas.drawRoundRect(rect, 8f, 8f, shieldPaint)
            }

            // Ability dot indicator
            if (piece.ability != com.chessroguelike.engine.Ability.NONE) {
                val dotX = boardOffsetX + piece.col * squareSize + squareSize - 10f
                val dotY = boardOffsetY + piece.row * squareSize + 10f
                canvas.drawCircle(dotX, dotY, 5f, abilityDotPaint)
            }
        }
    }

    private fun drawCoordinates(canvas: Canvas) {
        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = squareSize * 0.22f
            isFakeBoldText = true
        }
        for (i in 0 until ChessBoard.SIZE) {
            val isLight = i % 2 == 0
            coordPaint.color = if (isLight) Color.parseColor("#B58863") else Color.parseColor("#F0D9B5")
            // Column letters
            val letter = ('a' + i).toString()
            canvas.drawText(letter,
                boardOffsetX + i * squareSize + squareSize - coordPaint.textSize * 0.8f,
                boardOffsetY + ChessBoard.SIZE * squareSize - 2f, coordPaint)
            // Row numbers
            coordPaint.color = if ((i) % 2 == 0) Color.parseColor("#F0D9B5") else Color.parseColor("#B58863")
            canvas.drawText((ChessBoard.SIZE - i).toString(),
                boardOffsetX + 2f,
                boardOffsetY + i * squareSize + coordPaint.textSize + 2f, coordPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractionEnabled) return false
        if (event.action != MotionEvent.ACTION_UP) return true

        val col = ((event.x - boardOffsetX) / squareSize).toInt()
        val row = ((event.y - boardOffsetY) / squareSize).toInt()

        if (row !in 0 until ChessBoard.SIZE || col !in 0 until ChessBoard.SIZE) return true

        if (validMoves.any { it.toRow == row && it.toCol == col }) {
            onMoveSelected?.invoke(row, col)
        } else {
            onPieceSelected?.invoke(row, col)
        }

        return true
    }

    fun refresh() {
        invalidate()
    }
}
