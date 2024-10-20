package com.stem.genius;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class QuestionCollection extends AppCompatActivity {
    private RadioGroup radioGroup;
    private TextView lblQuestion,tvQuestionCounter, timerText;
    private RadioButton optionA, optionB, optionC, optionD;
    private String rightAnswer, Answer;
    public static List<QuestionModule> question_list;
    private int score, totalQuestionsPlayed;
    public static String SUBJECT_NAME = "";
    public static ArrayList<ArrayList<QuestionModule>> questionBank = new ArrayList<>();
    public static ArrayList<HashMap<String, String>> subjectList = new ArrayList<>();
    private LinearLayout rootLay;
    private RewardedAd rewardedAd;
    private AlertDialog alertDialog;
    private CountDownTimer adTimer, countDownTimer;

    // Timer settings
    private static final long TIME_LIMIT = 16000; // 16 seconds
    private boolean isTimerRunning = false;
    //isAdShowing
    private boolean isAdShowing = false;

    int totalQuestions = 15;
    int currentQuestionNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        initializeViews();
        int roundNumber = 1; // প্রথম রাউন্ড হিসেবে মান দিন
        manageQuestions(roundNumber); // রাউন্ডের জন্য সংখ্যা পাস করুন
        loadQuestion(); // Load the first question

        // বিজ্ঞাপন লোড করা অ্যাপ চালু হওয়ার সাথে সাথে
        loadRewardedAd();
    }
    private void updateQuestionCounter() {
        String counterText = currentQuestionNumber + "/" + totalQuestions;
        tvQuestionCounter.setText(counterText);
        currentQuestionNumber++;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void initializeViews() {
        rootLay = findViewById(R.id.rootLay);
        lblQuestion = findViewById(R.id.lblPergunta);
        optionA = findViewById(R.id.opcaoA);
        optionB = findViewById(R.id.opcaoB);
        optionC = findViewById(R.id.opcaoC);
        optionD = findViewById(R.id.opcaoD);
        radioGroup = findViewById(R.id.radioGroup);
        timerText = findViewById(R.id.timerText);
        score = 0;
        totalQuestionsPlayed = 0;
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter);
    }

    private final List<QuestionModule> usedQuestions = new ArrayList<>();
    private final List<List<QuestionModule>> roundsQuestions = new ArrayList<>(); // Rounds questions

    private void manageQuestions(int roundNumber) {
        // Check if we need to refill questions
        if (question_list.size() < 15) {
            refillQuestions(); // Fill questions from usedQuestions if we have less than 15 questions
        }

        // Remove previously used questions from the selection
        question_list.removeAll(usedQuestions); // Exclude used questions from the current selection

        // Shuffle and select 15 questions
        Collections.shuffle(question_list);
        List<QuestionModule> selectedQuestions = question_list.subList(0, Math.min(15, question_list.size()));

        // Save the selected questions to the roundsQuestions list
        roundsQuestions.add(new ArrayList<>(selectedQuestions));

        // Set new question list and move selected to usedQuestions
        question_list = new ArrayList<>(selectedQuestions);
        usedQuestions.addAll(selectedQuestions); // Add to usedQuestions
    }

    private void refillQuestions() {
        // Move used questions back to question_list if we have enough remaining questions
        if (usedQuestions.size() + question_list.size() >= 15) {
            question_list.addAll(usedQuestions);
            Collections.shuffle(question_list);
            usedQuestions.clear(); // Clear used questions after adding back to question_list
        }
    }





    protected void onPause() {
        super.onPause();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            loadQuestion();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rootLay.startAnimation(AnimationUtils.loadAnimation(QuestionCollection.this, R.anim.middle_to_top));
        if (!isAdShowing) {
            if (lblQuestion.getText() == null || lblQuestion.getText().toString().isEmpty()) {
                loadQuestion();
                startTimer();
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!isAdShowing && !isTimerRunning) {
            loadQuestion();
        }
    }

    private boolean doubleBackToExitPressedOnce = false;
    private Toast backToast;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            if (backToast != null) {
                backToast.cancel();
            }
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        backToast = Toast.makeText(this, "Please press BACK again to EXIT The Round", Toast.LENGTH_SHORT);
        backToast.show();

        // Reset the flag after 2 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    // Load the current question and answers
    private void loadQuestion() {
        if (question_list != null && !question_list.isEmpty()) {
            QuestionModule q = question_list.remove(0); // Get and remove the first question
            lblQuestion.setText(q.getQuestion());
            List<String> answers = q.getAnswers();
            optionA.setText(answers.get(0));
            optionB.setText(answers.get(1));
            optionC.setText(answers.get(2));
            optionD.setText(answers.get(3));
            rightAnswer = q.getRightAnswer();
            totalQuestionsPlayed++;
            updateQuestionCounter();// Increment question counter
            startTimer(); // Start timer for the question
        } else {
            // If no questions are left, navigate to ScoreActivity
            Intent intent = new Intent(this, ScoreActivity.class);
            intent.putExtra("score", score);
            intent.putExtra("totalQuestions", totalQuestionsPlayed);
            startActivity(intent);
            finish();
        }
    }

    // Handle answer selection
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
            return; // No option selected
        }
        radioGroup.clearCheck(); // Clear selection
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Stop timer
            isTimerRunning = false; // Update timer state
        }
        checkAnswer(Answer); // Check the selected answer
    }

    // Check the answer and navigate accordingly
    private void checkAnswer(String answer) {
        if (answer.equals(rightAnswer)) {
            score++; // Increase score for correct answer
            startActivity(new Intent(this, RightActivity.class));
        } else {
            showAdAlert();
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.admob_REWARDED_UNIT_ID), adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                Log.d("AdMob", "Rewarded ad loaded.");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                Log.d("AdMob", "Rewarded ad failed to load: " + adError.getMessage());
                rewardedAd = null;
            }
        });
    }

    private void showAdAlert() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.alert_box_ad, null);

        final TextView timerTextView = alertLayout.findViewById(R.id.timerTextView);
        Button btnWatchAd = alertLayout.findViewById(R.id.btnWatchAd);
        Button btnSkip = alertLayout.findViewById(R.id.btnSkip);

        timerTextView.setText("Time remaining: 6 seconds");

        alertDialog = new AlertDialog.Builder(this)
                .setView(alertLayout)
                .setCancelable(false)
                .create();

        // Set the alert dialog background to transparent
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        alertDialog.show();

        btnWatchAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkConnected()) {
                    showAd(); // Show ad function
                    isAdShowing = true; // Ad is being shown
                    alertDialog.dismiss(); // Close dialog after showing ad
                    extendTimer(20000); // Extend timer to 20 seconds for the current question
                } else {
                    Toast.makeText(QuestionCollection.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAdShowing = false;
                startActivity(new Intent(QuestionCollection.this, WrongActivity.class));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadQuestion(); // Load new question
                    }
                }, 2000); // Delay of 2 seconds
                alertDialog.dismiss();
            }
        });

        adTimer = new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Time remaining: " + millisUntilFinished / 1000 + " seconds");
            }

            @Override
            public void onFinish() {
                alertDialog.dismiss(); // Close dialog
                if (!isAdShowing) { // If the ad wasn't shown
                    Intent intent = new Intent(QuestionCollection.this, WrongActivity.class);
                    startActivity(intent);
                }
            }
        }.start();

        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning = false;
        }

        alertDialog.setOnDismissListener(dialog -> {
            adTimer.cancel(); // Cancel the timer
            if (!isAdShowing) {
                startTimer(); // Restart the timer for the previous question
            }
        });
    }



    // প্রশ্নের টাইমার ২০ সেকেন্ডে বাড়ানোর ফাংশন
    private void extendTimer(long newTimeInMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(newTimeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // এখানে টাইমার আপডেট হবে
            }

            @Override
            public void onFinish() {
                // টাইমার শেষ হলে যা করার দরকার
            }
        }.start();
        isTimerRunning = true;
    }



    // Method to show ad
    private void showAd() {
        if (rewardedAd != null) {
            rewardedAd.show(this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(RewardItem rewardItem) {
                    int rewardAmount = rewardItem.getAmount();
                    String rewardType = rewardItem.getType();
                    Log.d("AdMob", "User earned reward: " + rewardAmount + " " + rewardType);
                    Toast.makeText(QuestionCollection.this, "You have earned another chance!", Toast.LENGTH_SHORT).show();
                    isAdShowing = false;
                    resetTimer(20000); // Reset the timer to 20 seconds
                    loadRewardedAd(); // Load a new rewarded ad
                }
            });
            isAdShowing = true;
        } else {
            Log.d("AdMob", "Rewarded ad is not ready yet.");
            Toast.makeText(QuestionCollection.this, "Ad is not ready. Please try again.", Toast.LENGTH_SHORT).show();
            loadRewardedAd(); // Load a new rewarded ad
        }
    }



    // Start timer method
    private void startTimer() {
        isTimerRunning = true;
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel previous timer if any
        }
        countDownTimer = new CountDownTimer(TIME_LIMIT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time remaining: " + millisUntilFinished / 1000 + " s");
            }

            @Override
            public void onFinish() {
                Answer = " ";  // Indicate a wrong answer
                showAdAlert(); // Show alert for timeout
            }
        }.start();
    }


    private void resetTimer(long newTimeLimit) {
        isTimerRunning = true;
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel previous timer if any
        }
        countDownTimer = new CountDownTimer(newTimeLimit, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time remaining: " + millisUntilFinished / 1000 + " s");
            }

            @Override
            public void onFinish() {
                Answer = " ";  // Indicate a wrong answer
                showAdAlert(); // Show alert for timeout
            }
        }.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Clean up timer
        }
    }


    public static ArrayList<QuestionModule> questions;
    public static void createQuestionBank() {
        QuestionCollection.subjectList = new ArrayList<>();
        QuestionCollection.questionBank = new ArrayList<>();

        //------------- Subject 1
        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("বাংলাদেশের জাতীয় পশু কোনটি?", "A", "বাঘ", "হাতি", "হরিণ", "কুমির"));
                add(new QuestionModule("বাংলাদেশের স্বাধীনতা দিবস কবে?", "B", "২৬ জানুয়ারি", "২৬ মার্চ", "২১ ফেব্রুয়ারি", "১৬ ডিসেম্বর"));
                add(new QuestionModule("বাংলাদেশের প্রথম রাষ্ট্রপতি কে?", "A", "শেখ মুজিবুর রহমান", "জিয়াউর রহমান", "তাজউদ্দীন আহমেদ", "আবদুল হামিদ"));
                add(new QuestionModule("বাংলাদেশের জাতীয় ফুল কোনটি?", "D", "গোলাপ", "জুঁই", "মোতা", "শাপলা"));
                add(new QuestionModule("বাংলাদেশের মুদ্রার নাম কী?", "A", "টাকা", "ডলার", "রুপি", "ইউরো"));
                add(new QuestionModule("বাংলাদেশের জাতীয় পাখি কোনটি?", "C", "ময়ূর", "কাক", "দোয়েল", "কবুতর"));
                add(new QuestionModule("জাতিসংঘ দিবস কবে পালিত হয়?", "B", "১২ জানুয়ারি", "২৪ অক্টোবর", "৫ জুন", "৩ মে"));
                add(new QuestionModule("বাংলাদেশের রাজধানী কোনটি?", "A", "ঢাকা", "চট্টগ্রাম", "রাজশাহী", "সিলেট"));
                add(new QuestionModule("অলিম্পিকের প্রতীক কটি রিং নিয়ে গঠিত?", "D", "২", "৩", "৪", "৫"));
                add(new QuestionModule("বাংলাদেশের মোট বিভাগ সংখ্যা কত?", "C", "৬", "৭", "৮", "৯"));
                add(new QuestionModule("বিশ্বের দীর্ঘতম সমুদ্র সৈকত কোথায়?", "B", "মালদ্বীপ", "কক্সবাজার", "হাওয়াই", "গোয়া"));
                add(new QuestionModule("বাংলাদেশের প্রথম রাজধানী কোথায়?", "D", "চট্টগ্রাম", "খুলনা", "রাজশাহী", "মুজিবনগর"));
                add(new QuestionModule("বাংলাদেশের জাতীয় সংগীত কে রচনা করেছেন?", "A", "রবীন্দ্রনাথ ঠাকুর", "কাজী নজরুল ইসলাম", "লালন শাহ", "জসিম উদ্দিন"));
                add(new QuestionModule("সুন্দরবনের মোট কতটি বাঘ আছে?", "B", "২০০", "১০৬", "১৫০", "৭৫"));
                add(new QuestionModule("বাংলাদেশের মুক্তিযুদ্ধ কত সালে হয়?", "C", "১৯৬৫", "১৯৭২", "১৯৭১", "১৯৮০"));
                add(new QuestionModule("বিশ্বের বৃহত্তম মহাসাগর কোনটি?", "D", "আটলান্টিক", "আর্কটিক", "ভারত মহাসাগর", "প্রশান্ত মহাসাগর"));
                add(new QuestionModule("পৃথিবীর বৃহত্তম নদী কোনটি?", "A", "নীল", "আমাজন", "গঙ্গা", "যমুনা"));
                add(new QuestionModule("বাংলাদেশের জাতীয় স্মৃতিসৌধ কোথায় অবস্থিত?", "B", "রাজশাহী", "সাভার", "কুমিল্লা", "ময়মনসিংহ"));
                add(new QuestionModule("বাংলাদেশের বর্তমান প্রধানমন্ত্রী কে?", "A", "শেখ হাসিনা", "খালেদা জিয়া", "এরশাদ", "জিয়াউর রহমান"));
                add(new QuestionModule("বাংলাদেশের জাতীয় খেলা কোনটি?", "C", "ক্রিকেট", "ফুটবল", "কাবাডি", "হ্যান্ডবল"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে বড় জেলা কোনটি?", "D", "রাজশাহী", "রংপুর", "চট্টগ্রাম", "রাঙ্গামাটি"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে বড় মরুভূমি কোনটি?", "A", "সাহারা", "গোবি", "কালাহারি", "অ্যারাবিয়ান"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বড় শহর কোনটি?", "B", "নিউইয়র্ক", "টোকিও", "বেইজিং", "লন্ডন"));
                add(new QuestionModule("ইংরেজি বর্ণমালায় মোট কতটি বর্ণ আছে?", "C", "২৪", "২৫", "২৬", "২৭"));
                add(new QuestionModule("সৌরজগতের বৃহত্তম গ্রহ কোনটি?", "D", "বুধ", "শুক্র", "পৃথিবী", "বৃহস্পতি"));
                add(new QuestionModule("মোনালিসা চিত্রকর্মের শিল্পী কে?", "A", "লিওনার্দো দা ভিঞ্চি", "পাবলো পিকাসো", "ভিনসেন্ট ভ্যান গগ", "রাফায়েল"));
                add(new QuestionModule("সাবেক সোভিয়েত ইউনিয়নের কতটি রাষ্ট্রে বিভক্ত?", "B", "১০", "১৫", "২০", "২৫"));
                add(new QuestionModule("মিশরের বিখ্যাত পিরামিডগুলোর মধ্যে কোনটি সবচেয়ে বড়?", "C", "জোসার পিরামিড", "বেন্ট পিরামিড", "গিজার মহা পিরামিড", "মিডুম পিরামিড"));
                add(new QuestionModule("পৃথিবীর সর্বাধিক বৃষ্টিপাত হয় কোন স্থানে?", "D", "চেরাপুঞ্জি", "সিয়াটেল", "মেদেলিন", "মওসিনরাম"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে উঁচু পর্বতশৃঙ্গ কোনটি?", "A", "এভারেস্ট", "কে২", "কাঞ্চনজঙ্ঘা", "ম্যাককিনলে"));
                add(new QuestionModule("বাংলাদেশে কতটি মৌলিক অধিকার আছে?", "B", "১৫টি", "১৮টি", "২০টি", "২২টি"));
                add(new QuestionModule("এশিয়া মহাদেশের সবচেয়ে বড় দেশ কোনটি?", "C", "ভারত", "জাপান", "চীন", "ইন্দোনেশিয়া"));
                add(new QuestionModule("বিশ্বের উচ্চতম জলপ্রপাত কোনটি?", "A", "এঞ্জেল জলপ্রপাত", "নায়াগ্রা জলপ্রপাত", "ভিক্টোরিয়া জলপ্রপাত", "ইগুয়াজু জলপ্রপাত"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে দীর্ঘ নদী কোনটি?", "C", "পদ্মা", "যমুনা", "মেঘনা", "তিস্তা"));
                add(new QuestionModule("কোন দেশটি সর্বপ্রথম চাঁদে মানুষ পাঠায়?", "B", "রাশিয়া", "যুক্তরাষ্ট্র", "চীন", "জাপান"));
                add(new QuestionModule("পৃথিবীর বৃহত্তম বন কোনটি?", "D", "সুন্দরবন", "কঙ্গো", "তায়গা", "আমাজন"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে গভীর সাগর কোনটি?", "C", "আটলান্টিক", "আর্কটিক", "মেরিয়ানা ট্রেঞ্চ", "ভারত মহাসাগর"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বড় দ্বীপ কোনটি?", "A", "গ্রীনল্যান্ড", "মাদাগাস্কার", "নিউজিল্যান্ড", "বালি"));
                add(new QuestionModule("নোবেল পুরস্কার কোন দেশের প্রতিষ্ঠা?", "B", "জার্মানি", "সুইডেন", "নরওয়ে", "ডেনমার্ক"));
                add(new QuestionModule("বঙ্গবন্ধু স্যাটেলাইট-১ কোন বছর উৎক্ষেপণ করা হয়?", "C", "২০১৭", "২০১৬", "২০১৮", "২০১৯"));
                add(new QuestionModule("আফ্রিকা মহাদেশের সবচেয়ে বড় দেশ কোনটি?", "A", "আলজেরিয়া", "মিশর", "নাইজেরিয়া", "কেনিয়া"));
                add(new QuestionModule("বাংলাদেশের প্রধান নদী কোনটি?", "B", "যমুনা", "পদ্মা", "মেঘনা", "তিস্তা"));
                add(new QuestionModule("প্রথম বিশ্বযুদ্ধ কত সালে শুরু হয়?", "C", "১৯১২", "১৯১৩", "১৯১৪", "১৯১৫"));
                add(new QuestionModule("পৃথিবীর বৃহত্তম হ্রদ কোনটি?", "D", "ভিক্টোরিয়া", "হুরন", "সুপিরিয়র", "কাস্পিয়ান সাগর"));
                add(new QuestionModule("বাংলাদেশের প্রথম নারী প্রধানমন্ত্রী কে?", "A", "খালেদা জিয়া", "শেখ হাসিনা", "রওশন এরশাদ", "আলিয়া আক্তার"));
                add(new QuestionModule("সৌরজগতের ক্ষুদ্রতম গ্রহ কোনটি?", "B", "শুক্র", "বুধ", "মঙ্গল", "নেপচুন"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে পুরাতন সভ্যতা কোনটি?", "C", "গ্রিক", "রোমান", "মেসোপটেমিয়া", "চীনা"));
                add(new QuestionModule("বাংলাদেশের জাতীয় সংগীত কতটি চরণ নিয়ে গঠিত?", "A", "১০টি", "৮টি", "১২টি", "১৪টি"));
                add(new QuestionModule("প্রথম অলিম্পিক গেমস কোথায় অনুষ্ঠিত হয়?", "B", "রোম", "এথেন্স", "লন্ডন", "বার্লিন"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে ঠান্ডা স্থান কোথায়?", "D", "নরওয়ে", "আইসল্যান্ড", "আলাস্কা", "অ্যান্টার্কটিকা"));
                add(new QuestionModule("কোন প্রাণী সবচেয়ে দ্রুত গতিতে দৌড়াতে পারে?", "A", "চিতা", "সিংহ", "বাঘ", "জিরাফ"));
                add(new QuestionModule("বাংলাদেশের কোন শহরকে সিলিকন ভ্যালি বলা হয়?", "C", "চট্টগ্রাম", "রাজশাহী", "ঢাকা", "খুলনা"));
                add(new QuestionModule("গণপ্রজাতন্ত্রী বাংলাদেশের সংবিধান কোন সালে প্রণীত হয়?", "B", "১৯৭১", "১৯৭২", "১৯৭৩", "১৯৭৪"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বেশি জনসংখ্যার দেশ কোনটি?", "A", "চীন", "ভারত", "যুক্তরাষ্ট্র", "ইন্দোনেশিয়া"));
                add(new QuestionModule("এভারেস্টের উচ্চতা কত?", "C", "৮,৭৫৪ মিটার", "৮,৬৯৮ মিটার", "৮,৮৪৮ মিটার", "৮,৯৫০ মিটার"));
                add(new QuestionModule("বিশ্বের প্রাচীনতম বিশ্ববিদ্যালয় কোনটি?", "D", "অক্সফোর্ড", "ক্যামব্রিজ", "হার্ভার্ড", "নালন্দা"));
                add(new QuestionModule("বাংলাদেশের জাতীয় পতাকার নকশা কে করেছেন?", "A", "কামরুল হাসান", "জয়নুল আবেদিন", "শিল্পাচার্য", "সুলতান আহমেদ"));
                add(new QuestionModule("বাংলাদেশের জনপ্রিয়তম খেলা কোনটি?", "B", "ফুটবল", "ক্রিকেট", "হকি", "গলফ"));
                add(new QuestionModule("পৃথিবীর বৃহত্তম চিড়িয়াখানা কোন দেশে অবস্থিত?", "C", "জাপান", "রাশিয়া", "যুক্তরাষ্ট্র", "চীন"));
                add(new QuestionModule("মহাকাশে প্রথম কোন প্রাণী পাঠানো হয়?", "A", "কুকুর", "বিড়াল", "বাঁদর", "খরগোশ"));
                add(new QuestionModule("কোন দেশে সবচেয়ে বেশি ভাষা প্রচলিত?", "C", "ভারত", "চীন", "পাপুয়া নিউ গিনি", "নাইজেরিয়া"));
                add(new QuestionModule("বিশ্বের সবচেয়ে ঠান্ডা সাগর কোনটি?", "D", "আর্কটিক", "আটলান্টিক", "প্রশান্ত", "দক্ষিণ মহাসাগর"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে বড় বন্দর কোনটি?", "A", "চট্টগ্রাম", "মোংলা", "পায়রা", "টেকনাফ"));
                add(new QuestionModule("বিশ্বের সবচেয়ে ছোট স্বাধীন দেশ কোনটি?", "B", "মালদ্বীপ", "ভ্যাটিকান সিটি", "মোনাকো", "নাউরু"));
                add(new QuestionModule("কোন সাগরটি ‘ডেড সি’ নামে পরিচিত?", "C", "বাল্টিক সাগর", "আড্রিয়াটিক সাগর", "ডেড সি", "আলবোরান সাগর"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে বড় বিমানবন্দর কোনটি?", "D", "হিথ্রো", "জেএফকে", "দুবাই", "কিং ফাহাদ আন্তর্জাতিক বিমানবন্দর"));
                add(new QuestionModule("মাধ্যমিক পর্যায়ের শিক্ষার্থীদের জন্য প্রধান বিজ্ঞান বই কোনটি?", "C", "মহাকাশ", "পৃথিবী", "সহজ বিজ্ঞান", "রসায়ন"));
                add(new QuestionModule("প্রথম টেলিভিশন সম্প্রচার কোন দেশে শুরু হয়?", "B", "ফ্রান্স", "যুক্তরাজ্য", "যুক্তরাষ্ট্র", "জাপান"));
                add(new QuestionModule("কোন গ্রহে এক বছর সবচেয়ে দীর্ঘ?", "C", "মঙ্গল", "শুক্র", "নেপচুন", "বুধ"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে পুরাতন বিশ্ববিদ্যালয় কোনটি?", "A", "ঢাকা বিশ্ববিদ্যালয়", "রাজশাহী বিশ্ববিদ্যালয়", "চট্টগ্রাম বিশ্ববিদ্যালয়", "জাহাঙ্গীরনগর বিশ্ববিদ্যালয়"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বেশি আয়তনের দেশ কোনটি?", "B", "চীন", "রাশিয়া", "কানাডা", "যুক্তরাষ্ট্র"));
                add(new QuestionModule("বিশ্বের প্রথম মহাকাশচারী কে?", "A", "ইউরি গাগারিন", "নেইল আর্মস্ট্রং", "জন গ্লেন", "বাজ অলড্রিন"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে বেশি জনসংখ্যার শহর কোনটি?", "B", "সাংহাই", "টোকিও", "মুম্বাই", "কায়রো"));
                add(new QuestionModule("কোনটি বাংলাদেশের বৃহত্তম স্থলবন্দর?", "A", "বেনাপোল", "হিলি", "তামাবিল", "বুড়িমারী"));
                add(new QuestionModule("প্রথমবারের মতো অলিম্পিক গেমস কত সালে অনুষ্ঠিত হয়?", "C", "১৮৯২", "১৮৯৪", "১৮৯৬", "১৮৯৮"));
                add(new QuestionModule("কোন মহাসাগরে 'মালদ্বীপ' অবস্থিত?", "B", "আটলান্টিক", "ভারত মহাসাগর", "প্রশান্ত মহাসাগর", "আর্কটিক মহাসাগর"));
                add(new QuestionModule("কোন মহাদেশকে 'সাদা মহাদেশ' বলা হয়?", "D", "এশিয়া", "ইউরোপ", "উত্তর আমেরিকা", "অ্যান্টার্কটিকা"));
                add(new QuestionModule("কোন গ্রহের চাঁদের সংখ্যা সবচেয়ে বেশি?", "A", "শনি", "বৃহস্পতি", "মঙ্গল", "নেপচুন"));
                add(new QuestionModule("বাংলাদেশের জাতীয় কবি কে?", "C", "রবীন্দ্রনাথ ঠাকুর", "লালন শাহ", "কাজী নজরুল ইসলাম", "জসিম উদ্দিন"));
                add(new QuestionModule("পৃথিবীর উষ্ণতম স্থান কোনটি?", "A", "ডেথ ভ্যালি", "সাহারা মরুভূমি", "গোবি মরুভূমি", "আরব মরুভূমি"));
                add(new QuestionModule("মহাশূন্যে পাঠানো প্রথম কৃত্রিম উপগ্রহের নাম কি?", "B", "ভয়েজার ১", "স্পুটনিক ১", "হাবল", "আপোলো ১১"));
                add(new QuestionModule("বিশ্বের সবচেয়ে ছোট মহাদেশ কোনটি?", "C", "ইউরোপ", "দক্ষিণ আমেরিকা", "অস্ট্রেলিয়া", "আফ্রিকা"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে বড় গ্যাসক্ষেত্র কোনটি?", "A", "বিবিয়ানা", "তিতাস", "জালালাবাদ", "বখরাবাদ"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে বড় দ্বীপ কোনটি?", "A", "গ্রীনল্যান্ড", "মাদাগাস্কার", "শ্রীলঙ্কা", "নিউজিল্যান্ড"));
                add(new QuestionModule("কোন বর্ণমালায় সবচেয়ে বেশি অক্ষর রয়েছে?", "B", "ল্যাটিন", "খমের", "চীনা", "দেবনাগরী"));
                add(new QuestionModule("প্রথম বিশ্বযুদ্ধ কোন সালে শেষ হয়?", "C", "১৯১৬", "১৯১৭", "১৯১৮", "১৯১৯"));
                add(new QuestionModule("কোন দেশে সবচেয়ে বেশি সময় ধরে রাজতন্ত্র চালু রয়েছে?", "D", "জাপান", "স্পেন", "নরওয়ে", "যুক্তরাজ্য"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে বেশি চা উৎপন্ন হয় কোন জেলায়?", "A", "সিলেট", "মৌলভীবাজার", "হবিগঞ্জ", "চট্টগ্রাম"));
                add(new QuestionModule("কোনটি পৃথিবীর সবচেয়ে উঁচু পর্বতশৃঙ্গ?", "A", "এভারেস্ট", "কে২", "ম্যাককিনলে", "কাঞ্চনজঙ্ঘা"));
                add(new QuestionModule("বাংলাদেশে প্রথম আইটি পার্ক কোথায় স্থাপন করা হয়?", "B", "ঢাকা", "যশোর", "চট্টগ্রাম", "সিলেট"));
                add(new QuestionModule("কোন দেশ প্রথমে ক্রিকেট খেলে?", "C", "অস্ট্রেলিয়া", "ভারত", "ইংল্যান্ড", "দক্ষিণ আফ্রিকা"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বড় বনভূমি কোনটি?", "D", "তায়গা", "কঙ্গো", "তেঁতুলিয়া", "আমাজন বনভূমি"));
                add(new QuestionModule("বাংলাদেশের জাতীয় পতাকার সবুজ রঙ কী বোঝায়?", "A", "দেশের প্রাকৃতিক সৌন্দর্য", "মুক্তিযুদ্ধ", "বীরত্ব", "শান্তি"));
                add(new QuestionModule("পৃথিবীর সবচেয়ে ছোট দেশ কোনটি?", "B", "মোনাকো", "ভ্যাটিকান সিটি", "সান মারিনো", "লুক্সেমবার্গ"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে বড় হাওর কোনটি?", "C", "বাওর", "টাঙ্গুয়ার হাওর", "হাকালুকি হাওর", "কালীগঞ্জ হাওর"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বেশি বরফের পরিমাণ কোন মহাদেশে?", "C", "উত্তর আমেরিকা", "এশিয়া", "অ্যান্টার্কটিকা", "ইউরোপ"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে বড় হ্রদ কোনটি?", "D", "তিনবিঘা", "নাফ", "পদ্মা", "কাপ্তাই হ্রদ"));
                add(new QuestionModule("মহাশূন্যে অবতরণকারী প্রথম মানুষ কে?", "B", "জন গ্লেন", "নেইল আর্মস্ট্রং", "বাজ অলড্রিন", "ইউরি গাগারিন"));
                add(new QuestionModule("সৌরজগতের কোন গ্রহে সবচেয়ে বেশি সক্রিয় আগ্নেয়গিরি আছে?", "C", "বুধ", "শুক্র", "মঙ্গল", "শনি"));
                add(new QuestionModule("বাংলাদেশের কোন এলাকায় সবচেয়ে বেশি নদী রয়েছে?", "B", "ঢাকা", "বরিশাল", "সিলেট", "খুলনা"));
                add(new QuestionModule("বিশ্বের প্রথম আন্তর্জাতিক ক্রিকেট ম্যাচ কোন দুই দেশের মধ্যে হয়?", "A", "অস্ট্রেলিয়া ও ইংল্যান্ড", "ভারত ও পাকিস্তান", "ইংল্যান্ড ও দক্ষিণ আফ্রিকা", "অস্ট্রেলিয়া ও দক্ষিণ আফ্রিকা"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বড় ফুল কোনটি?", "B", "লিলি", "রাফলেসিয়া", "জুঁই", "টিউলিপ"));
                add(new QuestionModule("কোন দেশের পতাকায় সবচেয়ে বেশি রঙ আছে?", "D", "ক্যামেরুন", "জাপান", "বাংলাদেশ", "দক্ষিণ আফ্রিকা"));
                add(new QuestionModule("বাংলাদেশের প্রথম স্যাটেলাইটের নাম কী?", "A", "বঙ্গবন্ধু স্যাটেলাইট-১", "আকাশ স্যাটেলাইট", "নিউজিল্যান্ড স্যাটেলাইট", "বাংলা স্যাটেলাইট"));
                add(new QuestionModule("বাংলাদেশের প্রথম কাগজের মুদ্রা কোন সালে চালু হয়?", "C", "১৯৭১", "১৯৭২", "১৯৭৪", "১৯৭৫"));
                add(new QuestionModule("বিশ্বের সবচেয়ে বেশি এলাকা জুড়ে থাকা নদী কোনটি?", "D", "গঙ্গা", "ইয়াংজি", "ড্যানিউব", "নাইল"));
                add(new QuestionModule("বাংলাদেশের সবচেয়ে বড় সেতু কোনটি?", "A", "পদ্মা সেতু", "যমুনা সেতু", "হাতিয়া সেতু", "মেঘনা সেতু"));

            }
        };
        QuestionModule.createQuestionsForSubject("Simple Science", R.drawable.category_icon1, questions);



                questions = new ArrayList<QuestionModule>() {
                    {
                        add(new QuestionModule("পানির রাসায়নিক সংকেত কী?", "A", "H2O", "CO2", "O2", "NaCl"));
                        add(new QuestionModule("কোনটি ক্ষারক (Alkali) পদার্থ?", "B", "হাইড্রোক্লোরিক এসিড", "সোডিয়াম হাইড্রোক্সাইড", "কার্বন ডাইঅক্সাইড", "অক্সিজেন"));
                        add(new QuestionModule("কোন মৌলটি সবচেয়ে হালকা?", "C", "হিলিয়াম", "কার্বন", "হাইড্রোজেন", "নাইট্রোজেন"));
                        add(new QuestionModule("মানবদেহে অক্সিজেনের প্রধান বাহক কোনটি?", "D", "প্লাজমা", "লাল রক্তকণিকা", "লিম্ফ", "হিমোগ্লোবিন"));
                        add(new QuestionModule("কোনটি সাধারণ লবণের রাসায়নিক নাম?", "A", "সোডিয়াম ক্লোরাইড", "সোডিয়াম হাইড্রোক্সাইড", "ক্যালসিয়াম কার্বোনেট", "অ্যামোনিয়া"));
                        add(new QuestionModule("কোনটি জারণ (Oxidation) বিক্রিয়া?", "C", "পানি উৎপাদন", "তরলীকরণ", "ধাতুর মরিচা পড়া", "শর্করা গঠন"));
                        add(new QuestionModule("কোন এসিডটি ভিনেগারে পাওয়া যায়?", "B", "হাইড্রোক্লোরিক এসিড", "অ্যাসিটিক এসিড", "নাইট্রিক এসিড", "সালফিউরিক এসিড"));
                        add(new QuestionModule("কোন গ্যাসটি আগুনকে জ্বালাতে সহায়তা করে?", "A", "অক্সিজেন", "কার্বন ডাইঅক্সাইড", "নাইট্রোজেন", "হাইড্রোজেন"));
                        add(new QuestionModule("পরমাণুর কেন্দ্রে কোন কণাগুলি থাকে?", "B", "ইলেকট্রন", "প্রোটন ও নিউট্রন", "প্রোটন ও ইলেকট্রন", "নিউট্রন ও ইলেকট্রন"));
                        add(new QuestionModule("পিএইচ স্কেলে কোনটি একটি নিরপেক্ষ পদার্থ?", "C", "অ্যাসিড", "ক্ষার", "পানি", "অ্যামোনিয়া"));
                        add(new QuestionModule("কোন গ্যাসটি বায়ুমণ্ডলের সবচেয়ে বেশি অংশ দখল করে?", "A", "নাইট্রোজেন", "অক্সিজেন", "কার্বন ডাইঅক্সাইড", "আর্গন"));
                        add(new QuestionModule("কোনটি সবচেয়ে শক্তিশালী এসিড?", "B", "হাইড্রোক্লোরিক এসিড", "সালফিউরিক এসিড", "অ্যাসিটিক এসিড", "কার্বনিক এসিড"));
                        add(new QuestionModule("কোন ধাতুটি তরল অবস্থায় থাকে?", "C", "আয়রন", "অ্যালুমিনিয়াম", "পারদ", "তামা"));
                        add(new QuestionModule("অ্যানোডে কোনটি ঘটে?", "C", "ইলেকট্রন অর্জন", "প্রোটন ক্ষতি", "ইলেকট্রন ক্ষতি", "প্রোটন অর্জন"));
                        add(new QuestionModule("কোনটি একটি ইলেকট্রোলাইট?", "A", "সোডিয়াম ক্লোরাইড", "চিনি", "অ্যালকোহল", "অ্যামোনিয়া"));
                        add(new QuestionModule("কোন পদার্থটি রাসায়নিকভাবে অপরিবর্তনীয়?", "B", "জল", "ধাতু", "অ্যামোনিয়া", "ক্লোরিন"));
                        add(new QuestionModule("রাসায়নিক বিক্রিয়ায় অনুঘটক কী ভূমিকা পালন করে?", "C", "বিক্রিয়া ধীর করে", "বিক্রিয়া বন্ধ করে", "বিক্রিয়া দ্রুত করে", "বিক্রিয়া বিপরীত করে"));
                        add(new QuestionModule("পরমাণুর ভর সংখ্যা নির্ধারিত হয় কিসের দ্বারা?", "D", "ইলেকট্রন সংখ্যা", "প্রোটন সংখ্যা", "নিউট্রন সংখ্যা", "প্রোটন এবং নিউট্রন সংখ্যা"));
                        add(new QuestionModule("কোন মৌলটি পৃথিবীর ভূ-পৃষ্ঠে সবচেয়ে বেশি পাওয়া যায়?", "A", "অক্সিজেন", "সিলিকন", "অ্যালুমিনিয়াম", "আয়রন"));
                        add(new QuestionModule("কোনটি রাসায়নিক বন্ধন গঠন করে?", "B", "প্রোটন", "ইলেকট্রন", "নিউট্রন", "ফোটন"));
                        add(new QuestionModule("সোডিয়াম ক্লোরাইডের গলনাংক কত?", "C", "0°C", "500°C", "801°C", "1200°C"));
                        add(new QuestionModule("কোন গ্যাসটি গ্রীনহাউস প্রভাবের জন্য দায়ী?", "D", "নাইট্রোজেন", "অক্সিজেন", "হাইড্রোজেন", "কার্বন ডাইঅক্সাইড"));
                        add(new QuestionModule("অ্যালুমিনিয়ামের রাসায়নিক সংকেত কী?", "A", "Al", "Fe", "Au", "Ag"));
                        add(new QuestionModule("পানির ফুটনাঙ্ক কত?", "B", "90°C", "100°C", "120°C", "150°C"));
                        add(new QuestionModule("কোনটি ক্ষার ধাতু?", "C", "কপার", "আয়রন", "পটাশিয়াম", "সিলিকন"));
                        add(new QuestionModule("অক্সিজেনের কোন আইসোটোপটি সবচেয়ে বেশি সাধারণ?", "D", "O-15", "O-16", "O-17", "O-18"));
                        add(new QuestionModule("হাইড্রোক্লোরিক এসিডের রাসায়নিক সংকেত কী?", "A", "HCl", "NaCl", "H2O", "CO2"));
                        add(new QuestionModule("কোন ধাতু বায়ুর সংস্পর্শে এলে মরিচা ধরে?", "B", "অ্যালুমিনিয়াম", "আয়রন", "সোনা", "তামা"));
                        add(new QuestionModule("অ্যাসিটিক এসিডের রাসায়নিক সংকেত কী?", "C", "CH4", "CO2", "CH3COOH", "H2SO4"));
                        add(new QuestionModule("কোন উপাদানটি শক্ত, তরল এবং গ্যাস অবস্থায় পাওয়া যায়?", "D", "হাইড্রোজেন", "নাইট্রোজেন", "অক্সিজেন", "জল"));
                        add(new QuestionModule("কোনটি একটি ক্ষারীয় ধাতু?", "A", "লিথিয়াম", "সিলিকন", "তামা", "অ্যালুমিনিয়াম"));
                        add(new QuestionModule("কোনটি সাধারণ লবণের প্রধান উপাদান?", "B", "ক্যালসিয়াম", "সোডিয়াম", "ম্যাগনেসিয়াম", "ফসফরাস"));
                        add(new QuestionModule("কোনটি একটি ধাতু?", "B", "সালফার", "তামা", "কার্বন", "নাইট্রোজেন"));
                        add(new QuestionModule("কোন গ্যাসটি মানুষের দেহে শ্বাস-প্রশ্বাসে সরাসরি অংশগ্রহণ করে?", "A", "অক্সিজেন", "কার্বন মনোক্সাইড", "নাইট্রোজেন", "অর্জন"));
                        add(new QuestionModule("পৃথিবীর অভ্যন্তরে কোন উপাদানটি সবচেয়ে বেশি রয়েছে?", "D", "সিলিকন", "অ্যালুমিনিয়াম", "সালফার", "লোহা"));
                        add(new QuestionModule("কোনটি একটি আয়নিক যৌগ?", "C", "চিনি", "অ্যামোনিয়া", "সোডিয়াম ক্লোরাইড", "অ্যালকোহল"));
                        add(new QuestionModule("কোনটি একটি প্রোটিন?", "B", "গ্লুকোজ", "ইনসুলিন", "সেলুলোজ", "ডিএনএ"));
                        add(new QuestionModule("কোন গ্যাসটি জীবন ধারণের জন্য অপরিহার্য নয়?", "C", "অক্সিজেন", "কার্বন ডাইঅক্সাইড", "হিলিয়াম", "নাইট্রোজেন"));
                        add(new QuestionModule("কোনটি ফ্লোরিনের রাসায়নিক সংকেত?", "A", "F", "Fl", "Fe", "Fr"));
                        add(new QuestionModule("কোনটি তড়িৎ পরিবাহক নয়?", "C", "তামা", "লোহা", "প্লাস্টিক", "অ্যালুমিনিয়াম"));
                        add(new QuestionModule("কোনটি একটি জারণ-অপসারণ বিক্রিয়া?", "B", "ফুটন বিক্রিয়া", "রাসায়নিক দহন", "স্ফটিকীভবন", "কাঠ পুড়ে যাওয়া"));
                        add(new QuestionModule("পানির কঠিন অবস্থার নাম কী?", "D", "তরল", "বাষ্প", "অভ্র", "বরফ"));
                        add(new QuestionModule("কোনটি একটি অ্যালকোহল?", "A", "ইথানল", "অ্যাসিটিক এসিড", "সোডিয়াম হাইড্রক্সাইড", "সুক্রোজ"));
                        add(new QuestionModule("শর্করা কোনটি?", "B", "অ্যালবুমিন", "গ্লুকোজ", "হিমোগ্লোবিন", "সোডিয়াম"));
                        add(new QuestionModule("কোনটি জৈব যৌগ?", "C", "পানি", "হাইড্রোজেন", "মিথেন", "সোডিয়াম ক্লোরাইড"));
                        add(new QuestionModule("পৃথিবীর বায়ুমণ্ডলে মোট অক্সিজেনের শতকরা কত অংশ রয়েছে?", "B", "50%", "21%", "78%", "10%"));
                        add(new QuestionModule("কোন পদার্থটি প্লাস্টিক তৈরির জন্য ব্যবহৃত হয়?", "D", "অ্যালুমিনিয়াম", "কার্বন ডাইঅক্সাইড", "সিলিকা", "পলিথিন"));
                        add(new QuestionModule("কোনটি তাপ এবং তড়িৎ উভয়েরই ভালো পরিবাহক?", "A", "তামা", "কাঠ", "প্লাস্টিক", "কাঁচ"));
                        add(new QuestionModule("হাইড্রোজেনের কোন আইসোটোপটি সবচেয়ে বেশি সাধারণ?", "A", "প্রোটিয়াম", "ডিউটেরিয়াম", "ট্রাইটিয়াম", "হেলিয়াম"));
                        add(new QuestionModule("কোনটি তেজস্ক্রিয় মৌল?", "C", "হাইড্রোজেন", "সোডিয়াম", "ইউরেনিয়াম", "কার্বন"));
                        add(new QuestionModule("কোন যৌগটি নাইট্রোজেন এবং হাইড্রোজেন নিয়ে গঠিত?", "B", "কার্বন ডাইঅক্সাইড", "অ্যামোনিয়া", "মিথেন", "নাইট্রাস অক্সাইড"));
                        add(new QuestionModule("কোনটি একটি শক্তিশালী হাইড্রোকার্বন?", "A", "মিথেন", "অক্সিজেন", "কার্বন ডাইঅক্সাইড", "সোডিয়াম ক্লোরাইড"));
                        add(new QuestionModule("কোনটি পরমাণুর নিউক্লিয়াসে থাকে না?", "C", "প্রোটন", "নিউট্রন", "ইলেকট্রন", "নিউট্রিনো"));
                        add(new QuestionModule("মানবদেহে প্রোটিনের প্রধান উপাদান কোনটি?", "B", "সুক্রোজ", "আমিনো এসিড", "গ্লুকোজ", "ক্যালসিয়াম"));
                        add(new QuestionModule("কোনটি রাসায়নিক সংকেতের পরিবর্তন ঘটায়?", "D", "জারণ", "দ্রবণ", "বাষ্পীভবন", "রাসায়নিক বিক্রিয়া"));
                        add(new QuestionModule("কোন গ্যাসটি আর্সেনিক যৌগ থেকে মুক্তি পাওয়ার জন্য ব্যবহৃত হয়?", "B", "নাইট্রোজেন", "অ্যামোনিয়া", "অক্সিজেন", "কার্বন ডাইঅক্সাইড"));
                        add(new QuestionModule("কোনটি পারমাণবিক সংখ্যা পরিবর্তনের ফলে তৈরি হয়?", "D", "ভর সংখ্যা", "ইলেকট্রন বিন্যাস", "প্রোটন সংখ্যা", "আইসোটোপ"));
                        add(new QuestionModule("কোনটি মৌলিক পদার্থ নয়?", "B", "অক্সিজেন", "জল", "সোডিয়াম", "আয়রন"));
                        add(new QuestionModule("কোনটি একটি হ্যালোজেন গ্যাস?", "A", "ক্লোরিন", "অক্সিজেন", "হিলিয়াম", "নাইট্রোজেন"));
                        add(new QuestionModule("কোন যৌগটি সবচেয়ে বেশি ব্যবহার করা হয়?", "C", "হাইড্রোজেন পারক্সাইড", "অ্যামোনিয়া", "জল", "অ্যালকোহল"));
                        add(new QuestionModule("বায়ুমণ্ডলে সবচেয়ে বেশি যে গ্যাসটি রয়েছে, তার রাসায়নিক সংকেত কী?", "D", "O2", "CO2", "He", "N2"));
                        add(new QuestionModule("কোনটি কঠিন অবস্থায় থাকে না?", "B", "লোহা", "নাইট্রোজেন", "অ্যালুমিনিয়াম", "সোডিয়াম"));
                        add(new QuestionModule("বৈদ্যুতিক পরিবাহিতার ক্ষেত্রে কোনটি ব্যবহার করা হয়?", "C", "প্লাস্টিক", "কাঠ", "তামা", "কাঁচ"));
                        add(new QuestionModule("হাইড্রোজেনের রাসায়নিক সংকেত কী?", "A", "H2", "HCl", "HO", "H2O2"));
                        add(new QuestionModule("কোনটি অক্সিজেনের জারণ অবস্থা নির্দেশ করে?", "C", "0", "+1", "-2", "+2"));
                        add(new QuestionModule("কোনটি একটি রাসায়নিক বিক্রিয়া?", "C", "বাষ্পীভবন", "দ্রবণ", "জারণ", "পলিমারাইজেশন"));
                        add(new QuestionModule("অ্যামোনিয়ার রাসায়নিক সংকেত কী?", "D", "CO2", "CH4", "H2O", "NH3"));
                        add(new QuestionModule("কোনটি পারমাণবিক সংখ্যা ৮?", "B", "নাইট্রোজেন", "অক্সিজেন", "হাইড্রোজেন", "হিলিয়াম"));
                        add(new QuestionModule("কোনটি একটি শক্ত পদার্থ?", "A", "আয়রন", "জল", "অক্সিজেন", "হিলিয়াম"));
                        add(new QuestionModule("কোন মৌলটি সবচেয়ে বেশি রাসায়নিক বিক্রিয়ায় অংশ নেয়?", "C", "সিলিকন", "সোডিয়াম", "কার্বন", "অ্যালুমিনিয়াম"));
                        add(new QuestionModule("কোনটি একটি কণার আয়ন বলে বিবেচিত হয়?", "D", "ইলেকট্রন", "নিউট্রন", "ফোটন", "প্রোটন"));
                        add(new QuestionModule("কোনটি জৈব যৌগ নয়?", "B", "অ্যামিনো এসিড", "কার্বন ডাইঅক্সাইড", "মিথেন", "গ্লুকোজ"));
                        add(new QuestionModule("কোনটি অযৌগিক পদার্থ?", "A", "হিলিয়াম", "জল", "মিথেন", "অ্যামোনিয়া"));
                        add(new QuestionModule("কোনটি জীববিজ্ঞানের প্রয়োজনীয় মৌল নয়?", "C", "কার্বন", "হাইড্রোজেন", "আর্গন", "নাইট্রোজেন"));
                        add(new QuestionModule("সিসার রাসায়নিক সংকেত কী?", "D", "Si", "Sn", "Sr", "Pb"));
                        add(new QuestionModule("কোনটি সবচেয়ে হালকা গ্যাস?", "A", "হাইড্রোজেন", "অক্সিজেন", "নাইট্রোজেন", "কার্বন ডাইঅক্সাইড"));
                        add(new QuestionModule("কোনটি সবচেয়ে বেশি ইলেকট্রন বিন্যাস সহ মৌল?", "C", "হিলিয়াম", "সোডিয়াম", "আর্গন", "ক্লোরিন"));
                        add(new QuestionModule("কোন পদার্থটি বেসিক প্রকৃতির?", "B", "হাইড্রোজেন", "সোডিয়াম হাইড্রক্সাইড", "অ্যামোনিয়া", "জল"));
                        add(new QuestionModule("কোনটি একটি বেস?", "D", "অ্যাসিড", "সোডিয়াম ক্লোরাইড", "অ্যামোনিয়া", "ম্যাগনেসিয়াম হাইড্রক্সাইড"));
                        add(new QuestionModule("কোন মৌলটি শক্ত এবং পরিবাহী নয়?", "A", "কার্বন", "তামা", "অ্যালুমিনিয়াম", "সোনা"));
                        add(new QuestionModule("কোন পদার্থটি পানির চেয়ে ঘনত্ব কম?", "C", "লবণ", "অ্যালুমিনিয়াম", "তেল", "কাঁচ"));
                        add(new QuestionModule("কোনটি বাষ্পীভবনের উদাহরণ?", "B", "তরলীকরণ", "জল থেকে বাষ্প উৎপাদন", "গলন", "জারণ"));
                        add(new QuestionModule("কোনটি একক অণুতে পাওয়া যায়?", "D", "লিথিয়াম", "সোডিয়াম", "অ্যালুমিনিয়াম", "কার্বন ডাইঅক্সাইড"));
                        add(new QuestionModule("কোনটি ইলেকট্রন বিহীন পদার্থ?", "A", "প্রোটন", "নিউট্রন", "অণু", "কোয়ার্ক"));
                        add(new QuestionModule("কোনটি একটি মৌলিক ধাতু নয়?", "B", "সোনা", "প্লাটিনাম", "লোহা", "অ্যালুমিনিয়াম"));
                        add(new QuestionModule("কোন গ্যাসটি বাতাসের প্রধান অংশ?", "D", "অক্সিজেন", "আর্গন", "কার্বন ডাইঅক্সাইড", "নাইট্রোজেন"));
                        add(new QuestionModule("কোনটি একটি তেজস্ক্রিয় গ্যাস?", "C", "হিলিয়াম", "অক্সিজেন", "রাডন", "আর্গন"));
                        add(new QuestionModule("কোন পদার্থটি কার্বন অণুর সঙ্গে মিলে জৈব পদার্থ তৈরি করে?", "B", "অক্সিজেন", "হাইড্রোজেন", "নাইট্রোজেন", "হিলিয়াম"));
                        add(new QuestionModule("কোন পদার্থটি কাচ তৈরি করতে ব্যবহৃত হয়?", "D", "সোডিয়াম", "ক্লোরিন", "অ্যালুমিনিয়াম", "সিলিকা"));
                        add(new QuestionModule("কোনটি ইলেকট্রন শেয়ারিং করে রাসায়নিক বন্ধন তৈরি করে?", "C", "প্রোটন", "নিউট্রন", "ইলেকট্রন", "ফোটন"));
                        add(new QuestionModule("কোনটি একটি নাইট্রোজেন সমৃদ্ধ যৌগ?", "A", "অ্যামোনিয়া", "কার্বন ডাইঅক্সাইড", "অ্যামোনিয়াম", "মিথেন"));
                        add(new QuestionModule("কোনটি ধাতব নয়?", "B", "তামা", "সালফার", "অ্যালুমিনিয়াম", "জিঙ্ক"));
                        add(new QuestionModule("কোনটি তরল পদার্থের উদাহরণ?", "D", "সোনা", "তামা", "আর্গন", "পারদ"));
                        add(new QuestionModule("কোনটি একটি অ্যান্টিঅক্সিডেন্ট?", "C", "কার্বন ডাইঅক্সাইড", "হাইড্রোজেন", "ভিটামিন সি", "অ্যামোনিয়া"));
                        add(new QuestionModule("কোন মৌলটি পরমাণুতে নিউট্রন থাকে না?", "A", "হাইড্রোজেন", "হিলিয়াম", "কার্বন", "অক্সিজেন"));
                        add(new QuestionModule("কোনটি একটি আলকাইন হাইড্রোকার্বন?", "B", "ইথেন", "অ্যাসিটিলিন", "মিথেন", "বিউটেন"));
                        add(new QuestionModule("কোনটি একটি হাইড্রোজেন বন্ডিং উদাহরণ?", "D", "অক্সিজেন এবং নাইট্রোজেন", "সোডিয়াম এবং ক্লোরিন", "কার্বন এবং হাইড্রোজেন", "পানি এবং অ্যামোনিয়া"));
                        add(new QuestionModule("কোনটি একটি মৌলিক পদার্থের উদাহরণ?", "A", "সিলিকন", "জল", "ক্লোরোফর্ম", "অ্যালুমিনিয়াম অক্সাইড"));
                        add(new QuestionModule("কোনটি একটি পর্যায় সারণিতে ধাতু নয়?", "C", "লোহা", "তামা", "সালফার", "সোনা"));
                        add(new QuestionModule("বায়ুতে কোনটি একটি প্রধান উপাদান নয়?", "B", "নাইট্রোজেন", "জলীয় বাষ্প", "অক্সিজেন", "আর্গন"));
                        add(new QuestionModule("কোন গ্যাসটি ব্যাকটেরিয়ার মাধ্যমে নাইট্রোজেন সার্কেলে অন্তর্ভুক্ত হয়?", "D", "হিলিয়াম", "অক্সিজেন", "কার্বন ডাইঅক্সাইড", "নাইট্রাস অক্সাইড"));
                        add(new QuestionModule("কোনটি একটি ধাতব আইনের উদাহরণ?", "B", "পারদ", "পিতল", "সিসা", "জিঙ্ক"));
                        add(new QuestionModule("কোনটি জৈব পদার্থের গঠনমূলক মৌল?", "A", "কার্বন", "সোডিয়াম", "সালফার", "আর্গন"));
                        add(new QuestionModule("কোনটি একটি মৌল এবং যৌগ উভয়ের সংমিশ্রণ?", "C", "অ্যালুমিনিয়াম", "সোনা", "হিমোগ্লোবিন", "অক্সিজেন"));
                        add(new QuestionModule("কোন মৌলটি সবচেয়ে প্রতিক্রিয়াশীল?", "B", "অ্যালুমিনিয়াম", "ফ্লোরিন", "হাইড্রোজেন", "হিলিয়াম"));
                        add(new QuestionModule("কোনটি একটি উচ্চ শক্তি সম্পন্ন রাসায়নিক বন্ধন?", "A", "কোভ্যালেন্ট বন্ড", "ইলেকট্রোস্ট্যাটিক বন্ড", "ভ্যান ডার ওয়ালস বন্ড", "ইলেকট্রন বন্ড"));
                        add(new QuestionModule("পৃথিবীর সবচেয়ে কঠিন পদার্থ কোনটি?", "D", "লোহা", "অ্যালুমিনিয়াম", "কার্বন", "হীরা"));
                        add(new QuestionModule("কোন মৌলটি রংহীন গ্যাস?", "C", "ক্লোরিন", "জলীয় বাষ্প", "হিলিয়াম", "জল"));
                        add(new QuestionModule("কোনটি একটি প্রোটিনের মূল উপাদান?", "B", "শর্করা", "আমিনো অ্যাসিড", "লিপিড", "ভিটামিন"));
                        add(new QuestionModule("কোনটি ভরের নীতি দ্বারা পরিচালিত হয়?", "A", "মল সূত্র", "মোলারিটি", "প্রমাণ পদ্ধতি", "তরলীকরণ সূত্র"));
                        add(new QuestionModule("কোনটি তাপ শোষণকারী বিক্রিয়া?", "C", "দহন", "জারণ", "বাষ্পীভবন", "জমাট বাধা"));
                        add(new QuestionModule("কোনটি একটি পরিবাহী পদার্থ নয়?", "D", "তামা", "লোহা", "অ্যালুমিনিয়াম", "কাঠ"));
                        add(new QuestionModule("কোনটি একটি রাসায়নিক যৌগ?", "A", "জল", "অক্সিজেন", "নাইট্রোজেন", "হিলিয়াম"));
                        add(new QuestionModule("কোন গ্যাসটি মিথেনের প্রধান উপাদান?", "B", "অক্সিজেন", "কার্বন", "নাইট্রোজেন", "হাইড্রোজেন"));
                        add(new QuestionModule("কোনটি হিমোগ্লোবিনের একটি উপাদান?", "D", "ক্যালসিয়াম", "ফসফরাস", "সোডিয়াম", "লোহা"));
                        add(new QuestionModule("কোনটি একটি মৌলিক গ্যাস নয়?", "C", "হাইড্রোজেন", "নাইট্রোজেন", "জলীয় বাষ্প", "অক্সিজেন"));
                        add(new QuestionModule("কোনটি একটি শক্তিশালী বেসিক পদার্থ?", "B", "জল", "পটাশিয়াম হাইড্রোক্সাইড", "অ্যামোনিয়া", "সোডিয়াম ক্লোরাইড"));
                        add(new QuestionModule("কোনটি পরমাণুর শক্তি বর্ণনায় ব্যবহৃত হয়?", "A", "ইলেকট্রন বিন্যাস", "নিউট্রন সংখ্যা", "মৌল সংখ্যা", "ভর সংখ্যা"));
                        add(new QuestionModule("কোনটি একটি শক্ত পদার্থের ঘনত্ব নির্দেশ করে?", "D", "ভর", "ভলিউম", "তাপ", "দ্রবণীয়তা"));
                        add(new QuestionModule("কোনটি একটি ক্ষারীয় বেস?", "C", "অ্যামোনিয়া", "অ্যালুমিনিয়াম অক্সাইড", "সোডিয়াম হাইড্রোক্সাইড", "হাইড্রোজেন পারক্সাইড"));
                        add(new QuestionModule("কোনটি একটি শক্তিশালী তড়িৎ পরিবাহী?", "B", "অক্সিজেন", "সোনা", "জল", "অ্যালুমিনিয়াম"));
                        add(new QuestionModule("কোনটি একটি অণুর প্রধান অংশ?", "A", "প্রোটন", "ইলেকট্রন", "নিউট্রন", "ফোটন"));
                        add(new QuestionModule("কোনটি একটি রাসায়নিক পরিবর্তনের উদাহরণ?", "D", "কাঁচ গলানো", "লোহা গলানো", "পানি বাষ্পীভূত হওয়া", "কাগজ পুড়ে যাওয়া"));

                    }
                };
                QuestionModule.createQuestionsForSubject("Chemistry", R.drawable.category_icon4, questions);

        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("আলোর গতি কত?", "A", "3.00 × 10^8 মিটার/সেকেন্ড", "3.00 × 10^6 মিটার/সেকেন্ড", "3.00 × 10^5 মিটার/সেকেন্ড", "3.00 × 10^7 মিটার/সেকেন্ড"));
                add(new QuestionModule("ধ্বনির গতি কোথায় বেশি?", "D", "বায়ুতে", "জলে", "শূন্যে", "লোহার মধ্যে"));
                add(new QuestionModule("নিউটনের প্রথম সূত্রটি কী?", "C", "বস্তু স্থির থাকবে", "বস্তু চলমান থাকবে", "বস্তু বাহ্যিক শক্তি প্রাপ্ত না হলে তার অবস্থায় পরিবর্তন হবে না", "বস্তু নিচে পড়বে"));
                add(new QuestionModule("ভারতীয় পদার্থবিজ্ঞানী চন্দ্রশেখরের পুরো নাম কী?", "D", "চন্দ্রশেখর বনিক", "চন্দ্রশেখর সেনগুপ্ত", "চন্দ্রশেখর বসু", "সুব্রহ্মণ্যম চন্দ্রশেখর "));
                add(new QuestionModule("নিউটনের দ্বিতীয় সূত্রটি কী নিয়ে কাজ করে?", "A", "বল ও ত্বরণ", "ভর ও ত্বরণ", "বল ও ভর", "গতিশক্তি"));
                add(new QuestionModule("বৈদ্যুতিক প্রতিরোধের একক কী?", "B", "ভোল্ট", "ওহম", "ওয়াট", "অ্যাম্পিয়ার"));
                add(new QuestionModule("যান্ত্রিক কাজের একক কী?", "C", "নিউটন", "জুল", "ওয়াট", "প্যাসকেল"));
                add(new QuestionModule("কোনটি জুলের সমতুল্য?", "D", "নিউটন", "ওহম", "ক্যালরি", "কিলোওয়াট-ঘণ্টা"));
                add(new QuestionModule("কোনটি গতির পরিমাপ করে?", "A", "মিটার/সেকেন্ড", "মিটার", "নিউটন", "জুল"));
                add(new QuestionModule("শ্রবণের জন্য ধ্বনির সর্বনিম্ন ফ্রিকোয়েন্সি কত?", "B", "10 Hz", "20 Hz", "50 Hz", "100 Hz"));
                add(new QuestionModule("একটি চুম্বকীয় পদার্থ কীভাবে কাজ করে?", "C", "বৈদ্যুতিক ক্ষেত্র তৈরি করে", "তাপ উৎপন্ন করে", "চৌম্বক ক্ষেত্র তৈরি করে", "শক্তি সংরক্ষণ করে"));
                add(new QuestionModule("শূন্যে বাতাসের চাপ কী?", "D", "বায়ুমণ্ডলীয় চাপের সমান", "বায়ুমণ্ডলীয় চাপের চেয়ে বেশি", "বায়ুমণ্ডলীয় চাপের চেয়ে কম", "শূন্য"));
                add(new QuestionModule("নিউটনের তৃতীয় সূত্রটি কী বলে?", "A", "প্রতিটি ক্রিয়ার জন্য সমান ও বিপরীত প্রতিক্রিয়া থাকে", "গতি পরিবর্তন হয় না", "স্থির বস্তু চলতে শুরু করে", "ত্বরণ কমে যায়"));
                add(new QuestionModule("পানি গ্যাসে রূপান্তরিত হয় কোন অবস্থায়?", "B", "90°C", "100°C", "120°C", "150°C"));
                add(new QuestionModule("তাপমাত্রার একক কী?", "C", "ওয়াট", "জুল", "কেলভিন", "ওহম"));
                add(new QuestionModule("আলোকের প্রতিফলন কোন কোণে ঘটে?", "D", "0°", "45°", "60°", "90°"));
                add(new QuestionModule("শ্রবণযোগ্য ধ্বনির সর্বোচ্চ ফ্রিকোয়েন্সি কত?", "A", "20,000 Hz", "10,000 Hz", "50,000 Hz", "100,000 Hz"));
                add(new QuestionModule("কোন শক্তির উৎসকে নবায়নযোগ্য বলা হয়?", "B", "তেল", "সৌরশক্তি", "গ্যাস", "কয়লা"));
                add(new QuestionModule("প্রিজমের মাধ্যমে আলোর বিচ্ছুরণ হলে কতটি রং দেখা যায়?", "C", "৫টি", "৬টি", "৭টি", "৮টি"));
                add(new QuestionModule("তাপমাত্রা বৃদ্ধি পেলে কোন গ্যাসটি সম্প্রসারিত হয়?", "D", "অক্সিজেন", "হাইড্রোজেন", "নাইট্রোজেন", "সবগুলো"));
                add(new QuestionModule("ইলেকট্রনের চার্জ কী?", "A", "নেতিবাচক", "ধনাত্মক", "নিরপেক্ষ", "উভয়ই"));
                add(new QuestionModule("বৈদ্যুতিক সার্কিটে কারেন্টের প্রবাহ কোন দিক থেকে হয়?", "B", "নেতিবাচক থেকে ধনাত্মক", "ধনাত্মক থেকে নেতিবাচক", "ইলেকট্রন থেকে প্রোটন", "নিউট্রন থেকে ইলেকট্রন"));
                add(new QuestionModule("আলোর প্রতিসরণ কীভাবে ঘটে?", "C", "নির্দিষ্ট কোণে", "বিন্দু থেকে বিন্দুতে", "মাধ্যম পরিবর্তনের ফলে", "চৌম্বক ক্ষেত্রে"));
                add(new QuestionModule("ধাতু গলিত অবস্থায় কোনটি বেশি বিদ্যুৎ পরিবাহী?", "D", "অ্যালুমিনিয়াম", "তামা", "সোনা", "রূপা"));
                add(new QuestionModule("পৃথিবীর অভিকর্ষজ ত্বরণ কত?", "A", "9.8 m/s²", "10 m/s²", "8.8 m/s²", "9.5 m/s²"));
                add(new QuestionModule("চুম্বকীয় ক্ষেত্রের একক কী?", "B", "ওহম", "টেসলা", "জুল", "নিউটন"));
                add(new QuestionModule("কোনটি রেডিও তরঙ্গের বৈশিষ্ট্য?", "C", "উচ্চ ফ্রিকোয়েন্সি", "কম তরঙ্গদৈর্ঘ্য", "দীর্ঘ তরঙ্গদৈর্ঘ্য", "ছোট তরঙ্গদৈর্ঘ্য"));
                add(new QuestionModule("বাতাসে শব্দের গতি কত?", "D", "343 মিটার/সেকেন্ড", "300 মিটার/সেকেন্ড", "330 মিটার/সেকেন্ড", "350 মিটার/সেকেন্ড"));
                add(new QuestionModule("একটি বৈদ্যুতিক মোটর কী শক্তিতে রূপান্তরিত করে?", "A", "বৈদ্যুতিক শক্তি থেকে যান্ত্রিক শক্তি", "তাপ শক্তি থেকে যান্ত্রিক শক্তি", "তাপ শক্তি থেকে বৈদ্যুতিক শক্তি", "যান্ত্রিক শক্তি থেকে বৈদ্যুতিক শক্তি"));
                add(new QuestionModule("পদার্থের কোন অবস্থায় আয়তন স্থির থাকে?", "C", "গ্যাস", "তরল", "কঠিন", "প্লাজমা"));
                add(new QuestionModule("শূন্যের তাপমাত্রা কতো?", "C", "0 K", "-273.15°C", "0°C", "100°C"));
                add(new QuestionModule("কোনটি তাপ পরিবাহিতার জন্য শ্রেষ্ঠ?", "A", "তামা", "প্লাস্টিক", "গ্যাস", "কাঠ"));
                add(new QuestionModule("কোন পদার্থে দড়ির জলে দ্রবীভূত হয়?", "B", "শর্করা", "লবণ", "সিরামিক", "মেটাল"));
                add(new QuestionModule("দূরত্বের একক কী?", "A", "মিটার", "কিলোমিটার", "সেন্টিমিটার", "মাইল"));
                add(new QuestionModule("ধারণা কী?", "A", "শক্তি", "ভর", "গতি", "তাপ"));
                add(new QuestionModule("জল কি একটি যৌগ?", "A", "হ্যাঁ", "না", "শুধুমাত্র তরল", "শুধুমাত্র গ্যাস"));
                add(new QuestionModule("শূন্য তাপমাত্রায় কিভাবে জল বরফে রূপান্তরিত হয়?", "A", "হিমায়নের মাধ্যমে", "অবদমন", "অবশিষ্ট তাপ", "অবস্থান পরিবর্তন"));
                add(new QuestionModule("শব্দের জন্য কোন মাধ্যম প্রয়োজন?", "A", "গ্যাস", "অত্যধিক চাপ", "পদার্থ", "তাপ"));
                add(new QuestionModule("পদার্থের তিনটি অবস্থার নাম কি?", "A", "গ্যাস, তরল, কঠিন", "বাষ্প, বরফ, পানি", "মাথার, হৃদয়ের, শরীরের", "কঠিন, কঠিন, কঠিন"));
                add(new QuestionModule("কোনটি স্রোত উৎপন্ন করে?", "D", "চুম্বক", "ম্যাগনেট", "তাপ", "বিদ্যুৎ "));
                add(new QuestionModule("কোন শক্তির উত্স হচ্ছে সৌরশক্তি?", "A", "নবায়নযোগ্য", "অনবায়নযোগ্য", "যান্ত্রিক", "কেমিক্যাল"));
                add(new QuestionModule("ধাতুর গলনাঙ্ক সাধারণত কত?", "A", "1000°C", "500°C", "2000°C", "1500°C"));
                add(new QuestionModule("আলোর বেগ কি?", "C", "উষ্ণতার পরিবর্তন", "মাধ্যমের সমন্বয়", " পদার্থের গতির পরিবর্তন", "গতির প্রক্রিয়া"));
                add(new QuestionModule("বিদ্যুতের শৃঙ্খল পরিমাপের একক কি?", "A", "ওহম", "ভোল্ট", "ক্যালরি", "জুল"));
                add(new QuestionModule("আলোর প্রতিফলনের সময় কত প্রভা হয়?", "A", "নিখুঁত", "অন্ধকার", "সরাসরি", "বাঁকানো"));
                add(new QuestionModule("প্রথম আদর্শ নিউটনের তৃতীয় সূত্র কি?", "B", "অন্য কিছু","সক্রিয় ও প্রতিক্রিয়া"," স্থিরতা", "গতি"));
                add(new QuestionModule("বৈদ্যুতিক তত্ত্ব কী?", "C", "বিদ্যুৎচালক", "শক্তি", "বিদ্যুৎ", "ভর"));
                add(new QuestionModule("গ্যাসের তাপমাত্রা বাড়ালে কি হয়?", "D", "মহাকর্ষ", "বিদ্যুৎ", "আবেগ", "মহাকর্ষ বল"));
                add(new QuestionModule("রশ্মির জন্য কোন মাধ্যম বেশি কার্যকর?", "A", "জল", "বায়ু", "তরল", "অবস্থায়িত"));
                add(new QuestionModule("জ্যোতির্বিজ্ঞান কী?", "A", "পদার্থের কার্য", "জ্যোতিষ", "অভিকর্ষ", "চন্দ্র ও সূর্য"));
                add(new QuestionModule("দৃশ্যে ধূসর রং কোথা থেকে আসে?", "C", "অন্ধকার", "শুধুমাত্র আলো", "বস্তু ও আলো", "বিষাক্ত"));
                add(new QuestionModule("সপ্তম কক্ষপথের উচ্চতা কতো?", "A", "অনির্ধারিত", "আলোর সঙ্গে", "বৈদ্যুতিক", "পদার্থ"));
                add(new QuestionModule("কোন পদার্থ সবচেয়ে বেশি তাপ পরিবাহী?", "A", "সোনা", "প্লাস্টিক", "লবণ", "কঠিন"));
                add(new QuestionModule("প্রকৃতি শক্তি উৎপন্ন করে?", "D", "আলোর তরঙ্গ", "নেতিবাচক শক্তি", "চালক", "বায়ু"));
                add(new QuestionModule("কোন শক্তির মাপ পরিমাপ করা হয়?", "B", "অবস্থা", "গতি", "নির্দেশ", "উৎস"));
                add(new QuestionModule("বিদ্যুৎ উৎপাদনে কোনটি প্রয়োজন?", "A", "সোলার", "গ্যাস", "ম্যাগনেট", "অ্যালুমিনিয়াম"));
                add(new QuestionModule("কোন পদার্থ সর্বাধিক তাপমাত্রায় মিথেন রূপান্তরিত হয়?", "A", "জল", "কোল্ড", "গ্যাস", "কঠিন"));
                add(new QuestionModule("বৈদ্যুতিক শক্তি কত?", "A", "এনার্জি", "এন্ট্রি", "থার্মাল", "অ্যাপারেটাস"));
                add(new QuestionModule("কোন পদার্থ অম্ল তৈরি করে?", "A", "অক্সিজেন", "বায়ু", "জল", "কার্বন"));
                add(new QuestionModule("কোন কোণ থেকে আলোর প্রতিফলন ঘটে?", "C", "অর্ধেক", "পূর্ণ", "নিখুঁত", "বর্গ"));
                add(new QuestionModule("কোন বস্তুর আকার পরিবর্তন না হলে তা কি বলে?", "A", "স্থায়ী", "অস্থায়ী", "ম্যাটার", "এলাস্টিক"));
                add(new QuestionModule("জল কি একটি শুদ্ধ পদার্থ?", "A", "হ্যাঁ", "না", "শুধুমাত্র তরল", "শুধুমাত্র কঠিন"));
                add(new QuestionModule("ভর ও গতি সম্পর্কিত আইন কোনটি?", "A", "নিউটনের দ্বিতীয় সূত্র", "নিউটনের প্রথম সূত্র", "নিউটনের তৃতীয় সূত্র", "পাস্কালের সূত্র"));
                add(new QuestionModule("শক্তির একক কি?", "A", "ওয়াট", "জুল", "ভোল্ট", "অ্যাম্পিয়ার"));;
                add(new QuestionModule("গ্যাসের তাপমাত্রা বাড়লে কি হয়?", "A", "এটি সম্প্রসারিত হয়", "এটি সংকুচিত হয়", "এটি স্থির থাকে", "এটি ভেঙে যায়"));
                add(new QuestionModule("নিউটনের আইনগুলো কতটি?", "B", "দুইটি ","তিনটি",  "চারটি", "পাঁচটি"))
                ;add(new QuestionModule("কোন পদার্থ আলোর বেগ কমিয়ে দেয়?", "C", "অ্যালুমিনিয়াম", "জল", "অপটিক্যাল ফাইবার ", "বায়ু"));
                add(new QuestionModule("কোন পদার্থে চাপ প্রয়োগ করলে আকার পরিবর্তন হয়?", "D", "তরল ", "জল", "কঠিন", "বাতাস"));
                add(new QuestionModule("ভূমিকম্পের শক্তি পরিমাপের একক কি?", "A", "রিখটার স্কেল", "ওয়াট", "জুল", "নিউটন"));
                add(new QuestionModule("আলোর রং কতটি?", "C", "পাঁচটি", "নয়টি", "সাতটি", "এগারোটি"));
                add(new QuestionModule("বিদ্যুতের সার্কিটে কত ধরনের সার্কিট থাকে?", "A", "দুইটি", "একটি", "তিনটি", "চারটি"));
                add(new QuestionModule("শক্তির সংরক্ষণের আইন কি?", "B", "শক্তি তৈরি হয় না", "শক্তি ধ্বংস হয় না", "শক্তি পরিবর্তন হয়", "শক্তি গঠিত হয়"));
                add(new QuestionModule("প্রকৃতির কোন প্রক্রিয়া আলোর বর্ণের সৃষ্টি করে?", "B", "ম্যাটার","প্রিজম",  "কণা", "থার্মাল"));
                add(new QuestionModule("কোন পদার্থের অবস্থান পরিবর্তনের কারণে কাজ হয়?", "A", "যান্ত্রিক কাজ", "শক্তি", "জলবিদ্যুৎ", "ম্যাগনেটিক কাজ"));
                add(new QuestionModule("শূন্যে কণার গতি কি?", "D", "গতিশীল", "স্থিতিশীল", "নিরপেক্ষ" ,"অস্তিত্বহীন"));
                add(new QuestionModule("মেটাল গলানোর তাপমাত্রা সাধারণত কতো?", "A", "মেটাল অনুযায়ী পরিবর্তিত হয়", "একটি মান আছে", "কোন মান নেই", "সর্বদা 1000°C"));
                add(new QuestionModule("বিদ্যুতের উত্স কি?", "B", "নেতিবাচক চাপ","শক্তি", "আবেদন", "জলবায়ু"));
                add(new QuestionModule("দ্রব্যমানে তাপ প্রয়োগ করলে কি হয়?", "C", "সামঞ্জস্য হয়", "স্থির থাকে","গ্যাস তৈরি হয়", "মাঝে মাঝে ছড়িয়ে পড়ে"));
                add(new QuestionModule("কোন পদার্থ আলোর প্রতিফলন করে?", "D", "বায়ু", "তাপ", "বস্তু" ,"জল"));
                add(new QuestionModule("কোন পদার্থ কম্পনের ফলে শক্তি উৎপন্ন করে?", "C","সোনা", "অ্যালুমিনিয়াম","লৌহ", "তামা"));
                add(new QuestionModule("গ্যাসের চাপ কি?", "D", "নিরপেক্ষ চাপ", "কঠিন চাপ", "তরল চাপ","বায়ু চাপ"));
                add(new QuestionModule("তাপমাত্রা বাড়লে গ্যাসের কী হয়?", "A", "উচ্চতর চাপ সৃষ্টি হয়", "নিম্ন চাপ সৃষ্টি হয়", "শক্তি উৎপন্ন হয়", "স্থির থাকে"));
                add(new QuestionModule("শূন্যে চাপ কি?", "B", "অনেক কম", "শূন্য", "বিরল", "বেশি"));
                add(new QuestionModule("শক্তির সংরক্ষণের সূত্র কোনটি?", "A", "E = mc²", "F = ma", "v = u + at", "a = v/t"));
                add(new QuestionModule("কোন গ্যাস দ্রবণীয়?", "D", "হাইড্রোজেন", "মিথেন", "হেলিয়াম","অক্সিজেন"));
                add(new QuestionModule("অপটিক্যাল ফাইবার কী?", "A", "আলোকের পথ", "বিদ্যুৎ", "গ্যাস", "থার্মাল"));
                add(new QuestionModule("ধ্বনির জন্য কোন মাধ্যম প্রয়োজন?", "B", "বায়ু", "গ্যাস", "দ্রব", "তরল"));
                add(new QuestionModule("বায়ুমণ্ডলের চাপের জন্য প্রয়োজনীয় একক কি?", "A", "প্যাসকেল", "ওহম", "জুল", "নিউটন"));
                add(new QuestionModule("প্রথম পদার্থের চার্জ কি?", "D", "ধনাত্মক", "শূন্য", "নিরপেক্ষ", "নেতিবাচক"));
                add(new QuestionModule("কোন পদার্থ তাপ পরিবাহিতার জন্য খারাপ?", "A", "প্লাস্টিক", "মেটাল", "গ্যাস", "কঠিন"));
                add(new QuestionModule("ধাতুর অবস্থায় বিদ্যুৎ পরিবাহিত হয়?", "A", "হ্যাঁ", "না", "শুধুমাত্র তামা", "শুধুমাত্র অ্যালুমিনিয়াম"));
                add(new QuestionModule("গ্রহের জন্য গ্রাভিটেশনাল আইন কার?", "A", "নিউটন", "আইনস্টাইন", "গ্যালিলিও", "কপারনিকাস"));
                add(new QuestionModule("শব্দের বেগ কি?", "C", "রূপান্তরিত হয়", "ধ্রুব", "বায়ুর উপর নির্ভর করে", "অবিচল"));
                add(new QuestionModule("গতি নির্ধারণের জন্য কোন স্কেলার পরিমাণ?", "B", "অবস্থান", "সময়", "ভৌত অবস্থান", "গতি"));
                add(new QuestionModule("থার্মাল পরিবাহিতা কি?", "A", "তাপের পরিবহন", "বিদ্যুতের পরিবহন", "শব্দের পরিবহন", "দ্রবের পরিবহন"));
                add(new QuestionModule("পদার্থের মলিকুলার অবস্থান কিভাবে নির্ধারণ করা হয়?", "B", "জন্ম এবং সংমিশ্রণ", "তাপমাত্রা এবং চাপ", "আলোকশক্তি", "অর্থ্যোডিনামিক"));
                add(new QuestionModule("কোন পদার্থটি উচ্চ তাপ পরিবাহক?", "A", "তামা", "প্লাস্টিক", "গ্লাস", "কাঠ"));
                add(new QuestionModule("লঘু ওজনের গ্যাস কোনটি?", "C", "অক্সিজেন", "নাইট্রোজেন", "হাইড্রোজেন", "হেলিয়াম"));
                add(new QuestionModule("কোনটি একটি পারমাণবিক তাপ কেন্দ্র?", "A", "পারমাণবিক রি-অ্যাক্টর", "গ্যাস কেন্দ্র", "জল কেন্দ্র", "কয়লা কেন্দ্র"));
                add(new QuestionModule("তাপমাত্রার একক কি?", "D", "জুল", "প্যাসকেল", "ওহম","সেলসিয়াস"));
                add(new QuestionModule("পদার্থের চাপের একক কি?", "B", "প্যাসকেল", "নিউটন", "জুল", "কিলোগ্রাম"));

            }
        };
        QuestionModule.createQuestionsForSubject("Physics", R.drawable.category_icon3, questions);


        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("১ + ১ = ?", "A", "২", "৩", "৪", "১"));
                add(new QuestionModule("১০০ এর বর্গমূল কত?", "B", "৯", "১০", "১১", "১২"));
                add(new QuestionModule("৩ এর ঘনফল কত?", "C", "৯", "২৭", "৬", "৮১"));
                add(new QuestionModule("পাই এর মান কত?", "D", "২.১৪", "৪.১৩", "৩.১৫", "৩.১৪"));
                add(new QuestionModule("৪ এবং ৫ এর ল.সা.গু কত?", "A", "২০", "১০", "৫", "১৫"));
                add(new QuestionModule("কোনটি মৌলিক সংখ্যা?", "B", "৪", "৭", "১০", "৯"));
                add(new QuestionModule("দশমিক ০.২৫ কে ভগ্নাংশে প্রকাশ করলে কী হবে?", "C", "১/২", "৩/৪", "১/৪", "২/৩"));
                add(new QuestionModule("৭৫ এর শতকরা ২০ কত?", "A", "১৫", "২০", "১০", "৫"));
                add(new QuestionModule("গণিতের কোন ক্ষেত্রটি কোণ নিয়ে কাজ করে?", "A", "ত্রিকোণমিতি", "জ্যামিতি", "বীজগণিত", "পীথাগোরাস"));
                add(new QuestionModule("একটি পূর্ণ বৃত্তের কোণ কত ডিগ্রী?", "B", "১৮০", "৩৬০", "২৭০", "৯০"));
                add(new QuestionModule("একটি চতুর্ভুজের কতটি বাহু থাকে?", "C", "৩", "৫", "৪", "৬"));
                add(new QuestionModule("কোনো ত্রিভুজের তিনটি কোণের যোগফল কত?", "A", "১৮০°", "৩৬০°", "৯০°", "২৭০°"));
                add(new QuestionModule("একটি সংখ্যার ৫০% মানে কী?", "A", "সংখ্যাটির অর্ধেক", "সংখ্যাটির দ্বিগুণ", "সংখ্যাটির তিন গুণ", "সংখ্যাটির চার গুণ"));
                add(new QuestionModule("একটি সংখ্যার ঘাত ২ হলে সেটিকে কী বলে?", "B", "বর্গমূল", "বর্গ", "ঘন", "অতিপরিসীম"));
                add(new QuestionModule("কোনো পরিমাপ পদ্ধতিতে দৈর্ঘ্যের একক কী?", "C", "কিলোগ্রাম", "সেকেন্ড", "মিটার", "ওয়াট"));
                add(new QuestionModule("কোনটি বিজোড় সংখ্যা?", "D", "৪", "৬", "৮", "৯"));
                add(new QuestionModule("একটি কোণের সন্নিহিত কোণ কত ডিগ্রি?", "A", "১৮০°", "৯০°", "১২০°", "৬০°"));
                add(new QuestionModule("দুটি সংখ্যার গ.সা.গু ও ল.সা.গু এর গুণফল কী সমান হয়?", "B", "গুণফলের সমান", "সংখ্যাগুলোর গুণফলের সমান", "ভাগফলের সমান", "যোগফলের সমান"));
                add(new QuestionModule("সমকোণী ত্রিভুজের পীঠাগোরাস সূত্র কী?", "C", "a² = b² + c²", "a² + b² = c", "a² + b² = c²", "a = b² + c²"));
                add(new QuestionModule("ফ্যাক্টরিয়াল ৫ কত?", "A", "১২০", "২৪", "৬০", "১০০"));
                add(new QuestionModule("কোনো সংখ্যার থেকে শূন্য বিয়োগ করলে কী হয়?", "A", "সংখ্যাটি অপরিবর্তিত থাকে", "সংখ্যাটি দ্বিগুণ হয়", "সংখ্যাটি অর্ধেক হয়", "সংখ্যাটি শূন্য হয়"));
                add(new QuestionModule("সমান্তর ধারার প্রথম সংখ্যা ২ এবং দ্বিতীয় সংখ্যা ৪ হলে তৃতীয় সংখ্যা কত?", "B", "৫", "৬", "৮", "১০"));
                add(new QuestionModule("গণিতের কোন শাখা সংখ্যার সাথে কাজ করে?", "D", "জ্যামিতি", "ত্রিকোণমিতি", "বীজগণিত", "সংখ্যাতত্ত্ব"));
                add(new QuestionModule("একটি বর্গক্ষেত্রের ক্ষেত্রফল ২৫ বর্গমিটার হলে এর বাহু কত মিটার?", "D", "১০", "৫০", "১৫", "৫"));
                add(new QuestionModule("একটি সংখ্যার দ্বিগুণ থেকে ৪ বিয়োগ করলে ফল ১০ হলে সংখ্যাটি কত?", "A", "৭", "৬", "৫", "৮"));
                add(new QuestionModule("সমান্তর ধারায় ধারার যোগফল নির্ণয়ের সূত্র কী?", "B", "Sn = n(a + l)", "Sn = n/2(a + l)", "Sn = n(a - l)", "Sn = 2n(a + l)"));
                add(new QuestionModule("কোনো ত্রিভুজের উচ্চতা ও ভিত্তি ৮ মিটার এবং ৫ মিটার হলে এর ক্ষেত্রফল কত?", "C", "৪০ বর্গমিটার", "১৫ বর্গমিটার", "২০ বর্গমিটার", "১০ বর্গমিটার"));
                add(new QuestionModule("একটি সরলরেখার ঢাল কীভাবে নির্ণয় করা হয়?", "D", "y/x", "dy/dx", "Δx/Δy", "Δy/Δx"));
                add(new QuestionModule("৩৬ এবং ৪৮ এর গ.সা.গু কত?", "A", "১২", "১৮", "৬", "২৪"));
                add(new QuestionModule("পাই এর মান ২২/৭ এর দশমিক মান কী?", "B", "৩.১৬", "৩.১৪", "৩.১২", "৩.১৫"));
                add(new QuestionModule("৫ + ৬ = ?", "A", "১১", "১০", "৯", "১২"));
                add(new QuestionModule("২৫ এর বর্গমূল কত?", "C", "৬", "৫.৫", "৫", "৪"));
                add(new QuestionModule("২ এর ঘনফল কত?", "C", "৬", "৪", "৮", "১২"));
                add(new QuestionModule("কোনটি মৌলিক সংখ্যা নয়?", "D", "২", "৩", "৫", "৬"));
                add(new QuestionModule("৩ এবং ৯ এর ল.সা.গু কত?", "A", "৯", "৬", "৩", "২৭"));
                add(new QuestionModule("৯ + ৯ = ?", "B", "১৯", "১৮", "১৭", "২০"));
                add(new QuestionModule("দশমিক ০.৭৫ কে ভগ্নাংশে প্রকাশ করলে কী হবে?", "C", "২/৩", "৪/৫", "৩/৪", "১/২"));
                add(new QuestionModule("৫০ এর শতকরা ১০ কত?", "A", "৫", "১০", "১৫", "২০"));
                add(new QuestionModule("গণিতের কোন শাখা পরিমাপ নিয়ে কাজ করে?", "B", "বীজগণিত", "জ্যামিতি", "সংখ্যাতত্ত্ব", "পীথাগোরাস"));
                add(new QuestionModule("একটি ত্রিভুজের একটি কোণ ৬০° হলে অপর দুটি কোণের যোগফল কত?", "C", "৩০°", "৪৫°", "১২০°", "৬০°"));
                add(new QuestionModule("একটি সংখ্যার শতকরা ২৫ মানে কী?", "A", "সংখ্যাটির চতুর্থাংশ", "সংখ্যাটির অর্ধেক", "সংখ্যাটির তিন গুণ", "সংখ্যাটির দ্বিগুণ"));
                add(new QuestionModule("একটি সংখ্যার ঘাত ৩ হলে সেটিকে কী বলে?", "B", "বর্গ", "ঘন", "বর্গমূল", "চতুর্থমূল"));
                add(new QuestionModule("পরিমাপ পদ্ধতিতে সময়ের একক কী?", "C", "মিটার", "কিলোগ্রাম", "সেকেন্ড", "ডেসিমিটার"));
                add(new QuestionModule("কোনটি জোড় সংখ্যা নয়?", "D", "৮", "১০", "১২", "১৩"));
                add(new QuestionModule("একটি সমকোণী ত্রিভুজের ২টি কোণ কত ডিগ্রি হতে পারে?", "A", "৪৫° এবং ৪৫°", "৩০° এবং ৬০°", "৯০° এবং ৯০°", "১৮০° এবং ৯০°"));
                add(new QuestionModule("গ.সা.গু নির্ণয়ের সহজ পদ্ধতি কী?", "B", "যোগফল", "ভাগফল", "গুণফল", "বিয়োগফল"));
                add(new QuestionModule("কোনো সংখ্যার বর্গ নির্ণয় করা মানে কী?", "D", "সংখ্যা যোগ করা", "সংখ্যা ভাগ করা", "সংখ্যা গুণ করা", "সংখ্যা নিজেই গুণ করা"));
                add(new QuestionModule("ফ্যাক্টরিয়াল ৩ কত?", "A", "৬", "৩", "৯", "২৭"));
                add(new QuestionModule("কোনো সংখ্যা থেকে ৭ বিয়োগ করলে শূন্য হলে, সংখ্যাটি কত?", "A", "৭", "১৪", "১০", "৩"));
                add(new QuestionModule("গণিতের কোন শাখা সংখ্যার ধারা নিয়ে কাজ করে?", "C", "জ্যামিতি", "ত্রিকোণমিতি", "সিরিজ", "পীথাগোরাস সূত্র"));
                add(new QuestionModule("সমান্তর ধারার যোগফল নির্ণয়ের সূত্র কী?", "B", "Sn = n(a + l)", "Sn = n/2(a + l)", "Sn = n(a - l)", "Sn = 2n(a + l)"));
                add(new QuestionModule("ত্রিভুজের উচ্চতা ও ভিত্তি যথাক্রমে ৯ এবং ৪ হলে এর ক্ষেত্রফল কত?", "C", "৩৬", "১৮", "১৮", "১২"));
                add(new QuestionModule("১৮ এবং ২৪ এর গ.সা.গু কত?", "A", "৬", "৮", "১০", "১২"));
                add(new QuestionModule("পাই এর মান ৩.১৪ হলে এর শতকরা মান কত?", "A", "৩১৪%", "৩.১৪%", "২২/৭", "৩১০%"));
                add(new QuestionModule("৩ এর বর্গফল কত?", "C", "৬", "৮", "৯", "১০"));
                add(new QuestionModule("১ এর ঘনফল কত?", "A", "১", "২", "৩", "৪"));
                add(new QuestionModule("একটি পূর্ণ বৃত্তে কত কোণ থাকে?", "A", "৩৬০°", "১৮০°", "৯০°", "৭২°"));
                add(new QuestionModule("১৫ এর দ্বিগুণ কত?", "A", "৩০", "২০", "১০", "১৫"));
                add(new QuestionModule("৭ এর ২-এর গুণফল কত?", "B", "১২", "১৪", "২১", "২৮"));
                add(new QuestionModule("৪০ এর শতকরা ২৫ কত?", "B", "৫", "১০", "২০", "৩০"));
                add(new QuestionModule("গণিতের কোন শাখা আয়তনের হিসাব করে?", "C", "বীজগণিত", "সংখ্যাতত্ত্ব", "জ্যামিতি", "ত্রিকোণমিতি"));
                add(new QuestionModule("দুটি সংখ্যার যোগফল ১০ হলে সংখ্যা দুটি কী হতে পারে?", "A", "৫ এবং ৫", "৪ এবং ৬", "৩ এবং ৭", "২ এবং ৮"));
                add(new QuestionModule("একটি ত্রিভুজের উচ্চতা ৮ এবং ভিত্তি ৫ হলে এর ক্ষেত্রফল কত?", "B", "২০", "৪০", "১০", "২৫"));
                add(new QuestionModule("১৫ এবং ২০ এর ল.সা.গু কত?", "C", "১০", "৫", "৬০", "৩০"));
                add(new QuestionModule("একটি সংখ্যা কে ৩ দিয়ে গুণ করলে ফল কী হবে?", "B", "সংখ্যাটির দ্বিগুণ", "সংখ্যাটির তিন গুণ", "সংখ্যাটির পাঁচ গুণ", "সংখ্যাটির চতুর্গুণ"));
                add(new QuestionModule("একটি পূর্ণবৃত্তের পরিধি বের করার সূত্র কী?", "A", "২πr", "πr²", "r²", "২r"));
                add(new QuestionModule("দশমিক ০.১২৫ কে ভগ্নাংশে প্রকাশ করলে কী হবে?", "C", "১/৪", "২/৫", "১/৮", "১/১০"));
                add(new QuestionModule("একটি সংখ্যার দ্বিগুণ থেকে ৩ বিয়োগ করলে ফল ৯ হলে সংখ্যাটি কত?", "B", "৫", "৬", "৭", "৮"));
                add(new QuestionModule("২ + ২ = ?", "A", "৪", "৩", "৫", "৬"));
                add(new QuestionModule("১৬ এর বর্গমূল কত?", "B", "৫", "৪", "৬", "৩"));
                add(new QuestionModule("৩ এর ঘনফল কত?", "D", "৯", "৬", "১২", "২৭"));
                add(new QuestionModule("কোনটি মৌলিক সংখ্যা?", "C", "৪", "৬", "৭", "১০"));
                add(new QuestionModule("১২ এবং ১৫ এর ল.সা.গু কত?", "D", "১৮", "২৪", "১২", "৬০"));
                add(new QuestionModule("৮ + ৫ = ?", "A", "১৩", "১২", "১৪", "১৫"));
                add(new QuestionModule("দশমিক ০.২৫ কে ভগ্নাংশে প্রকাশ করলে কী হবে?", "A", "১/৪", "১/২", "৩/৪", "১/৩"));
                add(new QuestionModule("১০০ এর শতকরা ৫ কত?", "B", "১০", "৫", "২০", "১৫"));
                add(new QuestionModule("গণিতের কোন শাখা আকার নিয়ে কাজ করে?", "C", "বীজগণিত", "পীথাগোরাস", "জ্যামিতি", "সংখ্যাতত্ত্ব"));
                add(new QuestionModule("একটি ত্রিভুজের একটি কোণ ৩০° হলে অপর দুটি কোণের যোগফল কত?", "D", "৬০°", "১২০°", "৯০°", "১৫০°"));
                add(new QuestionModule("একটি সংখ্যার শতকরা ৫০ মানে কী?", "B", "সংখ্যাটির চতুর্থাংশ", "সংখ্যাটির অর্ধেক", "সংখ্যাটির তিন গুণ", "সংখ্যাটির দ্বিগুণ"));
                add(new QuestionModule("একটি সংখ্যার ঘাত ২ হলে সেটিকে কী বলে?", "A", "বর্গ", "ঘন", "বর্গমূল", "চতুর্থমূল"));
                add(new QuestionModule("পরিমাপ পদ্ধতিতে দৈর্ঘ্যের একক কী?", "A", "মিটার", "সেকেন্ড", "কিলোগ্রাম", "ডেসিমিটার"));
                add(new QuestionModule("কোনটি বিজোড় সংখ্যা?", "C", "৮", "১২", "৭", "১৪"));
                add(new QuestionModule("একটি সমকোণী ত্রিভুজের ১টি কোণ ৩০° হলে আরেকটি কোণ কত?", "B", "৬০°", "৩০°", "৯০°", "৪৫°"));
                add(new QuestionModule("ল.সা.গু নির্ণয়ের সহজ পদ্ধতি কী?", "A", "গুণফল", "ভাগফল", "যোগফল", "বিয়োগফল"));
                add(new QuestionModule("কোনো সংখ্যার বর্গমূল নির্ণয় করা মানে কী?", "C", "সংখ্যা যোগ করা", "সংখ্যা গুণ করা", "সংখ্যার মূল নির্ণয় করা", "সংখ্যা বিয়োগ করা"));
                add(new QuestionModule("ফ্যাক্টরিয়াল ৪ কত?", "D", "১০", "১২", "২০", "২৪"));
                add(new QuestionModule("কোনো সংখ্যা থেকে ১০ বিয়োগ করলে শূন্য হলে, সংখ্যাটি কত?", "B", "৫", "১০", "১৫", "২০"));
                add(new QuestionModule("গণিতের কোন শাখা ত্রিকোণ নিয়ে কাজ করে?", "B", "জ্যামিতি", "ত্রিকোণমিতি", "বীজগণিত", "পীথাগোরাস সূত্র"));
                add(new QuestionModule("সমান্তর ধারার প্রথম ১০টি সংখ্যার যোগফল নির্ণয়ের সূত্র কী?", "B", "Sn = n(a + d)", "Sn = n/2(2a + (n-1)d)", "Sn = 2n(a + d)", "Sn = n(a - d)"));
                add(new QuestionModule("ত্রিভুজের উচ্চতা ও ভিত্তি যথাক্রমে ১০ এবং ৫ হলে এর ক্ষেত্রফল কত?", "B", "২৫", "৫০", "১৫", "২০"));
                add(new QuestionModule("১৮ এবং ৩০ এর গ.সা.গু কত?", "B", "৩", "৬", "৫", "১০"));
                add(new QuestionModule("π এর মান ২২/৭ হলে এর শতকরা মান কত?", "B", "৩২২%", "৩১৪%", "৩.১৪%", "৩১০%"));
                add(new QuestionModule("৪ এর বর্গফল কত?", "C", "৬", "৮", "১৬", "১০"));
                add(new QuestionModule("২ এর ঘাত ৪ কত?", "B", "৮", "১৬", "১২", "২৪"));
                add(new QuestionModule("একটি বৃত্তের ব্যাসার্ধ ৫ হলে এর পরিধি কত?", "A", "৩১.৪", "৩১.৮", "৩০", "২৮"));
                add(new QuestionModule("১২ এর দ্বিগুণ কত?", "C", "১৫", "২০", "২৪", "৩০"));
                add(new QuestionModule("৫ এর ৩-এর গুণফল কত?", "C", "১০", "১২", "১৫", "২০"));
                add(new QuestionModule("৪০ এর শতকরা ৫০ কত?", "A", "২০", "১০", "৩০", "৫০"));
                add(new QuestionModule("গণিতের কোন শাখা আয়তক্ষেত্র নিয়ে কাজ করে?", "B", "বীজগণিত", "জ্যামিতি", "সংখ্যাতত্ত্ব", "পীথাগোরাস সূত্র"));
                add(new QuestionModule("দুটি সংখ্যার যোগফল ১২ হলে সংখ্যা দুটি কী হতে পারে?", "A", "৬ এবং ৬", "৫ এবং ৭", "৪ এবং ৮", "৩ এবং ৯"));
                add(new QuestionModule("একটি ত্রিভুজের উচ্চতা ১০ এবং ভিত্তি ৬ হলে এর ক্ষেত্রফল কত?", "A", "৩০", "২৫", "৬০", "৪০"));
                add(new QuestionModule("২০ এবং ২৫ এর ল.সা.গু কত?", "C", "১০", "১৫", "১০০", "৫০"));  // Correct answer: "১০০"
                add(new QuestionModule("একটি সংখ্যা কে ৪ দিয়ে গুণ করলে ফল কী হবে?", "D", "সংখ্যাটির দ্বিগুণ", "সংখ্যাটির তিন গুণ", "সংখ্যাটির পাঁচ গুণ", "সংখ্যাটির চতুর্গুণ"));
                add(new QuestionModule("একটি বৃত্তের ক্ষেত্রফল নির্ণয়ের সূত্র কী?", "B", "πr", "πr²", "r²", "২πr"));
                add(new QuestionModule("দশমিক ০.৬ কে ভগ্নাংশে প্রকাশ করলে কী হবে?", "A", "৩/৫", "২/৩", "৪/৫", "৫/৬"));
                add(new QuestionModule("একটি সংখ্যার তিন গুণ থেকে ২ বিয়োগ করলে ফল ৭ হলে সংখ্যাটি কত?", "D", "২", "৩", "৪", "৩.৫"));

            }
        };
        QuestionModule.createQuestionsForSubject("Math", R.drawable.math, questions);

        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("ইসলামের পাঁচটি স্তম্ভের প্রথমটি কী?", "A", "কালেমা", "সালাত", "জাকাত", "সিয়াম"));
                add(new QuestionModule("কুরআনের প্রথম সূরার নাম কী?", "B", "সূরা ইখলাস", "সূরা আল-ফাতিহা", "সূরা ইয়াসিন", "সূরা আন-নাস"));
                add(new QuestionModule("প্রতি দিন কতবার মুসলিমদের নামাজ পড়া ফরজ?", "C", "তিন বার", "চার বার", "পাঁচ বার", "ছয় বার"));
                add(new QuestionModule("মক্কার কোন পাহাড়ে নবী মুহাম্মদ (সা.) এর উপর প্রথম ওহী নাযিল হয়েছিল?", "D", "পাহাড় সাফা", "জাবাল আত-তুর", "জাবাল আন-নূর", "হেরা গুহায় "));
                add(new QuestionModule("হজের সময় হাজীরা কোন স্থানে আরাফাত দিবসে অবস্থান করেন?", "A", "আরাফাত ময়দান", "মিনা", "মুজদালিফা", "কাবা শরীফ"));
                add(new QuestionModule("ইসলামে কত বছর বয়স হলে রোজা ফরজ হয়?", "B", "১০", "১২", "১৩", "১৫"));
                add(new QuestionModule("কুরআনের মধ্যে মোট কতটি সূরা আছে?", "C", "১১২", "১১৩", "১১৪", "১১৫"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর জন্মস্থান কোথায়?", "D", "মদিনা", "তাইফ", "যুদ্ধের সময় মদিনা", "মক্কা"));
                add(new QuestionModule("মুসলমানদের পবিত্র দিনের নাম কী?", "A", "শুক্রবার", "শনিবার", "রবিবার", "সোমবার"));
                add(new QuestionModule("ইসলামের দ্বিতীয় খলিফা কে ছিলেন?", "B", "আবু বকর (রাঃ)", "উমর ইবনে খাত্তাব (রাঃ)", "উসমান ইবনে আফফান (রাঃ)", "আলী ইবনে আবু তালিব (রাঃ)"));
                add(new QuestionModule("আল্লাহর নবী মুহাম্মদ (সা.) কত বছর বয়সে নবুওয়াত পেয়েছিলেন?", "C", "৩০ বছর", "৩৫ বছর", "৪০ বছর", "৪৫ বছর"));
                add(new QuestionModule("ইসলাম ধর্মের প্রধান গ্রন্থ কোনটি?", "D", "তাওরাত", "ইঞ্জিল", "যবুর", "কুরআন"));
                add(new QuestionModule("কত সালে হিজরি বর্ষ শুরু হয়েছিল?", "A", "৬৩৮  খ্রিস্টাব্দ", "৬১০ খ্রিস্টাব্দ", "৫৭০ খ্রিস্টাব্দ", "৬৩২ খ্রিস্টাব্দ"));
                add(new QuestionModule("ইসলামে কোন মাসে রোজা পালন করা হয়?", "B", "মহররম", "রমজান", "জিলহজ", "শাবান"));
                add(new QuestionModule("মুসলমানদের পবিত্র শহর কোনটি?", "C", "জেরুজালেম", "তাইফ", "মক্কা", "দামেস্ক"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর মিরাজের ঘটনা কোথায় ঘটেছিল?", "D", "তাইফ", "মদিনা", "বাইতুল মুকাদ্দাস", "মক্কা"));
                add(new QuestionModule("কুরআনের ভাষা কোনটি?", "A", "আরবি", "ফার্সি", "উর্দু", "তুর্কি"));
                add(new QuestionModule("আল্লাহর ৯৯টি গুণবাচক নামের আরেকটি নাম কী?", "B", "আল-খালিক", "আল-রহমান", "আল-মালিক", "আল-আলিম"));
                add(new QuestionModule("ইসলামি ক্যালেন্ডারের প্রথম মাস কোনটি?", "C", "রবিউল আউয়াল", "রমজান", "মহররম", "জিলহজ"));
                add(new QuestionModule("যাকাত দেওয়া ফরজ কেন?", "D", "সম্পদশালী হওয়ার জন্য", "সমাজের আর্থিক ভারসাম্য রক্ষার জন্য", "দারিদ্র্য বিমোচনের জন্য", "আল্লাহর সন্তুষ্টির জন্য"));
                add(new QuestionModule("কত বছর পর মুসলমানরা মক্কা পুনরুদ্ধার করেছিল?", "A", "৮বছর", "১০ বছর", "১২ বছর", "১৪ বছর"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর মক্কা থেকে মদিনায় হিজরতকে কী বলে?", "B", "হজ", "হিজরত", "তাওবাহ", "ইস্তিখারা"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর মদিনায় আগমনের পর কোন মসজিদ প্রতিষ্ঠা করেছিলেন?", "C", "মসজিদ আল-হারাম", "মসজিদ আল-আকসা", "মসজিদে নববী", "মসজিদে কুবা"));
                add(new QuestionModule("আল্লাহর রাসূল মুহাম্মদ (সা.) এর চাচার নাম কী?", "D", "আবু সুফিয়ান", "আবু লাহাব", "আবু তালিব", "আব্বাস ইবনে আব্দুল মুত্তালিব"));
                add(new QuestionModule("কুরআনে মোট কতটি পারা আছে?", "A", "৩০", "৪০", "২০", "১০"));
                add(new QuestionModule("জুমার নামাজের খুতবা কোন ভাষায় দেওয়া হয়?", "B", "হিব্রু", "আরবি", "উর্দু", "ফার্সি"));
                add(new QuestionModule("মুসলিমদের বছরে কতবার যাকাত দিতে হয়?", "A", "একবার", "দুইবার", "তিনবার", "চারবার"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর প্রথম স্ত্রী কে ছিলেন?", "D", "আয়েশা (রাঃ)", "হাফসা (রাঃ)", "যয়নাব (রাঃ)", "খাদিজা (রাঃ)"));
                add(new QuestionModule("ইসলামে কোন রাতে শবে কদর পালন করা হয়?", "A", "রমজানের ২৭তম রাত", "মহররমের ১০ম দিন", "শাবানের ১৫তম রাত", "জিলহজের ৯ম দিন"));
                add(new QuestionModule("কোন কাজটি ইসলামে হারাম?", "B", "দান করা", "সুদ খাওয়া", "রোজা রাখা", "নামাজ পড়া"));
                add(new QuestionModule("ইসলামের দ্বিতীয় স্তম্ভ কী?", "B", "কালেমা", "সালাত", "যাকাত", "সিয়াম"));
                add(new QuestionModule("কুরআনের সবচেয়ে ছোট সূরা কোনটি?", "A", "সূরা আল-কাওসার", "সূরা আল-ফাতিহা", "সূরা ইখলাস", "সূরা আল-আস্‌র"));
                add(new QuestionModule("হজের সময় কোন স্থানে শয়তানকে পাথর মারা হয়?", "C", "কাবা শরীফ", "মিনা", "জামারাত", "মুযদালিফা"));
                add(new QuestionModule("রমজান মাসে কোন রাতকে 'হাজার মাসের চেয়েও উত্তম' বলা হয়?", "B", "শবে মিরাজ", "শবে বরাত", "শবে মওলিদ", "শবে কদর"));
                add(new QuestionModule("ইসলামে কিসের ওপর জাকাত দেওয়া হয়?", "B", "নামাজের ওপর", "সম্পদের ওপর", "রোজার ওপর", "কুরবানির ওপর"));
                add(new QuestionModule("মুসলিমদের পবিত্র গ্রন্থের নাম কী?", "D", "তাওরাত", "ইঞ্জিল", "যবুর", "কুরআন"));
                add(new QuestionModule("হজ পালন করা ইসলামের কত নম্বর স্তম্ভ?", "C", "প্রথম", "তৃতীয়", "পঞ্চম", "চতুর্থ"));
                add(new QuestionModule("ইসলামের কোন স্তম্ভটি মুসলমানদের জন্য বছরে একবার ফরজ?", "D", "যাকাত", "রোজা", "সালাত", "হজ"));
                add(new QuestionModule("জাকাত কী পরিমাণ সম্পদের ওপর ফরজ?", "A", "২.৫%", "৫%", "১০%", "৭.৫%"));
                add(new QuestionModule("কতবার কুরআনে বিসমিল্লাহির রাহমানির রাহিম বলা হয়েছে?", "C", "১১৩", "১১৫", "১১৪", "১১৬"));
                add(new QuestionModule("কোনটি ইসলামের প্রধান উৎস?", "D", "হাদিস", "ইজমা", "কিয়াস", "কুরআন"));
                add(new QuestionModule("ইসলামে নারীদের জন্য হিজাব পরা ফরজ কেন?", "B", "সম্মান বজায় রাখার জন্য", "শালীনতা বজায় রাখার জন্য", "শরীর ঢাকার জন্য", "পুরুষদের আকর্ষণ এড়ানোর জন্য"));
                add(new QuestionModule("মুসা (আ.) কোন জাতির নবী ছিলেন?", "A", "বনি ইসরাইল", "কুরাইশ", "আনসার", "মুহাজির"));
                add(new QuestionModule("মদিনায় নবী মুহাম্মদ (সা.) কোন চুক্তি করেছিলেন?", "D", "হুদাইবিয়ার চুক্তি", "মক্কার চুক্তি", "তাইফের চুক্তি", "মদিনার সনদ"));
                add(new QuestionModule("কোরবানি কখন পালন করা হয়?", "C", "রমজানের শেষে", "মহররমের ১০ তারিখে", "ঈদুল আজহার সময়", "শবেকদরের সময়"));
                add(new QuestionModule("মুসলিমদের প্রতি সপ্তাহে বিশেষ কোন দিন জুমার নামাজ পড়তে হয়?", "B", "রবিবার", "শুক্রবার", "শনিবার", "বৃহস্পতিবার"));
                add(new QuestionModule("মুসলমানদের সিয়াম পালন করার প্রধান উদ্দেশ্য কী?", "A", "আত্মসংযম ও আল্লাহর কাছে সমর্পণ", "শরীরের বিশ্রাম", "অর্থনৈতিক সহায়তা", "খাবারের অপচয় রোধ"));
                add(new QuestionModule("নবী ইব্রাহিম (আ.)-এর প্রথম স্ত্রীর নাম কী?", "C", "মারইয়াম", "খাদিজা", "সারা", "হাজেরা"));
                add(new QuestionModule("মুসলিমদের জন্য নামাজ পড়া ফরজ কেন?", "B", "শারীরিক ফিটনেসের জন্য", "আল্লাহর সঙ্গে সম্পর্ক বজায় রাখার জন্য", "দারিদ্র্য বিমোচনের জন্য", "মানুষের সঙ্গে সম্পর্ক রক্ষার জন্য"));
                add(new QuestionModule("নবী ঈসা (আ.) কোন ধর্মের প্রবর্তক ছিলেন?", "D", "ইসলাম", "ইহুদি", "মজুসী", "খ্রিস্টান"));
                add(new QuestionModule("কুরআনের প্রথম আয়াত কোথায় নাযিল হয়েছিল?", "A", "হেরা গুহায়", "মদিনায়", "মক্কায়", "তাইফে"));
                add(new QuestionModule("ইসলামে কোরবানির পশুর বয়স কমপক্ষে কত হওয়া চাই?", "B", "১ বছর", "২ বছর", "৩ বছর", "৬ মাস"));
                add(new QuestionModule("জাকাত কাকে দেওয়া উচিত?", "C", "ধনীকে", "মধ্যবিত্তকে", "গরিব ও মিসকিনদের", "যুদ্ধবন্দিদের"));
                add(new QuestionModule("কোন মাসে হজ পালিত হয়?", "D", "মহররম", "রমজান", "শাবান", "জিলহজ"));
                add(new QuestionModule("ইসলামে রাতের সালাতকে কী বলা হয়?", "A", "তাহাজ্জুদ", "ইশা", "ফজর", "তরাবীহ"));
                add(new QuestionModule("কুরআনের সর্বশেষ সূরা কোনটি?", "C", "সূরা আল-কাওসার", "সূরা আল-আলাক", "সূরা আন-নাস", "সূরা আল-ইখলাস"));
                add(new QuestionModule("ইসলামে মোহাম্মদ (সা.)-এর মক্কা বিজয়ের বছর কোনটি?", "B", "হিজরি ৬", "হিজরি ৮", "হিজরি ১০", "হিজরি ৯"));
                add(new QuestionModule("কাবার চারপাশে কতবার তাওয়াফ করা হয়?", "D", "৫ বার", "৬ বার", "৭ বার", "৮ বার"));
                add(new QuestionModule("রোজা ভাঙার জন্য কোন জিনিস প্রথমে খাওয়া হয়?", "A", "খেজুর", "জল", "দুধ", "মধু"));
                add(new QuestionModule("মদিনায় ইসলাম প্রচারকারীদের কী বলা হয়?", "B", "মুহাজির", "আনসার", "কুরাইশ", "আউস"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর মিরাজের সময় সিদরাতুল মুনতাহা কোথায় অবস্থিত?", "C", "জমিনে", "মক্কায়", "সপ্তম আসমানে", "জান্নাতে"));
                add(new QuestionModule("ইসলামে দানশীলতা কোন নামে পরিচিত?", "A", "সাদাকা", "ফিতরা", "ইসলাহ", "হাওলা"));
                add(new QuestionModule("মক্কা কোন দেশের শহর?", "B", "ইরাক", "সৌদি আরব", "ইরান", "জর্ডান"));
                add(new QuestionModule("মুসা (আ.) এর সময়ে ফেরাউনের রাজত্ব ছিল কোথায়?", "D", "সৌদি আরবে", "ইরাকে", "ইরানে", "মিসরে"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর কোন পুত্র শিশু অবস্থায় মারা যান?", "C", "আবদুল্লাহ", "ইবরাহিম", "কাসেম", "আল-হাসান"));
                add(new QuestionModule("ইসলামের তৃতীয় খলিফা কে ছিলেন?", "A", "উসমান ইবনে আফফান (রাঃ)", "উমর ইবনে খাত্তাব (রাঃ)", "আবু বকর (রাঃ)", "আলী ইবনে আবু তালিব (রাঃ)"));
                add(new QuestionModule("কোন আয়াতে কুরআন পাঠ করার কথা প্রথম নাযিল হয়?", "B", "সূরা ফাতিহা", "সূরা আল-আলাক", "সূরা ইখলাস", "সূরা কাওসার"));
                add(new QuestionModule("নবী ঈসা (আ.)-কে কীভাবে আকাশে তোলা হয়েছিল?", "D", "স্বাভাবিক মৃত্যু", "যুদ্ধের সময়", "তাঁর অনুসারীরা তাঁকে উঠিয়েছিলেন", "আল্লাহর নির্দেশে সরাসরি আকাশে তোলা হয়"));
                add(new QuestionModule("কোন পদ্ধতিতে মক্কা থেকে মদিনায় হিজরত করেছিলেন নবী মুহাম্মদ (সা.)?", "B", "পায়ে হেঁটে", "উটের পিঠে", "ঘোড়ার পিঠে", "পানি পথে"));
                add(new QuestionModule("জুমার নামাজে ইমামের পর মুসল্লীরা কাকে অনুসরণ করেন?", "A", "ইমাম", "মুয়াজ্জিন", "খতিব", "কাজী"));
                add(new QuestionModule("মুসলমানদের সবচেয়ে দীর্ঘ রোজার মাস কোনটি?", "C", "মহররম", "শাবান", "রমজান", "জিলহজ"));
                add(new QuestionModule("ইসলামে কোন মাসে শবেবরাত পালিত হয়?", "D", "রজব", "মহররম", "জিলহজ", "শাবান"));
                add(new QuestionModule("কুরআনে কোন নবীর জীবনের ঘটনা সবচেয়ে বেশি উল্লেখ করা হয়েছে?", "B", "নবী ঈসা (আ.)", "নবী মুসা (আ.)", "নবী ইবরাহিম (আ.)", "নবী ইউসুফ (আ.)"));
                add(new QuestionModule("ইসলামে ঈদের নামাজের পর যে বিশেষ দোয়া করা হয় তাকে কী বলে?", "A", "তাকবির", "তাহলিল", "তাসবিহ", "তাহাজ্জুদ"));
                add(new QuestionModule("জাকাতুল ফিতর কী?", "C", "রমজানের শেষে নামাজ", "রমজানের শেষ রোজা", "ফিতরা দেওয়ার প্রথা", "কুরবানির আগে দান"));
                add(new QuestionModule("কুরআনের কোন সূরাতে সূরা ফাতিহার নাম উল্লেখ আছে?", "D", "সূরা বাকারাহ", "সূরা নূর", "সূরা আল-ইখলাস", "কোনটিতে নেই"));
                add(new QuestionModule("কুরআনের কোন সূরা দুইবার নাযিল হয়েছে?", "B", "সূরা আল-আস্‌র", "সূরা আল-ফাতিহা", "সূরা আল-ইখলাস", "সূরা কাওসার"));
                add(new QuestionModule("নবী ইবরাহিম (আ.) আল্লাহর আদেশে কোন প্রিয় সন্তানকে কোরবানি করতে গিয়েছিলেন?", "A", "ইসমাইল (আ.)", "ইসহাক (আ.)", "ইয়াকুব (আ.)", "ইউসুফ (আ.)"));
                add(new QuestionModule("আল্লাহর ৯৯টি গুণবাচক নামের মধ্যে কোন নামটি 'সর্বজ্ঞাতা' বোঝায়?", "C", "আল-হাকিম", "আল-খালিক", "আল-আলিম", "আল-রহিম"));
                add(new QuestionModule("মুসলিমদের কোন কাজের মাধ্যমে জীবন শুরু হয়?", "D", "নামাজ", "রোজা", "হজ", "কালেমা পাঠ"));
                add(new QuestionModule("ইসলামে কোন মাসে কোরবানির পশু জবাই করা হয়?", "C", "রমজান", "শাবান", "জিলহজ", "মহররম"));
                add(new QuestionModule("কোন ঘোড়ার পিঠে চড়ে নবী মুহাম্মদ (সা.) মিরাজে গিয়েছিলেন?", "B", "ইসরাফিল", "বোরাক", "জিবরাইল", "আজরাইল"));
                add(new QuestionModule("ইসলামে কোন সময়ে তাওবাহ করা হয়?", "A", "পাপ করার পর", "নামাজ পড়ার আগে", "রোজা রাখার পরে", "হজ পালনের পরে"));
                add(new QuestionModule("কোন নবীকে 'খলিলুল্লাহ' বলা হয়?", "C", "নবী মুসা (আ.)", "নবী ঈসা (আ.)", "নবী ইবরাহিম (আ.)", "নবী ইউসুফ (আ.)"));
                add(new QuestionModule("হিজরি বর্ষপঞ্জিতে কোন বছর মক্কা বিজয় হয়েছিল?", "B", "হিজরি ৬", "হিজরি ৮", "হিজরি ১০", "হিজরি ৯"));
                add(new QuestionModule("কুরআনের কোন সূরায় নারীদের অধিকার নিয়ে আলোচনা করা হয়েছে?", "A", "সূরা আন-নিসা", "সূরা আল-মায়েদা", "সূরা আল-বাকারা", "সূরা ইউসুফ"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) কোন মাসে জন্মগ্রহণ করেছিলেন?", "C", "মহররম", "রজব", "রবিউল আউয়াল", "শাবান"));
                add(new QuestionModule("ইসলামে কোন মাসের রোজা রাখা মুসলমানদের জন্য ফরজ?", "D", "মহররম", "রজব", "জিলকদ", "রমজান"));
                add(new QuestionModule("ইসলামের তৃতীয় স্তম্ভ কোনটি?", "B", "কালেমা", "যাকাত", "সালাত", "হজ"));
                add(new QuestionModule("মুসলিমদের সাপ্তাহিক সবচেয়ে গুরুত্বপূর্ণ নামাজ কোনটি?", "A", "জুমার নামাজ", "ফজরের নামাজ", "ইশার নামাজ", "তাহাজ্জুদের নামাজ"));
                add(new QuestionModule("কোনটি ঈদুল আজহার প্রধান উদ্দেশ্য?", "C", "দানের মাধ্যমে আল্লাহর সন্তুষ্টি অর্জন", "রোজা রাখা", "কোরবানি করা", "নামাজ পড়া"));
                add(new QuestionModule("কোন নবীকে কুরআনে 'রুহুল্লাহ' বলা হয়েছে?", "B", "নবী মুসা (আ.)", "নবী ঈসা (আ.)", "নবী ইবরাহিম (আ.)", "নবী ইউসুফ (আ.)"));
                add(new QuestionModule("ইসলামে কোন পাত্রে মদ পান করা নিষিদ্ধ?", "A", "সোনা ও রুপার পাত্র", "কাঠের পাত্র", "মাটির পাত্র", "লৌহের পাত্র"));
                add(new QuestionModule("নবী দাউদ (আ.) কোন দেশের রাজা ছিলেন?", "A", "ইসরাইল", "মিসর", "ইরাক", "সৌদি আরব"));
                add(new QuestionModule("কোন হাদিস সংকলনটি ইসলামে সবচেয়ে বিশুদ্ধ বলে গণ্য করা হয়?", "C", "সুনান ইবনে মাজাহ", "সুনান আত-তিরমিজি", "সহিহ বুখারি", "মুসনাদ আহমাদ"));
                add(new QuestionModule("কোনটি ইসলামের প্রথম যুদ্ধ?", "B", "যুদ্ধ-উহুদ", "যুদ্ধ-বদর", "যুদ্ধ-খন্দক", "যুদ্ধ-হুনাইন"));
                add(new QuestionModule("নবী মুহাম্মদ (সা.) এর পিতার নাম কী?", "A", "আবদুল্লাহ", "আবু তালিব", "আবদুল মুত্তালিব", "আবু সুফিয়ান"));
                add(new QuestionModule("কোন নবীকে আল্লাহ তায়ালা সরাসরি কথা বলেছেন?", "C", "নবী ইবরাহিম (আ.)", "নবী ঈসা (আ.)", "নবী মুসা (আ.)", "নবী নূহ (আ.)"));
                add(new QuestionModule("ইসলামের কোন মাসে বিশেষ গুরুত্ব দিয়ে লাইলাতুল কদর পালন করা হয়?", "D", "মহররম", "শাবান", "রজব", "রমজান"));

            }
        };
        QuestionModule.createQuestionsForSubject("Islam", R.drawable.islam, questions);



        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("পাইথনে কোন ডেটা টাইপটি পরিবর্তনশীল নয়?", "A", "tuple", "list", "dictionary", "set"));
                add(new QuestionModule("পাইথনে কোনটি লুপ ব্যবহারের জন্য নয়?", "C", "for", "while", "loop", "break"));
                add(new QuestionModule("পাইথনের কোন কিওয়ার্ডটি ফাংশন সংজ্ঞায়িত করতে ব্যবহৃত হয়?", "B", "class", "def", "return", "lambda"));
                add(new QuestionModule("পাইথনে কোনটি একটি বৈধ ভেরিয়েবল নাম নয়?", "D", "my_var", "_num", "num_2", "2num"));
                add(new QuestionModule("পাইথনে কোন মডিউলটি এলোমেলো সংখ্যা তৈরি করতে ব্যবহৃত হয়?", "A", "random", "math", "time", "os"));
                add(new QuestionModule("পাইথনের কোনটি একটি অবজেক্ট ওরিয়েন্টেড ফিচার?", "B", "লুপিং", "ইনহেরিটেন্স", "কন্ডিশনাল", "ভেরিয়েবল ডিক্লারেশন"));
                add(new QuestionModule("পাইথনের কোন ফাংশনটি ব্যবহারকারীর ইনপুট গ্রহণ করতে ব্যবহৃত হয়?", "C", "print()", "len()", "input()", "type()"));
                add(new QuestionModule("পাইথনের কোনটি ফাইল পড়তে ব্যবহৃত মোড নয়?", "D", "'r'", "'rb'", "'r+'", "'rw'"));
                add(new QuestionModule("পাইথনে কোনটি একটি ইন্টারপ্রেটার?", "B", "C", "Python", "Java", "PHP"));
                add(new QuestionModule("পাইথনে কোনটি আসল সংখ্যা নয়?", "C", "int", "float", "char", "complex"));
                add(new QuestionModule("পাইথনে কোনটি একটি সেট ডেটা টাইপ নয়?", "A", "list", "set", "frozenset", "dictionary"));
                add(new QuestionModule("পাইথনে একটি ভেরিয়েবলের টাইপ জানতে কোন ফাংশনটি ব্যবহৃত হয়?", "D", "print()", "str()", "len()", "type()"));
                add(new QuestionModule("পাইথনের কোন বিল্ট-ইন ফাংশনটি সর্বনিম্ন মান দেয়?", "B", "max()", "min()", "sum()", "count()"));
                add(new QuestionModule("পাইথনের কোনটি প্রাথমিকভাবে ব্রাউজার ভিত্তিক নয়?", "C", "Django", "Flask", "Tkinter", "Bottle"));
                add(new QuestionModule("পাইথনে ফাংশনের ভেতরে কোনো ভেরিয়েবলকে গ্লোবাল করার জন্য কোন কীওয়ার্ডটি ব্যবহৃত হয়?", "A", "global", "nonlocal", "def", "return"));
                add(new QuestionModule("পাইথনে কোনটি একটি লিস্টের মেথড নয়?", "D", "append()", "extend()", "insert()", "find()"));
                add(new QuestionModule("পাইথনে একটি স্ট্রিংয়ে সব অক্ষর ছোট করতে কোন মেথডটি ব্যবহৃত হয়?", "B", "upper()", "lower()", "capitalize()", "title()"));
                add(new QuestionModule("পাইথনে একটি ক্লাসের উদাহরণ তৈরি করতে কোন কিওয়ার্ডটি ব্যবহৃত হয়?", "A", "class", "new", "init", "super"));
                add(new QuestionModule("পাইথনে কোন কিওয়ার্ডটি এরর হ্যান্ডলিংয়ের জন্য ব্যবহৃত হয়?", "C", "return", "finally", "try", "else"));
                add(new QuestionModule("পাইথনে ডেটা টাইপ পরিবর্তনের প্রক্রিয়াটিকে কী বলা হয়?", "B", "টাইপিং", "টাইপ কাস্টিং", "কনভার্সন", "কাস্টিং"));
                add(new QuestionModule("পাইথনে '==' এবং 'is' এর মধ্যে পার্থক্য কী?", "A", "'==' মানের তুলনা করে, 'is' অবজেক্টের আইডেন্টিটির তুলনা করে", "'==' আইডেন্টিটির তুলনা করে, 'is' মানের তুলনা করে", "'==' এবং 'is' সমান", "দুটি একই কাজ করে"));
                add(new QuestionModule("পাইথনের কোনটি একটি আরিথমেটিক অপারেটর নয়?", "D", "+", "-", "*", "and"));
                add(new QuestionModule("পাইথনে ডিকশনারিতে কোন মেথডটি আইটেম মুছে ফেলার জন্য ব্যবহৃত হয়?", "A", "pop()", "delete()", "remove()", "clear()"));
                add(new QuestionModule("পাইথনের কোন স্টেটমেন্টটি একাধিক শর্ত পরীক্ষা করতে ব্যবহৃত হয়?", "C", "if", "else", "elif", "switch"));
                add(new QuestionModule("পাইথনে রিকারশন কি?", "B", "একটি ফাংশনের পুনরাবৃত্তি", "একটি ফাংশনের নিজেকে কল করা", "একটি লুপের পুনরাবৃত্তি", "একাধিক ফাংশনের কল করা"));
                add(new QuestionModule("পাইথনে কোনটি এড়িয়ে যাওয়া স্টেটমেন্ট?", "C", "pass", "break", "continue", "return"));
                add(new QuestionModule("পাইথনে একটি তালিকার শেষ আইটেম যোগ করতে কোন মেথডটি ব্যবহৃত হয়?", "A", "append()", "add()", "insert()", "update()"));
                add(new QuestionModule("পাইথনে কোন লুপটি নির্দিষ্ট শর্ত পর্যন্ত চালানো হয়?", "B", "for", "while", "do-while", "loop"));
                add(new QuestionModule("পাইথনে একটি মডিউল ইনস্টল করতে কোন কমান্ডটি ব্যবহৃত হয়?", "B", "import", "pip", "install", "setup"));
                add(new QuestionModule("পাইথনে কোন ফাংশনটি একটি লিস্টের দৈর্ঘ্য দেয়?", "A", "len()", "size()", "length()", "count()"));
                add(new QuestionModule("পাইথনে একটি 'for' লুপের ভেতরে কোন স্টেটমেন্টটি লুপ সম্পূর্ণ হওয়ার আগে থামায়?", "A", "break", "pass", "continue", "return"));
                add(new QuestionModule("পাইথনে ডিকশনারির কী-এর মান বের করতে কোন মেথডটি ব্যবহৃত হয়?", "A", "get()", "keys()", "values()", "items()"));
                add(new QuestionModule("পাইথনে একটি ফাংশনে ডিফল্ট মান সেট করতে কোনটি ব্যবহৃত হয়?", "D", "optional", "set", "def", "parameter"));
                add(new QuestionModule("পাইথনে একটি লিস্ট থেকে ডুপ্লিকেট সরাতে কোনটি ব্যবহৃত হয়?", "B", "tuple()", "set()", "list()", "dict()"));
                add(new QuestionModule("পাইথনে কোন ডেটা স্ট্রাকচারটি কী এবং মানের জোড়া সংরক্ষণ করে?", "D", "list", "tuple", "set", "dictionary"));
                add(new QuestionModule("পাইথনে কোনটি স্ট্রিং মেথড নয়?", "C", "strip()", "find()", "extend()", "replace()"));
                add(new QuestionModule("পাইথনে লিস্টকে সর্ট করতে কোন মেথডটি ব্যবহৃত হয়?", "A", "sort()", "sorted()", "order()", "arrange()"));
                add(new QuestionModule("পাইথনে '==' এবং '!=' অপারেটরগুলি কী পরীক্ষা করে?", "C", "আইডেন্টিটি", "টাইপ", "সমতা ও অসমতা", "অপারেশন"));
                add(new QuestionModule("পাইথনে কোনটি ফাংশনের আউটপুট নয়?", "B", "print()", "function()", "len()", "input()"));
                add(new QuestionModule("পাইথনে একটি কমা-আলাদা মানের ফাইল (CSV) খুলতে কোন মডিউলটি ব্যবহৃত হয়?", "A", "csv", "pandas", "json", "fileinput"));
                add(new QuestionModule("পাইথনে এনক্রিপশন ও ডিক্রিপশনের জন্য কোন মডিউলটি ব্যবহৃত হয়?", "C", "math", "random", "cryptography", "os"));
                add(new QuestionModule("পাইথনে কোনটি একটি ইনডেক্সড কালেকশন নয়?", "D", "list", "tuple", "string", "set"));
                add(new QuestionModule("পাইথনে কোন মেথডটি একটি ইটারেটর রিটার্ন করে?", "A", "iter()", "next()", "start()", "go()"));
                add(new QuestionModule("পাইথনে কোন স্টেটমেন্টটি একটি এক্সসেপশন তৈরি করতে ব্যবহৃত হয়?", "C", "catch", "try", "raise", "except"));
                add(new QuestionModule("পাইথনে কোনটি একটি লজিক্যাল অপারেটর নয়?", "A", "==", "and", "or", "not"));
                add(new QuestionModule("পাইথনে মেমরি ম্যানেজমেন্ট কোনটি পরিচালনা করে?", "C", "ক্লাস", "লুপ", "গারবেজ কালেক্টর", "ডেটা টাইপ"));
                add(new QuestionModule("পাইথনে কোন কিওয়ার্ডটি ফাংশনের আউটপুট ফেরত দিতে ব্যবহৃত হয়?", "A", "return", "yield", "output", "send"));
                add(new QuestionModule("পাইথনে কোনটি ইমপোর্ট করার জন্য ব্যবহার করা হয়?", "A", "import", "include", "require", "using"));
                add(new QuestionModule("পাইথনে কোন ডেটা টাইপটি সংখ্যা ও দশমিক উভয়ই ধারণ করতে পারে?", "B", "int", "float", "complex", "decimal"));
                add(new QuestionModule("পাইথনে কোন অপারেটরটি একই সময়ে মান ও স্থানীয়ভাবে তুলনা করে?", "D", "===", "==", "<=", "is"));
                add(new QuestionModule("পাইথনে একটি নতুন ক্লাস তৈরি করতে কোন কিওয়ার্ডটি ব্যবহার হয়?", "A", "class", "def", "new", "static"));
                add(new QuestionModule("পাইথনে কোন ফাংশনটি একটি লিস্ট থেকে সর্বাধিক মান নির্ধারণ করে?", "A", "max()", "min()", "sum()", "count()"));
                add(new QuestionModule("পাইথনে একটি ভেরিয়েবলে স্ট্রিং এবং সংখ্যা উভয়ই রাখতে কোনটি ব্যবহার হয়?", "C", "float", "int", "str", "mix"));
                add(new QuestionModule("পাইথনে কোন কিওয়ার্ডটি ক্লাসের ইনহেরিটেন্স নির্দেশ করে?", "A", "class", "inherits", "extends", "super"));
                add(new QuestionModule("পাইথনে কোনটি ডেটা স্ট্রাকচার নয়?", "D", "dictionary", "set", "string", "table"));
                add(new QuestionModule("পাইথনে কোনটি লুপের জন্য সঠিক?", "B", "loop: for", "for: loop", "loop: while", "while: loop"));
                add(new QuestionModule("পাইথনে কোন মেথডটি একটি স্ট্রিংয়ের শেষে সাদা স্থান মুছে ফেলে?", "C", "strip()", "lstrip()", "rstrip()", "trim()"));
                add(new QuestionModule("পাইথনে কোনটি একটি সঠিক ডিকশনারি?", "A", "{'key': 'value'}", "{'key'; 'value'}", "['key', 'value']", "{key: value}"));
                add(new QuestionModule("পাইথনে একটি ডাটা টাইপ বদলাতে কোনটি ব্যবহার করা হয়?", "C", "change", "convert", "cast", "modify"));
                add(new QuestionModule("পাইথনে কোন অপারেটরটি বুলিয়ান মান ফেরত দেয়?", "B", "and", "or", "not", "xor"));
                add(new QuestionModule("পাইথনে একটি লিস্টের শেষ থেকে একটি আইটেম মুছতে কোন মেথডটি ব্যবহৃত হয়?", "A", "pop()", "remove()", "delete()", "clear()"));
                add(new QuestionModule("পাইথনে একটি ফাংশনের জন্য ডিফল্ট আর্গুমেন্ট কীভাবে সেট করা হয়?", "A", "def func(arg=default)", "func(arg=default)", "def func(arg)", "func(arg)"));
                add(new QuestionModule("পাইথনে কোনটি এলোমেলো সংখ্যা তৈরি করার জন্য ব্যবহার হয়?", "A", "random()", "choice()", "randint()", "sample()"));
                add(new QuestionModule("পাইথনে কোন ফাংশনটি সমস্ত আইটেম একটি লিস্টে যোগ করে?", "C", "join()", "add()", "sum()", "combine()"));
                add(new QuestionModule("পাইথনে কোনটি একটি গণনা করার জন্য ব্যবহার করা হয়?", "A", "count()", "sum()", "avg()", "total()"));
                add(new QuestionModule("পাইথনে কোন কিওয়ার্ডটি ডিকশনারিতে একটি নতুন আইটেম যোগ করতে ব্যবহৃত হয়?", "A", "update()", "add()", "insert()", "append()"));
                add(new QuestionModule("পাইথনে কোনটি ইনপুট পাওয়ার জন্য ব্যবহৃত হয়?", "B", "read()", "input()", "get()", "scan()"));
                add(new QuestionModule("পাইথনে একটি ইন্টারপ্রেটার কি?", "C", "কোড রান করার প্রক্রিয়া", "কোড লেখার প্রক্রিয়া", "কোড ডিবাগ করার প্রক্রিয়া", "কোড বিশ্লেষণের প্রক্রিয়া"));
                add(new QuestionModule("পাইথনে কোনটি একটি সঠিক ফাংশন ডিক্লারেশন?", "A", "def func():", "function def:", "def: func", "function():"));
                add(new QuestionModule("পাইথনে কোন মেথডটি একটি টাপল তৈরি করে?", "A", "tuple()", "set()", "list()", "dict()"));
                add(new QuestionModule("পাইথনে কোন কিওয়ার্ডটি লুপ থেকে বেরিয়ে আসতে ব্যবহৃত হয়?", "C", "continue", "exit", "break", "return"));
                add(new QuestionModule("পাইথনে একটি স্ট্রিংয়ের দৈর্ঘ্য বের করতে কোন ফাংশনটি ব্যবহৃত হয়?", "A", "len()", "length()", "size()", "count()"));
                add(new QuestionModule("পাইথনে একটি সারি (array) তৈরি করতে কোনটি ব্যবহার করা হয়?", "A", "list()", "array()", "collection()", "set()"));
                add(new QuestionModule("পাইথনে ডেটা সংগ্রহের জন্য কোনটি সঠিক?", "A", "list", "variable", "object", "data"));
                add(new QuestionModule("পাইথনে কোনটি একটি ফর লুপের সঠিক গঠন?", "A", "for i in range(10):", "for i: range(10)", "for in range(10):", "for range(10): i"));
                add(new QuestionModule("পাইথনে কোনটি একটি স্ট্রিংয়ের সব অক্ষর বড় করতে ব্যবহৃত হয়?", "B", "capitalize()", "upper()", "lower()", "title()"));
                add(new QuestionModule("পাইথনে কোন ফাংশনটি একটি লিস্টের আইটেমগুলি ফিরিয়ে দেয়?", "A", "list()", "values()", "return()", "items()"));
                add(new QuestionModule("পাইথনে কোন অপারেটরটি একটি স্ট্রিংয়ের সাথে যোগ করতে ব্যবহৃত হয়?", "A", "+", "-", "*", "/"));
                add(new QuestionModule("পাইথনে কোনটি একটি সঠিক কোড ব্লক?", "B", "if x > 0:\n    print(x)", "if x > 0:\nprint(x)", "if x > 0: print(x)", "if(x > 0): {print(x)}"));
                add(new QuestionModule("পাইথনে কোনটি একটি ইনপুট সিস্টেমের কিপ্যাড নয়?", "C", "get()", "input()", "keypad()", "read()"));
                add(new QuestionModule("পাইথনে কোনটি অবজেক্ট ইনহেরিটেন্সের জন্য ব্যবহৃত হয়?", "C", "class", "def", "object", "function"));
                add(new QuestionModule("পাইথনে কোনটি ভুলভাবে ব্যবহৃত হয়?", "A", "def 1function():", "def function_1():", "def function1():", "def function():"));
                add(new QuestionModule("পাইথনে কোনটি একটি ইটারেটর?", "C", "iter()", "next()", "zip()", "map()"));
                add(new QuestionModule("পাইথনে একটি লিস্টে কোন ফাংশনটি ব্যবহার করা হয়?", "D", "map()", "filter()", "list()", "range()"));
                add(new QuestionModule("পাইথনে কোনটি ডিকশনারি এর একটি বৈশিষ্ট্য নয়?", "A", "Ordered", "Mutable", "Dynamic", "Key-value pairs"));
                add(new QuestionModule("পাইথনে কিভাবে একটি টাপল তৈরি করা হয়?", "A", "()", "[]", "{}", "<>"));
                add(new QuestionModule("পাইথনে কোনটি বিল্ট-ইন ফাংশন নয়?", "D", "min()", "max()", "print()", "display()"));


            }
        };
        QuestionModule.createQuestionsForSubject("Python", R.drawable.python, questions);


        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("What is the synonym of 'happy'?", "C", "Sad", "Angry", "Joyful", "Bored"));
                add(new QuestionModule("Which word is an antonym of 'difficult'?", "B", "Hard", "Easy", "Complicated", "Challenging"));
                add(new QuestionModule("What is the past tense of 'go'?", "A", "Went", "Gone", "Going", "Go"));
                add(new QuestionModule("Which of the following is a conjunction?", "D", "Quickly", "Beautiful", "Run", "And"));
                add(new QuestionModule("What is the plural form of 'child'?", "B", "Childs", "Children", "Childes", "Childern"));
                add(new QuestionModule("What is the synonym of 'big'?", "C", "Small", "Tiny", "Large", "Little"));
                add(new QuestionModule("Which of the following is a noun?", "B", "Quickly", "Happiness", "Beautiful", "Run"));
                add(new QuestionModule("What is the opposite of 'hot'?", "B", "Warm", "Cold", "Cool", "Spicy"));
                add(new QuestionModule("Which word is a verb?", "D", "Quick", "Quickly", "Quicker", "Run"));
                add(new QuestionModule("What is the correct plural of 'mouse'?", "B", "Mouses", "Mice", "Mouses", "Micey"));
                add(new QuestionModule("Which of the following is an adverb?", "C", "Joy", "Joyful", "Quickly", "Quick"));
                add(new QuestionModule("What is the past tense of 'eat'?", "B", "Eaten", "Ate", "Eats", "Eating"));
                add(new QuestionModule("What is the superlative form of 'good'?", "C", "Goodest", "Better", "Best", "Most good"));
                add(new QuestionModule("What is the main purpose of a conjunction?", "C", "To describe", "To modify", "To connect clauses", "To replace nouns"));
                add(new QuestionModule("Which of the following sentences is correct?", "B", "She go to school.", "She goes to school.", "She going to school.", "She gone to school."));
                add(new QuestionModule("What is the correct way to say the time: 3:30?", "B", "Three and half", "Half past three", "Three thirty", "Three and thirty"));
                add(new QuestionModule("Which of the following is a determiner?", "C", "Quickly", "Run", "The", "Happiness"));
                add(new QuestionModule("What is the opposite of 'empty'?", "A", "Full", "Vacant", "Available", "Unused"));
                add(new QuestionModule("What is the meaning of 'diligent'?", "B", "Lazy", "Hardworking", "Careless", "Slow"));
                add(new QuestionModule("Which word means 'a place where books are kept'?", "A", "Library", "Store", "Classroom", "Office"));
                add(new QuestionModule("What is the past tense of 'write'?", "A", "Wrote", "Written", "Writed", "Writing"));
                add(new QuestionModule("What is the opposite of 'rich'?", "B", "Wealthy", "Poor", "Affluent", "Lavish"));
                add(new QuestionModule("Which of the following is a proper noun?", "B", "city", "London", "country", "river"));
                add(new QuestionModule("What is the meaning of 'exhilarating'?", "B", "Boring", "Exciting", "Sad", "Tiring"));
                add(new QuestionModule("Which word is a preposition?", "C", "And", "But", "Under", "So"));
                add(new QuestionModule("What is the synonym of 'difficult'?", "B", "Easy", "Hard", "Simple", "Straightforward"));
                add(new QuestionModule("What is the correct form of the sentence: 'He play soccer.'?", "B", "He playing soccer.", "He plays soccer.", "He played soccer.", "He playes soccer."));
                add(new QuestionModule("Which of the following is a collective noun?", "A", "Herd", "Dog", "Cat", "House"));
                add(new QuestionModule("What is the opposite of 'bright'?", "A", "Dark", "Light", "Shiny", "Dim"));
                add(new QuestionModule("What is the past tense of 'swim'?", "A", "Swam", "Swimmed", "Swim", "Swimming"));
                add(new QuestionModule("Which word is an interjection?", "A", "Wow", "Quickly", "Beautiful", "Happy"));
                add(new QuestionModule("What is the plural of 'leaf'?", "B", "Leafs", "Leaves", "Leafes", "Leefs"));
                add(new QuestionModule("What is the meaning of 'optimistic'?", "A", "Hopeful", "Pessimistic", "Realistic", "Negative"));
                add(new QuestionModule("Which of the following is an abstract noun?", "B", "Table", "Happiness", "Dog", "House"));
                add(new QuestionModule("What is the correct past tense of 'sing'?", "A", "Sang", "Singed", "Singing", "Sangy"));
                add(new QuestionModule("Which of the following is a homonym?", "A", "Bark", "Dog", "Tree", "Run"));
                add(new QuestionModule("What is the opposite of 'easy'?", "A", "Hard", "Simple", "Straightforward", "Clear"));
                add(new QuestionModule("Which word is a modal verb?", "A", "Can", "Run", "Quickly", "Jump"));
                add(new QuestionModule("What is the superlative form of 'bad'?", "C", "Worse", "Badest", "Worst", "Most bad"));
                add(new QuestionModule("Which sentence is grammatically correct?", "B", "They was happy.", "They are happy.", "They is happy.", "They be happy."));
                add(new QuestionModule("What is the past participle of 'go'?", "B", "Went", "Gone", "Goes", "Going"));
                add(new QuestionModule("Which of the following is a synonym for 'fast'?", "A", "Quick", "Slow", "Steady", "Cautious"));
                add(new QuestionModule("What is the meaning of 'benevolent'?", "A", "Kind", "Mean", "Selfish", "Rude"));
                add(new QuestionModule("What is the correct form of the verb: 'He (to go) to school every day.'?", "A", "Goes", "Go", "Going", "Gone"));
                add(new QuestionModule("What is the synonym of 'difficult'?", "A", "Challenging", "Simple", "Straightforward", "Easy"));
                add(new QuestionModule("What is the plural form of 'foot'?", "B", "Feets", "Feet", "Foots", "Footes"));
                add(new QuestionModule("Which of the following is a pronoun?", "C","Running","Quickly", "They", "House"));
                add(new QuestionModule("What is the synonym of 'sad'?", "D", "Excited","Joyful", "Cheerful","Unhappy"));
                add(new QuestionModule("Which of the following is a compound word?", "A", "Toothbrush", "Tooth", "Brush", "Teeth"));
                add(new QuestionModule("What is the correct plural of 'cactus'?", "B", "Cactuses", "Cacti", "Cactuss","Cactii" ));
                add(new QuestionModule("What is the past tense of 'speak'?", "C", "Spoken", "Speaked", "Spoke", "Speaking"));
                add(new QuestionModule("What is the superlative form of 'rich'?", "D", "More rich", "Richestest", "Most rich" ,"Richest"));
                add(new QuestionModule("What is the main purpose of an adjective?", "A", "To describe nouns", "To connect clauses", "To show action", "To indicate time"));
                add(new QuestionModule("What is the correct way to say the time: 2:15?", "B", "Two fifteen", "Quarter past two", "Two and fifteen", "Fifteen past two"));
                add(new QuestionModule("Which of the following is a possessive pronoun?", "C","They","I", "Mine", "Them"));
                add(new QuestionModule("What is the opposite of 'noisy'?", "D","Blaring", "Loud", "Shouty",  "Quiet"));
                add(new QuestionModule("What is the meaning of 'gregarious'?", "A", "Sociable", "Shy", "Reserved", "Lonely"));
                add(new QuestionModule("Which word means 'a person who writes books'?", "B", "Reader", "Author", "Editor", "Publisher"));
                add(new QuestionModule("What is the past tense of 'catch'?", "C", "Catch", "Catching", "Caught", "Catched"));
                add(new QuestionModule("What is the opposite of 'deep'?", "D", "Wide", "Long", "Narrow", "Shallow"));
                add(new QuestionModule("Which of the following is an exclamation?", "A", "Hooray!", "Quick", "Happy", "Run"));
                add(new QuestionModule("What is the meaning of 'intricate'?", "B","Simple", "Complex", "Plain", "Rough"));
                add(new QuestionModule("Which word is a conjunction?", "C", "Quickly", "Beautiful",  "And","Run"));
                add(new QuestionModule("What is the correct form of the sentence: 'They (to be) happy.'?", "D","They is happy.", "They were happy.", "They be happy." ,"They are happy."));
                add(new QuestionModule("Which of the following is a countable noun?", "A", "Apple", "Water", "Air", "Sand"));
                add(new QuestionModule("What is the plural of 'baby'?", "B","Babieses", "Babies", "Baby's", "Babieses"));
                add(new QuestionModule("What is the opposite of 'full'?", "C", "Empty", "Vacant", "Open", "Available"));
                add(new QuestionModule("Which word is a gerund?", "A", "Swimming", "Swim", "Swims", "Swam"));
                add(new QuestionModule("What is the meaning of 'meticulous'?", "D", "Careless", "Rushed", "Negligent" ,"Careful"));
                add(new QuestionModule("What is the past participle of 'see'?", "A", "Seen", "Saw", "Seeing", "Seeed"));
                add(new QuestionModule("What is the opposite of 'cheap'?", "B", "Inexpensive","Expensive","Affordable", "Reasonable"));
                add(new QuestionModule("Which of the following is a simile?", "C","Brave lion", "Lion bravery", "As brave as a lion",  "Bravery of a lion"));
                add(new QuestionModule("What is the superlative form of 'tall'?", "D","More tall", "Tallestest", "Most tall", "Tallest"));
                add(new QuestionModule("Which word is an abstract noun?", "A", "Freedom", "Table", "Dog", "House"));
                add(new QuestionModule("What is the meaning of 'benevolent'?", "B", "Cruel",  "Kind","Selfish", "Mean"));
                add(new QuestionModule("Which of the following is a homophone?", "C","Flower", "Tree",  "Flour", "Bush"));
                add(new QuestionModule("What is the correct form of the verb: 'She (to eat) dinner at 6 PM.'?", "D","Eat", "Eating", "Eaten", "Eats"));
                add(new QuestionModule("What is the past tense of 'find'?", "A", "Found", "Finded", "Finding", "Finds"));
                add(new QuestionModule("What is the synonym of 'quick'?", "A", "Fast", "Slow", "Lazy", "Deliberate"));
                add(new QuestionModule("Which word is an antonym of 'simple'?", "B", "Easy", "Complex", "Straightforward", "Clear"));
                add(new QuestionModule("What is the past tense of 'think'?", "A", "Thought", "Thinked", "Thinking", "Thinks"));
                add(new QuestionModule("Which of the following is a coordinating conjunction?", "B","Although",  "Or", "Because", "Since"));
                add(new QuestionModule("What is the plural form of 'tooth'?", "B", "Tooths", "Teeth", "Tooths", "Toothes"));
                add(new QuestionModule("What is the opposite of 'hard'?", "C", "Tough", "Rigid",  "Soft","Stiff"));
                add(new QuestionModule("Which word is an adjective?", "D","Run", "Quickly", "Is", "Happy"));
                add(new QuestionModule("What is the synonym of 'angry'?", "A", "Mad", "Happy", "Excited", "Pleased"));
                add(new QuestionModule("Which of the following is an infinitive?", "D","Running", "Ran", "Runs", "To run"));
                add(new QuestionModule("What is the opposite of 'light'?", "A", "Heavy", "Bright", "Dim", "Dark"));
                add(new QuestionModule("Which word is an adverb of manner?", "B","Fast", "Quickly", "Run", "Slow"));
                add(new QuestionModule("What is the correct plural of 'analysis'?", "B", "Analysises", "Analyses", "Analyzes", "Analyss"));
                add(new QuestionModule("Which of the following is an auxiliary verb?", "A", "Is", "Running", "Happy", "Table"));
                add(new QuestionModule("What is the past tense of 'forget'?", "C","Forgetted", "Forgetting", "Forgot",  "Forgets"));
                add(new QuestionModule("What is the superlative form of 'bad'?", "D", "Worst", "Badder", "More bad", "Baddest", "To show relationships"));
                add(new QuestionModule("Which of the following sentences is correct?", "B", "He run fast.", "He runs fast.", "He running fast.", "He ran fast."));
                add(new QuestionModule("What is the correct way to say the time: 5:45?", "C", "Five forty-five", "Five and forty-five", "Quarter to six", "Five to forty-five"));
                add(new QuestionModule("Which of the following is a reflexive pronoun?", "D","He", "They", "She", "Myself"));
                add(new QuestionModule("What is the opposite of 'rich'?", "A", "Poor", "Affluent", "Wealthy", "Lavish"));
                add(new QuestionModule("What is the meaning of 'authentic'?", "B","Fake", "Genuine", "False", "Artificial"));
                add(new QuestionModule("Which word is a collective noun?", "C","Bird", "Sky", "Flock", "Tree"));
                add(new QuestionModule("What is the meaning of 'perplexed'?", "D","Clear", "Certain", "Simple", "Confused"));
            }
        };
        QuestionModule.createQuestionsForSubject("English", R.drawable.english, questions);




        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("Java programming language কে ডেভেলপ করেছে?", "A", "James Gosling", "Bill Gates", "Steve Jobs", "Guido van Rossum"));
                add(new QuestionModule("Java কি ধরনের প্রোগ্রামিং ভাষা?", "B", "Procedural", "Object-oriented", "Functional", "Scripting"));
                add(new QuestionModule("Java Virtual Machine (JVM) এর কাজ কী?", "C", "Code optimization", "Memory allocation", "Bytecode execution", "Network management"));
                add(new QuestionModule("Java এর কোন সংস্করণে ল্যাম্বডা এক্সপ্রেশন যুক্ত হয়েছে?", "D", "Java 5", "Java 6", "Java 7", "Java 8"));
                add(new QuestionModule("Java এর কোন মেমরি ম্যানেজমেন্ট টুলটি ব্যবহার করা হয়?", "B", "Destructor", "Garbage Collector", "Manual memory management", "Memory Allocator"));
                add(new QuestionModule("কোনটি Java এর জন্য একটি বৈধ ডাটা টাইপ নয়?", "C", "int", "boolean", "string", "double"));
                add(new QuestionModule("Java-তে ইনহেরিটেন্স কী?", "A", "একটি ক্লাস অন্য একটি ক্লাস থেকে বৈশিষ্ট্য পায়", "একটি ফাংশন অন্য ফাংশন থেকে বৈশিষ্ট্য পায়", "একটি ভ্যারিয়েবল অন্য ভ্যারিয়েবল থেকে বৈশিষ্ট্য পায়", "একটি অবজেক্ট অন্য অবজেক্ট থেকে বৈশিষ্ট্য পায়"));
                add(new QuestionModule("Java এ কোনটি একটি অবজেক্ট ওরিয়েন্টেড প্রিন্সিপল?", "B", "Recursion", "Polymorphism", "Algorithm", "Data encapsulation"));
                add(new QuestionModule("কোনটি Java এ অ্যারে ডিক্লারেশনের সঠিক সিনট্যাক্স?", "A", "int[] arr = new int[5];", "int arr(5);", "int arr[] = 5;", "int[5] arr;"));
                add(new QuestionModule("Java এর কোনটি exception handling এর জন্য ব্যবহৃত হয়?", "B", "check", "try-catch", "loop", "array"));
                add(new QuestionModule("Java প্রোগ্রামে package কি কাজ করে?", "D", "Method inheritance", "Object creation", "Memory management", "Class management"));
                add(new QuestionModule("Java তে main মেথড কিভাবে ডিফাইন করা হয়?", "A", "public static void main(String[] args)", "public void main()", "void static main(String args[])", "public static main(String args[])"));
                add(new QuestionModule("Java তে কোন access modifier সর্বাধিক সীমাবদ্ধতা দেয়?", "D", "public", "protected", "default", "private"));
                add(new QuestionModule("Java তে কোনটি Thread তৈরি করতে ব্যবহৃত হয়?", "B", "Runnable object", "Thread class", "Process class", "Executor interface"));
                add(new QuestionModule("Java এর কোনটি abstract ক্লাস নয়?", "C", "Object", "Number", "Thread", "AbstractList"));
                add(new QuestionModule("Java তে String ক্লাস immutable কেন?", "A", "কারণ একবার তৈরি হলে তা পরিবর্তন করা যায় না", "কারণ এটি শুধুমাত্র প্রাইমিটিভ ডেটা টাইপ সাপোর্ট করে", "কারণ এটি স্ট্যাটিক ভ্যারিয়েবল ব্যবহার করে", "কারণ এটি ইনহেরিটেন্স সাপোর্ট করে না"));
                add(new QuestionModule("Java এর কোনটি একটি collection interface নয়?", "C", "List", "Set", "Node", "Map"));
                add(new QuestionModule("Java তে কোনটি একটি wrapper class নয়?", "D", "Integer", "Boolean", "Character", "int"));
                add(new QuestionModule("Java এর কোনটি multithreading এর সুবিধা?", "A", "Concurrent execution", "Higher memory usage", "Slower execution", "Less debugging complexity"));
                add(new QuestionModule("Java তে 'this' কি নির্দেশ করে?", "A", "Current object", "Parent class", "Static method", "Global variable"));
                add(new QuestionModule("Java তে কোনটি static মেথড নয়?", "D", "Math.sqrt()", "Thread.sleep()", "System.exit()", "String.length()"));
                add(new QuestionModule("Java তে কোনটি synchronization এর কাজ?", "B", "Memory optimization", "Thread safety", "Data abstraction", "Process execution"));
                add(new QuestionModule("Java তে Enum কী?", "C", "একটি array", "একটি class", "একটি special data type", "একটি loop"));
                add(new QuestionModule("Java তে exception handling কিভাবে শুরু করা হয়?", "B", "throw statement দিয়ে", "try block দিয়ে", "catch block দিয়ে", "throws clause দিয়ে"));
                add(new QuestionModule("Java তে switch statement এ কোন ধরনের ডেটা টাইপ সাপোর্ট করা হয় না?", "C", "int", "char", "double", "String"));
                add(new QuestionModule("Java তে 'finally' ব্লকের কাজ কী?", "B", "অব্যবহৃত ভ্যারিয়েবল পরিষ্কার করা", "exception হোক বা না হোক নির্দিষ্ট কোড রান করা", "প্রোগ্রাম বন্ধ করা", "try-catch block বন্ধ করা"));
                add(new QuestionModule("Java তে কোন keywordটি ইন্টারফেসকে ইমপ্লিমেন্ট করতে ব্যবহৃত হয়?", "C", "extends", "inherits", "implements", "returns"));
                add(new QuestionModule("Java তে hashCode() এবং equals() মেথড কোন class থেকে আসে?", "A", "Object", "HashMap", "ArrayList", "HashSet"));
                add(new QuestionModule("Java তে কোনটি final modifier এর বৈশিষ্ট্য?", "C", "অবজেক্টের পরিবর্তন প্রতিরোধ করা", "মেথডের রিটার্ন টাইপ পরিবর্তন করা", "ভ্যারিয়েবলের মান পরিবর্তন প্রতিরোধ করা", "ক্লাসকে পাবলিক করা"));
                add(new QuestionModule("Java তে HashMap এবং Hashtable এর মধ্যে পার্থক্য কী?", "B", "HashMap synchronized, Hashtable synchronized নয়", "HashMap unsynchronized, Hashtable synchronized", "দুটোই synchronized", "দুটোই unsynchronized"));
                add(new QuestionModule("Java তে কোনটি একটি Marker Interface?", "C", "Runnable", "Serializable", "Cloneable", "Comparable"));
                add(new QuestionModule("Java তে iterator এর কাজ কী?", "A", "collection এর উপর iterate করা", "collection sort করা", "collection merge করা", "collection delete করা"));
                add(new QuestionModule("Java তে Generics এর সুবিধা কী?", "D", "Memory optimization", "Faster execution", "Type casting", "Type safety"));
                add(new QuestionModule("Java তে কোনটি Checked Exception?", "A", "IOException", "NullPointerException", "ArithmeticException", "ArrayIndexOutOfBoundsException"));
                add(new QuestionModule("Java তে কোনটি Runtime Exception নয়?", "A", "FileNotFoundException", "NullPointerException", "ArithmeticException", "ClassCastException"));
                add(new QuestionModule("Java তে কোনটি একটি core class library নয়?", "D", "java.util", "java.lang", "java.io", "java.script"));
                add(new QuestionModule("Java তে ক্লাস লোডার কী কাজ করে?", "B", "Memory release", "Class loading", "Exception handling", "Thread execution"));
                add(new QuestionModule("Java তে কোনটি java.lang package এর অংশ নয়?", "D", "Math", "String", "System", "Scanner"));
                add(new QuestionModule("Java তে কোনটি Single Responsibility Principle এর উদাহরণ?", "A", "একটি ক্লাস একটি কাজ করে", "একটি মেথড দুটি কাজ করে", "একটি অবজেক্ট একাধিক কাজ করে", "একটি প্যাকেজ সব কাজ করে"));
                add(new QuestionModule("Java তে কোনটি final class নয়?", "B", "Math", "StringBuffer", "String", "Integer"));
                add(new QuestionModule("Java তে কোনটি primitive ডেটা টাইপ নয়?", "C", "int", "boolean", "String", "double"));
                add(new QuestionModule("Java তে encapsulation এর সুবিধা কী?", "B", "Inheritance", "Data hiding", "Polymorphism", "Memory management"));
                add(new QuestionModule("Java তে কোনটি superclass হিসাবে ব্যবহৃত হয়?", "A", "Object", "Class", "Interface", "Package"));
                add(new QuestionModule("Java তে 'super' কী নির্দেশ করে?", "D", "Local variable", "Static variable", "Method reference", "Parent class"));
                add(new QuestionModule("Java তে কোনটি একটি immutable object?", "B", "ArrayList", "String", "HashMap", "StringBuilder"));
                add(new QuestionModule("Java তে কোনটি static keyword এর বৈশিষ্ট্য?", "A", "Class-level member", "Object-level member", "Thread safety", "Memory optimization"));
                add(new QuestionModule("Java তে method overloading কী?", "C", "একই নামের মেথড ওভাররাইড করা", "মেথড এর ভেতরে আরেকটি মেথড", "একই নামের মেথড ভিন্ন প্যারামিটার দিয়ে ডিফাইন করা", "একাধিক মেথড একসাথে কল করা"));
                add(new QuestionModule("Java তে কোনটি synchronized ব্লকের কাজ?", "B", "Data encapsulation", "Thread safety", "Method overloading", "Inheritance"));
                add(new QuestionModule("Java তে abstract ক্লাস এবং ইন্টারফেসের মধ্যে পার্থক্য কী?", "C", "Abstract class স্ট্যাটিক মেথড সাপোর্ট করে না", "Interface এ inheritance নেই", "Abstract class মেথডের ডিফল্ট ইমপ্লিমেন্টেশন থাকতে পারে", "Interface ক্লাস থেকে ইনস্ট্যান্স তৈরি করা যায়"));
                add(new QuestionModule("Java তে কোনটি ArrayList এবং LinkedList এর মধ্যে পার্থক্য?", "D", "দুটোই ordered collection নয়", "ArrayList random access সাপোর্ট করে না", "LinkedList constant time access সাপোর্ট করে", "ArrayList random access সাপোর্ট করে, LinkedList constant time insert/remove সাপোর্ট করে"));
                add(new QuestionModule("Java তে কোনটি একটি immutable collection তৈরি করার জন্য ব্যবহৃত হয়?", "A", "Collections.unmodifiableList()", "Collections.singletonList()", "Arrays.asList()", "List.of()"));
                add(new QuestionModule("Java তে কোনটি একটি checked exception নয়?", "D", "SQLException", "IOException", "ClassNotFoundException", "NullPointerException"));
                add(new QuestionModule("Java তে কোনটি enum এর বৈশিষ্ট্য?", "B", "মাল্টি-থ্রেডিং সাপোর্ট করা", "Predefined constant values ধারণ করা", "Lambda expression ব্যবহার করা", "Constructor overloading সাপোর্ট করা"));
                add(new QuestionModule("Java তে কোনটি Collections framework এর অন্তর্গত নয়?", "C", "List", "Queue", "File", "Set"));
                add(new QuestionModule("Java তে কোনটি PriorityQueue এর বৈশিষ্ট্য?", "A", "Elements natural order এ থাকে", "Insertion order maintain করে", "Duplicate elements সাপোর্ট করে না", "Thread-safe"));
                add(new QuestionModule("Java তে কোনটি একটি method reference নয়?", "D", "::new", "::toString", "::equals", "::run"));
                add(new QuestionModule("Java তে enum কিভাবে ডিফাইন করা হয়?", "B", "class keyword দিয়ে", "enum keyword দিয়ে", "static keyword দিয়ে", "final keyword দিয়ে"));
                add(new QuestionModule("Java তে কোনটি 'this()' এবং 'super()' এর মধ্যে পার্থক্য?", "C", "'this()' static মেথড কল করে", "'super()' child class এর constructor কল করে", "'this()' current class এর constructor কল করে", "'super()' object কল করে"));
                add(new QuestionModule("Java তে Lambda expression কোন সংস্করণে যুক্ত হয়েছে?", "D", "Java 6", "Java 7", "Java 9", "Java 8"));
                add(new QuestionModule("Java তে finalize() মেথড কোন ক্লাসে ডিফাইন করা আছে?", "A", "Object", "Runtime", "GarbageCollector", "Thread"));
                add(new QuestionModule("Java তে কোনটি একটি concrete ক্লাস নয়?", "B", "ArrayList", "AbstractMap", "HashSet", "LinkedHashMap"));
                add(new QuestionModule("Java তে কোনটি instanceof অপারেটরের কাজ?", "C", "Method call করা", "Object creation", "Object type চেক করা", "Array traversal"));
                add(new QuestionModule("Java তে TreeMap এর বৈশিষ্ট্য কী?", "B", "Insertion order maintain করে", "Sorted order maintain করে", "Unordered collection", "Constant time insertion"));
                add(new QuestionModule("Java তে recursion কী?", "A", "একটি মেথড তার নিজের মধ্যে নিজেকে কল করে", "একটি মেথড অন্য একটি মেথডকে কল করে", "একটি অবজেক্ট অন্য একটি অবজেক্টকে কল করে", "একটি ভ্যারিয়েবল অন্য একটি ভ্যারিয়েবলকে রেফার করে"));
                add(new QuestionModule("Java তে কোনটি wrapper class নয়?", "D", "Float", "Character", "Double", "long"));
                add(new QuestionModule("Java তে HashSet এর বৈশিষ্ট্য কী?", "C", "Ordered collection", "Thread-safe collection", "Unique elements ধারণ করে", "Indexed collection"));
                add(new QuestionModule("Java তে কোনটি Data Structure নয়?", "B", "LinkedList", "Thread", "Stack", "Queue"));
                add(new QuestionModule("Java তে method overriding কী?", "A", "Parent class এর মেথডকে child class এ রেডিফাইন করা", "একই নামের মেথড তৈরি করা", "একটি মেথড আরেকটি মেথডকে কল করা", "একটি মেথড ভিন্ন প্যারামিটার দিয়ে ডিফাইন করা"));
                add(new QuestionModule("Java তে কোনটি একটি mutable object?", "B", "String", "StringBuilder", "Integer", "Character"));
                add(new QuestionModule("Java তে কোনটি ইন্টারফেস নয়?", "C", "Runnable", "Serializable", "HashSet", "Comparable"));
                add(new QuestionModule("Java তে static block কখন execute হয়?", "A", "Class লোডিং সময়", "Method কল করার সময়", "Object তৈরি করার সময়", "Thread তৈরি করার সময়"));
                add(new QuestionModule("Java তে Thread lifecycle এ কোনটি অবস্থা নয়?", "C", "New", "Runnable", "Waiting", "Terminated"));
                add(new QuestionModule("Java তে java.util package এর অংশ কোনটি?", "D", "Math", "Runtime", "Thread", "ArrayList"));
                add(new QuestionModule("Java তে কোনটি একটি Function interface নয়?", "B", "Predicate", "Object", "Consumer", "Supplier"));
                add(new QuestionModule("Java তে 'volatile' কী নির্দেশ করে?", "B", "Memory leak", "Thread communication", "Data loss prevention", "Garbage collection"));
                add(new QuestionModule("Java তে HashMap এবং TreeMap এর মধ্যে পার্থক্য কী?", "A", "HashMap unordered, TreeMap sorted", "HashMap sorted, TreeMap unordered", "দুটোই sorted", "দুটোই unordered"));
                add(new QuestionModule("Java তে কোনটি object cloning সাপোর্ট করে?", "D", "Thread", "Runnable", "AbstractList", "Cloneable"));
                add(new QuestionModule("Java তে composition কী?", "C", "একটি ক্লাস অন্য ক্লাসকে extends করে", "একটি মেথড অন্য মেথডকে override করে", "একটি ক্লাস অন্য ক্লাসের অবজেক্ট ব্যবহার করে", "একটি ভ্যারিয়েবল অন্য ভ্যারিয়েবলকে encapsulate করে"));
                add(new QuestionModule("Java তে কোনটি একটি functional interface?", "A", "Runnable", "Thread", "ArrayList", "Comparator"));
                add(new QuestionModule("Java তে ExecutorService এর কাজ কী?", "D", "Garbage collection", "Memory allocation", "Thread creation", "Thread pool management"));
                add(new QuestionModule("Java তে কোনটি interface এর বৈশিষ্ট্য নয়?", "D", "Method declaration থাকে", "Multiple inheritance সাপোর্ট করে", "Abstract method থাকে", "Constructor থাকে"));
                add(new QuestionModule("Java তে Anonymous class কি?", "B", "একটি class যা final করা যায়", "একটি class যার কোন নাম নেই", "একটি class যা শুধু একটি method সাপোর্ট করে", "একটি class যার কোন constructor নেই"));
                add(new QuestionModule("Java তে কোনটি Exception handling এর জন্য ব্যবহৃত হয় না?", "C", "throw", "catch", "final", "try"));
                add(new QuestionModule("Java তে কোনটি Reflection API এর কাজ?", "A", "Class এর runtime behavior inspect করা", "Thread এর state পরিবর্তন করা", "Memory management করা", "Garbage collection করা"));
                add(new QuestionModule("Java তে কোনটি Stack memory তে সংরক্ষণ করা হয়?", "B", "Object", "Primitive variables", "Heap variables", "Static methods"));
                add(new QuestionModule("Java তে কোথায় JVM থাকে?", "A", "Java Runtime Environment (JRE)", "Java Development Kit (JDK)", "Operating system", "Garbage Collector"));
                add(new QuestionModule("Java তে কোনটি null pointer exception এর উদাহরণ?", "D", "int a = 0;", "String s = \"Hello\";", "int[] arr = new int[5];", "Object obj = null; obj.toString();"));
                add(new QuestionModule("Java তে কোনটি finalize() মেথডের কাজ নয়?", "C", "Object destroy করা", "Garbage collection এর আগে কল করা", "Memory release করা", "Object এর reference cleanup করা"));
                add(new QuestionModule("Java তে কোনটি static মেথডে ব্যবহৃত হতে পারে না?", "B", "Local variables", "this keyword", "Static variables", "Method parameters"));
                add(new QuestionModule("Java তে কোনটি Runtime polymorphism এর উদাহরণ?", "A", "Method overriding", "Method overloading", "Constructor overloading", "Static block execution"));
                add(new QuestionModule("Java তে কোনটি String pool এর বৈশিষ্ট্য?", "A", "Duplicate String object এড়ানো", "String object এর size বৃদ্ধি করা", "String object mutable করা", "String object দ্রুত garbage collect করা"));
                add(new QuestionModule("Java তে কোনটি I/O stream এর প্রকারভেদ নয়?", "C", "InputStream", "OutputStream", "DataStream", "Reader"));
                add(new QuestionModule("Java তে কোনটি method signature এর অংশ নয়?", "D", "Method name", "Parameter list", "Return type", "Method body"));
                add(new QuestionModule("Java তে কোনটি instanceof অপারেটরের ব্যবহার নয়?", "C", "Object এর type চেক করা", "Inheritance চেক করা", "Method call করা", "Runtime type checking"));
                add(new QuestionModule("Java তে BufferedReader class কিসের জন্য ব্যবহৃত হয়?", "B", "Binary file read করার জন্য", "Character file read করার জন্য", "Object serialization এর জন্য", "Thread synchronization এর জন্য"));
                add(new QuestionModule("Java তে Generics কী সাপোর্ট করে না?", "C", "Type safety", "Compile-time checking", "Primitive data type", "Code reusability"));
                add(new QuestionModule("Java তে কোনটি keyword inheritance সাপোর্ট করে?", "B", "implements", "extends", "throws", "final"));
                add(new QuestionModule("Java তে কোনটি Memory leak এর কারণ হতে পারে?", "D", "Object creation", "Static block", "Thread creation", "Unreachable objects holding references"));
                add(new QuestionModule("Java তে PriorityBlockingQueue কিসের জন্য ব্যবহৃত হয়?", "C", "Sorting elements", "Handling random access", "Thread-safe priority queue management", "Implementing synchronized collections"));
                add(new QuestionModule("Java তে কোনটি TreeSet এর বৈশিষ্ট্য?", "B", "Duplicates সাপোর্ট করে", "Sorted order maintain করে", "Random access সাপোর্ট করে", "Indexed collection নয়"));
            }
        };
        QuestionModule.createQuestionsForSubject("Java", R.drawable.java, questions);

        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("ইন্টারনেটে তথ্য আদান-প্রদানের জন্য কোনটি প্রোটোকল ব্যবহৃত হয়?", "A", "TCP/IP", "HTTP", "SMTP", "FTP"));
                add(new QuestionModule("কোনটি সফটওয়্যার লাইফ সাইকেল এর একটি ধাপ নয়?", "C", "Planning", "Design", "Networking", "Testing"));
                add(new QuestionModule("কোনটি ইন্টারনেট সংযোগের একটি ধরণ?", "A", "DSL", "TCP", "Ethernet", "SMTP"));
                add(new QuestionModule("কোনটি কম্পিউটারে ডেটা ট্রান্সফারের গতি পরিমাপের একক?", "B", "Bytes", "Bits per second (bps)", "Hertz", "Megabytes"));
                add(new QuestionModule("একটি সিস্টেমের ক্ষতির ঝুঁকি কমাতে ব্যবহৃত প্রযুক্তি কোনটি?", "D", "Encryption", "Firewall", "Antivirus", "All of the above"));
                add(new QuestionModule("কোনটি মোবাইল অপারেটিং সিস্টেমের উদাহরণ নয়?", "C", "iOS", "Android", "Windows 10", "Symbian"));
                add(new QuestionModule("কোনটি স্টোরেজ ডিভাইস নয়?", "D", "Hard Disk", "SSD", "USB Drive", "RAM"));
                add(new QuestionModule("কোনটি ক্লাউড কম্পিউটিং এর সুবিধা নয়?", "D", "Flexibility", "Scalability", "Cost-efficiency", "High maintenance cost"));
                add(new QuestionModule("URL এর পুরো রূপ কী?", "A", "Uniform Resource Locator", "Universal Resource Link", "Unique Resource Locator", "Uniform Reference Link"));
                add(new QuestionModule("কোনটি মাল্টিমিডিয়া এর উপাদান নয়?", "D", "Text", "Audio", "Video", "Algorithm"));
                add(new QuestionModule("কোনটি প্রথম প্রোগ্রামেবল কম্পিউটার?", "A", "ENIAC", "UNIVAC", "EDSAC", "Colossus"));
                add(new QuestionModule("কোনটি অপারেটিং সিস্টেমের প্রধান কাজ?", "C", "Data Storage", "Web Browsing", "Resource Management", "Document Editing"));
                add(new QuestionModule("ICT তে কোনটি ডেটা এনক্রিপশনের জন্য ব্যবহৃত হয়?", "A", "AES", "HTML", "HTTP", "POP3"));
                add(new QuestionModule("কোনটি e-commerce এর উদাহরণ?", "C", "Sending emails", "Chatting online", "Online shopping", "Playing games"));
                add(new QuestionModule("মাল্টিপ্লেক্সিং এর কাজ কী?", "B", "Multiple computers ব্যবহার করা", "Multiple signals একত্রিত করা", "Single process চালানো", "Multi-threading"));
                add(new QuestionModule("কম্পিউটারের CPU এর প্রধান অংশ কোনটি?", "A", "ALU এবং CU", "Hard Drive", "RAM", "Power Supply"));
                add(new QuestionModule("কোনটি সফটওয়্যার আপডেটের উদ্দেশ্য নয়?", "D", "Bug fix", "Security enhancement", "Performance improvement", "Reducing functionality"));
                add(new QuestionModule("কোনটি মোবাইল নেটওয়ার্ক প্রজন্ম নয়?", "D", "2G", "3G", "4G", "6G"));
                add(new QuestionModule("কোনটি ল্যাপটপের একটি হার্ডওয়্যার অংশ?", "A", "Keyboard", "Operating System", "Application Software", "Anti-virus"));
                add(new QuestionModule("কোনটি মেমোরি একক নয়?", "C", "Kilobyte", "Megabyte", "Gigahertz", "Terabyte"));
                add(new QuestionModule("কম্পিউটারের কোনটি ইনপুট ডিভাইস?", "A", "Mouse", "Monitor", "Printer", "Speaker"));
                add(new QuestionModule("কোনটি ইন্টারনেটে একটি নিরাপত্তা হুমকি?", "B", "Spamming", "Phishing", "Blogging", "Browsing"));
                add(new QuestionModule("কোনটি সাইবার অপরাধের একটি উদাহরণ?", "A", "Hacking", "Gaming", "Browsing", "Messaging"));
                add(new QuestionModule("কোনটি কম্পিউটার নেটওয়ার্কিং এর একটি টপোলজি?", "D", "Ring", "Star", "Bus", "All of the above"));
                add(new QuestionModule("একটি কম্পিউটারের প্রধান প্রসেসিং অংশ কোনটি?", "C", "RAM", "Hard Disk", "CPU", "Power Supply"));
                add(new QuestionModule("কোনটি প্রাইভেট নেটওয়ার্কের উদাহরণ?", "B", "Internet", "Intranet", "Extranet", "VPN"));
                add(new QuestionModule("ইন্টারনেট ব্রাউজারে কোন প্রোটোকল ব্যবহার করা হয়?", "A", "HTTP", "FTP", "SMTP", "DNS"));
                add(new QuestionModule("কোনটি ICT ডেটা নিরাপত্তার জন্য প্রয়োজনীয় নয়?", "C", "Firewall", "Encryption", "Advertising", "Antivirus"));
                add(new QuestionModule("কোনটি কম্পিউটার ভাইরাসের উদাহরণ?", "A", "Trojan", "Router", "Operating System", "Email"));
                add(new QuestionModule("Wi-Fi এর পুরো রূপ কী?", "B", "Wired Fidelity", "Wireless Fidelity", "Web Fidelity", "Wide Fidelity"));
                add(new QuestionModule("HTML কোনটির জন্য ব্যবহৃত হয়?", "C", "Data Encryption", "Operating System Development", "Web Page Designing", "Networking"));
                add(new QuestionModule("কোনটি ICT সিস্টেমের প্রধান উপাদান?", "A", "Hardware", "Media", "Cloud", "Interface"));
                add(new QuestionModule("কোনটি ভাইরাস প্রতিরোধের একটি পদ্ধতি নয়?", "C", "Antivirus ব্যবহার করা", "Regular সফটওয়্যার আপডেট করা", "Spamming", "ফায়ারওয়াল ইনস্টল করা"));
                add(new QuestionModule("কোনটি দ্রুততম ইন্টারনেট সংযোগের প্রযুক্তি?", "B", "DSL", "Fiber Optic", "Dial-up", "Satellite"));
                add(new QuestionModule("কোনটি কম্পিউটার অপারেটিং সিস্টেমের উদাহরণ?", "A", "Windows", "MS Office", "Adobe Photoshop", "Oracle"));
                add(new QuestionModule("কোনটি প্রোগ্রামিং ল্যাঙ্গুয়েজ নয়?", "D", "Python", "Java", "C++", "HTML"));
                add(new QuestionModule("মোবাইল ডেটা নেটওয়ার্কে কোনটি সর্বাধুনিক প্রযুক্তি?", "C", "2G", "3G", "5G", "WiMAX"));
                add(new QuestionModule("কোনটি পাসওয়ার্ড সুরক্ষার একটি উপায়?", "D", "Encryption", "Two-factor authentication", "Strong password ব্যবহার", "All of the above"));
                add(new QuestionModule("কোনটি ম্যালওয়্যার নয়?", "C", "Virus", "Trojan", "Firmware", "Ransomware"));
                add(new QuestionModule("কোনটি কম্পিউটার স্টোরেজের একক নয়?", "C", "Megabyte", "Gigabyte", "Nanosecond", "Terabyte"));
                add(new QuestionModule("ICT তে কোনটি ওয়্যারলেস যোগাযোগের একটি উদাহরণ?", "A", "Bluetooth", "Ethernet", "USB", "Fiber Optic"));
                add(new QuestionModule("কোনটি অপারেটিং সিস্টেমের কাজ নয়?", "D", "Memory management", "Process management", "File management", "Software development"));
                add(new QuestionModule("কোনটি প্রাইমারি মেমোরির উদাহরণ?", "B", "Hard Disk", "RAM", "SSD", "Flash Drive"));
                add(new QuestionModule("কোনটি একটি নিরাপদ ডেটা ট্রান্সমিশন প্রোটোকল?", "B", "HTTP", "HTTPS", "FTP", "SMTP"));
                add(new QuestionModule("কোনটি সামাজিক যোগাযোগ মাধ্যম নয়?", "C", "Facebook", "Twitter", "Google Drive", "Instagram"));
                add(new QuestionModule("কোনটি CPU এর একটি অংশ?", "A", "ALU", "RAM", "Hard Drive", "Monitor"));
                add(new QuestionModule("কোনটি ডেটা ট্রান্সমিশনের জন্য একটি নিরাপত্তা হুমকি?", "B", "Data encryption", "Packet sniffing", "Firewall", "VPN"));
                add(new QuestionModule("কোনটি ইন্টারনেট অ্যাক্সেসের একটি ওয়্যারড পদ্ধতি?", "C", "Wi-Fi", "Bluetooth", "Ethernet", "NFC"));
                add(new QuestionModule("কোনটি একটি ক্লাউড স্টোরেজ সেবা?", "D", "Oracle", "Microsoft Word", "Google Chrome", "Google Drive"));
                add(new QuestionModule("কোনটি ফাইল কমপ্রেশন টুলের উদাহরণ?", "B", "VLC", "WinRAR", "Photoshop", "Excel"));
                add(new QuestionModule("কোনটি ওয়েবসাইটের জন্য ব্যবহৃত প্রোগ্রামিং ভাষা?", "A", "HTML", "SQL", "Python", "C++"));
                add(new QuestionModule("কোনটি তথ্যের গোপনীয়তা রক্ষার জন্য ব্যবহৃত হয়?", "C", "Firewall", "Cache", "Encryption", "Compression"));
                add(new QuestionModule("কোনটি একটি ম্যালওয়্যারের উদাহরণ?", "A", "Worm", "Router", "Firewall", "Cloud"));
                add(new QuestionModule("কোনটি ইন্টারনেটের একটি প্রধান উপাদান?", "B", "Wi-Fi", "ISP (Internet Service Provider)", "Bluetooth", "USB"));
                add(new QuestionModule("ICT তে কোনটি input device নয়?", "D", "Keyboard", "Scanner", "Microphone", "Printer"));
                add(new QuestionModule("ডেটাবেসে কোনটি ডেটার redundancy কমানোর জন্য ব্যবহৃত হয়?", "A", "Normalization", "Denormalization", "Indexing", "Fragmentation"));
                add(new QuestionModule("কোনটি মোবাইল কম্পিউটিং এর উদাহরণ?", "B", "Desktop", "Tablet", "Mainframe", "Supercomputer"));
                add(new QuestionModule("কোনটি রাউটার এর কাজ?", "C", "Data storage", "Virus protection", "Data packet forwarding", "File compression"));
                add(new QuestionModule("ডেটা ব্যাকআপ এর প্রধান উদ্দেশ্য কী?", "B", "Data sharing", "Data recovery", "Data deletion", "Data encryption"));
                add(new QuestionModule("ICT তে কোনটি নেটওয়ার্ক নিরাপত্তার একটি পদ্ধতি?", "A", "Firewall", "Multitasking", "Multithreading", "Multiprocessing"));
                add(new QuestionModule("কোনটি কম্পিউটার মেমোরির অস্থায়ী ধরণ?", "B", "ROM", "RAM", "Hard Disk", "SSD"));
                add(new QuestionModule("কোনটি প্রোগ্রামিং ভাষা?", "C", "HTML", "Excel", "Python", "Photoshop"));
                add(new QuestionModule("কোনটি কম্পিউটারের আউটপুট ডিভাইস?", "A", "Monitor", "Keyboard", "Mouse", "Scanner"));
                add(new QuestionModule("কোনটি কম্পিউটারে ডেটা ট্রান্সমিশন গতি নির্ধারণ করতে ব্যবহৃত হয়?", "C", "Watts", "Hertz", "Bits per second (bps)", "Decibel"));
                add(new QuestionModule("কোনটি সার্চ ইঞ্জিন নয়?", "D", "Google", "Bing", "Yahoo", "Microsoft Word"));
                add(new QuestionModule("কোনটি পেরিফেরাল ডিভাইসের উদাহরণ?", "C", "CPU", "RAM", "Printer", "Cache"));
                add(new QuestionModule("কোনটি অপারেটিং সিস্টেমের কাজ?", "A", "File management", "Virus protection", "Creating software", "Internet browsing"));
                add(new QuestionModule("কোনটি ইমেইল প্রোটোকল নয়?", "D", "SMTP", "POP3", "IMAP", "IPSec"));
                add(new QuestionModule("কোনটি ডেটাবেস ম্যানেজমেন্ট সিস্টেমের উদাহরণ?", "A", "MySQL", "PHP", "Java", "HTML"));
                add(new QuestionModule("কোনটি ব্লুটুথ এর প্রধান ব্যবহার?", "B", "Data encryption", "Short-range wireless communication", "High-speed data transmission", "Long-range networking"));
                add(new QuestionModule("ইন্টারনেটে ডোমেইন নাম সিস্টেম (DNS) এর কাজ কী?", "C", "Data encryption", "Packet switching", "Domain name থেকে IP address খোঁজা", "File compression"));
                add(new QuestionModule("কোনটি ডিস্কের ডেটা সংরক্ষণ প্রযুক্তি?", "A", "Hard Disk", "RAM", "ROM", "Monitor"));
                add(new QuestionModule("কোনটি একটি বায়োমেট্রিক সিস্টেমের উদাহরণ?", "B", "Password", "Fingerprint scanner", "OTP", "Encryption"));
                add(new QuestionModule("কোনটি ভার্চুয়াল রিয়ালিটি ডিভাইসের উদাহরণ?", "D", "Router", "Firewall", "Scanner", "VR Headset"));
                add(new QuestionModule("কোনটি তথ্য সুরক্ষার জন্য ব্যবহৃত একটি নিরাপত্তা ব্যবস্থা?", "A", "Encryption", "Decryption", "Format", "Backup"));
                add(new QuestionModule("কোনটি ডিজিটাল পেমেন্ট সিস্টেম নয়?", "D", "PayPal", "Google Pay", "Apple Pay", "Barcode"));
                add(new QuestionModule("ICT তে কোনটি ইন্টারনেটের জন্য প্রয়োজনীয় নয়?", "B", "IP Address", "USB Drive", "Router", "Modem"));
                add(new QuestionModule("কোনটি সফটওয়্যার ডেভেলপমেন্ট টুলের উদাহরণ?", "A", "Eclipse", "Microsoft Excel", "Adobe Reader", "VLC Media Player"));
                add(new QuestionModule("কোনটি ডেটা ট্রান্সফার করার একটি মাধ্যম?", "C", "Firewall", "Antivirus", "FTP", "Algorithm"));
                add(new QuestionModule("ICT তে কোনটি সফটওয়্যার ডেভেলপমেন্ট লাইফ সাইকেলের প্রথম ধাপ?", "A", "Requirement Analysis", "Design", "Testing", "Implementation"));
                add(new QuestionModule("কোনটি ডেটাবেসে ডেটা পুনরুদ্ধারের জন্য ব্যবহৃত হয়?", "C", "DELETE", "INSERT", "SELECT", "UPDATE"));
                add(new QuestionModule("কম্পিউটারে ডেটা সংরক্ষণের সবচেয়ে বড় ইউনিট কোনটি?", "D", "Kilobyte", "Megabyte", "Gigabyte", "Terabyte"));
                add(new QuestionModule("কোনটি একটি সার্ভার এর কাজ?", "B", "Data input", "Providing services to clients", "Graphics rendering", "File compression"));
                add(new QuestionModule("কোনটি অ্যাসেম্বলি ভাষার উদাহরণ?", "A", "MOV", "HTML", "Python", "SQL"));
                add(new QuestionModule("কোনটি ওয়েব ব্রাউজারের উদাহরণ?", "C", "Google Drive", "Dropbox", "Mozilla Firefox", "SQL Server"));
                add(new QuestionModule("ICT তে কোনটি কম্পিউটার নেটওয়ার্কিং এর একটি প্রোটোকল?", "D", "HTTP", "FTP", "SMTP", "All of the above"));
                add(new QuestionModule("ডেটা স্টোরেজের জন্য কোনটি ইলেকট্রনিক চিপ ব্যবহৃত হয়?", "B", "Hard Disk", "Flash Memory", "Magnetic Tape", "Optical Disk"));
                add(new QuestionModule("কোনটি কম্পিউটারের BIOS এর কাজ?", "A", "Startup configuration management", "Data processing", "File management", "Network security"));
                add(new QuestionModule("কোনটি সার্কিট সুইচিং এর উদাহরণ?", "D", "Packet switching", "Datagram switching", "Frame relay", "Telephone network"));
                add(new QuestionModule("কোনটি ফাইল শেয়ারিং এর জন্য ব্যবহৃত সফটওয়্যার?", "C", "VLC", "Notepad", "BitTorrent", "Photoshop"));
                add(new QuestionModule("কোনটি একটি রিলেশনাল ডেটাবেস ম্যানেজমেন্ট সিস্টেম?", "B", "Hadoop", "Oracle", "MongoDB", "Neo4j"));
                add(new QuestionModule("কোনটি ICT তে একটি ট্রান্সমিশন মিডিয়া নয়?", "D", "Optical Fiber", "Coaxial Cable", "Twisted Pair Cable", "Printer"));
                add(new QuestionModule("কোনটি কম্পিউটারের পাওয়ার সাপ্লাই এর উদাহরণ?", "B", "RAM", "SMPS", "Hard Drive", "GPU"));
                add(new QuestionModule("ICT তে কোনটি একটি ফ্রেমওয়ার্ক?", "C", "MySQL", "Adobe Illustrator", "Spring", "Google Chrome"));
                add(new QuestionModule("কোনটি অপারেটিং সিস্টেমে মাল্টিপ্রসেসিং সাপোর্ট করে?", "A", "Linux", "MS Word", "Photoshop", "Notepad"));
                add(new QuestionModule("কোনটি কম্পিউটার ডেটা ট্রান্সফার এর জন্য ব্যবহৃত পদ্ধতি?", "C", "SSD", "RAM", "USB", "ROM"));
                add(new QuestionModule("কোনটি কম্পিউটার নেটওয়ার্কে ব্যবহৃত টপোলজি?", "B", "Ring", "Mesh", "Circular", "Triangular"));
                add(new QuestionModule("কোনটি একটি ওপেন সোর্স সফটওয়্যার?", "A", "Linux", "Microsoft Office", "Adobe Photoshop", "CorelDRAW"));
                add(new QuestionModule("কোনটি মোবাইল অ্যাপ্লিকেশন ডেভেলপমেন্টের জন্য ব্যবহৃত হয়?", "D", "Windows", "Oracle", "PHP", "Android Studio"));
                add(new QuestionModule("কোনটি কম্পিউটারের মাদারবোর্ড এর একটি অংশ?", "B", "LCD", "Chipset", "Hard Drive", "SMPS"));
                add(new QuestionModule("কোনটি কম্পিউটার ভাইরাসের থেকে ডেটা রক্ষা করার জন্য ব্যবহৃত হয়?", "D", "Firewall", "Multitasking", "Caching", "Antivirus"));
                add(new QuestionModule("কোনটি তথ্য প্রক্রিয়াকরণ চক্রের ধাপ?", "A", "Input", "Control", "Feedback", "Execution"));
                add(new QuestionModule("ICT তে কোনটি স্টার টপোলজির বৈশিষ্ট্য?", "A", "Central node control", "Equal connection speed", "Multiple parent nodes", "Closed-loop network"));
                add(new QuestionModule("কোনটি একটি ক্লাউড কম্পিউটিং পরিষেবা নয়?", "D", "IaaS", "SaaS", "PaaS", "RSS"));
            }
        };
        QuestionModule.createQuestionsForSubject("ICT", R.drawable.ict, questions);

        questions = new ArrayList<QuestionModule>() {
            {
                add(new QuestionModule("ওজোন স্তরটি পৃথিবীর কোন স্তরে অবস্থিত?", "B", "ট্রপোস্ফিয়ার", "স্ট্রাটোস্ফিয়ার", "মেসোস্ফিয়ার", "থার্মোস্ফিয়ার"));
                add(new QuestionModule("অম্লবৃষ্টির প্রধান কারণ কী?", "A", "সালফার ডাই অক্সাইড এবং নাইট্রোজেন অক্সাইডের নির্গমন", "বনভূমি নিধন", "অতিরিক্ত সার ব্যবহার", "বৈশ্বিক উষ্ণায়ন"));
                add(new QuestionModule("বিশ্ব উষ্ণায়নের মূল কারণ কী?", "A", "কার্বন ডাই অক্সাইড নির্গমন", "চন্দ্রের গতিবিধি", "পৃথিবীর ঘূর্ণন", "সৌরজগতের পরিবর্তন"));
                add(new QuestionModule("জলবায়ু পরিবর্তনের কারণে কোনটি ঘটতে পারে?", "D", "বন্যা", "খরা", "বনভূমির ক্ষতি", "উপরের সবগুলো"));
                add(new QuestionModule("কোনটি একটি পুনর্নবীকরণযোগ্য শক্তির উৎস?", "C", "প্রাকৃতিক গ্যাস", "পেট্রোলিয়াম", "সৌরশক্তি", "কয়লা"));
                add(new QuestionModule("জলবায়ু পরিবর্তন প্রতিরোধের জন্য কোনটি সবচেয়ে কার্যকর পদক্ষেপ?", "B", "গাড়ির ব্যবহার বাড়ানো", "বৈদ্যুতিক গাড়ি ব্যবহারে উৎসাহ দেওয়া", "কৃষিজমি বাড়ানো", "প্লাস্টিক ব্যবহার বৃদ্ধি"));
                add(new QuestionModule("কোনটি একটি জীবাশ্ম জ্বালানি?", "A", "কয়লা", "বায়ু শক্তি", "জল শক্তি", "সৌর শক্তি"));
                add(new QuestionModule("জল দূষণের প্রধান কারণ কী?", "C", "মাটি ক্ষয়", "বায়ু দূষণ", "শিল্প বর্জ্য", "ওজোন স্তরের ক্ষতি"));
                add(new QuestionModule("পানিতে অতিরিক্ত পুষ্টি উপাদান যোগ হওয়ার ফলে কোনটি ঘটে?", "B", "জলজ উদ্ভিদের বৃদ্ধি হ্রাস", "ইউট্রিফিকেশন", "মাটি ক্ষয়", "ওজোন স্তর ক্ষয়"));
                add(new QuestionModule("বায়ু দূষণ থেকে রক্ষা পাওয়ার জন্য কোন প্রযুক্তিটি ব্যবহৃত হয়?", "C", "জল পরিশোধন", "জলাশয় নির্মাণ", "ফিল্টারিং সিস্টেম", "বায়ু প্রবাহ বৃদ্ধি"));
                add(new QuestionModule("প্লাস্টিকের পুনর্ব্যবহারযোগ্য কোড সাধারণত কতটি শ্রেণিতে ভাগ করা হয়?", "D", "২", "৩", "৫", "৭"));
                add(new QuestionModule("বনভূমি ধ্বংসের কারণে কোনটি ঘটতে পারে?", "D", "মাটি ক্ষয়", "বন্যপ্রাণী ক্ষতি", "জলবায়ু পরিবর্তন", "উপরের সবগুলো"));
                add(new QuestionModule("গ্রিনহাউস প্রভাবের ফলে কোনটি ঘটে?", "A", "পৃথিবীর তাপমাত্রা বৃদ্ধি", "পৃথিবীর তাপমাত্রা হ্রাস", "বায়ু চলাচল বৃদ্ধি", "ওজোন স্তর ক্ষয়"));
                add(new QuestionModule("জলাশয় দূষণের কারণে কোনটি ঘটে?", "B", "উচ্চ অক্সিজেনের মাত্রা", "জীবজন্তুর মৃত্যু", "উদ্ভিদের বৃদ্ধি", "মৃত্তিকার উর্বরতা বৃদ্ধি"));
                add(new QuestionModule("জলাশয় পুনরুদ্ধারের জন্য কোন পদ্ধতিটি ব্যবহার করা হয়?", "A", "বায়ো-রিমেডিয়েশন", "ইনকিনারেশন", "ক্যাপিং", "ডিগ্রেডেশন"));
                add(new QuestionModule("জীববৈচিত্র্য সংরক্ষণের প্রধান উপায় কী?", "C", "শিল্পায়ন বাড়ানো", "কৃষি জমি বাড়ানো", "বন্যপ্রাণী সংরক্ষণ", "মাটি ক্ষয় রোধ"));
                add(new QuestionModule("পুনর্ব্যবহারযোগ্য শক্তির কোনটি উদাহরণ নয়?", "D", "সৌর শক্তি", "বায়ু শক্তি", "জল শক্তি", "প্রাকৃতিক গ্যাস"));
                add(new QuestionModule("বায়ু দূষণের ফলে কী ধরনের স্বাস্থ্য সমস্যা দেখা দেয়?", "A", "শ্বাসকষ্ট", "ডায়রিয়া", "রক্তচাপ হ্রাস", "হার্ট অ্যাটাক"));
                add(new QuestionModule("জলবায়ু পরিবর্তনের সাথে কোনটি সম্পর্কিত নয়?", "D", "কার্বন ডাই অক্সাইড", "মিথেন", "নাইট্রাস অক্সাইড", "অ্যামোনিয়া"));
                add(new QuestionModule("বায়ু দূষণ পরিমাপের জন্য কোনটি ব্যবহৃত হয়?", "B", "অক্সিজেন সূচক", "বায়ু মানের সূচক", "জলমান সূচক", "বৃষ্টিপাত সূচক"));
                add(new QuestionModule("যেসব গাছ কার্বন ডাই অক্সাইড শোষণ করতে সক্ষম তাদেরকে কী বলা হয়?", "A", "কার্বন সিঙ্ক", "প্রডিউসার", "কনজিউমার", "ডিকম্পোজার"));
                add(new QuestionModule("ভূগর্ভস্থ জলের স্তরগুলোকে কী বলা হয়?", "C", "জলাশয়", "নদী", "অ্যাকুইফার", "বাঁধ"));
                add(new QuestionModule("কোন ধরনের কৃষিকাজ পরিবেশের জন্য সবচেয়ে ক্ষতিকারক?", "D", "প্রাকৃতিক চাষ", "জৈব কৃষি", "পুনর্ব্যবহারযোগ্য চাষ", "সরাসরি সেচ চাষ"));
                add(new QuestionModule("কোনটি একটি প্রধান গ্রিনহাউস গ্যাস নয়?", "D", "কার্বন ডাই অক্সাইড", "মিথেন", "নাইট্রাস অক্সাইড", "অক্সিজেন"));
                add(new QuestionModule("ওজোন স্তরের ক্ষতির প্রধান কারণ কী?", "A", "সিএফসি (ক্লোরোফ্লুরোকার্বন) নির্গমন", "নাইট্রোজেন নির্গমন", "অম্লবৃষ্টি", "বায়ু দূষণ"));
                add(new QuestionModule("পরিবেশগতভাবে টেকসই কৃষি পদ্ধতির উদাহরণ কী?", "C", "অতিরিক্ত রাসায়নিক সার ব্যবহার", "প্লাস্টিক ব্যবহারে বৃদ্ধি", "জৈব চাষ", "কৃষি জমিতে গাছ কাটা"));
                add(new QuestionModule("নদীর প্লাবন ঘটার প্রধান কারণ কী?", "B", "পাহাড়ি এলাকা", "নদীর পাড় বাঁধ ভাঙা", "অতিরিক্ত মৎস্য শিকার", "বনভূমি বৃদ্ধি"));
                add(new QuestionModule("পৃথিবীর তাপমাত্রা বৃদ্ধির ফলস্বরূপ কোনটি ঘটতে পারে?", "D", "বন্যা", "খরা", "বন্যপ্রাণী বিলুপ্তি", "উপরের সবগুলো"));
                add(new QuestionModule("বায়ুমণ্ডলীয় দূষণ রোধে কোনটি সবচেয়ে কার্যকর?", "A", "বায়ু পরিশোধন যন্ত্র ব্যবহার", "কার্বন নির্গমন বৃদ্ধি", "গাড়ির সংখ্যা বৃদ্ধি", "প্লাস্টিকের ব্যবহার বাড়ানো"));
                add(new QuestionModule("কোনটি একটি জীবাশ্ম জ্বালানি নয়?", "C", "কয়লা", "প্রাকৃতিক গ্যাস", "সৌর শক্তি", "পেট্রোলিয়াম"));
                add(new QuestionModule("বায়ুমণ্ডলে গ্রিনহাউস গ্যাস বৃদ্ধি পেলে কোনটি ঘটে?", "A", "তাপমাত্রা বৃদ্ধি", "তাপমাত্রা হ্রাস", "শক্তি উৎপাদন বৃদ্ধি", "বায়ু চলাচল বৃদ্ধি"));
                add(new QuestionModule("জীববৈচিত্র্য কমার প্রধান কারণ কী?", "B", "বনভূমি বৃদ্ধি", "বনভূমি ধ্বংস", "জলাশয় নির্মাণ", "জলবায়ু পরিবর্তন"));
                add(new QuestionModule("প্লাস্টিকের ব্যবহার কমাতে কোন পদ্ধতি কার্যকর?", "C", "অতিরিক্ত প্লাস্টিকের ব্যবহার", "সিঙ্গেল-ইউজ প্লাস্টিকের ব্যবহার বাড়ানো", "পুনর্ব্যবহারযোগ্য প্লাস্টিক ব্যবহার", "প্লাস্টিক পোড়ানো"));
                add(new QuestionModule("কোনটি পুনর্নবীকরণযোগ্য শক্তি নয়?", "D", "সৌর শক্তি", "বায়ু শক্তি", "জল শক্তি", "প্রাকৃতিক গ্যাস"));
                add(new QuestionModule("জলবায়ু পরিবর্তনের কারণে সমুদ্রের স্তর বৃদ্ধি পায় কেন?", "A", "বরফ গলে যাওয়া", "বায়ু চলাচল বৃদ্ধি", "বৃষ্টিপাত কমে যাওয়া", "তাপমাত্রা"));
                add(new QuestionModule("কোন গ্যাসটি প্রধানত গ্রিনহাউস প্রভাব সৃষ্টি করে?", "B", "অক্সিজেন", "কার্বন ডাই অক্সাইড", "নাইট্রোজেন", "হাইড্রোজেন"));
                add(new QuestionModule("জীববৈচিত্র্যের ক্ষতি হলে কোনটি সবচেয়ে বেশি ক্ষতিগ্রস্ত হয়?", "A", "ইকোসিস্টেম", "শিল্প উৎপাদন", "মানুষের বসবাস", "শক্তি উৎপাদন"));
                add(new QuestionModule("নিচের কোনটি পুনর্ব্যবহারযোগ্য উপাদান নয়?", "D", "কাগজ", "কাচ", "লোহা", "প্লাস্টিক"));
                add(new QuestionModule("পরিবেশ দূষণের সবচেয়ে বড় উৎস কোনটি?", "C", "বনভূমি ধ্বংস", "প্রাকৃতিক দুর্যোগ", "শিল্প বর্জ্য", "জলাশয় দূষণ"));
                add(new QuestionModule("কোনটি পরিবেশ সংরক্ষণে সাহায্য করে?", "B", "প্লাস্টিক পোড়ানো", "গাছ লাগানো", "জ্বালানি ব্যবহারে বৃদ্ধি", "ফসিল জ্বালানির ব্যবহার"));
                add(new QuestionModule("মিথেন কোন কাজে সবচেয়ে বেশি নির্গত হয়?", "A", "পশুপালন", "গাড়ির দূষণ", "কারখানার ধোঁয়া", "জলাশয় দূষণ"));
                add(new QuestionModule("ভূগর্ভস্থ জল কত গভীরে পাওয়া যায়?", "B", "১-২ ফুট", "১০০-২০০ ফুট", "৩০০-৪০০ ফুট", "১০০০ ফুটের নিচে"));
                add(new QuestionModule("কোনটি একটি টেকসই কৃষি পদ্ধতির উদাহরণ?", "C", "কীটনাশক ব্যবহার", "ফসলের জন্য প্রচুর জল ব্যবহার", "পুনর্ব্যবহারযোগ্য সার ব্যবহার", "বনভূমি নিধন"));
                add(new QuestionModule("পরিবেশ দূষণের ফলে সৃষ্ট স্বাস্থ্য সমস্যা কী?", "D", "অ্যাজমা", "ফুসফুসের ক্যান্সার", "হৃদরোগ", "উপরের সবগুলো"));
                add(new QuestionModule("কোনটি গ্লোবাল ওয়ার্মিংয়ের লক্ষণ নয়?", "D", "তাপমাত্রা বৃদ্ধি", "বরফ গলে যাওয়া", "সমুদ্রের স্তর বৃদ্ধি", "বায়ুর চাপ হ্রাস"));
                add(new QuestionModule("ওজোন স্তরকে রক্ষা করার জন্য কোনটি প্রয়োজন?", "A", "সিএফসি গ্যাসের ব্যবহার বন্ধ করা", "প্লাস্টিক ব্যবহার", "জলাশয় দূষণ", "অতিরিক্ত কৃষি জমি বৃদ্ধি"));
                add(new QuestionModule("কোনটি পরিবেশ দূষণের প্রাথমিক কারণ?", "C", "বায়ু প্রবাহ বৃদ্ধি", "উদ্ভিদের ক্ষতি", "শিল্প উৎপাদন", "বনভূমি বৃদ্ধি"));
                add(new QuestionModule("বায়ু দূষণের ফলে কী ঘটে?", "B", "বৃষ্টি কমে যায়", "অ্যাসিড বৃষ্টি হয়", "মাটি ক্ষয় হয়", "মৃত্তিকার উর্বরতা বৃদ্ধি"));
                add(new QuestionModule("প্রাকৃতিক সম্পদের অপচয় কমাতে কোন পদ্ধতিটি কার্যকর?", "C", "উচ্চ শক্তি ব্যবহার", "প্লাস্টিক পোড়ানো", "পুনর্ব্যবহার", "জীবাশ্ম জ্বালানির ব্যবহার বৃদ্ধি"));
                add(new QuestionModule("কোন পদক্ষেপটি জল দূষণ প্রতিরোধে কার্যকর?", "A", "শিল্প বর্জ্য পরিশোধন", "বায়ু দূষণ বৃদ্ধি", "গাছপালা নিধন", "অতিরিক্ত সার ব্যবহার"));
                add(new QuestionModule("প্লাস্টিকের পরিবর্তে কোন উপাদানটি পরিবেশবান্ধব?", "D", "পেট্রোলিয়াম", "নাইলন", "রাবার", "জৈব পদার্থ"));
                add(new QuestionModule("কোনটি জলবায়ু পরিবর্তনের সবচেয়ে বড় কারণ?", "B", "অক্সিজেনের পরিমাণ বৃদ্ধি", "গ্রিনহাউস গ্যাস নির্গমন", "জলাশয়ের সংখ্যা বৃদ্ধি", "জীবাশ্ম জ্বালানির ব্যবহার হ্রাস"));
                add(new QuestionModule("বৈশ্বিক উষ্ণায়ন কমাতে কোনটি প্রয়োজন?", "D", "প্লাস্টিক ব্যবহারে উৎসাহিত করা", "কয়লার ব্যবহার বাড়ানো", "গাছ কাটা", "বৈদ্যুতিক যানবাহনের ব্যবহার বাড়ানো"));
                add(new QuestionModule("জল সংরক্ষণে কোন পদ্ধতিটি সবচেয়ে কার্যকর?", "A", "ফসল চাষে জল কম ব্যবহার করা", "অতিরিক্ত জলাধার নির্মাণ", "পানির অপচয় বৃদ্ধি", "নদীর জল প্রবাহ কমানো"));
                add(new QuestionModule("জীববৈচিত্র্য সংরক্ষণের জন্য কোনটি গুরুত্বপূর্ণ?", "C", "শিল্পায়ন", "শক্তি উৎপাদন", "বনভূমি সংরক্ষণ", "কারখানা স্থাপন"));
                add(new QuestionModule("কোনটি একটি পরিবেশ বান্ধব শক্তির উৎস?", "B", "প্রাকৃতিক গ্যাস", "বায়ু শক্তি", "পেট্রোলিয়াম", "কয়লা"));
                add(new QuestionModule("মাটি দূষণের ফলে কী ঘটে?", "C", "মৃত্তিকার উর্বরতা বৃদ্ধি", "উদ্ভিদের বৃদ্ধি বৃদ্ধি পায়", "খাদ্য উৎপাদনে হ্রাস", "গাছপালা বেশি জন্মায়"));
                add(new QuestionModule("কোনটি বনভূমি সংরক্ষণের প্রধান উপায়?", "A", "অতিরিক্ত গাছ লাগানো", "জমির উন্নয়ন", "গাছ কাটার সংখ্যা বাড়ানো", "বনভূমিতে কারখানা স্থাপন"));
                add(new QuestionModule("কোনটি সবুজ শক্তি উৎসের উদাহরণ নয়?", "D", "সৌর শক্তি", "বায়ু শক্তি", "জল শক্তি", "কয়লা"));
                add(new QuestionModule("পরিবেশগত স্থিতিশীলতা নিশ্চিত করতে কোনটি গুরুত্বপূর্ণ?", "B", "বনভূমি ধ্বংস", "প্রাকৃতিক সম্পদ সংরক্ষণ", "প্লাস্টিক ব্যবহার", "জীবাশ্ম জ্বালানির ব্যবহার বৃদ্ধি"));
                add(new QuestionModule("কোনটি জীবাশ্ম জ্বালানি ব্যবহারের একটি বিকল্প?", "C", "কয়লা", "পেট্রোল", "সৌরশক্তি", "ডিজেল"));
                add(new QuestionModule("কোন পদক্ষেপটি বনভূমি ক্ষয় রোধে কার্যকর?", "A", "বনভূমি পুনরুদ্ধার", "শহর সম্প্রসারণ", "কারখানা স্থাপন", "শিল্পায়ন"));
                add(new QuestionModule("জলবায়ু পরিবর্তনের জন্য কোনটি দায়ী?", "D", "বনভূমি বৃদ্ধি", "বায়ু দূষণ কমানো", "অতিরিক্ত কৃষি চাষ", "গ্রিনহাউস গ্যাস নির্গমন বৃদ্ধি"));
                add(new QuestionModule("কোনটি বায়ু দূষণের একটি বড় উৎস?", "A", "যানবাহনের ধোঁয়া", "নদীর পানি", "বনভূমি ক্ষয়", "কৃষি জমি"));
                add(new QuestionModule("পরিবেশ বান্ধব শক্তি উৎপাদনে কোনটি বেশি কার্যকর?", "B", "প্রাকৃতিক গ্যাস", "বায়ু টারবাইন", "কয়লা বিদ্যুৎ", "ফসিল ফুয়েল"));
                add(new QuestionModule("কোনটি জলবায়ু পরিবর্তন প্রতিরোধে একটি সমাধান নয়?", "D", "পুনর্ব্যবহারযোগ্য শক্তি ব্যবহার", "বন সংরক্ষণ", "শক্তি সঞ্চয়", "প্লাস্টিক পোড়ানো"));
                add(new QuestionModule("কোনটি বায়ুমণ্ডলীয় দূষণ পরিমাপের একটি উপায়?", "C", "জল স্তর সূচক", "বৃষ্টিপাতের মাত্রা", "বায়ু মানের সূচক", "তাপমাত্রা মান সূচক"));
                add(new QuestionModule("কোনটি টেকসই কৃষি পদ্ধতির উদাহরণ?", "A", "জৈব চাষ", "প্লাস্টিকের ব্যবহার", "কীটনাশক ব্যবহার", "অতিরিক্ত সেচ"));
                add(new QuestionModule("কোনটি পরিবেশগতভাবে ক্ষতিকর পদার্থ?", "C", "জল", "মাটি", "প্লাস্টিক", "সৌর শক্তি"));
                add(new QuestionModule("কোন পদক্ষেপটি পরিবেশগত দূষণ কমাতে সাহায্য করে?", "B", "জলাশয় নির্মাণ", "বায়ু পরিশোধন", "গাছপালা ধ্বংস", "প্লাস্টিক পোড়ানো"));
                add(new QuestionModule("গ্রিনহাউস গ্যাস বৃদ্ধির ফলে কোনটি ঘটতে পারে?", "A", "বৈশ্বিক উষ্ণতা বৃদ্ধি", "বরফের স্তর বৃদ্ধি", "বায়ু চলাচল বৃদ্ধি", "তাপমাত্রা হ্রাস"));
                add(new QuestionModule("জলবায়ু পরিবর্তনের ফলে সমুদ্রের কোন প্রভাব পড়ে?", "C", "বায়ু প্রবাহ বৃদ্ধি", "তাপমাত্রা হ্রাস", "সমুদ্রের স্তর বৃদ্ধি", "বরফের পরিমাণ বৃদ্ধি"));
                add(new QuestionModule("কোনটি টেকসই শক্তির উদাহরণ?", "D", "কয়লা", "পেট্রোলিয়াম", "প্রাকৃতিক গ্যাস", "সৌর শক্তি"));
                add(new QuestionModule("কোনটি বায়ুদূষণের সরাসরি ফলাফল?", "B", "পানির স্তর বৃদ্ধি", "অ্যাসিড বৃষ্টি", "বনভূমি বৃদ্ধি", "বন্যপ্রাণী সংরক্ষণ"));
                add(new QuestionModule("কোনটি পরিবেশ সংরক্ষণের একটি বৈশ্বিক উদ্যোগ?", "A", "কিয়োটো প্রটোকল", "প্যারিস চুক্তি", "জাতিসংঘ", "গ্রিনপিস"));
                add(new QuestionModule("বায়ু দূষণ মাপার জন্য কোন যন্ত্রটি ব্যবহার করা হয়?", "C", "অ্যানিমোমিটার", "থার্মোমিটার", "এয়ার কোয়ালিটি মনিটর", "বারোমিটার"));
                add(new QuestionModule("কোনটি একটি জীবাশ্ম জ্বালানির প্রকার নয়?", "D", "কয়লা", "তেল", "প্রাকৃতিক গ্যাস", "বায়ু শক্তি"));
                add(new QuestionModule("গ্রিনহাউস প্রভাব কোনটি বৃদ্ধি করে?", "A", "গ্লোবাল ওয়ার্মিং", "অক্সিজেনের মাত্রা", "পানি প্রবাহ", "জলাশয়ের সংখ্যা"));
                add(new QuestionModule("কোন প্রক্রিয়াটি কার্বন ডাই অক্সাইডের মাত্রা কমায়?", "C", "শিল্প উৎপাদন", "গাড়ির নির্গমন", "গাছপালা রোপণ", "প্লাস্টিকের ব্যবহার"));
                add(new QuestionModule("বায়ু দূষণের উৎস কী?", "B", "গাছের বৃদ্ধি", "যানবাহনের ধোঁয়া", "বৃষ্টিপাত", "মৃত্তিকা ক্ষয়"));
                add(new QuestionModule("পরিবেশ রক্ষার জন্য কোন শক্তির উৎসটি বেশি টেকসই?", "D", "কয়লা", "পেট্রোল", "প্রাকৃতিক গ্যাস", "সৌর শক্তি"));
                add(new QuestionModule("কোনটি পরিবেশের জন্য সবচেয়ে ক্ষতিকারক?", "A", "অপচনশীল বর্জ্য", "জৈব সার", "পুনর্ব্যবহারযোগ্য প্লাস্টিক", "বায়ু প্রবাহ"));
                add(new QuestionModule("বৃষ্টির পানিতে অতিরিক্ত কার্বন ডাই অক্সাইড মিশে গেলে কী ঘটে?", "C", "জলদূষণ", "জলজ উদ্ভিদের বৃদ্ধি", "অ্যাসিড বৃষ্টি", "মাটি ক্ষয়"));
                add(new QuestionModule("কোনটি পুনর্ব্যবহারযোগ্য শক্তি উৎস নয়?", "B", "সৌর শক্তি", "প্রাকৃতিক গ্যাস", "বায়ু শক্তি", "জল শক্তি"));
                add(new QuestionModule("জল দূষণের ফলে কোনটি সবচেয়ে বেশি ক্ষতিগ্রস্ত হয়?", "D", "শিল্প উৎপাদন", "বায়ু প্রবাহ", "শক্তি উৎপাদন", "জলজ প্রাণী ও উদ্ভিদ"));
                add(new QuestionModule("কোনটি টেকসই কৃষির উদাহরণ?", "A", "জল সংরক্ষণ চাষাবাদ", "কীটনাশক ব্যবহারের বৃদ্ধি", "বনভূমি নিধন", "অতিরিক্ত সার ব্যবহার"));
                add(new QuestionModule("কোনটি জৈব পদার্থের উদাহরণ?", "C", "প্লাস্টিক", "কাচ", "কাগজ", "ধাতু"));
                add(new QuestionModule("গ্লোবাল ওয়ার্মিংয়ের কারণে কোনটি ঘটতে পারে?", "B", "বায়ু প্রবাহ হ্রাস", "সমুদ্রের স্তর বৃদ্ধি", "পাহাড়ের উঁচুতা হ্রাস", "বরফের স্তর বৃদ্ধি"));
                add(new QuestionModule("কোন পদক্ষেপটি জল দূষণ প্রতিরোধে সহায়ক?", "D", "শিল্প কারখানার সংখ্যা বৃদ্ধি", "বনভূমি ধ্বংস", "জীবাশ্ম জ্বালানির ব্যবহার", "শিল্প বর্জ্য পরিশোধন"));
                add(new QuestionModule("কোনটি কার্বন পদার্থ নয়?", "D", "কাঠ", "কাগজ", "জৈব সার", "লোহা"));
                add(new QuestionModule("কোনটি মাটির উর্বরতা বৃদ্ধিতে সহায়ক?", "A", "জৈব সার", "প্লাস্টিক", "কাচ", "তেল"));
                add(new QuestionModule("কোনটি বৈদ্যুতিক শক্তির উৎস?", "C", "কয়লা", "প্রাকৃতিক গ্যাস", "জলবিদ্যুৎ", "তেল"));
                add(new QuestionModule("জলবায়ু পরিবর্তনের প্রভাবে কোনটি ঘটতে পারে?", "B", "বৃষ্টিপাত কমে যাওয়া", "খরা", "বায়ু প্রবাহ বৃদ্ধি", "তাপমাত্রা হ্রাস"));
                add(new QuestionModule("বনভূমি ক্ষয়ের প্রধান কারণ কী?", "A", "অতিরিক্ত গাছ কাটা", "বায়ু প্রবাহ বৃদ্ধি", "পাহাড়ি এলাকা", "জল প্রবাহ বৃদ্ধি"));
                add(new QuestionModule("জলবায়ু পরিবর্তন মোকাবিলায় কোনটি একটি কার্যকর সমাধান?", "C", "গাড়ির সংখ্যা বৃদ্ধি", "শিল্পায়ন বাড়ানো", "নবায়নযোগ্য শক্তির ব্যবহার", "জলাশয় সংখ্যা বৃদ্ধি"));
                add(new QuestionModule("কোনটি পরিবেশগত দূষণ কমানোর একটি উপায়?", "B", "প্লাস্টিক পোড়ানো", "বর্জ্য পুনর্ব্যবহার", "অতিরিক্ত সার ব্যবহার", "জীবাশ্ম জ্বালানির ব্যবহার বৃদ্ধি"));
                add(new QuestionModule("কোনটি জীববৈচিত্র্যের ক্ষতির প্রভাব নয়?", "D", "ইকোসিস্টেমের ক্ষতি", "বন্যপ্রাণীর বিলুপ্তি", "উদ্ভিদের প্রজাতি হ্রাস", "মৃত্তিকার উর্বরতা বৃদ্ধি"));
                add(new QuestionModule("বায়ুমণ্ডলের কোন স্তরে ওজোন স্তর অবস্থিত?", "B", "ট্রপোস্ফিয়ার", "স্ট্রাটোস্ফিয়ার", "মেসোস্ফিয়ার", "থার্মোস্ফিয়ার"));
            }
        };
        QuestionModule.createQuestionsForSubject("Environmental Science", R.drawable.environment, questions);


        /*  questions = new ArrayList<QuestionModule>() {
           {

          }
        };
        QuestionModule.createQuestionsForSubject("", R.drawable.islam, questions); */

            }
        }


