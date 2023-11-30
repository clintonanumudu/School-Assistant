package com.example.schoolassistant2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StudyFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private String refreshedAccessToken;
    private String currentQuestion = "";
    private String groupMode = "new";
    private int frontOrBack = 0;
    private int currentSquareNumber = 1;
    private int groupNumber = -1;
    private int percentStudied = 0;
    private View view;
    private Map<String, String> questionGroup;
    private List<String> completedQuestions = new ArrayList<>();
    private List<String> answerResults = new ArrayList<>();
    private List<Integer> completedGroups = new ArrayList<>();
    private List<Map<String, String>> questionsAndAnswers;
    public StudyFragment() {}
    public static StudyFragment newInstance(String param1, String param2) {
        StudyFragment fragment = new StudyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_study, container, false);
        initializeViews();
        return view;
    }

    private void initializeViews() {
        questionsAndAnswers = createQADictionary();
        changeGroup();
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        Button correctBtn = view.findViewById(R.id.correct);
        Button wrongBtn = view.findViewById(R.id.wrong);
        Button flashcard = view.findViewById(R.id.flashcard);

        correctBtn.setOnClickListener(v -> {
            updateStreak("correct");
            changeQuestion();
        });

        wrongBtn.setOnClickListener(v -> {
            updateStreak("wrong");
            changeQuestion();
        });

        flashcard.setOnClickListener(v -> {
            if (frontOrBack == 0) {
                String question = (String) flashcard.getText();
                flashcard.setText(questionGroup.get(question));
                frontOrBack = 1;
            } else if (frontOrBack == 1) {
                flashcard.setText(currentQuestion);
                frontOrBack = 0;
            }
        });
    }

    private List<Map<String, String>> createQADictionary() {
        String filePath = requireContext().getFilesDir() + "/q_and_a.json";
        try {
            String jsonString = readFromFile(filePath);
            if (jsonString != null) {
                JSONObject jsonObject = new JSONObject(jsonString);
                JSONArray flashcardsArray = jsonObject.getJSONArray("flashcards");
                List<Map<String, String>> dividedGroups = new ArrayList<>();
                List<Map.Entry<String, String>> shuffledEntries = new ArrayList<>();
                for (int i = 0; i < flashcardsArray.length(); i++) {
                    JSONObject flashcard = flashcardsArray.getJSONObject(i);
                    String question = flashcard.getString("question");
                    String answer = flashcard.getString("answer");
                    shuffledEntries.add(new AbstractMap.SimpleEntry<>(question, answer));
                }
                Collections.shuffle(shuffledEntries);
                int totalEntries = shuffledEntries.size();
                int startIndex = 0;
                int endIndex;
                while (startIndex < totalEntries) {
                    endIndex = Math.min(startIndex + 5, totalEntries);
                    List<Map.Entry<String, String>> sublist = shuffledEntries.subList(startIndex, endIndex);
                    Map<String, String> subgroup = new LinkedHashMap<>();
                    for (Map.Entry<String, String> entry : sublist) {
                        subgroup.put(entry.getKey(), entry.getValue());
                    }
                    dividedGroups.add(subgroup);
                    startIndex = endIndex;
                }
                return dividedGroups;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readFromFile(String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = new FileInputStream(new File(filePath));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            inputStream.close();
        } catch (IOException e) {
            Log.e("Study Fragment", "Error reading from file: " + e.getMessage());
        }
        return stringBuilder.toString();
    }

    private void changeQuestion() {
        List<String> keys = new ArrayList<>(questionGroup.keySet());
        String randomQuestion = "";
        while (completedQuestions.contains(randomQuestion) || randomQuestion.equals("")) {
            if (currentSquareNumber == 6 || (questionGroup.size() < 5 && currentSquareNumber == questionGroup.size() + 1)) {
                currentSquareNumber = 1;
                if (!answerResults.contains("wrong")) {
                    changeGroup();
                }
                else {
                    repeatGroup();
                }
                return;
            }
            Random random = new Random();
            int randomIndex = random.nextInt(keys.size());
            randomQuestion = keys.get(randomIndex);
        }
        Button flashcard = view.findViewById(R.id.flashcard);
        flashcard.setText(randomQuestion);
        currentQuestion = randomQuestion;
        frontOrBack = 0;
        if (!completedQuestions.contains(randomQuestion)) {
            completedQuestions.add(randomQuestion);
        }
        Log.d("CQ", completedGroups.toString());
    }

    private void changeGroup() {
        completedQuestions.clear();
        LinearLayout linearLayout = view.findViewById(R.id.streak);
        int childCount = linearLayout.getChildCount();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < childCount; i++) {
                    View child = linearLayout.getChildAt(i);
                    if (child instanceof TextView) {
                        child.setBackgroundResource(R.drawable.streak_undone);
                    }
                }
            }
        }, 100);
        changePercentStudied();
        if (groupMode == "new") {
            while (completedGroups.contains(groupNumber) || groupNumber == -1) {
                Random random = new Random();
                int randomIndex = random.nextInt(questionsAndAnswers.size());
                groupNumber = randomIndex;
            }
            completedGroups.add(groupNumber);
            if (completedGroups.size() > 0) {
                groupMode = "review";
            }
        }
        else if (groupMode == "review") {
            if (completedGroups.size() == questionsAndAnswers.size() && !answerResults.contains("wrong")) {
                repeatGroup();
                percentStudied = 100;
                TextView studyProgress = view.findViewById(R.id.studyprogress);
                studyProgress.setText(percentStudied + "% Studied");
                return;
            }
            Random rand = new Random();
            int randomIndex = rand.nextInt(completedGroups.size());
            groupNumber = completedGroups.get(randomIndex);
            groupMode = "new";
        }
        questionGroup = questionsAndAnswers.get(groupNumber);
        changeQuestion();
    }

    private void repeatGroup() {
        completedQuestions.clear();
        LinearLayout linearLayout = view.findViewById(R.id.streak);
        int childCount = linearLayout.getChildCount();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < childCount; i++) {
                    View child = linearLayout.getChildAt(i);
                    if (child instanceof TextView) {
                        child.setBackgroundResource(R.drawable.streak_undone);
                    }
                }
            }
        }, 100);
        changePercentStudied();
        changeQuestion();
    }

    private void updateStreak(String answerIs) {
        TextView streakSquare = view.findViewById(getResources().getIdentifier("square" + currentSquareNumber, "id", requireActivity().getPackageName()));
        if (answerIs == "correct") {
            streakSquare.setBackgroundResource(R.drawable.streak_correct);
        }
        else if (answerIs == "wrong") {
            streakSquare.setBackgroundResource(R.drawable.streak_wrong);
        }
        answerResults.add(answerIs);
        Log.d("answers", answerResults.toString());
        currentSquareNumber++;
    }

    private void changePercentStudied() {
        if (!answerResults.contains("wrong") && (groupMode.equals("new") || completedGroups.size() == 1)) {
            percentStudied += Math.round(100 / questionsAndAnswers.size());
            TextView studyProgress = view.findViewById(R.id.studyprogress);
            studyProgress.setText(percentStudied + "% Studied");
        }
        answerResults.clear();
    }
}
