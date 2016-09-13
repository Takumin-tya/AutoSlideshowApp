package jp.techacademy.takumi.fukushima.autoslideshowapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MainActivity extends Activity implements View.OnClickListener{

    private static final int PERMISSION_REQUEST_CODE = 100;
    ImageView imageView;
    ArrayList<Uri> uris = new ArrayList<Uri>(); //取得したURI格納用
    int nowIndex = 0;                          //現在開いている画像の番号格納用
    boolean slideshow = false;               //スライドショー開始/停止判定

    ImageSwitcher imageSwitcher;
    View mainView;                             //メインVIew
    View containerView;                        //ボタン用View
    Animation inAnimation;                    //ボタン表示用アニメーション
    Animation outAnimation;                   //ボタン非表示用アニメーション

    Button playStopButton;  //スライドショーの開始/停止ボタン
    Button prevButton;      //前の画像へ
    Button nextButton;      //次の画像へ

    int timerCounter = 0;

    float touchX;     //タッチイベント時の座標(X)
    float touchY;     //タッチイベント時の座標(Y)

    ArrayList<Integer> imageID = new ArrayList<Integer>();

    //タイマークラスで２秒毎に更新
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
            if(0 < timerCounter){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        hideButton();
                    }
                });
            }
            if(timerCounter <= 1) {
                timerCounter++;
            }
        }
    }

    //タイマーのインスタンス化
    Timer timer = null;
    TimerTask timerTask;
    android.os.Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //アニメーションの設定
        inAnimation = (Animation) AnimationUtils.loadAnimation(this, R.anim.in_animation);
        outAnimation = (Animation) AnimationUtils.loadAnimation(this, R.anim.out_animation);

        containerView = (View) findViewById(R.id.contenerView);


        mainView = (View) findViewById(R.id.mainView);

        //ボタンの定義
        playStopButton = (Button) findViewById(R.id.playStopButton);
        playStopButton.setOnClickListener(this);

        prevButton = (Button) findViewById(R.id.prevButton);
        prevButton.setOnClickListener(this);

        nextButton = (Button) findViewById(R.id.nextButton);
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
    }

    //ボタンクリック
    @Override
    public void onClick(View v){

        timerCounter = 0;

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
            if(slideshow){
                playStopButton.setText("||");
                timer = new Timer();
                timerTask = new MainTimerTask();
                handler = new android.os.Handler();
                timer.schedule(timerTask, 0, 2000);
                prevButton.setVisibility(View.GONE);
                nextButton.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }else{
                playStopButton.setText("▶");
                timer.cancel();
                timer = null;
                prevButton.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    //画像の切り替え
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

    //パーミッションの判定
    @Override
    public void onRequestPermissionsResult(int resultCode, String permission[], int[] grantResult){
        switch (resultCode){
            case PERMISSION_REQUEST_CODE:
                if(grantResult[0] == PackageManager.PERMISSION_GRANTED){
                    //許可された
                    getContentInfo();
                }else{
                    //許可されなかった
                    showAlertDialog(1);
                }
                break;
            default:
                break;
        }
    }

    //画像の取得
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

        }
        if(0 < uris.size()) {
            //imageView.setImageURI(uris.get(0));
            imageSwitcher.setImageURI(uris.get(0));

        }else{
            showAlertDialog(2);
        }
        cursor.close();
    }

    //画面タッチイベント
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
                if (containerView.getVisibility() == View.GONE){
                    containerView.startAnimation(inAnimation);
                    containerView.setVisibility(View.VISIBLE);
                    timerCounter = 0;
                }else{
                    if(touchX - event.getX() < 100 && event.getX() - touchX < 100) {
                        hideButton();
                    }
                }
                break;
        }

        return true;
    }

    //ボタンの非表示
    public void hideButton(){
        if(containerView.getVisibility() != View.GONE) {
            containerView.startAnimation(outAnimation);
            containerView.setVisibility(View.GONE);
        }
    }

    //エラーダイアログの表示
    private void showAlertDialog(int error){

        //エラーコード1:パーミッションが許可されませんでした。
        //エラーコード2:画像がありません。
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("エラーメッセージ");
        if(error == 1) {
            Log.d("Android", "アクセス権なし");
            alertDialogBuilder.setMessage("ストレージのアクセス権限がありません。\nアプリを終了します。");
        }else if(error == 2){
            alertDialogBuilder.setMessage("ストレージに画像が存在しません。\nアプリを終了します。");
        }
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
