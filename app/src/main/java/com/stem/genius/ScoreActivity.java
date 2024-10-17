package com.stem.genius;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ScoreActivity extends AppCompatActivity {
    TextView txtscore;
    TextView txtStatus, tvSubjectName, txtMasterScore; // Added TextView for Master Score
    MediaPlayer audio;
    ImageView imgBack;
    SharedPreferences sharedPreferences;

    // Define constant values for master points
    private static final int POINTS_FOR_7 = 1;
    private static final int POINTS_FOR_10 = 2;
    private static final int POINTS_FOR_15 = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_score);

        imgBack = findViewById(R.id.imgBack);
        tvSubjectName = findViewById(R.id.tvSubjectName);
        txtscore = findViewById(R.id.txtscore);
        txtStatus = findViewById(R.id.txtStatus);
        txtMasterScore = findViewById(R.id.txtMasterScore); // Initialize new TextView for Master Score

        sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        Intent intent = getIntent();

        int score = intent.getIntExtra("score", 0);
        int totalQuestions = intent.getIntExtra("totalQuestions", 15); // Default to 15 if not provided
        String scoreDisplay = score + "/" + totalQuestions;

        txtscore.setText(scoreDisplay); // Display score as correct/total
        txtStatus.setText(setStatus(score)); // Use score only for status
        tvSubjectName.setText(QuestionCollection.SUBJECT_NAME);

        // Calculate and update Master Score
        int currentMasterPoints = calculateMasterPoints(score);

        // Check if points are lost
        if (currentMasterPoints < 0) {
            txtMasterScore.setText("You lost " + Math.abs(currentMasterPoints) + " STEM Point"); // Display lost points
        } else {
            txtMasterScore.setText("You Win " + currentMasterPoints + " STEM Point");  // Show won points
        }

        // Update the total master score in SharedPreferences
        updateTotalMasterScore(currentMasterPoints);

        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent home = new Intent(ScoreActivity.this, MainActivity.class);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(home);
                finish();
            }
        });

        // Save score display in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("savedScore", scoreDisplay + " (" + QuestionCollection.SUBJECT_NAME + ")");
        editor.apply();
    }

    private String setStatus(int score) {
        if (score >= 8) {
            audio = MediaPlayer.create(this, R.raw.high_score);
            audio.start();  // Playing high score audio
            Log.d("ScoreActivity", "Playing high score audio for score: " + score);
            return "Congratulations! You did very well";
        }
        if (score >= 5) {
            audio = MediaPlayer.create(this, R.raw.medium_score);
            audio.start();  // Playing medium score audio
            Log.d("ScoreActivity", "Playing medium score audio for score: " + score);
            return "Good job! Try again";
        }
        audio = MediaPlayer.create(this, R.raw.low_score);
        audio.start();  // Playing low score audio
        Log.d("ScoreActivity", "Playing low score audio for score: " + score);
        return "You need to do better :)";
    }

    private int calculateMasterPoints(int score) {
        int currentMasterPoints = 0;

        if (score >= 15) {
            currentMasterPoints = POINTS_FOR_15; // 5 points for answering all correctly
        } else if (score >= 10) {
            currentMasterPoints = POINTS_FOR_10; // 2 points for answering 10 correctly
        } else if (score >= 7) {
            currentMasterPoints = POINTS_FOR_7; // 1 point for answering 7 correctly
        } else if (score < 5) {
            currentMasterPoints = -2; // 2 points minus for score below 5
        } else {
            currentMasterPoints = -1; // 1 point minus for score between 5 and 6
        }

        return currentMasterPoints; // Return the current master points or minus
    }

    private void updateTotalMasterScore(int currentMasterPoints) {
        int masterScore = sharedPreferences.getInt("masterScore", 0); // Retrieve previous score
        int totalMasterScore = masterScore + currentMasterPoints; // Calculate new total

        // Make sure the totalMasterScore doesn't go below 0
        if (totalMasterScore < 0) {
            totalMasterScore = 0;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("masterScore", totalMasterScore); // Update total master score
        editor.apply();
    }

    @Override
    public void onDestroy() {
        if (audio != null) {
            audio.release(); // Release audio resources
            audio = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent home = new Intent(ScoreActivity.this, MainActivity.class);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
        finish();
    }
}
