package com.example.GmodAndroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class ConnectActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Restore last entered IP:
        SharedPreferences prefs = getPreferences(0);
        String IPEntry = prefs.getString("LastIP", "192.168.1.");
        EditText ipTextEntry = (EditText) findViewById(R.id.txtIP);
        ipTextEntry.setText(IPEntry);
    }
    
    /**
     * resolveIP
     * Take a string and make a byte array of it that can be used in a socket.
     * @throws InvalidArgumentException if the string doesn't hold a valid IPV4 address
     * @param text the IPaddress string
     * @return
     */
    public static byte[] resolveIP(String text){
    	byte[] result = new byte[4];
    	
    	String[] split = text.split("\\.", 4);
    	for (int i = 0;i < split.length;i++){
    		result[i] = (byte) Integer.parseInt(split[i]);
    	}
    	
    	return result;
    }
    
    /**
     * DoConnect
     * Bound to the "Connect" button 
     * @param v
     */
    public void DoConnect(View v){
    	EditText ipEntry = (EditText) findViewById(R.id.txtIP);
    	
    	TextView text = (TextView) findViewById(R.id.txtStatus);
    	text.setText("Attempting to connect");
    	
    	text.setText("");
    	CheckBox chk = (CheckBox) findViewById(R.id.chkSendAcceleration);
    	
    	Intent controlIntent = new Intent(this, ControlActivity.class);
    	controlIntent.putExtra("IP", ipEntry.getText().toString());
    	controlIntent.putExtra("Port", 54325);
    	controlIntent.putExtra("Result", "");
    	controlIntent.putExtra("SendAcceleration", chk.isChecked());
		startActivityForResult(controlIntent, RESULT_FIRST_USER);
    }
    
    /*
     * This is called when my Gmod Control activity (the one with the buttons) closes
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent controlIntent){
    	super.onActivityResult(requestCode, resultCode, controlIntent);
    	
    	if (resultCode == RESULT_FIRST_USER + 1){ // RESULT_FIRST_USER + 1 means a connection error
    		TextView text = (TextView) findViewById(R.id.txtStatus);
        	text.setText(controlIntent.getStringExtra("Status"));
    	}
    }

    /*
     * onStop
     * when the activity stops (back button or home button pressed)
     * (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    public void onStop(){
    	super.onStop();
    	
    	// Save the entered IP address
    	SharedPreferences prefs = getPreferences(0);
    	SharedPreferences.Editor editor = prefs.edit();

        EditText ipTextEntry = (EditText) findViewById(R.id.txtIP);
    	editor.putString("LastIP", ipTextEntry.getText().toString());
    	editor.commit();
    }
    
    
}