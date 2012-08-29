package com.example.GmodAndroid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.widget.Toast;

public class Connect implements Runnable {
	private final int port = 54325;
	private DatagramSocket connection;
	private byte[] IP;
	private DatagramPacket lastPacket;
	private Activity currentActivity;
	
	/**
	 * Constructor
	 * @param ip The IP to connect to
	 */
	public Connect(byte[] ip, Activity ac){
		this.IP = ip;
		currentActivity = ac;
	}
	
	private void doError(String error){
		final String message = error;
		currentActivity.runOnUiThread(new Runnable(){
			public void run() {
				
				if (currentActivity instanceof ControlActivity){
					Toast.makeText(currentActivity, message, Toast.LENGTH_LONG).show();
					currentActivity.finish();
				}
			}
		});
	}
	
	/**
	 * Actually connect
	 */
	private void connect(){
		try{
			InetSocketAddress address = new InetSocketAddress(Inet4Address.getByAddress(IP), port);
			connection = new DatagramSocket();
			connection.connect(address);
		}
		catch(SocketException e){
			doError("Could not connect! " + e.getMessage());
		} catch (UnknownHostException e) {
			doError("Could not connect! This IP doesn't exist!");
		}
	}
	
	/**
	 * Prepare data for sending
	 * @param packet
	 */
	public void setDataToSend(DatagramPacket packet){
		this.lastPacket = packet;
	}
	
	/**
	 * Actually send the packet
	 * @param packet
	 */
	private void sendPacket(DatagramPacket packet){
		try {
			connection.send(packet);
			lastPacket = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Indicate that we want a handshake
	 */
	private DatagramPacket handshakePacket;
	public void setHandshake(DatagramPacket packet){
		handshakePacket = packet;
	}
	
	/**
	 * Perform the handshake
	 * @param packet
	 */
	private void doHandShake(DatagramPacket packet){
		sendPacket(packet);
		
		try{
			connection.setSoTimeout(500);
			byte[] ackbyte = new byte[1];
			DatagramPacket ack = new DatagramPacket(ackbyte, 1);
			
			connection.receive(ack);
		}
		catch(PortUnreachableException e){
			doError("Could not connect! Computer is not running Garry's mod!");
		}
		catch(SocketTimeoutException e){
			doError("Could not connect! Computer doesn't exist!");
		}
		catch(SocketException e){
			doError("Could not connect! " + e.getMessage());
		}
		catch(IOException e){
		}
		
		handshakePacket = null;
	}
	

	/**
	 * Run
	 * Connect to the actual port
	 */
	public void run() {
		if (connection == null || !connection.isConnected())
			connect();
		
		if (handshakePacket != null)
			doHandShake(handshakePacket);
		
		if (lastPacket != null)
			sendPacket(lastPacket);
		
	}
	
	/**
	 * Close the connection when the object is destroyed
	 */
	@Override
	protected void finalize() throws Throwable {
		connection.close();
		super.finalize();
	}

}
