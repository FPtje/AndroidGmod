/**
 * 
 */
package com.example.GmodAndroid;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

import android.app.Activity;

/**
 * @author falco
 * Garry's mod Connection Protocol
 * The implementation of the application layer protocol called GCP.
 */
public class GCPprotocol {
	
	private Connect connection;
	private Thread connectionThread;
	private Activity activity;
	
	/**
	 * Constructor
	 * @param a
	 */
	public GCPprotocol(Activity activity){
		this.activity = activity;
	}
	
	/**
	 * MsgTypes
	 * @author falco
	 * The forms of messages this protocol is able to send
	 */
	public static enum MsgTypes{
		SYN (0),
		ACK (1),
		ORIENTATION (2),
		ACCELERATION (3),
		BUTTON (4),
		TEXT (5),
		FINGERMOVEMENT (6),
		HOVER (7);
		
		public final int Code;
		MsgTypes(int c){
			this.Code = c;
		}
	}
	
	/**
	 * DoConnect
	 * Connect to the remote host
	 * Errors are to be handled by the caller
	 * @param IP 
	 */
	public void DoConnect(byte[] IP){
		connection = new Connect(IP, activity);
		
		connectionThread = new Thread(connection);
		connectionThread.start();
	}
	
	/**
	 * Handshake
	 * Perform a two-way handshake through UDP to see if a host is alive (and that he packets aren't being sent into a black hole)
	 * Errors are to be handled by the caller
	 * @param timeout how long to wait
	 * @param sock the socket to test
	 * @return the address that responded to the query
	 * @throws SocketException
	 * @throws IOException
	 */
	public void Handshake(int timeout) throws IOException{
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1 + 4 * 3); // Type code and 3 floats
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeByte(MsgTypes.SYN.Code);
		
		byte[] data = bos.toByteArray();
		
		DatagramPacket p = new DatagramPacket(data, 1);
		connection.setHandshake(p);
		
		// Wait for the previous thread to end, so we don't run two connection threads at once
		try {
			connectionThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		connectionThread = new Thread(connection);
		connectionThread.start();
	}
	
	/**
	 * Simplified handshake overload
	 * @throws SocketException
	 * @throws IOException
	 */
	public void Handshake() throws IOException{
		Handshake(500);
	}

	/**
	 * Send a packet to Garry's mod
	 * @param packet the packet to send
	 */
	private void sendData(DatagramPacket packet){
		connection.setDataToSend(packet);
		try {
			connectionThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		connectionThread = new Thread(connection);
		connectionThread.start();
	}
	
	/**
	 * Send information that comes in a float array of three values
	 * @param code
	 * @param xyz
	 */
	private void sendXYZ(int code, float[] xyz){
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1 + 4 * 3); // Type code and 3 floats
		DataOutputStream dos = new DataOutputStream(bos);
		try{
			dos.writeByte(code);// Write the code (ORIENTATION or ACCELERATION)
			
			for (int i = 0;i < xyz.length;i++){
				dos.writeFloat(xyz[i]); // Write the three floats that describe the angle
			}
			
			byte[] data = bos.toByteArray();
			
			DatagramPacket p = new DatagramPacket(data, data.length);
			sendData(p);
		}catch(IOException ioe){
			// This UDP packet is lost.
		}
	}
	
	/**
	 * SendOrientation
	 * Send the orientation data
	 * @param orientation
	 */
	public void SendOrientation(float[] orientation){
		sendXYZ(MsgTypes.ORIENTATION.Code, orientation);
	}
	
	/**
	 * SendAcceleration
	 * Send the last detected acceleration data
	 * @param acceleration
	 */
	public void SendAcceleration(float[] acceleration){
		sendXYZ(MsgTypes.ACCELERATION.Code, acceleration);
	}
	
	/**
	 * SendButton
	 * Send the press/release status of a button
	 * @param btn The number of the button that has been pressed
	 * @param pressed Pressed or released
	 */
	public void SendButton(int btn, int pressed){
		try{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			dos.writeByte(MsgTypes.BUTTON.Code);
			dos.writeShort((short) btn);
			dos.writeBoolean(pressed == 0?true:false);
			
			byte[] data = bos.toByteArray();
			DatagramPacket p = new DatagramPacket(data, data.length);
			
			Thread.sleep(10); // Otherwise not all packets get sent
			sendData(p);
		}catch(Exception E){
			// Lost package
		}
	}
	
	/**
	 * Send data about hovering
	 * @param pressed 1 when the stylus starts hovering, 0 when it stops hovering
	 */
	public void SendHover(int pressed){
		try{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			dos.writeByte(MsgTypes.HOVER.Code);
			dos.writeBoolean(pressed == 1?true:false);
			
			byte[] data = bos.toByteArray();
			DatagramPacket p = new DatagramPacket(data, data.length);
			Thread.sleep(10); // Otherwise not all packets get sent
			sendData(p);
		}catch(Exception E){
			// Lost package
		}
	}
	
	/**
	 * sendText
	 * Sends text to Garry's mod
	 * @param text
	 */
	public void SendText(String text){
		byte[] txtBytes = (text + '\n').getBytes();// String has to be newline-terminated
		final int byteSize = 15;
		
		try {			
			for (int i = 0; i + byteSize < txtBytes.length + byteSize;i += byteSize){
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(bos);
				
				dos.writeByte(MsgTypes.TEXT.Code);
				dos.write(txtBytes, i, Math.min(byteSize, txtBytes.length - i));
				
				byte[] data = bos.toByteArray();
				DatagramPacket p = new DatagramPacket(data, data.length);
				sendData(p);
			}
		} catch (IOException e) {
			// Don't throw an error. That would only annoy the user.
		}
	}
	
	/**
	 * sendFingerMovement
	 * Sends a finger movement event holding the new finger position
	 * @param x x of the new position
	 * @param y y of the new position
	 */
	private long lastSendFinger = 0; // Make sure it doesn't send the finger movement too often
	private float[] lastXY = new float[2];
	public void SendFingerMovement(float x, float y){
		final long sendDelay = 50;
		
		if (System.currentTimeMillis() - lastSendFinger < sendDelay || (lastXY[0] == x && lastXY[1] == y))
			return; // Simply disregard data when the same data or when it's sending too fast
		
		lastSendFinger = System.currentTimeMillis();
		lastXY[0] = x;
		lastXY[1] = y;
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		
		try {
			dos.writeByte(MsgTypes.FINGERMOVEMENT.Code);
			dos.writeFloat(x);
			dos.writeFloat(y);
			
			byte[] data = bos.toByteArray();
			DatagramPacket p = new DatagramPacket(data, data.length);
			sendData(p);
		} catch (IOException e) {
		}
	}
}
