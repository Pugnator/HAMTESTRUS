package com.nonamegarage.hamtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

class DBHelper extends SQLiteOpenHelper {

    private String pathToSaveDBFile;
    private final Context myContext;

    public DBHelper(Context context, String filePath) {
        super(context, "hamtest.db", null, 1);
        this.myContext = context;
        pathToSaveDBFile = new StringBuffer(filePath).append("/").append("databases/hamtest.db").toString();
        try {
            copyDataBase();
        } catch (IOException e) {

        }
    }

    private void copyDataBase() throws IOException {
        OutputStream os = new FileOutputStream(pathToSaveDBFile);
        InputStream is = myContext.getAssets().open("db/hamtest.db");
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }
        is.close();
        os.flush();
        os.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            try {
                copyDataBase();
            } catch (IOException e) {

            }
        }
    }
}

public class QuestionActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private ImageView qPic;
    private TextView qText;
    private RadioGroup rGroup;
    private LinearLayout qLayout;
    private ProgressBar progress;
    private Button nextBtn;
    private RadioGroup rgroup;

    private ArrayList<RadioButton> rButList;

    public static final String PREFS_FILE = "Рreference";
    private int category = -1;
    private int backButtonCount = 0;
    private int answers_passed = 0;
    private int errors_number = 0;
    private int last_question_id = 1;
    private int max_errors_possible = 5;
    private int questions_number_max = 20;
    private Chronometer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        setTitle(getString(R.string.app_name));
        qText = (TextView) findViewById(R.id.question);
        rGroup = (RadioGroup) findViewById(R.id.RadioGroup);
        qLayout = (LinearLayout) findViewById(R.id.qLayout);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        nextBtn = (Button) findViewById(R.id.nextBtn);
        rgroup = (RadioGroup) findViewById(R.id.RadioGroup);
        dbHelper = new DBHelper(this, getApplicationInfo().dataDir);

        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        category = settings.getInt("category", 0);

        rButList = new ArrayList<RadioButton>();
        int count = rGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View o = rGroup.getChildAt(i);
            if (o instanceof RadioButton) {
                RadioButton btn = (RadioButton) o;
                btn.setId(i + 1000);
                rButList.add((RadioButton) o);
            }
        }
        resetTestState();
    }

    @Override
    public void onBackPressed() {
        if (backButtonCount >= 1) {
            SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            editor.putInt("category", category);
            editor.commit();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Нажмите \"Назад\" ещё раз, чтобы выйти", Toast.LENGTH_SHORT).show();
            backButtonCount++;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("category", category);
        editor.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_test);
    }

    public void resetTestState() {
        NumberPicker np = (NumberPicker) findViewById(R.id.LevelSelector);
        np.setMaxValue(4);
        np.setMinValue(3);
        np.setValue(category);
        np.setVisibility(View.VISIBLE);
        np.setWrapSelectorWheel(false);
        np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        timer = (Chronometer) findViewById(R.id.chrono);
        timer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long elapsedMillis = SystemClock.elapsedRealtime()
                        - timer.getBase();

                if (elapsedMillis > 3600 * 1000) {
                    String strElapsedMillis = "Время вышло!";
                    timer.stop();
                    Toast.makeText(getApplicationContext(),
                            strElapsedMillis, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        timer.setBase(SystemClock.elapsedRealtime());
        rgroup.clearCheck();
        qLayout.setVisibility(View.GONE);
        progress.setProgress(0);
        progress.setSecondaryProgress(0);

        backButtonCount = 0;
        answers_passed = 0;
        errors_number = 0;
        last_question_id = 1;
        questions_number_max = getMaxQuestionNumber(category);
        progress.setMax(questions_number_max);
    }

    byte[] getQuestionPicture(int index){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db.isOpen()) {
            try {
                Cursor c = db.rawQuery("select image from images where idx=" + index, null);
                if (c.moveToFirst()) {
                    byte[] pic=c.getBlob(0);
                    return pic;
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public int getMaxQuestionNumber(int category) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db.isOpen()) {
            try {
                Cursor c = db.rawQuery("select qnum from categories where level=" + category, null);
                if (c.moveToFirst()) {
                    return c.getInt(0);
                }
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    public void expandImage (View curView)
    {

    }

    public void parseQuestion(View curview) {

        if (0 == answers_passed) {

            questions_number_max = getMaxQuestionNumber(category);
            NumberPicker np = (NumberPicker) findViewById(R.id.LevelSelector);
            category = np.getValue();
            setTitle("Категория " + Integer.toString(category));
            np.setVisibility(View.GONE);
            qLayout.setVisibility(View.VISIBLE);
            timer.start();
            Log.d(getString(R.string.app_name), "Questions number: " + Integer.toString(questions_number_max));
            nextBtn.setText("Ответ");
        } else if (questions_number_max == answers_passed) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setCancelable(true);
            if (errors_number <= max_errors_possible) {
                Log.d(getString(R.string.app_name), "Errors: " + Integer.toString(errors_number));
                alertBuilder.setMessage("Сдано! Ошибок:" + Integer.toString(errors_number));
            } else {
                Log.d(getString(R.string.app_name), "Errors: " + Integer.toString(errors_number));
                alertBuilder.setMessage("Не сдан! Ошибок: " + Integer.toString(errors_number));
            }
            AlertDialog alert = alertBuilder.create();
            alert.show();
            setTitle(getString(R.string.app_name));
            nextBtn.setText("Заново");
            timer.stop();
        }

        RadioGroup rgroup = (RadioGroup) findViewById(R.id.RadioGroup);
        if (answers_passed >= 1) {
            int answer = rgroup.getCheckedRadioButtonId();

            if (-1 >= answer) {
                Toast.makeText(getApplicationContext(), "Выберите ответ", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            answer -= 1001;
            if ("Ответ" == nextBtn.getText()) {
                Log.d(getString(R.string.app_name), "Question " + Integer.toString(answers_passed) + " out of " + Integer.toString(questions_number_max));
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db.isOpen()) {
                    try {
                        String request = "select serial from answers where question=" + last_question_id + " and is_correct=1";
                        Cursor c = db.rawQuery(request, null);
                        if (c.moveToFirst()) {
                            int correct_answer = c.getInt(0);
                            if (answer == correct_answer) {
                                Log.d(getString(R.string.app_name), request);
                                Log.d(getString(R.string.app_name), "OK: Answer is " + Integer.toString(answer) + " when correct is " + Integer.toString(correct_answer));
                                rButList.get(answer - 1).setTextColor(Color.GREEN);
                                Toast.makeText(getApplicationContext(), "Правильно!", Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Log.d(getString(R.string.app_name), "WRONG: Answer is " + Integer.toString(answer) + " when correct is " + Integer.toString(correct_answer));
                                errors_number++;
                                rButList.get(answer - 1).setTextColor(Color.RED);
                                rButList.get(correct_answer - 1).setTextColor(Color.GREEN);
                                Toast.makeText(getApplicationContext(), "Неправильно!", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                        c.close();
                    } catch (Exception e) {

                    }
                }
                nextBtn.setText("Следующий вопрос");
                return;
            } else if ("Заново" == nextBtn.getText()) {
                resetTestState();
                return;
            } else {
                qPic.setVisibility(View.GONE);
                nextBtn.setText("Ответ");
                rgroup.clearCheck();
            }

        }

        progress.setProgress(answers_passed++);
        progress.setSecondaryProgress(answers_passed);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db.isOpen()) {
            try {
                Cursor c = db.rawQuery("select ID,question_text,question_image from questions where category=" + category + " ORDER BY RANDOM() LIMIT 1", null);
                int image_id =-1;
                if (c.moveToFirst()) {
                    image_id = c.getInt(2);
                    qText.setText(c.getString(1).trim());
                    last_question_id = c.getInt(0);
                }
                qPic = (ImageView) findViewById(R.id.qpic);
                if(0 != image_id)
                {
                    qPic.setVisibility(View.VISIBLE);
                    byte[] arr = getQuestionPicture(image_id);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(arr , 0, arr.length);
                    qPic.setImageBitmap(bitmap);
                }
                else
                {
                    qPic.setVisibility(View.GONE);
                }


                c = db.rawQuery("select answer_text from answers where question=" + last_question_id, null);
                Iterator<RadioButton> rButIter = rButList.iterator();
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    RadioButton btn = rButIter.next();
                    btn.setText("");
                    btn.setTextColor(Color.BLACK);
                    btn.setText(c.getString(0).trim());
                }
                c.close();
            } catch (Exception e) {

            }
        }
    }
}
