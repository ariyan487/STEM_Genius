package com.stem.genius;

import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuestionCollection extends AppCompatActivity {
    RadioGroup radioGroup;
    TextView lblQuestion, timerText;
    RadioButton optionA;
    RadioButton optionB;
    RadioButton optionC;
    RadioButton optionD;
    Button confirm;
    String rightAnswer;
    String Answer;
    public static List<QuestionModule> question_list;
    int score;
    public static String SUBJECT_NAME = "";
    public static ArrayList<ArrayList<QuestionModule>> questionBank = new ArrayList<>();
    public static ArrayList<HashMap<String, String>> subjectList = new ArrayList<>();
    LinearLayout rootLay;


    // Timer variables
    private CountDownTimer countDownTimer;
    private static final long TIME_LIMIT = 16000; // 15 seconds
    private boolean isTimerRunning = false; // To track timer state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        rootLay = findViewById(R.id.rootLay);
        confirm = findViewById(R.id.confirm);
        lblQuestion = findViewById(R.id.lblPergunta);
        optionA = findViewById(R.id.opcaoA);
        optionB = findViewById(R.id.opcaoB);
        optionC = findViewById(R.id.opcaoC);
        optionD = findViewById(R.id.opcaoD);
        score = 0;
        radioGroup = findViewById(R.id.radioGroup);
        timerText = findViewById(R.id.timerText);
        loadQuestion();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadQuestion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rootLay != null) {
            rootLay.startAnimation(AnimationUtils.loadAnimation(QuestionCollection.this, R.anim.middle_to_top));
        }

        // Check if a question is currently being displayed
        if (lblQuestion.getText() != null && !lblQuestion.getText().toString().isEmpty()) {
            // Re-display the current question and options
            return; // Exit if a question is already displayed
        }

        // Load a new question only if no question is being displayed
        loadQuestion();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    // Load question method
    private void loadQuestion() {
        if (question_list != null && question_list.size() > 0) {
            QuestionModule q = question_list.remove(0);
            lblQuestion.setText(q.getQuestion());
            List<String> answers = q.getAnswers();

            optionA.setText(answers.get(0));
            optionB.setText(answers.get(1));
            optionC.setText(answers.get(2));
            optionD.setText(answers.get(3));
            rightAnswer = q.getRightAnswer();

            // Start timer
            startTimer();
        } else {
            Intent intent = new Intent(this, ScoreActivity.class);
            intent.putExtra("score", Math.max(score, 0)); // Score must be 0 or higher
            startActivity(intent);
            finish();
        }
    }

    // Load answer method
    public void loadAnswer(View view) {
        int op = radioGroup.getCheckedRadioButtonId();

        if (op == R.id.opcaoA) {
            Answer = "A";
        } else if (op == R.id.opcaoB) {
            Answer = "B";
        } else if (op == R.id.opcaoC) {
            Answer = "C";
        } else if (op == R.id.opcaoD) {
            Answer = "D";
        } else {
            return;
        }

        radioGroup.clearCheck();

        // Stop timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning = false; // Update timer state
        }

        // Check answer
        checkAnswer(Answer);
    }

    private void checkAnswer(String Answer) {
        if (Answer.equals(rightAnswer)) {
            this.score += 1; // Increase score for correct answer
            startActivity(new Intent(this, RightActivity.class));
        } else {
            if (score > 0) {
                score--; // Decrease score for wrong answer, but not below 0
            }
            showAdAlert(); // Show alert for wrong answer
        }
    }

    private void showAdAlert() {
        // Create a TextView for Alert Box
        final TextView timerTextView = new TextView(this);
        timerTextView.setText("Time left: 6s");
        timerTextView.setPadding(20, 20, 20, 20); // Add some padding
        timerTextView.setTextSize(18); // Increase timer text size
        timerTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // Set timer text color to red
        timerTextView.setGravity(Gravity.CENTER); // Center the text

        // Create Alert Dialog
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Wrong Answer")
                .setMessage("Play ad to get one more chance")
                .setPositiveButton("Play Ad", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAd(); // Call function to show ad
                    }
                })
                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (score > 0) {
                            score--; // Decrease score, but not below 0
                        }
                        Intent intent = new Intent(QuestionCollection.this, WrongActivity.class);
                        startActivity(intent); // Navigate to WrongActivity
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create();

        // Show Alert Dialog
        alertDialog.setView(timerTextView); // Set timer TextView in Dialog
        alertDialog.show();

        // 6 seconds countdown timer
        CountDownTimer adTimer = new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Time left: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                // Dismiss Alert
                alertDialog.dismiss();
                // Mark answer as wrong
                if (score > 0) {
                    score--; // Decrease score, but not below 0
                }
                Intent intent = new Intent(QuestionCollection.this, WrongActivity.class);
                startActivity(intent); // Navigate to WrongActivity
            }
        };
        adTimer.start();

        // Stop question timer
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Stop question timer
            isTimerRunning = false; // Update timer state
        }

        // If Skip button is clicked, stop timer
        alertDialog.setOnDismissListener(dialog -> {
            adTimer.cancel(); // Stop ad timer
            // Restart question timer
            startTimer(); // Restart question timer
        });
    }

    // Function to show ad
    private void showAd() {
        // Add your AdMob ad showing code here
    }

    // Start timer method
    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel previous timer if any
        }

        countDownTimer = new CountDownTimer(TIME_LIMIT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time left: " + millisUntilFinished / 1000 + "s");
                isTimerRunning = true; // Update timer state
            }

            @Override
            public void onFinish() {
                // If time runs out, mark as wrong answer
                Answer = " ";  // This is to indicate a wrong answer
                showAdAlert(); // Show alert
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public static ArrayList<QuestionModule> questions;
    public static void createQuestionBank() {
        QuestionCollection.subjectList = new ArrayList<>();
        QuestionCollection.questionBank = new ArrayList<>();

        //------------- Subject 1
        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("ঢাকার পূর্ব নাম কি ছিল?", "D", "ইসলামাবাদ", "আগরতলা", "বিজয়নগর", "জাহাঙ্গিরনগর"));
                add(new QuestionModule("রোকেয়া দিবস কত তারিখে পালন করা হয়?", "C", "১৭ ফেব্রুয়ারি", "১৯ ফেব্রুয়ারি", "৯ ডিসেম্বর", "২২ ফেব্রুয়ারি"));
                add(new QuestionModule("দিল্লির হিংহাসনে অধিষ্ঠিত প্রথম মুসলিম নারী কে?", "B", "রোকেয়া ইসলাম", "সুলতানা রাজিয়া", "সালেহা বেগম", "পারভিন সুলতানা"));
                add(new QuestionModule("উপমহাদেশের প্রথম অস্কার জয়ী কে?", "B", "রবীন্দ্রনাথ ঠাকুর", "সত্যজিৎ রায়", "জুবায়ের হোসেন", "নুহাশ হুমায়ুন"));
                add(new QuestionModule("বাংলাদেশের গভীরতম নদী কোনটি?", "D", "পদ্মা", "যমুনা", "ব্রহ্মপুত্র", "মেধনা"));
            }
        };
        QuestionModule.createQuestionsForSubject("সাধারন জ্ঞান", R.drawable.category_icon1, questions);
    }
}