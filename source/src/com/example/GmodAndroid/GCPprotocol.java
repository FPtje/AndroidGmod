/**
 * 
 */
package com.example.GmodAndroid;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author falco
 * Garry's mod Connection Protocol
 * The implementation of the application layer protocol called GCP.
 */
public class GCPprotocol {
	private DatagramSocket connection;
	private final int port = 54325;
	
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
		TEXT (5);
		
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
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public void DoConnect(byte[] IP) throws SocketException, UnknownHostException{
		InetSocketAddress address = new InetSocketAddress(Inet4Address.getByAddress(IP), port);
		connection = new DatagramSocket();
		connection.connect(address);
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
	public InetAddress Handshake(int timeout, DatagramSocket sock) throws SocketException, IOException{
		if (sock == null) throw new SocketException("Socket does not exist");
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1 + 4 * 3); // Type code and 3 floats
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeByte(MsgTypes.SYN.Code);
		
		byte[] data = bos.toByteArray();
		
		DatagramPacket p = new DatagramPacket(data, 1);
		sock.send(p);
		
		//
		//listen.bind(new InetSocketAddress(sock.getLocalAddress(), sock.getLocalPort())); // listen for reply
		sock.setSoTimeout(timeout);
		byte[] ackbyte = new byte[1];
		DatagramPacket ack = new DatagramPacket(ackbyte, 1);
		
		sock.receive(ack);
		return ack.getAddress();
	}
	
	/**
	 * Simplified handshake overload
	 * @throws SocketException
	 * @throws IOException
	 */
	public void Handshake() throws SocketException, IOException{
		Handshake(500, connection);
	}
	
	/**
	 * Close the connection
	 */
	public void Close(){
		connection.disconnect();
		connection.close();
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
			connection.send(p);
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
			connection.send(p);
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
				connection.send(p);
			}
		} catch (IOException e) {
			return; // Don't throw an error. That would only annoy the user.
		}
	}
}
