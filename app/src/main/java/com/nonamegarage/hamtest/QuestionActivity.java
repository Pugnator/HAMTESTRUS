package com.nonamegarage.hamtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
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
    private final int max_errors_possible = 5;
    private final int questions_number_max = 20;
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
                RadioButton btn = (RadioButton)o;
                btn.setId(i + 1000);
                rButList.add((RadioButton) o);
            }
        }
        resetTestState();
    }

    @Override
    public void onBackPressed()
    {
        if(backButtonCount >= 1)
        {
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
        }
        else
        {
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

    public void resetTestState()
    {
        NumberPicker np = (NumberPicker) findViewById(R.id.LevelSelector);
        np.setMaxValue(4);
        np.setMinValue(3);
        np.setValue(category);
        np.setVisibility(View.VISIBLE);
        np.setWrapSelectorWheel(false);
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
    }

    public void parseQuestion(View curview) {

        switch (answers_passed) {
            case 0:
                NumberPicker np = (NumberPicker) findViewById(R.id.LevelSelector);
                category = np.getValue();
                setTitle("Категория "+ Integer.toString(category));
                np.setVisibility(View.GONE);
                qLayout.setVisibility(View.VISIBLE);
                timer.start();
                nextBtn.setText("Ответ");
                break;
            case questions_number_max:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setCancelable(true);
                if(errors_number <= max_errors_possible)
                {
                    builder1.setMessage("Сдано!");
                }
                else
                {
                    builder1.setMessage("Не сдан!");
                }
                AlertDialog alert11 = builder1.create();
                alert11.show();
                //qLayout.setVisibility(View.GONE);
                setTitle(getString(R.string.app_name));
                nextBtn.setText("Заново");
                timer.stop();
                break;
            default:
                break;
        }

        RadioGroup rgroup = (RadioGroup) findViewById(R.id.RadioGroup);
        if (answers_passed >= 1) {
            int answer = rgroup.getCheckedRadioButtonId();

            if (-1 >= answer) {
                Toast.makeText(getApplicationContext(), "Выберите ответ", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            answer-=1002;
            if ("Ответ" == nextBtn.getText()) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db.isOpen()) {
                    try {
                        Cursor c = db.rawQuery("select serial from answers where question=" + last_question_id + " and is_correct=1", null);
                        if (c.moveToFirst()) {
                            if(answer == c.getInt(0))
                            {
                                rButList.get(answer).setTextColor(Color.GREEN);
                                Toast.makeText(getApplicationContext(), "Правильно!", Toast.LENGTH_SHORT)
                                        .show();
                            }
                            else
                            {
                                errors_number++;
                                rButList.get(answer).setTextColor(Color.RED);
                                rButList.get(c.getInt(0) -1).setTextColor(Color.GREEN);
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
            }
            else if ("Заново" == nextBtn.getText())
            {
                resetTestState();
                return;
            }
            else {
                nextBtn.setText("Ответ");
                rgroup.clearCheck();
            }

        }

        progress.setProgress(answers_passed++);
        progress.setSecondaryProgress(answers_passed);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db.isOpen()) {
            try {
                Cursor c = db.rawQuery("select ID,question_text from questions where category=" + category + " ORDER BY RANDOM() LIMIT 1", null);

                if (c.moveToFirst()) {
                    qText.setText(c.getString(1).trim());
                    last_question_id = c.getInt(0);
                }

                c = db.rawQuery("select answer_text from answers where question=" + last_question_id, null);
                Iterator<RadioButton> rButIter = rButList.iterator();
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    RadioButton btn = rButIter.next();
                    btn.setTextColor(Color.BLACK);
                    btn.setText(c.getString(0).trim());
                }
                c.close();
            } catch (Exception e) {

            }
        }
    }
}
