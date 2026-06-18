package proj.mobapp.sudoku_sliding_puzzles;

import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    TextView tvMessage;
    Button btSubmitSolution, btExit, btRules;

    int[][] sudoku = new int[9][9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game_portrait);

        init();
    }

    public void init(){
        btSubmitSolution = findViewById(R.id.btSubmitSolution);
        tvMessage = findViewById(R.id.tvMessage);
        btExit = findViewById(R.id.btExit);
        btRules = findViewById(R.id.btRules);
    }


}
