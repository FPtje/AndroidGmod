package com.example.GmodAndroid;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ControlActivity extends Activity implements SensorEventListener {
	private GCPprotocol gcp;
	private boolean sendAcceleration;
	
	private SensorManager sensorMGR;
	
	private PowerManager pm;
	private PowerManager.WakeLock wakeLock;
	
	/**
	 * Connect to the Garry's mod socket
	 * @param ip
	 * @param port
	 */
	private void doConnect(String ip, int port){
		try{
			gcp = new GCPprotocol();
			gcp.DoConnect(ConnectActivity.resolveIP(ip));
		}catch(Exception E){
			Intent I = new Intent();
			I.putExtra("Status", "Connection failed: "+E.getMessage());// Send status data to parent app
			setResult(RESULT_FIRST_USER + 1, I);
			finish();
		}
	}
	
	/*
	 * Right before the application is started
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control);
        String IP = getIntent().getStringExtra("IP");
        int port = getIntent().getIntExtra("Port", 54325);
        sendAcceleration = getIntent().getBooleanExtra("SendAcceleration", false);

        // Connect:
        doConnect(IP, port);
        
        // Setup sensor:
        sensorMGR = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor orientation = sensorMGR.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        Sensor acceleration = sensorMGR.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        try{
        	sensorMGR.registerListener(this, orientation, SensorManager.SENSOR_DELAY_UI);
        	sensorMGR.registerListener(this, acceleration, SensorManager.SENSOR_DELAY_UI);
        }catch(Exception E){
        	TextView text = (TextView) findViewById(R.id.txtControl);
        	text.setText("You don't have a sensor!");
        }
        storedOrientation = new float[3];
        storedAcceleration = new float[3];
        
        // setup buttons
        ImageView touch = (ImageView) findViewById(R.id.touchView);
        touch.setOnTouchListener(onTouch);
        
        // Setup EditText
        EditText txtSend = (EditText) findViewById(R.id.txtSend);
        txtSend.setOnEditorActionListener(txtSendEnter); // See below for definition txtSendEnter
    }
	
	/*	onResume
	 * prevents the phone from locking.
	 * because that will disconnect the phone
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume(){
		super.onResume();
		pm = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Gmod control prevent sleep");
		// Prevent the phone from locking
        wakeLock.acquire();
	}
	
	/*
	 * Unused interface method
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
	
	/**
	 * rotateAngles
	 * A workaround for the following rotation problem:
	 * if the pitch is above somewhere near 45 degrees, the Azimuth suddenly rotates 180 degrees 
	 * @param ang
	 * @return the proper angle that can be used
	 */
	private float[] rotateAngles(float[] ang){
		float[] rotation = ang;
		
		if ((ang[1] >= (45 - ang[2] / 3.8) && ang[2] < 0) || (ang[1] >= (45 + ang[2] / 3.8) && ang[2] >= 0)){ // Azimuth is affected by pitch and roll at this point
			rotation[0] = (ang[0] + 180) % 360 + 2 * ang[2];
		}
		
		if ((ang[1] <= (-45 + ang[2] / 3.8)  && ang[2] < 0) || (ang[1] <= (-45 - ang[2] / 3.8)  && ang[2] >= 0)) {
			rotation[0] = ang[0] - 2 * ang[2];
		}
		
		return rotation;
	}
	
	/**
	 * compareOrientationValues
	 * Compare the orientation values to the stored int values
	 * @return true if the orientation of the phone has significantly changed
	 */
	private float[] compareRotationValues(float[] oldvals, float[] newvals){
		float[] roundedValues = new float[3];
		
		roundedValues[0] = Math.round(newvals[0]);
		roundedValues[1] = Math.round(newvals[1]);
		roundedValues[2] = Math.round(newvals[2]);
		
		if (oldvals[0] != roundedValues[0] || oldvals[1] != roundedValues[1] || oldvals[2] != roundedValues[2]){
			return roundedValues;
		}
		
		return null;
	}
	
	private float[] storedOrientation; // stored sensor values
	private float[] storedAcceleration;
	private float[] gravity; // The acceleration needs to remove the gravity force
	
	private double lastAcceleration;
	
	/*
	 * (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION){
			// Check for changes in the rounded angle since last iteration
			float[] rounded = compareRotationValues(storedOrientation, rotateAngles(event.values));
			boolean changed = rounded != null;
			
			if (changed){ // Only send when they have significantly changed
				storedOrientation = rounded;
				gcp.SendOrientation(event.values);
			}
		}else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && sendAcceleration){
			float[] rounded = compareRotationValues(storedAcceleration, event.values);
			boolean changed = rounded != null;
			if (changed){
				gravity = (gravity != null?gravity:new float[3]);
				
				// Gravity calculation from http://developer.android.com/reference/android/hardware/SensorEvent.html#values
				double dT = (event.timestamp - lastAcceleration) / 10e6;
				lastAcceleration = event.timestamp;
				
				
				final float timeconstant = 80;
				float alpha = (float) (timeconstant / (timeconstant + dT));
				gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

		        float[] toSend = event.values;
		        toSend[0] = toSend[0] - gravity[0];
		        toSend[1] = toSend[1] - gravity[1];
		        toSend[2] = toSend[2] - gravity[2];

				gcp.SendAcceleration(toSend);
				storedAcceleration = rounded;
			}
		}
		
	}


	/*
	 * Release all resources and locks
	 * (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop(){
		gcp.Close();
		setResult(RESULT_FIRST_USER); // Set result to an OK value
		sensorMGR.unregisterListener(this); // stop the sensors from listening
		wakeLock.release(); // Release the wake lock and re-enable the phone's sleep function.
		super.onStop();
	}
	
	/*
	 * buttonUnderLocation
	 * Decides which button is under your finger.
	 * Used in multi touch
	 */
	private int buttonUnderLocation(View v, float x, float y){
		float width = v.getWidth();
		float height = v.getHeight();
		
		if (x < width / 2){
			if (y < height / 3)
				return 1;
			else if (y < height * (2.0/3))
				return 3;
			else
				return 5;
		}else {
			if (y < height / 3)
				return 2;
			else if (y < height * (2.0/3))
				return 4;
			else
				return 6;
		}
	}
	/**
	 * onButtonTouch
	 * Called when a "button" is touched
	 * Uses an ImageView to allow multitouch buttons
	 * @param v
	 */
	private OnTouchListener onTouch = new OnTouchListener() {
	    public boolean onTouch(View v, MotionEvent me) {
	    	int action = me.getActionMasked();
	    	int buttonNr = buttonUnderLocation(v, me.getX(me.getActionIndex()), me.getY(me.getActionIndex()));
	    	
	    	if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN )
	    		gcp.SendButton(buttonNr, 0);
	    	else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
	    		gcp.SendButton(buttonNr, 1);
	    	
			return true;
	    }
	};
	
	/**
	 * txtSendClick
	 * When the Send EditText gets clicked.
	 * @param v
	 */
	public void txtSendClick(View v){
		EditText txtSend = (EditText) v;
		txtSend.getEditableText().clear();
	}
	
	private OnEditorActionListener txtSendEnter = new OnEditorActionListener() {
		public boolean onEditorAction(TextView txtSend, int actionId, KeyEvent event) {
			Editable txt =txtSend.getEditableText(); 
			gcp.SendText(txt.toString());
			txt.clear();
			return false;
		}
	};
}
