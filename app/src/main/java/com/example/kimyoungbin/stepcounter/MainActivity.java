package com.example.kimyoungbin.stepcounter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

  private boolean isPush;

  /* Avoid counting faster than stepping */
  private boolean pocketFlag;
  private boolean callingFlag;
  private boolean handHeldFlag;
  private boolean handTypingFlag;

  /* Set threshold */
  private final double maxPocketThs = 15.0;
  private final double minPocketThs = 11.5;
  private final double maxCallingThs = 12.7;
  private final double minCallingThs = 11.1;
  private final double maxTypingThs = 13.5;
  private final double minTypingThs = 11.0;
  private final double maxHeldThs = 13.8;
  private final double minHeldThs = 11.0;
  private final double handHeldXThs = 1.0;
  private final double handHeldZThs = 1.5;
  private final double handTypingThs = 0.5;

  private int stepCount;
  private int compassCount;

  // Using the Accelometer & Gyroscoper
  private SensorManager mSensorManager = null;

  // Using the Accelometer
  private SensorEventListener mAccLis;
  private Sensor mAccelometerSensor = null;

  // Using the Gyroscoper
  private SensorEventListener mGyroLis;
  private Sensor mGgyroSensor = null;

  // Using the Closesensor
  private SensorEventListener mClsLis;
  private Sensor mClsSensor = null;

  // Using the Dirsensor
  private SensorEventListener mDirLis;
  private Sensor mDirSensor = null;


  // Value
  private int startValue;
  private int firstValue;
  private int lastValue;
  private int compassValue;
  //private int lastCompassValue;
  private int temp;
  private boolean isStart;

  // To distinguish state
  private boolean isPocket;
  private boolean isHandHeld;
  private boolean isHandTyping;
  private boolean isPocketToHand;

  private float distance;

  // prevent abnormal count
  private long startTime;
  private long endTime;

  // View information
  private int displayWidth;
  private int displayHeight;
  private int oneStepWidth;
  private int oneStepHeight;
  private int stepCheck;

  @Override
  protected void onResume() {
    super.onResume();
    mSensorManager.registerListener(this, mClsSensor, SensorManager.SENSOR_DELAY_FASTEST);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


    isPush = true;
    pocketFlag = true;
    handHeldFlag = true;
    handTypingFlag = true;
    isPocket = false;
    stepCount = 0;
    compassCount = 0;
    isStart = false;

    //Using the Sensors
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    //Using the Accelometer
    mAccelometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    //Using the Gyroscoper
    mGgyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    //Using the DirSensor
    mDirSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

    //Using the Closesensor
    mClsSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    mClsLis = new SensorEventListener() {
      @Override
      public void onSensorChanged(SensorEvent event) {
        float[] v = event.values;
        distance = v[0];
        //Log.e("DISTANCE", String.valueOf(distance));

        if (distance < 5.0) {
          startTime = 0;
          isPocket = true;
        } else {
          isPocket = false;
        }
      }

      @Override
      public void onAccuracyChanged(Sensor sensor, int i) {

      }
    };

    DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();

    displayWidth = dm.widthPixels;
    displayHeight = dm.heightPixels;

    oneStepWidth = displayWidth / 27;
    oneStepHeight = displayHeight / 67;

    final ViewEx viewEx = new ViewEx(this);

    //Touch Listener for Accelometer
    findViewById(R.id.a_start).setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        if (isPush) {
          mDirLis = new mDirectionListener();
          mAccLis = new AccelometerListener();
          mGyroLis = new GyroscopeListener();
          isStart = true;
          mSensorManager.registerListener(mAccLis, mAccelometerSensor, SensorManager.SENSOR_DELAY_UI);
          mSensorManager.registerListener(mGyroLis, mGgyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
          mSensorManager.registerListener(mClsLis, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
          mSensorManager.registerListener(mDirLis, mDirSensor, SensorManager.SENSOR_DELAY_NORMAL);
          isPush = false;
          setContentView(viewEx);
        } else {
          mSensorManager.unregisterListener(mAccLis);
          mSensorManager.unregisterListener(mClsLis);
          mSensorManager.unregisterListener(mDirLis);
          isStart = false;
          isPush = true;
        }
      }
    });

  }

  protected class ViewEx extends View {

    private Bitmap img;
    int imgWidth;
    int imgHeight;
    int curWidth;
    int curHeight;
    double doubleCurWidth;
    double doubleCurHeight;
    int sizeWidth;
    int sizeHeight;
    Rect dst;
    Path path;

    public ViewEx(Context context) {
      super(context);
      setBackgroundColor(Color.WHITE);
      Resources r = context.getResources();
      path = new Path();
      img = BitmapFactory.decodeResource(r, R.drawable.ic_navigation_black_24dp);
      imgWidth = img.getWidth();
      imgHeight = img.getHeight();

      curWidth = displayWidth / 2;
      curHeight = displayHeight / 2;

      /* length of one side */
      sizeWidth = imgWidth / 4;
      sizeHeight = imgHeight / 4;

      stepCheck = 1;

      mHandler.sendEmptyMessageDelayed(0, 10);
    }

    public void onDraw(Canvas canvas) {

      Paint MyPaint = new Paint();
      MyPaint.setStrokeWidth(5f);
      MyPaint.setStyle(Paint.Style.STROKE);
      MyPaint.setColor(Color.RED);

      /* initial location */
      path.moveTo(curWidth, curHeight);

      /* calculate direction and draw on screen */
      if(stepCheck == stepCount) {
        doubleCurWidth = curWidth + (oneStepWidth * Math.sin(Math.toRadians(compassValue)));
        doubleCurHeight = curHeight - (oneStepHeight * Math.cos(Math.toRadians(compassValue)));

        curWidth = (int)doubleCurWidth;
        curHeight = (int)doubleCurHeight;

        stepCheck++;
      }

      /* draw path in a line */
      path.lineTo(curWidth, curHeight);

      /* rotate image angle */
      Matrix matrix = new Matrix();
      matrix.postRotate(compassValue);
      Bitmap newImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);

      /* image size, location setting */
      dst = new Rect(curWidth - sizeWidth, curHeight - sizeHeight, curWidth + sizeWidth, curHeight + sizeHeight);
      canvas.drawBitmap(newImg, null, dst, null);

      canvas.drawPath(path, MyPaint);
      super.onDraw(canvas);
    }

    Handler mHandler=new Handler(){

      public void handleMessage(Message msg)
      {
        invalidate();
        mHandler.sendEmptyMessageDelayed(0, 10);
      }

    };
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.e("LOG", "onPause()");
    mSensorManager.unregisterListener(mAccLis);
    mSensorManager.unregisterListener(mClsLis);
    mSensorManager.unregisterListener(mDirLis);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.e("LOG", "onDestroy()");
    mSensorManager.unregisterListener(mAccLis);
    mSensorManager.unregisterListener(mClsLis);
    mSensorManager.unregisterListener(mDirLis);
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {

  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }


  private class AccelometerListener implements SensorEventListener {


    @Override
    public void onSensorChanged(SensorEvent event) {
      String str;

      if(startTime == 0)
        startTime = event.timestamp;
      else
        endTime = event.timestamp;

      if(endTime - startTime > 1700000000)
        isPocketToHand = true;
      else
        isPocketToHand = false;
      double accX = event.values[0];
      double accY = event.values[1];
      double accZ = event.values[2];

      double angleXZ = Math.atan2(accX, accZ) * 180 / Math.PI;
      double angleYZ = Math.atan2(accY, accZ) * 180 / Math.PI;

      double tmp = (accX * accX) + (accY * accY) + (accZ * accZ);
      final double E = Math.sqrt(tmp);

      /*Log.e("LOG", "ACCELOMETER           [X]:" + String.format("%.4f", event.values[0])
          + "           [Y]:" + String.format("%.4f", event.values[1])
          + "           [Z]:" + String.format("%.4f", event.values[2])
          + "           [E]:" + String.format("%.4f", E)
      + "           [angleXZ]: " + String.format("%.4f", angleXZ)
      + "           [angleYZ]: " + String.format("%.4f", angleYZ));*/

      /** In the pocket **/
      if (isPocket) {
        if (E > minPocketThs && E < maxPocketThs && pocketFlag && isPocketToHand) {
          stepCount++;

          Log.e("LOG", "ACCELOMETER           [E]:" + String.format("%.4f", E));
          Log.e("LOG", String.valueOf(stepCount));
          pocketFlag = false;

          Handler mHandler = new Handler();
          mHandler.postDelayed(new Runnable() {
            public void run() {
              pocketFlag = true;
            }
          }, 400);
        }

        /** Calling **/
        else if (E > minCallingThs && E < maxCallingThs && callingFlag && isPocketToHand) {
          stepCount++;

          //Log.e("LOG", "ACCELOMETER           [E]:" + String.format("%.4f", E));
          //Log.e("LOG", String.valueOf(stepCount));
          callingFlag = false;

          Handler mHandler = new Handler();
          mHandler.postDelayed(new Runnable() {
            public void run() {
              callingFlag = true;
            }
          }, 400);
        }
      }

      else {
        /** Walking with typing **/
        if (E > minTypingThs && E < maxTypingThs && isHandTyping && handTypingFlag && isPocketToHand) {
          stepCount++;

          //Log.e("LOG", "ACCELOMETER           [E]:" + String.format("%.4f", E));
          //Log.e("LOG", String.valueOf(stepCount));
          isHandTyping = false;
          handTypingFlag = false;

          Handler mHandler = new Handler();
          mHandler.postDelayed(new Runnable() {
            public void run() {
              handTypingFlag = true;
            }
          }, 400);
        }
        /** Hand held working **/
        else if (E > minHeldThs && E < maxHeldThs && isHandHeld && handHeldFlag) {
          stepCount++;

          isHandHeld = false;
          //Log.e("LOG", "ACCELOMETER           [E]:" + String.format("%.4f", E));
          //Log.e("LOG", String.valueOf(stepCount));
          handHeldFlag = false;

          Handler mHandler = new Handler();
          mHandler.postDelayed(new Runnable() {
            public void run() {
              handHeldFlag = true;
            }
          }, 400);
        }
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
  }

  private class GyroscopeListener implements SensorEventListener {

    @Override
    public void onSensorChanged(SensorEvent event) {

      /* receives the angular velocity of each axis. */
      double gyroX = event.values[0];
      double gyroY = event.values[1];
      double gyroZ = event.values[2];

       /*
       Log.e("LOG", "GYROSCOPE           [X]:" + String.format("%.4f", event.values[0])
          + "           [Y]:" + String.format("%.4f", event.values[1])
          + "           [Z]:" + String.format("%.4f", event.values[2]));
       */
      /* detect gyroZ motion when walking with hand */
      if(Math.abs(gyroZ) > handHeldZThs)
        isHandHeld = true;

      /* if gyroX moves a lot, it is not time to walking with hand */
      if(Math.abs(gyroX) > handHeldXThs)
        isHandHeld = false;

      /* detect few motion when walking while typing */
      if(Math.abs(gyroX) < handTypingThs && Math.abs(gyroY) < handTypingThs
          && Math.abs(gyroZ) < handTypingThs)
        isHandTyping = true;
      else
        isHandTyping = false;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
  }

  private class mDirectionListener implements SensorEventListener {
    @Override
    public void onSensorChanged(SensorEvent event) {
      if(event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
        String str;
        int val = 0;
        //str = "azimuth(z): "+(int)event.values[0];
        val = (int) event.values[0];
        if (compassCount < 2) {
          if (isStart == true) {
            startValue = val;
            firstValue = val;
            lastValue = val;
            compassCount++;
          }
          if(compassCount == 2){
            startValue = val;
            firstValue = val;
            isStart = false;
          }

          compassValue = 0;
          Log.e("compassValue : ", String.format("compass: %d, temp: %d, start: %d, last: %d, first: %d", compassValue, temp, startValue, lastValue, firstValue));
        }else{
          //lastCompassValue = compassValue;
          firstValue = lastValue;
          lastValue = val;
          compassValue = lastValue - startValue;
          temp = lastValue - firstValue;
          if (temp < 0) {
            temp = temp + 360;
          }
          if(compassValue < 0){
            compassValue = compassValue + 360;
          }
          /*
          if(temp<5){
            compassValue = lastCompassValue;
          }
          */
          Log.e("compassValue : ", String.format("compass: %d, start: %d, last: %d, first: %d", compassValue, startValue, lastValue, firstValue));
        }
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
  }
}