package proj.mobapp.sudoku_sliding_puzzles;

public class PuzzleTile {
    private int value;      // sudoku digit shown on the tile (1-9)
    private boolean blank;  // true if this is the empty slot
    private boolean locked; // true if the tile is fixed and cannot be moved

    public PuzzleTile(int value, boolean blank) {
        this.value = value;
        this.blank = blank;
        this.locked = false;
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
        if (blank) this.value = 0;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
