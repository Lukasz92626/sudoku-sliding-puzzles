package proj.mobapp.sudoku_sliding_puzzles;

public class PuzzleTile {
    private int value;      // sudoku digit shown on the tile (1-9)
    private boolean blank;  // true if this is the empty slot

    public PuzzleTile(int value, boolean blank) {
        this.value = value;
        this.blank = blank;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isBlank() {
        return blank;
    }

    public void setBlank(boolean blank) {
        this.blank = blank;
    }
}
