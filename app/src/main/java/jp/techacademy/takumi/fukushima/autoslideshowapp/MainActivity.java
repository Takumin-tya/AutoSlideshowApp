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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int PERMISSION_REQUEST_CODE = 100;
    ImageView imageView;
    ArrayList<Uri> uris = new ArrayList<Uri>(); //取得したURI格納用
    int nowIndex = 0;                          //現在開いている画像の番号格納用
    boolean slideshow = false;               //スライドショー開始/停止判定
    ImageSwitcher imageSwitcher;

    float touchX;     //タッチイベント時の座標(X)
    float touchY;     //タッチイベント時の座標(Y)

    ArrayList<Integer> imageID = new ArrayList<Integer>();


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

        Button playStopButton = (Button) findViewById(R.id.playStopButton);
        playStopButton.setOnClickListener(this);

        Button prevButton = (Button) findViewById(R.id.prevButton);
        prevButton.setOnClickListener(this);

        Button nextButton = (Button) findViewById(R.id.nextButton);
        nextButton.setOnClickListener(this);

        imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher);
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory(){
            @Override
            public View makeView(){
                imageView = new ImageView(getApplicationContext());
                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return imageView;
            }
        });

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

        imageSwitcher.setInAnimation(this, android.R.anim.fade_in);
        imageSwitcher.setOutAnimation(this, android.R.anim.fade_out);

        if(nowIndex < 0){
            nowIndex = uris.size() - 1;
        }else if(nowIndex >= uris.size()){
            nowIndex = 0;
        }
        //imageView.setImageURI(uris.get(nowIndex));
        imageSwitcher.setImageURI(uris.get(nowIndex));
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
                imageID.add(fieldIndex);
                Long id = cursor.getLong(fieldIndex);
                uris.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));
            }while (cursor.moveToNext());

            //imageView.setImageURI(uris.get(0));
            imageSwitcher.setImageURI(uris.get(0));
        }
        cursor.close();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                //touchY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if(!slideshow) {
                    if (100 < touchX - event.getX()) {
                        //次へ
                        movePosition(1);
                    } else if (100 < event.getX() - touchX) {
                        //前へ
                        movePosition(-1);
                    }
                }
                break;
        }

        return true;
    }
}
