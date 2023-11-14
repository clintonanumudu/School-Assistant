package com.example.schoolassistant2;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StudyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StudyFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private String refreshedAccessToken;

    public StudyFragment() {
        // Required empty public constructor
    }

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

    private Map<String, String> questionsAndAnswers;
    private String currentQuestion = "";
    private int frontOrBack = 0;
    private int streak = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_study, container, false);

        changeQuestion(view);

        TextView streakText = view.findViewById(R.id.streak);

        Button correctBtn = view.findViewById(R.id.correct);

        Button wrongBtn = view.findViewById(R.id.wrong);

        Button flashcard = view.findViewById(R.id.flashcard);

        correctBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streak++;
                streakText.setText(String.valueOf(streak) + " correct questions in a row");
                String question = (String) flashcard.getText();
                while (((String)flashcard.getText()).equals(question)) {
                    changeQuestion(view);
                }
            }
        });

        wrongBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streak = 0;
                streakText.setText(String.valueOf(streak) + " correct questions in a row");
                String question = (String) flashcard.getText();
                while (((String)flashcard.getText()).equals(question)) {
                    changeQuestion(view);
                }
            }
        });

        flashcard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (frontOrBack == 0) {
                    String question = (String) flashcard.getText();
                    flashcard.setText(questionsAndAnswers.get(question));
                    frontOrBack = 1;
                }
                else if (frontOrBack == 1) {
                    flashcard.setText(currentQuestion);
                    frontOrBack = 0;
                }
            }
        });

        return view;
    }

    private void changeQuestion(View view) {

        questionsAndAnswers = createQADictionary(getContext());

        // Get a list of only the questions
        List<String> keys = new ArrayList<>(questionsAndAnswers.keySet());

        // Choose a random question from the bunch
        Random random = new Random();
        int randomIndex = random.nextInt(keys.size());
        String randomQuestion = keys.get(randomIndex);
        
        // Set the text of the flashcard to the random question
        Button flashcard = view.findViewById(R.id.flashcard);
        flashcard.setText(randomQuestion);
        currentQuestion = randomQuestion;
        frontOrBack = 0;
    }

    private Map<String, String> createQADictionary(Context context) {
        // Get the external storage directory
        File externalStorageDir = Environment.getExternalStorageDirectory();

        // Specify the file path
        String filePath = externalStorageDir + "/Documents/School Assistant/q_and_a.json";

        // Read the JSON file and convert it to a JSON object
        try {
            String jsonString = readFromFile(filePath);
            JSONObject jsonObject = new JSONObject(jsonString);

            // Get the "flashcards" array
            JSONArray flashcardsArray = jsonObject.getJSONArray("flashcards");

            // Create a dictionary/map to store questions and answers
            Map<String, String> qaDictionary = new HashMap<>();

            // Iterate over each flashcard
            for (int i = 0; i < flashcardsArray.length(); i++) {
                JSONObject flashcard = flashcardsArray.getJSONObject(i);

                // Extract question and answer
                String question = flashcard.getString("question");
                String answer = flashcard.getString("answer");

                // Store in the dictionary/map
                qaDictionary.put(question, answer);
            }

            return qaDictionary;

        } catch (JSONException e) {
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

    public void createQAndAFile(Context context) {
        // JSON content
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Question 1", "Answer 1");
            jsonObject.put("Question 2", "Answer 2");
            jsonObject.put("Question 3", "Answer 3");
            jsonObject.put("Question 4", "Answer 4");
            jsonObject.put("Question 5", "Answer 5");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get the app's private directory
        File directory = context.getFilesDir();

        // Create the file
        File file = new File(directory, "q_and_a.json");

        try {
            // Write the JSON content to the file
            FileWriter writer = new FileWriter(file);
            writer.write(jsonObject.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readAndLogJSONFile(Context context) {
        // Get the app's private directory
        File directory = context.getFilesDir();

        // Create the file object
        File file = new File(directory, "q_and_a.json");

        try {
            // Read the file content
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            // Log the JSON content
            Log.d("File Content", content.toString());

            // If you want to parse the JSON content, you can do it here
            JSONObject jsonObject = new JSONObject(content.toString());
            // Now you can work with the JSON data
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
