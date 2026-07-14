package proj.mobapp.sudoku_sliding_puzzles;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

// Unit tests for the sudoku generator
public class SudokuGeneratorTest {

    private static final int SIDE = 9; // board size (9x9)
    private static final int BOX = 3; // subgrid size (3x3)
    private static final int REPEAT_RUNS = 50; // number of repeats for stability test

    private SudokuGenerator generator; // instance of the generator under test

    @Before
    public void setUp() {
        generator = new SudokuGenerator();
    }

    // checks that the board is 9x9
    @Test
    public void generatedBoardHasCorrectDimensions() {
        int[][] board = generator.generateSudoku();

        assertEquals(SIDE, board.length);
        for (int[] row : board) {
            assertEquals(SIDE, row.length);
        }
    }

    // checks that each digit 0-8 appears exactly 9 times
    @Test
    public void everyDigitZeroToEightAppearsExactlyNineTimes() {
        int[][] board = generator.generateSudoku();

        int[] counts = new int[SIDE];
        for (int[] row : board) {
            for (int value : row) {
                assertTrue("Digit out of range: " + value, value >= 0 && value < SIDE);
                counts[value]++;
            }
        }

        for (int digit = 0; digit < SIDE; digit++) {
            assertEquals("Digit " + digit + " should appear 9 times", 9, counts[digit]);
        }
    }

    // checks that every row is a permutation of 0-8
    @Test
    public void everyRowContainsAllDigitsExactlyOnce() {
        int[][] board = generator.generateSudoku();

        for (int r = 0; r < SIDE; r++) {
            assertTrue("Row " + r + " is not a valid permutation of 0-8", isPermutationOfZeroToEight(board[r]));
        }
    }

    // checks that every column is a permutation of 0-8
    @Test
    public void everyColumnContainsAllDigitsExactlyOnce() {
        int[][] board = generator.generateSudoku();

        for (int c = 0; c < SIDE; c++) {
            int[] column = new int[SIDE];
            for (int r = 0; r < SIDE; r++) {
                column[r] = board[r][c];
            }
            assertTrue("Column " + c + " is not a valid permutation of 0-8", isPermutationOfZeroToEight(column));
        }
    }

    // checks that every 3x3 subgrid is a permutation of 0-8
    @Test
    public void every3x3SubgridContainsAllDigitsExactlyOnce() {
        int[][] board = generator.generateSudoku();

        for (int boxRow = 0; boxRow < BOX; boxRow++) {
            for (int boxCol = 0; boxCol < BOX; boxCol++) {
                int[] box = new int[SIDE];
                int index = 0;
                for (int r = 0; r < BOX; r++) {
                    for (int c = 0; c < BOX; c++) {
                        box[index++] = board[boxRow * BOX + r][boxCol * BOX + c];
                    }
                }
                assertTrue(
                        "Subgrid (" + boxRow + "," + boxCol + ") is not a valid permutation of 0-8",
                        isPermutationOfZeroToEight(box)
                );
            }
        }
    }

    // repeatedly generates boards and checks they are always valid
    @Test
    public void repeatedGenerationAlwaysProducesAValidBoard() {
        for (int run = 0; run < REPEAT_RUNS; run++) {
            int[][] board = generator.generateSudoku();
            assertTrue("Invalid board on run " + run, isValidSudoku(board));
        }
    }

    // checks that consecutive calls produce different boards
    @Test
    public void consecutiveCallsProduceDifferentBoards() {
        int[][] first = generator.generateSudoku();
        boolean foundDifference = false;

        for (int attempt = 0; attempt < 5 && !foundDifference; attempt++) {
            int[][] next = generator.generateSudoku();
            if (!boardsAreEqual(first, next)) {
                foundDifference = true;
            }
        }

        assertTrue("Generator produced identical boards on every attempt", foundDifference);
    }

    // helpers

    // checks that the array contains each value 0-8 exactly once
    private boolean isPermutationOfZeroToEight(int[] values) {
        boolean[] seen = new boolean[SIDE];
        for (int value : values) {
            if (value < 0 || value >= SIDE || seen[value]) {
                return false;
            }
            seen[value] = true;
        }
        return true;
    }

    // checks the whole board's validity (rows, columns, subgrids)
    private boolean isValidSudoku(int[][] board) {
        for (int r = 0; r < SIDE; r++) {
            if (!isPermutationOfZeroToEight(board[r])) return false;
        }
        for (int c = 0; c < SIDE; c++) {
            int[] column = new int[SIDE];
            for (int r = 0; r < SIDE; r++) column[r] = board[r][c];
            if (!isPermutationOfZeroToEight(column)) return false;
        }
        for (int boxRow = 0; boxRow < BOX; boxRow++) {
            for (int boxCol = 0; boxCol < BOX; boxCol++) {
                int[] box = new int[SIDE];
                int index = 0;
                for (int r = 0; r < BOX; r++) {
                    for (int c = 0; c < BOX; c++) {
                        box[index++] = board[boxRow * BOX + r][boxCol * BOX + c];
                    }
                }
                if (!isPermutationOfZeroToEight(box)) return false;
            }
        }
        return true;
    }

    // compares two boards cell by cell
    private boolean boardsAreEqual(int[][] a, int[][] b) {
        for (int r = 0; r < SIDE; r++) {
            for (int c = 0; c < SIDE; c++) {
                if (a[r][c] != b[r][c]) return false;
            }
        }
        return true;
    }
}