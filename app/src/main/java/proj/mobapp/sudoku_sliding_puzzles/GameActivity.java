package proj.mobapp.sudoku_sliding_puzzles;

import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import java.util.Arrays;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {
    TextView tvMessage;
    Button btSubmitSolution, btExit, btRules;

    int[][] sudoku = new int[9][9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game_portrait);

        init();

        generateSudoku();
    }

    private void init() {
        btSubmitSolution = findViewById(R.id.btSubmitSolution);
        tvMessage = findViewById(R.id.tvMessage);
        btExit = findViewById(R.id.btExit);
        btRules = findViewById(R.id.btRules);

        for (int[] row : sudoku) {
            Arrays.fill(row, -1);
        }
    }

    private void generateSudoku() {
        int base = 3;
        int side = base * base;

        int[][] pattern = new int[side][side];
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                pattern[r][c] = (base * (r % base) + r / base + c) % side;
            }
        }

        Random rand = new Random();

        // Losowa permutacja cyfr
        List<Integer> digits = new ArrayList<>();
        for (int d = 0; d < side; d++) digits.add(d);
        Collections.shuffle(digits, rand);

        int[][] result = new int[side][side];
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                result[r][c] = digits.get(pattern[r][c]);
            }
        }

        // Losowa zamiana wierszy w ramach każdego pasa (3 pasy po 3 wiersze)
        result = shuffleRowsWithinBands(result, base, rand);

        // Losowa zamiana kolumn w ramach każdego pasa
        result = shuffleColsWithinBands(result, base, rand);

        // Losowa zamiana całych pasów wierszy
        result = shuffleBands(result, base, rand, true);

        // Losowa zamiana całych pasów kolumn
        result = shuffleBands(result, base, rand, false);

        sudoku = result;

        for (int i = 0; i < side; i++) {
            Log.d("Sudoku", "Row " + i + ": " + Arrays.toString(sudoku[i]));
        }
    }

    // Randomly reorders rows within each 3-row band.
    private int[][] shuffleRowsWithinBands(int[][] grid, int base, Random rand) {
        int side = grid.length;
        int[][] out = new int[side][side];
        for (int band = 0; band < base; band++) {
            List<Integer> rowsInBand = new ArrayList<>();
            for (int k = 0; k < base; k++) rowsInBand.add(band * base + k);
            Collections.shuffle(rowsInBand, rand);
            for (int k = 0; k < base; k++) {
                out[band * base + k] = grid[rowsInBand.get(k)];
            }
        }
        return out;
    }

    // Randomly reorders columns within each 3-column band.
    private int[][] shuffleColsWithinBands(int[][] grid, int base, Random rand) {
        int side = grid.length;
        int[][] out = new int[side][side];
        for (int band = 0; band < base; band++) {
            List<Integer> colsInBand = new ArrayList<>();
            for (int k = 0; k < base; k++) colsInBand.add(band * base + k);
            Collections.shuffle(colsInBand, rand);
            for (int r = 0; r < side; r++) {
                for (int k = 0; k < base; k++) {
                    out[r][band * base + k] = grid[r][colsInBand.get(k)];
                }
            }
        }
        return out;
    }

    // Randomly reorders entire row or column bands.
    private int[][] shuffleBands(int[][] grid, int base, Random rand, boolean rows) {
        int side = grid.length;
        int[][] out = new int[side][side];
        List<Integer> bandOrder = new ArrayList<>();
        for (int b = 0; b < base; b++) bandOrder.add(b);
        Collections.shuffle(bandOrder, rand);

        for (int newBand = 0; newBand < base; newBand++) {
            int oldBand = bandOrder.get(newBand);
            for (int k = 0; k < base; k++) {
                if (rows) {
                    out[newBand * base + k] = grid[oldBand * base + k];
                } else {
                    for (int r = 0; r < side; r++) {
                        out[r][newBand * base + k] = grid[r][oldBand * base + k];
                    }
                }
            }
        }
        return out;
    }
}
