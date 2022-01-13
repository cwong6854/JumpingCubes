package jump61;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Formatter;

import java.util.function.Consumer;

import static jump61.Side.*;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Curtis Wong
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _N = N;
        _history = new ArrayList<>();
        _numMoves = 0;
        Square[][] initialboard = new Square[N][N];
        for (int i = 0; i < initialboard.length; i += 1) {
            for (int j = 0; j < initialboard[1].length; j += 1) {
                initialboard[i][j] = square(Side.WHITE, 1);
            }
        }
        _currentboard = initialboard;
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        _N = board0.size();
        this.copy(board0);
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        Square[][] initialboard = new Square[N][N];
        for (int i = 0; i < initialboard.length; i += 1) {
            for (int j = 0; j < initialboard[1].length; j += 1) {
                initialboard[i][j] = square(Side.WHITE, 1);
            }
        }
        _currentboard = initialboard;
        _numMoves = 0;
        _N = N;
        _history = new ArrayList<>();
        announce();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        _history = new ArrayList<>();
        _numMoves = 0;
        for (int i = 0; i < board.size(); i += 1) {
            for (int j = 0; j < board.size(); j += 1) {
                Side player = board.get(i + 1, j + 1).getSide();
                int spots = board.get(i + 1, j + 1).getSpots();
                _currentboard[i][j] = square(player, spots);
            }
        }
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        Square internalsquare = square(Side.WHITE, 1);
        for (int i = 0; i < _currentboard.length; i += 1) {
            for (int j = 0; j < _currentboard[1].length; j += 1) {
                internalsquare = board.get(i + 1, j + 1);
                _currentboard[i][j] = square(internalsquare.getSide(),
                        internalsquare.getSpots());
            }
        }
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _currentboard.length;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        int row = row(n) - 1;
        int col = col(n) - 1;
        return _currentboard[row][col];
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int totalspots = 0;
        for (int i = 0; i < _currentboard.length; i += 1) {
            for (int j = 0; j < _currentboard[1].length; j += 1) {
                totalspots +=  _currentboard[i][j].getSpots();
            }
        }
        return totalspots;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     * to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

        /** Returns true iff it would currently be legal for PLAYER to
         * add a spot to square #N. */
    boolean isLegal(Side player, int n) {
        Square numSquare = get(n);
        if (exists(n) && whoseMove() == player) {
            if (numSquare.getSide() == player
                    || numSquare.getSide().equals(WHITE)
                    && (numSquare.getSpots() < neighbors(n))) {
                return true;
            }
        }
        return false;
    }


    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        for (int i = 0; i < size() * size(); i += 1) {
            if (!player.equals(get(i).getSide())) {
                return true;
            }
        }
        return false;
    }


    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        Side red = RED;
        Side blue = BLUE;
        int redsq = 0;
        int bluesq = 0;
        for (int i = 0; i < _currentboard.length; i += 1) {
            for (int j = 0; j < _currentboard[1].length; j += 1) {
                if (_currentboard[i][j].getSide().equals(RED)) {
                    redsq += 1;
                } else if (_currentboard[i][j].getSide().equals(BLUE)) {
                    bluesq += 1;
                }
            }
        }
        if (redsq == size() * size()) {
            return red;
        } else if (bluesq == size() * size()) {
            return blue;
        } else {
            return null;
        }
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int total = 0;
        for (int i = 0; i < _currentboard.length; i += 1) {
            for (int j = 0; j < _currentboard[1].length; j += 1) {
                if (_currentboard[i][j].getSide() == side) {
                    total += 1;
                }
            }
        }
        return total;
    }

    /** Add a spot from PLAYER at row R, column C. Assumes
     * isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        if (getWinner() == null) {
            if (_numMoves == 0) {
                Board initial = new Board(_N);
                initial.internalCopy(this);
                _history.add(initial);
            }
            Board gameboard = new Board(_N);
            int spots = _currentboard[r - 1][c - 1].getSpots();
            if (isLegal(player) || spots > neighbors(r, c)) {
                if (isLegal(player, r, c) && spots < neighbors(r, c)) {
                    _currentboard[r - 1][c - 1] = square(player, spots + 1);
                    _numMoves += 1;
                    gameboard.internalCopy(this);
                    _history.add(gameboard);
                }
                if (isLegal(player, r, c) && spots == neighbors(r, c)
                        || spots > neighbors(r, c)) {
                    _currentboard[r - 1][c - 1] = square(player, 1);
                    _numMoves += 1;
                    jump(sqNum(r, c));
                    gameboard.internalCopy(this);
                    _history.add(gameboard);
                }
            }
        }
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        int row = row(n);
        int col = col(n);
        addSpot(player, row, col);

    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        if (num > 0) {
            _currentboard[row(n) - 1][col(n) - 1] = square(player, num);
        } else {
            _currentboard[row(n) - 1][col(n) - 1] = square(WHITE, num);
        }
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        if (_numMoves == 0) {
            throw new GameException("Games has currently no moves");
        } else {
            _numMoves -= 1;
            Board back = _history.get(_numMoves);
            _currentboard = back._currentboard;
        }
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        int row = row(S);
        int col = col(S);
        boolean up = exists(row - 1, col);
        boolean down = exists(row + 1, col);
        boolean left = exists(row, col - 1);
        boolean right = exists(row, col + 1);
        Side sides = _currentboard[row - 1][col - 1].getSide();
        if (getWinner() == null) {
            if (down) {
                _currentboard[row][col - 1] = square(sides,
                        _currentboard[row][col - 1].getSpots() + 1);
                if (_currentboard[row][col - 1].getSpots()
                        > neighbors(row + 1, col)) {
                    addSpot(sides, sqNum(row + 1, col));
                }
            }
            if (up) {
                _currentboard[row - 2][col - 1] = square(sides,
                        _currentboard[row - 2][col - 1].getSpots() + 1);
                if (_currentboard[row - 2][col - 1].getSpots()
                        > neighbors(row - 1, col)) {
                    addSpot(sides, sqNum(row - 1, col));
                }
            }
            if (right) {
                _currentboard[row - 1][col] = square(sides,
                        _currentboard[row - 1][col].getSpots() + 1);
                if (_currentboard[row - 1][col].getSpots()
                        > neighbors(row, col + 1)) {
                    addSpot(sides, sqNum(row, col + 1));
                }
            }
            if (left) {
                _currentboard[row - 1][col - 2] = square(sides,
                        _currentboard[row - 1][col - 2].getSpots() + 1);
                if (_currentboard[row - 1][col - 2].getSpots()
                        > neighbors(row, col - 1)) {
                    addSpot(sides, sqNum(row, col - 1));
                }
            }
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int i = 0; i < _N; i += 1) {
            out.format("    ");
            for (int j = 0; j < _N; j += 1) {
                Square currsquare = _currentboard[i][j];
                Side side = currsquare.getSide();
                int spots = currsquare.getSpots();
                String string = "";
                if (side == WHITE) {
                    string = "-";
                }
                if (side == RED) {
                    string = "r";
                }
                if (side == BLUE) {
                    string = "b";
                }
                out.format("%d%s ", spots, string);
            }
            out.format("%n");
        }
        out.format("===%n");
        return out.toString();
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            return this == obj;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** N number of board array. */
    private int _N;

    /** Current contents of the board. */
    private Square[][] _currentboard;

    /** Number of moves on the board. */
    private int _numMoves;

    /** History of the game. */
    private ArrayList<Board> _history = new ArrayList<>();

}
