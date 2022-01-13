package jump61;

import java.util.ArrayList;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();

        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        _foundMove = -1;
        assert getSide() == work.whoseMove();
        redvalid = new ArrayList<>();
        bluevalid = new ArrayList<>();
        int totalsquares = work.size() * work.size();
        if (getSide() == RED) {
            for (int i = 0; i < totalsquares; i += 1) {
                int sqNum = i;
                if (!work.get(sqNum).getSide().equals(BLUE)) {
                    redvalid.add(sqNum);
                    _foundMove = sqNum;
                }
            }
        } else if (getSide() == BLUE) {
            for (int i = 0; i < totalsquares; i += 1) {
                int sqNum = i;
                if (!work.get(sqNum).getSide().equals(RED)) {
                    bluevalid.add(sqNum);
                    _foundMove = sqNum;
                }
            }
        }
        return _foundMove;
    }

    /** Return the value of BOARD searching through DEPTH level and
     * recording iff SAVEMOVE. Move should have minimal value through
     * BETA and ALPHA values if SENSE = -1. */
    private int minPlayerValue(Board board, int depth, boolean saveMove,
                               int sense, int alpha, int beta) {
        if (board.getWinner() != null || depth == 0) {
            return staticEval(board, _foundMove);
        }
        int bestSoFar = (int) Float.POSITIVE_INFINITY;
        for (int i = 0; i < bluevalid.size(); i += 1) {
            Board next = new Board(board.size());
            next.copy(board);
            next.addSpot(BLUE, i);
            int response = maxPlayerValue(next, depth - 1, false,
                    -sense, alpha, beta);
            if (response < bestSoFar) {
                bestSoFar = response;
                beta = Math.min(alpha, bestSoFar);
                if (alpha >= beta) {
                    return bestSoFar;
                }
            }
        }
        return bestSoFar;
    }
    /** Return the value of BOARD searching through DEPTH level and
     * recording iff SAVEMOVE. Move should have maximal value through
     * BETA and ALPHA values if SENSE = -1. */
    private int maxPlayerValue(Board board, int depth, boolean saveMove,
                               int sense, int alpha, int beta) {
        if (board.getWinner() != null || depth == 0) {
            return staticEval(board, _foundMove);
        }
        int bestSoFar = (int) Float.NEGATIVE_INFINITY;
        for (int i = 0; i < redvalid.size(); i += 1) {
            Board next = new Board(board.size());
            next.copy(board);
            next.addSpot(RED, i);
            int response = minPlayerValue(next, depth - 1, false,
                    -sense, alpha, beta);
            if (response > bestSoFar) {
                bestSoFar = response;
                beta = Math.max(alpha, bestSoFar);
                if (alpha >= beta) {
                    return bestSoFar;
                }
            }
        }
        return bestSoFar;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        if (sense == -1) {
            return minPlayerValue(board, depth, saveMove, sense, alpha, beta);
        } else if (sense == 1) {
            return maxPlayerValue(board, depth, saveMove, sense, alpha, beta);
        } else {
            throw new GameException("Sense must be 1 or -1.");
        }
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        int redeval = b.numOfSide(RED);
        int blueeval = b.numOfSide(BLUE);
        return Math.round(redeval - blueeval);
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;

    /** Stored history of all the possible valid moves for RED side. */
    private ArrayList<Integer> redvalid = new ArrayList<>();

    /** Stored history of all the possible valid moves for BLUE side. */
    private ArrayList<Integer> bluevalid = new ArrayList<>();
}
