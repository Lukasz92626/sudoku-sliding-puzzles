package proj.mobapp.sudoku_sliding_puzzles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LevelChoiceActivity extends AppCompatActivity {
    TextView tvWelcome, tvSuccessFail;
    EditText etLevelChoice;
    Button btStart, btBackToMenu;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_level_choice);

        init();

        displayStats();
    }

    private void init() {
        tvWelcome = findViewById(R.id.tvWelcome);
        etLevelChoice = findViewById(R.id.etLevelChoice);
        btStart = findViewById(R.id.btStart);
        tvSuccessFail = findViewById(R.id.tvSuccessFail);
        btBackToMenu = findViewById(R.id.btBackToMenu);

        btStart.setOnClickListener(v -> {
            String input = etLevelChoice.getText().toString().trim();
            int level;
            try {
                level = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                return;
            }

            if (level < 1 || level > 7) {
                return;
            }

            Intent intent = new Intent(LevelChoiceActivity.this, GameActivity.class);
            intent.putExtra("level", level);
            startActivity(intent);
        });

        btBackToMenu.setOnClickListener(v -> {
            Intent intent = new Intent(LevelChoiceActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void displayStats() {
        SharedPreferences prefs = getSharedPreferences("stats", MODE_PRIVATE);
        int wins = prefs.getInt("wins", 0);
        int losses = prefs.getInt("losses", 0);
        tvSuccessFail.setText("Wins: " + wins + " | Losses: " + losses);
    }
}
