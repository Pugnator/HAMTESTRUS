package com.nonamegarage.hamtest;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
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

    DBHelper dbHelper;
    private int category = 4;
    private int answers_passed = 0;
    private int errors_number = 0;
    private int last_question_id = 1;
    private Chronometer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        dbHelper = new DBHelper(this, getApplicationInfo().dataDir);
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
    }

    public void parseQuestion(View curview) {
        TextView qText = (TextView) findViewById(R.id.question);
        RadioGroup rGroup = (RadioGroup) findViewById(R.id.RadioGroup);
        ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        Button nextBtn = (Button) findViewById(R.id.nextBtn);
        LinearLayout qLayout = (LinearLayout) findViewById(R.id.qLayout);

        switch (answers_passed) {
            case 0:
                qLayout.setVisibility(View.VISIBLE);
                timer.start();
                nextBtn.setText("Ответ");
                break;
            case 25:
                if(19 >= 25 - errors_number)
                {
                    nextBtn.setText("Сдано!");
                }
                else
                {
                    nextBtn.setText("Не сдано!");
                }
                qLayout.setVisibility(View.GONE);
                nextBtn.setText("Заново");
                timer.stop();
                break;
            default:
                break;
        }

        int count = rGroup.getChildCount();
        ArrayList<RadioButton> rButList = new ArrayList<RadioButton>();
        for (int i = 0; i < count; i++) {
            View o = rGroup.getChildAt(i);
            if (o instanceof RadioButton) {
                rButList.add((RadioButton) o);
            }
        }

        if (answers_passed >= 1) {
            int answer = 5;
            for (int i = 0; i < 4; ++i) {
                if (rButList.get(i).isChecked()) {
                    answer = i;
                    break;
                }
            }

            if (5 == answer) {
                Toast.makeText(getApplicationContext(), "Выберите ответ", Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            if ("Ответ" == nextBtn.getText()) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db.isOpen()) {
                    try {
                        Cursor c = db.rawQuery("select serial from answers where question=" + last_question_id + " and is_correct=1", null);
                        if (c.moveToFirst()) {
                            if(answer+1 == c.getInt(0))
                            {
                                rButList.get(answer).setTextColor(Color.GREEN);
                                Toast.makeText(getApplicationContext(), "Правильно!", Toast.LENGTH_SHORT)
                                        .show();
                            }
                            else
                            {
                                errors_number++;
                                rButList.get(answer).setTextColor(Color.RED);
                                rButList.get(c.getInt(0)-1).setTextColor(Color.GREEN);
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
            } else {
                nextBtn.setText("Ответ");
                for (int i = 0; i < 4; ++i) {
                    rButList.get(i).setChecked(false);
                    rButList.get(i).setTextColor(Color.BLACK);
                }
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
                    rButIter.next().setText(c.getString(0).trim());
                }
                c.close();
            } catch (Exception e) {

            }
        }
    }
}
