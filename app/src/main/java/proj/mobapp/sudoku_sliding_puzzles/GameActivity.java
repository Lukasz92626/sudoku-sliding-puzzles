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
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;

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
    Button btSubmitSolution, btExit, btSensor;

    int[][] sudoku = new int[9][9];

    private static final int SIDE = 9;

    private int level = 1;
    private int squareSize = 3;
    private int squareRow0 = 0;
    private int squareCol0 = 0;

    RecyclerView recyclerView;
    PuzzleAdapter puzzleAdapter;
    List<PuzzleTile> tiles = new ArrayList<>();
    int blankIndex;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private static final float TILT_THRESHOLD = 3.5f;
    private static final float TILT_RESET_THRESHOLD = 1.5f;  // próg "telefon wrócił do płaskiego"
    private boolean tiltConsumed = false;

    private boolean controlsLocked = false;

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

        level = getIntent().getIntExtra("level", 1);
        squareSize = Math.max(3, Math.min(SIDE, level + 2));

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
        if (controlsLocked) return;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float rawX = event.values[0];
        float rawY = event.values[1];

        // Przemapuj osie akcelerometru względem aktualnej rotacji ekranu,
        // żeby przechylenie zawsze odpowiadało kierunkowi na planszy.
        int rotation = ((WindowManager) getSystemService(WINDOW_SERVICE))
                .getDefaultDisplay().getRotation();

        float x, y;
        switch (rotation) {
            case Surface.ROTATION_90:   // landscape: telefon obrócony w lewo
                x = -rawY;
                y = rawX;
                break;
            case Surface.ROTATION_270:  // landscape: telefon obrócony w prawo
                x = rawY;
                y = -rawX;
                break;
            case Surface.ROTATION_180:  // portrait do góry nogami
                x = -rawX;
                y = -rawY;
                break;
            default:                    // ROTATION_0 — zwykły portrait
                x = rawX;
                y = rawY;
                break;
        }

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

        tiltConsumed = true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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

    private void init() {
        btSubmitSolution = findViewById(R.id.btSubmitSolution);
        tvMessage = findViewById(R.id.tvMessage);
        btExit = findViewById(R.id.btExit);
        btSensor = findViewById(R.id.btSensor);
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

        btSensor.setOnClickListener(v -> {
            if(controlsLocked)
                btSensor.setText("Disable sensor control");
            else
                btSensor.setText("Enable sensor control");
            controlsLocked = !controlsLocked;
        });

        btExit.setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void buildBoard() {
        Random rand = new Random();
        int maxOffset = SIDE - squareSize;
        List<Integer> zeroPositions = new ArrayList<>();

        do {
            squareRow0 = (maxOffset > 0) ? rand.nextInt(maxOffset + 1) : 0;
            squareCol0 = (maxOffset > 0) ? rand.nextInt(maxOffset + 1) : 0;

            zeroPositions.clear();
            for (int r = squareRow0; r < squareRow0 + squareSize; r++) {
                for (int c = squareCol0; c < squareCol0 + squareSize; c++) {
                    if (sudoku[r][c] == 0) {
                        zeroPositions.add(r * SIDE + c);
                    }
                }
            }
        } while (zeroPositions.isEmpty());

        tiles.clear();
        blankIndex = -1;

        for (int r = 0; r < SIDE; r++) {
            for (int c = 0; c < SIDE; c++) {
                int value = sudoku[r][c];
                PuzzleTile tile = new PuzzleTile(value, false);

                boolean inSquare = isInsideSquare(r, c);
                tile.setLocked(!inSquare);

                tiles.add(tile);
            }
        }

        blankIndex = zeroPositions.get(rand.nextInt(zeroPositions.size()));
        tiles.get(blankIndex).setBlank(true);

        recyclerView.setLayoutManager(new GridLayoutManager(this, SIDE));
        puzzleAdapter = new PuzzleAdapter(tiles, this::onTileClicked);
        recyclerView.setAdapter(puzzleAdapter);
        recyclerView.setItemAnimator(null);

        makeRecyclerSquare();
    }

    private boolean isInsideSquare(int row, int col) {
        return row >= squareRow0 && row < squareRow0 + squareSize
                && col >= squareCol0 && col < squareCol0 + squareSize;
    }

    private boolean isInsideSquare(int pos) {
        return isInsideSquare(pos / SIDE, pos % SIDE);
    }

    private void onTileClicked(int position) {
        if (controlsLocked) return;
        if (!isInsideSquare(position)) return;
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

        if (row > 0) addIfInsideSquare(result, pos - SIDE);        // up
        if (row < SIDE - 1) addIfInsideSquare(result, pos + SIDE);  // down
        if (col > 0) addIfInsideSquare(result, pos - 1);            // left
        if (col < SIDE - 1) addIfInsideSquare(result, pos + 1);     // right

        return result;
    }

    private void addIfInsideSquare(List<Integer> list, int pos) {
        if (isInsideSquare(pos)) {
            list.add(pos);
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

    private void syncSudokuFromTiles() {
        for (int r = 0; r < SIDE; r++) {
            for (int c = 0; c < SIDE; c++) {
                int flatIndex = r * SIDE + c;
                PuzzleTile tile = tiles.get(flatIndex);
                sudoku[r][c] = tile.isBlank() ? 0 : tile.getValue();
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
                if (repetations != 1) {
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
                if (repetations != 1) {
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
                    if (repetitions != 1) {
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

        if (sourcePos != -1 && isInsideSquare(sourcePos) && !tiles.get(sourcePos).isLocked()) {
            swapTiles(sourcePos, blankIndex);
            blankIndex = sourcePos;
        }
    }
}
