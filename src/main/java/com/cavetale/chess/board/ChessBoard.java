package com.cavetale.chess.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public final class ChessBoard {
    private final ChessPiece[] board = new ChessPiece[64];
    private ChessColor activeColor = ChessColor.WHITE;
    private boolean whiteCanCastleKingside;
    private boolean whiteCanCastleQueenside;
    private boolean blackCanCastleKingside;
    private boolean blackCanCastleQueenside;
    private ChessSquare enPassantSquare;
    private int halfMoveClock;
    private int fullMoveClock;

    public static final String FEN_START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public ChessBoard() { }

    public ChessBoard(final ChessBoard original) {
        for (int i = 0; i < 64; i += 1) {
            this.board[i] = original.board[i];
        }
        this.activeColor = original.activeColor;
        this.whiteCanCastleKingside = original.whiteCanCastleKingside;
        this.whiteCanCastleQueenside = original.whiteCanCastleQueenside;
        this.blackCanCastleKingside = original.blackCanCastleKingside;
        this.blackCanCastleQueenside = original.blackCanCastleQueenside;
        this.enPassantSquare = original.enPassantSquare;
        this.halfMoveClock = original.halfMoveClock;
        this.fullMoveClock = original.fullMoveClock;
    }

    public ChessBoard clone() {
        return new ChessBoard(this);
    }

    public ChessPiece getPieceAt(ChessSquare square) {
        return board[square.ordinal()];
    }

    public ChessPiece getPieceAt(ChessFile file, ChessRank rank) {
        return board[file.ordinal() + 8 * rank.ordinal()];
    }

    public ChessPiece getPieceAt(int x, int y) {
        if (!isOnBoard(x, y)) {
            throw new IllegalArgumentException("Outside of board: " + x + ", " + y);
        }
        return board[x + 8 * y];
    }

    public void setPieceAt(ChessSquare square, ChessPiece piece) {
        board[square.ordinal()] = piece;
    }

    public void setPieceAt(ChessFile file, ChessRank rank, ChessPiece piece) {
        board[file.ordinal() + 8 * rank.ordinal()] = piece;
    }

    public void setPieceAt(int x, int y, ChessPiece piece) {
        if (!isOnBoard(x, y)) {
            throw new IllegalArgumentException("Outside of board: " + x + ", " + y);
        }
        board[x + 8 * y] = piece;
    }

    public boolean canCastleKingside() {
        return activeColor == ChessColor.WHITE
            ? whiteCanCastleKingside
            : blackCanCastleKingside;
    }

    public boolean canCastleQueenside() {
        return activeColor == ChessColor.WHITE
            ? whiteCanCastleQueenside
            : blackCanCastleQueenside;
    }

    public void setCanCastleKingside(boolean value) {
        if (activeColor == ChessColor.WHITE) {
            whiteCanCastleKingside = value;
        } else {
            blackCanCastleKingside = value;
        }
    }

    public void setCanCastleQueenside(boolean value) {
        if (activeColor == ChessColor.WHITE) {
            whiteCanCastleQueenside = value;
        } else {
            blackCanCastleQueenside = value;
        }
    }

    public void move(ChessMove move) {
        move(move.from(), move.to(), move.promotion());
    }

    /**
     * Move piece from one square to another.
     * This will update the board properly.  It performs some checks
     * but ultimately relies on the move to be come from
     * getLegalMoves() in order to make legal moves.
     */
    public void move(final ChessSquare from, final ChessSquare to, final ChessPieceType promotion) {
        // Get info
        final ChessPiece piece = board[from.ordinal()];
        if (piece == null) {
            throw new IllegalArgumentException(from + " is empty!");
        }
        final ChessPiece taken = board[to.ordinal()];
        if (piece.color != activeColor) {
            throw new IllegalArgumentException(piece + " at " + from + " does not belong to " + activeColor);
        }
        // Move
        moveHelper(from, to);
        if (promotion != null) {
            setPieceAt(to, ChessPiece.of(activeColor, promotion));
        }
        // En passant detection
        if (piece.type == ChessPieceType.PAWN && from.x == to.x && to.y - from.y == getPawnDirection() * 2) {
            enPassantSquare = to;
        } else {
            enPassantSquare = null;
        }
        // Castle detection
        if (piece.type == ChessPieceType.KING && from == getNaturalKing() && to == getKingsideCastle() && canCastleKingside()) {
            moveHelper(getNaturalKingsideRook(), to.relative(-1, 0));
        }
        if (piece.type == ChessPieceType.KING && from == getNaturalKing() && to == getQueensideCastle() && canCastleQueenside()) {
            moveHelper(getNaturalQueensideRook(), to.relative(1, 0));
        }
        // Update castle flags
        if (from == getNaturalKing()) {
            setCanCastleKingside(false);
            setCanCastleQueenside(false);
        } else if (from == getNaturalKingsideRook()) {
            setCanCastleKingside(false);
        } else if (from == getNaturalQueensideRook()) {
            setCanCastleQueenside(false);
        }
        // Update clocks and flip
        if (activeColor == ChessColor.BLACK) {
            fullMoveClock += 1;
        }
        if (taken == null && piece.type != ChessPieceType.PAWN) {
            halfMoveClock += 1;
        } else {
            halfMoveClock = 0;
        }
        activeColor = activeColor == ChessColor.WHITE
            ? ChessColor.BLACK
            : ChessColor.WHITE;
    }

    public boolean isKingInCheck(ChessColor color) {
        for (int y = 0; y < 8; y += 1) {
            for (int x = 0; x < 8; x += 1) {
                final ChessPiece piece = getPieceAt(x, y);
                if (piece != null && piece.type == ChessPieceType.KING && piece.color == color) {
                    if (isInCheck(x, y, color)) return true;
                }
            }
        }
        return false;
    }

    public boolean isKingInCheck() {
        return isKingInCheck(activeColor);
    }

    public Map<ChessMove, ChessBoard> getLegalMoves() {
        // Collect naive moves in a list
        final var list = new ArrayList<ChessMove>();
        final int pawnDirection = getPawnDirection();
        for (ChessSquare from : ChessSquare.values()) {
            final ChessPiece piece = getPieceAt(from);
            if (piece == null || piece.color != activeColor) continue;
            switch (piece.type) {
            case PAWN: {
                // Forward once
                final var forward = ifOnBoardAndEmpty(from.x, from.y + pawnDirection);
                if (forward != null) {
                    list.add(new ChessMove(from, forward));
                    // Forward twice
                    if (from.rank == getNaturalPawnRank()) {
                        final var passer = ifOnBoardAndEmpty(from.x, from.y + pawnDirection * 2);
                        if (passer != null) list.add(new ChessMove(from, passer));
                    }
                }
                // Diagonal
                final var takeLeft = ifOnBoardAndEnemy(from.x - 1, from.y + pawnDirection);
                if (takeLeft != null) list.add(new ChessMove(from, takeLeft));
                final var takeRight = ifOnBoardAndEnemy(from.x + 1, from.y + pawnDirection);
                if (takeRight != null) list.add(new ChessMove(from, takeRight));
                // En passant
                if (enPassantSquare != null
                    && (from.x + 1 == enPassantSquare.x || from.x - 1 == enPassantSquare.x)
                    && (from.y + pawnDirection == enPassantSquare.y)) {
                    list.add(new ChessMove(from, enPassantSquare));
                }
                break;
            }
            case KNIGHT: {
                for (var knightMove : KNIGHT_MOVES) {
                    final var to = ifOnBoardAndJumpable(from.x + knightMove.x(), from.y + knightMove.y());
                    if (to != null) list.add(new ChessMove(from, to));
                }
                break;
            }
            case BISHOP: {
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x + i, from.y + i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x + i, from.y - i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x - i, from.y + i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x - i, from.y - i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                break;
            }
            case ROOK: {
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x + i, from.y);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x - i, from.y);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x, from.y + i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x, from.y - i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                break;
            }
            case QUEEN: {
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x + i, from.y + i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x + i, from.y - i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x - i, from.y + i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x - i, from.y - i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x + i, from.y);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x - i, from.y);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x, from.y + i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                for (int i = 1; i < 8; i += 1) {
                    final var to = ifOnBoardAndJumpable(from.x, from.y - i);
                    if (to == null) break;
                    list.add(new ChessMove(from, to));
                    if (getPieceAt(to) != null) break;
                }
                break;
            }
            case KING: {
                for (var kingMove : KING_MOVES) {
                    final var to = ifOnBoardAndJumpable(from.x + kingMove.x(), from.y + kingMove.y());
                    if (to != null) list.add(new ChessMove(from, to));
                }
                if (canCastleKingside()
                    && from == getNaturalKing()
                    && !isInCheck(from)
                    && !isInCheck(from.relative(1, 0)) && isEmpty(from.relative(1, 0))
                    && !isInCheck(from.relative(2, 0)) && isEmpty(from.relative(2, 0))) {
                    list.add(new ChessMove(from, from.relative(2, 0)));
                }
                if (canCastleQueenside()
                    && from == getNaturalKing()
                    && !isInCheck(from)
                    && !isInCheck(from.relative(-1, 0)) && isEmpty(from.relative(-1, 0))
                    && !isInCheck(from.relative(-2, 0)) && isEmpty(from.relative(-2, 0))) {
                    list.add(new ChessMove(from, from.relative(-2, 0)));
                }
            }
            default: break;
            }
        }
        Collections.sort(list, Comparator.comparing(ChessMove::toString));
        // Confirm if each move is legal
        final var result = new LinkedHashMap<ChessMove, ChessBoard>();
        for (ChessMove move : list) {
            final ChessPiece piece = getPieceAt(move.from());
            if (piece.type == ChessPieceType.PAWN && move.to().getRank() == getPromotionRank()) {
                // Special case: Pawn promotion
                for (ChessPieceType promotion : List.of(ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT)) {
                    final ChessMove move2 = new ChessMove(move.from(), move.to(), promotion);
                    final ChessBoard nextBoard = clone();
                    nextBoard.move(move2);
                    if (nextBoard.isKingInCheck(activeColor)) continue;
                    result.put(move2, nextBoard);
                }
            } else {
                final ChessBoard nextBoard = clone();
                nextBoard.move(move);
                if (nextBoard.isKingInCheck(activeColor)) continue;
                result.put(move, nextBoard);
            }
        }
        return result;
    }

    public void loadStartingPosition() {
        loadFenString(FEN_START);
    }

    public void loadFenString(String fen) {
        final String[] fields = fen.split(" ");
        if (fields.length != 6) {
            throw new IllegalArgumentException("FEN must have 6 fields: " + fen);
        }
        // Board
        final String[] rankFields = fields[0].split("/");
        if (rankFields.length != 8) {
            throw new IllegalArgumentException("FEN must have 8 ranks: " + fields[0]);
        }
        for (int y = 0; y < 8; y += 1) {
            int x = 0;
            final String rankField = rankFields[y];
            for (int i = 0; i < rankField.length(); i += 1) {
                final char c = rankField.charAt(i);
                if (c >= '1' && c <= '9') {
                    int value = (int) (c - '1' + 1);
                    for (int j = 0; j < value; j += 1) {
                        setPieceAt(x++, 7 - y, null);
                    }
                } else {
                    final ChessPiece piece = ChessPiece.ofFenChar(c);
                    if (piece == null) {
                        throw new IllegalArgumentException("Illegel piece: " + c);
                    }
                    setPieceAt(x++, 7 - y, piece);
                }
            }
        }
        // Active Color
        final String activeColorString = fields[1];
        if (activeColorString.length() != 1) {
            throw new IllegalArgumentException("Illegal active color: " + activeColorString);
        }
        activeColor = ChessColor.ofFenChar(activeColorString.charAt(0));
        if (activeColor == null) {
            throw new IllegalArgumentException("Illegal active color: " + activeColorString);
        }
        // Castle
        whiteCanCastleKingside = false;
        whiteCanCastleQueenside = false;
        blackCanCastleKingside = false;
        blackCanCastleQueenside = false;
        final String castleString = fields[2];
        if (!castleString.equals("-")) {
            for (int i = 0; i < castleString.length(); i += 1) {
                final char c = castleString.charAt(i);
                switch (c) {
                case 'K': whiteCanCastleKingside = true; break;
                case 'Q': whiteCanCastleQueenside = true; break;
                case 'k': blackCanCastleKingside = true; break;
                case 'q': blackCanCastleQueenside = true; break;
                default: throw new IllegalArgumentException("Invalid castle symbol: " + c);
                }
            }
        }
        // En passant
        final String enPassantString = fields[3];
        enPassantSquare = null;
        if (enPassantString.length() != 1) {
            throw new IllegalArgumentException("Invalid en passant value: " + enPassantString);
        } else if (!enPassantString.equals("-")) {
            enPassantSquare = ChessSquare.ofName(enPassantString);
            if (enPassantSquare == null) {
                throw new IllegalArgumentException("Invalid en passant value: " + enPassantString);
            }
        }
        // Halfmove clock
        final String halfMoveString = fields[4];
        try {
            halfMoveClock = Integer.parseInt(halfMoveString);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Halfmove clock must be a number: " + halfMoveString);
        }
        if (halfMoveClock < 0) {
            throw new IllegalArgumentException("Halfmove clock must be a positive number: " + halfMoveString);
        }
        // Fullmove clock
        final String fullMoveString = fields[5];
        try {
            fullMoveClock = Integer.parseInt(fullMoveString);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Fullmove clock must be a number: " + fullMoveString);
        }
        if (fullMoveClock < 0) {
            throw new IllegalArgumentException("Fullmove clock must be a positive number: " + fullMoveString);
        }
    }

    @Override
    public String toString() {
        return "ChessBoard(" + toFenString() + ")";
    }

    public String toFenString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 7; y >= 0; y -= 1) {
            if (y < 7) sb.append("/");
            int empty = 0;
            for (int x = 0; x < 8; x += 1) {
                ChessPiece piece = getPieceAt(x, y);
                if (piece == null) {
                    empty += 1;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(piece.fenChar);
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
        }
        sb.append(' ');
        sb.append(activeColor.fenChar);
        sb.append(' ');
        if (!whiteCanCastleKingside && !whiteCanCastleQueenside && !blackCanCastleKingside && !blackCanCastleQueenside) {
            sb.append("-");
        } else {
            if (whiteCanCastleKingside) sb.append('K');
            if (whiteCanCastleQueenside) sb.append('Q');
            if (blackCanCastleKingside) sb.append('k');
            if (blackCanCastleQueenside) sb.append('q');
        }
        sb.append(' ');
        if (enPassantSquare == null) {
            sb.append('-');
        } else {
            sb.append(enPassantSquare.name);
        }
        sb.append(' ');
        sb.append(halfMoveClock);
        sb.append(' ');
        sb.append(fullMoveClock);
        return sb.toString();
    }

    public String toAsciiBoard() {
        StringBuilder sb = new StringBuilder();
        for (int y = 7; y >= 0; y -= 1) {
            sb.append(ChessRank.get(y).digit);
            sb.append(' ');
            for (int x = 0; x < 8; x += 1) {
                ChessPiece piece = getPieceAt(x, y);
                sb.append(piece == null ? ' ' : piece.unicodeSymbol);
                sb.append(' ');
            }
            sb.append("\n");
        }
        sb.append("  ");
        for (ChessFile file : ChessFile.values()) {
            sb.append(file.letter);
            sb.append(' ');
        }
        return sb.toString();
    }

    private void moveHelper(ChessSquare from, ChessSquare to) {
        board[to.ordinal()] = board[from.ordinal()];
        board[from.ordinal()] = null;
    }

    private boolean isOnBoard(int x, int y) {
        return x >= 0 && x <= 7 && y >= 0 && y <= 7;
    }

    private boolean isEmpty(ChessSquare square) {
        return getPieceAt(square) == null;
    }

    private ChessPiece getSecurePieceAt(int x, int y) {
        if (!isOnBoard(x, y)) return null;
        return getPieceAt(x, y);
    }

    private ChessSquare ifHasEnemyPiece(int x, int y) {
        final ChessPiece piece = getSecurePieceAt(x, y);
        return piece != null && piece.color != activeColor
            ? ChessSquare.at(x, y)
            : null;
    }

    private boolean hasPiece(int x, int y, ChessColor color, ChessPieceType type) {
        final ChessPiece piece = getSecurePieceAt(x, y);
        if (piece == null) return false;
        return piece.type == type && piece.color == color;
    }

    private boolean hasPiece(int x, int y, ChessColor color, ChessPieceType type, ChessPieceType type2) {
        final ChessPiece piece = getSecurePieceAt(x, y);
        if (piece == null) return false;
        return (piece.type == type || piece.type == type2) && piece.color == color;
    }

    private boolean isOffBoardOrBlocked(int x, int y) {
        return !isOnBoard(x, y)
            || getSecurePieceAt(x, y) != null;
    }

    /**
     * Return the square if it is on the board, and is empty or has an
     * enemy piece.
     */
    private ChessSquare ifOnBoardAndJumpable(int x, int y) {
        if (!isOnBoard(x, y)) return null;
        final ChessPiece piece = getPieceAt(x, y);
        return piece == null || piece.color != activeColor
            ? ChessSquare.at(x, y)
            : null;
    }

    private ChessSquare ifOnBoardAndEnemy(int x, int y) {
        if (!isOnBoard(x, y)) return null;
        final ChessPiece piece = getPieceAt(x, y);
        return piece != null && piece.color != activeColor
            ? ChessSquare.at(x, y)
            : null;
    }

    private ChessSquare ifOnBoardAndEmpty(int x, int y) {
        return (isOnBoard(x, y) && getPieceAt(x, y) == null)
            ? ChessSquare.at(x, y)
            : null;
    }

    private boolean isInCheck(int x, int y, ChessColor color) {
        final int pawnDirection = getPawnDirection(color);
        final ChessColor enemy = color.other();
        // Pawns
        if (hasPiece(x - 1, y + pawnDirection, enemy, ChessPieceType.PAWN)) return true;
        if (hasPiece(x + 1, y + pawnDirection, enemy, ChessPieceType.PAWN)) return true;
        // Knights
        if (hasPiece(x + 2, y + 1, enemy, ChessPieceType.KNIGHT)) return true;
        if (hasPiece(x + 1, y + 2, enemy, ChessPieceType.KNIGHT)) return true;
        if (hasPiece(x - 2, y - 1, enemy, ChessPieceType.KNIGHT)) return true;
        if (hasPiece(x - 1, y - 2, enemy, ChessPieceType.KNIGHT)) return true;
        if (hasPiece(x + 2, y - 1, enemy, ChessPieceType.KNIGHT)) return true;
        if (hasPiece(x + 1, y - 2, enemy, ChessPieceType.KNIGHT)) return true;
        if (hasPiece(x - 2, y + 1, enemy, ChessPieceType.KNIGHT)) return true;
        if (hasPiece(x - 1, y + 2, enemy, ChessPieceType.KNIGHT)) return true;
        // Bishop or Queen
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x + i, y + i, enemy, ChessPieceType.BISHOP, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x + i, y + i)) break;
        }
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x + i, y - i, enemy, ChessPieceType.BISHOP, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x + i, y - i)) break;
        }
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x - i, y + i, enemy, ChessPieceType.BISHOP, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x - i, y + i)) break;
        }
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x - i, y - i, enemy, ChessPieceType.BISHOP, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x - i, y - i)) break;
        }
        // Rook or Queen
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x + i, y, enemy, ChessPieceType.ROOK, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x + i, y)) break;
        }
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x, y + i, enemy, ChessPieceType.ROOK, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x, y + i)) break;
        }
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x - i, y, enemy, ChessPieceType.ROOK, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x - i, y)) break;
        }
        for (int i = 1; i < 8; i += 1) {
            if (hasPiece(x, y - i, enemy, ChessPieceType.ROOK, ChessPieceType.QUEEN)) return true;
            if (isOffBoardOrBlocked(x, y - i)) break;
        }
        return false;
    }

    private boolean isInCheck(ChessSquare square) {
        return isInCheck(square.x, square.y, activeColor);
    }

    private int getPawnDirection(ChessColor color) {
        return color == ChessColor.WHITE ? 1 : -1;
    }

    private int getPawnDirection() {
        return getPawnDirection(activeColor);
    }

    private ChessSquare getNaturalKing() {
        return activeColor == ChessColor.WHITE
            ? ChessSquare.E1
            : ChessSquare.E8;
    }

    private ChessSquare getKingsideCastle() {
        return activeColor == ChessColor.WHITE
            ? ChessSquare.G1
            : ChessSquare.G8;
    }

    private ChessSquare getQueensideCastle() {
        return activeColor == ChessColor.WHITE
            ? ChessSquare.C1
            : ChessSquare.C8;
    }

    private ChessSquare getNaturalKingsideRook() {
        return activeColor == ChessColor.WHITE
            ? ChessSquare.H1
            : ChessSquare.H8;
    }

    private ChessSquare getNaturalQueensideRook() {
        return activeColor == ChessColor.WHITE
            ? ChessSquare.A1
            : ChessSquare.A8;
    }

    private ChessRank getNaturalPawnRank() {
        return activeColor == ChessColor.WHITE
            ? ChessRank.RANK_2
            : ChessRank.RANK_7;
    }

    private ChessRank getPromotionRank() {
        return activeColor == ChessColor.WHITE
            ? ChessRank.RANK_8
            : ChessRank.RANK_1;
    }

    private static final List<Vec2i> KNIGHT_MOVES = List.of(new Vec2i(2, 1),
                                                            new Vec2i(1, 2),
                                                            new Vec2i(-2, -1),
                                                            new Vec2i(-1, -2),
                                                            new Vec2i(2, -1),
                                                            new Vec2i(1, -2),
                                                            new Vec2i(-2, 1),
                                                            new Vec2i(-1, 2));

    private static final List<Vec2i> KING_MOVES = List.of(new Vec2i(1, 1),
                                                          new Vec2i(1, 0),
                                                          new Vec2i(1, -1),
                                                          new Vec2i(0, -1),
                                                          new Vec2i(-1, -1),
                                                          new Vec2i(-1, 0),
                                                          new Vec2i(-1, 1),
                                                          new Vec2i(0, 1));
}
