package proj.mobapp.sudoku_sliding_puzzles;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    TextView tvTitle;
    Button btPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        init();
    }

    public void init(){
        tvTitle = findViewById(R.id.tvTitle);
        btPlay = findViewById(R.id.btPlay);

        btPlay.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LevelChoiceActivity.class);
            startActivity(intent);
        });
    }
}