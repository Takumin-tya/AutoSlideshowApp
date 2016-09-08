package jp.techacademy.takumi.fukushima.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int PERMISSION_REQUEST_CODE = 100;
    ImageView imageView;
    ArrayList<Uri> uris = new ArrayList<Uri>();
    int nowIndex = 0;
    boolean slideshow = false;


    public  class MainTimerTask extends TimerTask{
        @Override
        public void run(){
            if(slideshow){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        movePosition(1);
                    }
                });
            }
        }
    }

    Timer timer = new Timer();
    TimerTask timerTask = new MainTimerTask();
    android.os.Handler handler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);

        Button playStopButton = (Button) findViewById(R.id.playStopButton);
        playStopButton.setOnClickListener(this);

        Button prevButton = (Button) findViewById(R.id.prevButton);
        prevButton.setOnClickListener(this);

        Button nextButton = (Button) findViewById(R.id.nextButton);
        nextButton.setOnClickListener(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //パーミッションの確認
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                //許可されている
                getContentInfo();
            }else{
                //許可されていない
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }else{
            getContentInfo();
        }

        timer.schedule(timerTask, 0, 2000);
    }

    @Override
    public void onClick(View v){
        if(v.getId() == R.id.prevButton){
            if(!slideshow) {
                movePosition(-1);
            }
        }else if(v.getId() == R.id.nextButton){
            if(!slideshow) {
                movePosition(1);
            }
        }else if(v.getId() == R.id.playStopButton){
            slideshow = !slideshow;
        }
    }

    private void movePosition(int move){
        nowIndex = nowIndex + move;

        if(nowIndex < 0){
            nowIndex = uris.size() - 1;
        }else if(nowIndex >= uris.size()){
            nowIndex = 0;
        }
        imageView.setImageURI(uris.get(nowIndex));
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, String permission[], int[] grantResult){
        switch (resultCode){
            case PERMISSION_REQUEST_CODE:
                if(grantResult[0] == PackageManager.PERMISSION_GRANTED){
                    //許可された
                    getContentInfo();
                }
                break;
            default:
                break;
        }
    }

    private void getContentInfo(){
        //画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,   //データの種類
                null,   //項目
                null,   //フィルター条件
                null,   //フィルター用パラメータ
                null    //ソート
        );

        if(cursor.moveToFirst()){

            do {
                int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                Long id = cursor.getLong(fieldIndex);
                uris.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));
            }while (cursor.moveToNext());

            imageView.setImageURI(uris.get(0));
        }
        cursor.close();
    }
}
