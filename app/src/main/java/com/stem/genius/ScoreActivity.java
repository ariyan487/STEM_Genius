package com.stem.genius;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ScoreActivity extends AppCompatActivity {
    TextView txtscore,as;
    TextView txtStatus, tvSubjectName, txtMasterScore;
    MediaPlayer audio;
    ImageView imgBack;
    SharedPreferences sharedPreferences;

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
        as = findViewById(R.id.as);
        txtMasterScore = findViewById(R.id.txtMasterScore);
        sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        Intent intent = getIntent();
        int score = intent.getIntExtra("score", 0);
        int totalQuestions = intent.getIntExtra("totalQuestions", 15); // Default to 15 if not provided
        String scoreDisplay = score + "/" + totalQuestions;
        txtscore.setText(scoreDisplay);
        txtStatus.setText(setStatus(score));
        tvSubjectName.setText(QuestionCollection.SUBJECT_NAME);

        int currentMasterPoints = calculateMasterPoints(score);
        if (currentMasterPoints < 0) {
            txtMasterScore.setText("You lost " + Math.abs(currentMasterPoints) + " STEM Point");
        } else {
            txtMasterScore.setText("You Win " + currentMasterPoints + " STEM Point");
        }

        updateTotalMasterScore(currentMasterPoints);
        updateScoreCounts(score);

        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent home = new Intent(ScoreActivity.this, MainActivity.class);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(home);
                finish();
            }
        });

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("savedScore", scoreDisplay + " (" + QuestionCollection.SUBJECT_NAME + ")");
        editor.apply();



        as.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int totalMasterScore = sharedPreferences.getInt("masterScore", 0);
                int count15 = sharedPreferences.getInt("count_15", 0);
                int count10 = sharedPreferences.getInt("count_10", 0);
                int count7 = sharedPreferences.getInt("count_7", 0);

                LayoutInflater inflater = getLayoutInflater();
                View alertLayout = inflater.inflate(R.layout.alert_box_cardview, null);
                TextView tvMasterScore = alertLayout.findViewById(R.id.tvMasterScore);
                tvMasterScore.setText("Your master score is: " + totalMasterScore);

                TextView tvRules = alertLayout.findViewById(R.id.tvRules);
                String underlinedText = "Rules of STEM point:\n";
                String remainingText = "Score 15 = 5 STEM points -->> (" + count15 + " times)\n"
                        + "Score 10 = 2 STEM points -->> (" + count10 + " times)\n"
                        + "Score 7 = 1 STEM point   -->> (" + count7 + " times)\n"
                        + "Score < 5 = -2 STEM points\n"
                        + "Score 5-6 = -1 STEM point";

                SpannableString spannableString = new SpannableString(underlinedText + remainingText);
                spannableString.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 0);
                tvRules.setText(spannableString);


                AlertDialog.Builder builder = new AlertDialog.Builder(ScoreActivity.this);
                builder.setView(alertLayout);

                AlertDialog dialog = builder.create();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                Button btnDismiss = alertLayout.findViewById(R.id.btnDismiss);
                btnDismiss.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

    }

    private String setStatus(int score) {
        if (score >= 8) {
            audio = MediaPlayer.create(this, R.raw.high_score);
            audio.start();
            return "Congratulations! You did very well";
        }
        if (score >= 5) {
            audio = MediaPlayer.create(this, R.raw.medium_score);
            audio.start();
            return "Good job! Try again";
        }
        audio = MediaPlayer.create(this, R.raw.low_score);
        audio.start();
        return "You need to do better :)";
    }

    private int calculateMasterPoints(int score) {
        int currentMasterPoints = 0;
        if (score >= 15) {
            currentMasterPoints = POINTS_FOR_15;
        } else if (score >= 10) {
            currentMasterPoints = POINTS_FOR_10;
        } else if (score >= 7) {
            currentMasterPoints = POINTS_FOR_7;
        } else if (score < 5) {
            currentMasterPoints = -2;
        } else {
            currentMasterPoints = -1;
        }
        return currentMasterPoints;
    }

    private void updateTotalMasterScore(int currentMasterPoints) {
        int masterScore = sharedPreferences.getInt("masterScore", 0);
        int totalMasterScore = masterScore + currentMasterPoints;

        if (totalMasterScore < 0) {
            totalMasterScore = 0;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("masterScore", totalMasterScore);
        editor.apply();
    }

    private void updateScoreCounts(int score) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (score >= 15) {
            int count = sharedPreferences.getInt("count_15", 0) + 1;
            editor.putInt("count_15", count);
        } else if (score >= 10) {
            int count = sharedPreferences.getInt("count_10", 0) + 1;
            editor.putInt("count_10", count);
        } else if (score >= 7) {
            int count = sharedPreferences.getInt("count_7", 0) + 1;
            editor.putInt("count_7", count);
        }
        editor.apply();
    }
}
