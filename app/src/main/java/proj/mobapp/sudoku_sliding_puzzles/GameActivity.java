package proj.mobapp.sudoku_sliding_puzzles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.util.Log;

import android.view.View;
import android.view.ViewGroup;

import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Random;
import java.util.Arrays;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    TextView tvMessage;
    Button btSubmitSolution, btExit, btRules;

    int[][] sudoku = new int[9][9];

    private static final int SIDE = 9;

    RecyclerView recyclerView;
    PuzzleAdapter puzzleAdapter;
    List<PuzzleTile> tiles = new ArrayList<>();
    int blankIndex;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private static final float TILT_THRESHOLD = 3.5f;
    private static final float TILT_RESET_THRESHOLD = 1.5f;  // próg "telefon wrócił do płaskiego"
    private boolean tiltConsumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_game_portrait);
        }
        else {
            setContentView(R.layout.activity_game_landscape);
        }

        for (int[] row : sudoku) {
            Arrays.fill(row, -1);
        }

        init();

        generateSudoku();
        buildBoard();
        scrambleBoard(10);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];

        // Jeśli telefon wrócił do pozycji neutralnej, odblokuj możliwość następnego ruchu
        if (Math.abs(x) < TILT_RESET_THRESHOLD && Math.abs(y) < TILT_RESET_THRESHOLD) {
            tiltConsumed = false;
            return;
        }

        // Ruch już wykonano więc czekamy na wypłaszczenie
        if (tiltConsumed) return;

        if (Math.abs(x) < TILT_THRESHOLD && Math.abs(y) < TILT_THRESHOLD) return;

        if (Math.abs(x) > Math.abs(y)) {
            if (x > TILT_THRESHOLD) {
                moveFromDirection(Direction.RIGHT_TO_BLANK);
            } else if (x < -TILT_THRESHOLD) {
                moveFromDirection(Direction.LEFT_TO_BLANK);
            }
        } else {
            if (y > TILT_THRESHOLD) {
                moveFromDirection(Direction.TOP_TO_BLANK);
            } else if (y < -TILT_THRESHOLD) {
                moveFromDirection(Direction.BOTTOM_TO_BLANK);
            }
        }

        tiltConsumed = true;  // blokuj kolejne ruchy aż telefon wróci do płaskiego
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void init() {
        btSubmitSolution = findViewById(R.id.btSubmitSolution);
        tvMessage = findViewById(R.id.tvMessage);
        btExit = findViewById(R.id.btExit);
        btRules = findViewById(R.id.btRules);
        recyclerView = findViewById(R.id.recyclerView);

        btSubmitSolution.setOnClickListener(v -> {
            syncSudokuFromTiles();
            if (checkBoard()) {
                tvMessage.setText("You solved correctly!");
                saveWin();
            } else {
                tvMessage.setText("The board contains errors.");
                saveLoss();
            }
        });

        btExit.setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_game_portrait);
        } else {
            setContentView(R.layout.activity_game_landscape);
        }

        init();

        recyclerView.setLayoutManager(new GridLayoutManager(this, SIDE));
        puzzleAdapter = new PuzzleAdapter(tiles, this::onTileClicked);
        recyclerView.setAdapter(puzzleAdapter);
        recyclerView.setItemAnimator(null);

        makeRecyclerSquare();
    }

    private void buildBoard() {
        tiles.clear();
        blankIndex = -1;

        List<Integer> zeroPositions = new ArrayList<>();
        for (int r = 0; r < SIDE; r++) {
            for (int c = 0; c < SIDE; c++) {
                int value = sudoku[r][c];
                tiles.add(new PuzzleTile(value, false));
                if (value == 0) {
                    zeroPositions.add(r * SIDE + c);
                }
            }
        }

        // Pick one of the zero-cells at random to be the blank slot.
        Random rand = new Random();
        blankIndex = zeroPositions.get(rand.nextInt(zeroPositions.size()));
        tiles.get(blankIndex).setBlank(true);

        recyclerView.setLayoutManager(new GridLayoutManager(this, SIDE));
        puzzleAdapter = new PuzzleAdapter(tiles, this::onTileClicked);
        recyclerView.setAdapter(puzzleAdapter);
        recyclerView.setItemAnimator(null);

        makeRecyclerSquare();
    }

    private void onTileClicked(int position) {
        if (isAdjacent(position, blankIndex)) {
            swapTiles(position, blankIndex);
            blankIndex = position;
        }
    }

    private boolean isAdjacent(int a, int b) {
        int rowA = a / SIDE, colA = a % SIDE;
        int rowB = b / SIDE, colB = b % SIDE;
        int rowDiff = Math.abs(rowA - rowB);
        int colDiff = Math.abs(colA - colB);
        return (rowDiff + colDiff) == 1; // exactly one step up/down/left/right
    }

    private void swapTiles(int posA, int posB) {
        RecyclerView.ViewHolder vhA = recyclerView.findViewHolderForAdapterPosition(posA);
        RecyclerView.ViewHolder vhB = recyclerView.findViewHolderForAdapterPosition(posB);

        if (vhA == null || vhB == null) {
            performSwap(posA, posB);
            return;
        }

        View viewA = vhA.itemView;
        View viewB = vhB.itemView;

        float dx = viewB.getX() - viewA.getX();
        float dy = viewB.getY() - viewA.getY();

        ObjectAnimator animAX = ObjectAnimator.ofFloat(viewA, View.TRANSLATION_X, 0f, dx);
        ObjectAnimator animAY = ObjectAnimator.ofFloat(viewA, View.TRANSLATION_Y, 0f, dy);

        ObjectAnimator animBX = ObjectAnimator.ofFloat(viewB, View.TRANSLATION_X, 0f, -dx);
        ObjectAnimator animBY = ObjectAnimator.ofFloat(viewB, View.TRANSLATION_Y, 0f, -dy);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animAX, animAY, animBX, animBY);
        set.setDuration(150);

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                viewA.setTranslationX(0f);
                viewA.setTranslationY(0f);
                viewB.setTranslationX(0f);
                viewB.setTranslationY(0f);

                performSwap(posA, posB);
            }
        });

        set.start();
    }

    private void performSwap(int posA, int posB) {
        PuzzleTile tileA = tiles.get(posA);
        PuzzleTile tileB = tiles.get(posB);
        tiles.set(posA, tileB);
        tiles.set(posB, tileA);
        puzzleAdapter.notifyItemChanged(posA);
        puzzleAdapter.notifyItemChanged(posB);
    }

    private void scrambleBoard(int moves) {
        Random rand = new Random();
        int currentBlank = blankIndex;

        for (int i = 0; i < moves; i++) {
            List<Integer> neighbors = getNeighborIndices(currentBlank);
            int swapWith = neighbors.get(rand.nextInt(neighbors.size()));

            PuzzleTile tileA = tiles.get(swapWith);
            PuzzleTile tileB = tiles.get(currentBlank);
            tiles.set(currentBlank, tileA);
            tiles.set(swapWith, tileB);

            currentBlank = swapWith;
        }

        blankIndex = currentBlank;
        puzzleAdapter.notifyDataSetChanged();
    }

    private List<Integer> getNeighborIndices(int pos) {
        List<Integer> result = new ArrayList<>();
        int row = pos / SIDE, col = pos % SIDE;

        if (row > 0) result.add(pos - SIDE);        // up
        if (row < SIDE - 1) result.add(pos + SIDE);  // down
        if (col > 0) result.add(pos - 1);            // left
        if (col < SIDE - 1) result.add(pos + 1);     // right

        return result;
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

    private void syncSudokuFromTiles() {
        for (int r = 0; r < SIDE; r++) {
            for (int c = 0; c < SIDE; c++) {
                int flatIndex = r * SIDE + c;
                sudoku[r][c] = tiles.get(flatIndex).getValue();
            }
        }
    }

    private boolean checkBoard() {
        // checking lines
        for (int i = 0; i < 9; ++i){
            for (int j = 0; j < 9; ++j) {
                int repetations = 0;
                for(int m = 0; m < 9; ++m) {
                    if (sudoku[i][m] == j) {
                        repetations++;
                    }
                }
                if (repetations > 1) {
                    return false;
                }
            }
        }

        // checking rows
        for (int i = 0; i < 9; ++i){
            for (int j = 0; j < 9; ++j) {
                int repetations = 0;
                for(int m = 0; m < 9; ++m) {
                    if (sudoku[m][i] == j) {
                        repetations++;
                    }
                }
                if (repetations > 1) {
                    return false;
                }
            }
        }

        // checking squares
        for (int boxRow = 0; boxRow < 3; ++boxRow) {
            for (int boxCol = 0; boxCol < 3; ++boxCol) {
                for (int digit = 0; digit < 9; ++digit) {
                    int repetitions = 0;
                    for (int r = boxRow * 3; r < boxRow * 3 + 3; ++r) {
                        for (int c = boxCol * 3; c < boxCol * 3 + 3; ++c) {
                            if (sudoku[r][c] == digit) {
                                repetitions++;
                            }
                        }
                    }
                    if (repetitions > 1) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void saveWin() {
        SharedPreferences prefs = getSharedPreferences("stats", MODE_PRIVATE);
        int wins = prefs.getInt("wins", 0);
        prefs.edit().putInt("wins", wins + 1).apply();
    }

    private void saveLoss() {
        SharedPreferences prefs = getSharedPreferences("stats", MODE_PRIVATE);
        int losses = prefs.getInt("losses", 0);
        prefs.edit().putInt("losses", losses + 1).apply();
    }

    private void makeRecyclerSquare() {
        final View parent = (View) recyclerView.getParent();
        parent.post(() -> {
            int availableWidth = parent.getWidth();
            int availableHeight = parent.getHeight();
            int size = Math.min(availableWidth, availableHeight);

            ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
            params.width = size;
            params.height = size;
            recyclerView.setLayoutParams(params);
        });
    }

    private enum Direction {
        LEFT_TO_BLANK, RIGHT_TO_BLANK, TOP_TO_BLANK, BOTTOM_TO_BLANK
    }

    private void moveFromDirection(Direction direction) {
        int row = blankIndex / SIDE;
        int col = blankIndex % SIDE;
        int sourcePos = -1;

        switch (direction) {
            case LEFT_TO_BLANK:
                if (col > 0) sourcePos = blankIndex - 1;
                break;
            case RIGHT_TO_BLANK:
                if (col < SIDE - 1) sourcePos = blankIndex + 1;
                break;
            case TOP_TO_BLANK:
                if (row > 0) sourcePos = blankIndex - SIDE;
                break;
            case BOTTOM_TO_BLANK:
                if (row < SIDE - 1) sourcePos = blankIndex + SIDE;
                break;
        }

        if (sourcePos != -1) {
            swapTiles(sourcePos, blankIndex);
            blankIndex = sourcePos;
        }
    }
}
