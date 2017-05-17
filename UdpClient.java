//Joshua Itagaki
//CS 380

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class UdpClient {
	public static void main(String[] args) throws Exception {
		try (Socket socket = new Socket("codebank.xyz", 38005)){
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();	
			byte[] data = new byte[4];
			data[0] = (byte)0xDE;
			data[1] = (byte)0xAD;
			data[2] = (byte)0xBE;
			data[3] = (byte)0xEF;
			/* 
			 * 0xCAFEBABE	Everything fine
			 * 0xBAADFOOD	Problem with ipv4 packet
			 * 0xCAFEDOOD	Incorrect destination port in UDP
			 * 0xDEADCODE	Invalid udp check sum
			 * 0xBBADBEEF	Incorrect udp data length
			 */
			out.write(ipv4(data));	//ipv4 handshake
			byte[] message = new byte[4];
			in.read(message);
			System.out.print("Handshake response: 0x");
			for(byte num: message){
				System.out.printf("%02X", num);
			}
			System.out.println();
			byte[] destPort = new byte[2];
			in.read(destPort);
			System.out.println("Port number received: " + destPort[0] + destPort[1]);
			for(int i = 1; i < 13; i++){
				int udpsize = (int)Math.pow(2.0, (double)i);
				byte[] packet = udp(destPort, udpsize);
				long start = System.currentTimeMillis();
				out.write(packet);
				in.read(message);
				long end = (System.currentTimeMillis() - start);
				System.out.println("Sending packet with " + udpsize + " bytes of data");
				System.out.print("Response: 0x");
				for(byte num: message){
					System.out.printf("%02X", num);
				}
				System.out.println();
				System.out.println("RTT: " + end + "ms");
			}
		}
	}
	
	private static byte[] udp(byte[] data, int size){
		int length = size + 8;
		byte[] packet = new byte[length];
		//Source
		packet[0] = 0;
		packet[1] = 0;
		//Destination
		packet[2] = (byte)(data[1]);
		packet[3] = (byte)(data[2]);
		//length
		packet[4] = (byte)(length >> 8);
		packet[5] = (byte) length;
		//initialize udp check
		packet[6] = 0;
		packet[7] = 0;
		//data 
		Random rand = new Random();
		for(int i = 0; i < size; i++){
			packet[i + 8] = (byte)rand.nextInt();
		}
		//udp checksum
		short sum = udpChecksum(packet);
		packet[6] = (byte)(sum >> 8);
		packet[7] = (byte)sum;
		return packet;
	}
	
	private static short udpChecksum(byte[] array){
		short sum = 0;
		int length = array.length + 12;
		byte[] packet = new byte[length];
		//Source
		packet[0] = 0;
		packet[1] = 0;
		packet[2] = 0;
		packet[3] = 0;
		//Destination
		packet[4] = (byte)0x34;
		packet[5] = (byte)0x21;
		packet[6] = (byte)0x83;
		packet[7] = (byte)0x10;
		
		packet[8] = 0;
		//Protocol
		packet[9] = (byte)0x11;
		//length
		packet[10] = (byte)(array.length >> 8);
		packet[11] = (byte)(array.length);
		for(int i = 12; i <array.length; i++){
			packet[i] = array[i+8];
		}
		return checksum(packet, packet.length);
	}
	
	private static byte[] ipv4(byte[] data){
		byte[] packet = new byte[28 + data.length];
		new Random().nextBytes(packet);
		packet[0] = (byte)0x45;	//Version & HLen
		packet[1] = 0;	//TOS not implemented
		packet[2] = (byte)(packet.length >> 8);	//length first byte
		packet[3] = (byte)(packet.length);	//length second byte
		packet[4] = 0;	//ident not implemented
		packet[5] = 0;	//ident not implemented
		packet[6] = (byte)0x40;	//Flags
		packet[7] = 0;	//offset
		packet[8] = (byte)0x32;	//TTL 
		packet[9] = (byte)0x11; //UDP
			
		for(int j = 12; j <= 15; j++){	//packets 12-15
			packet[j] = 0;	//source address is all 0's
		}
		
		packet[16] = 52;	//packets 16-19
		packet[17] = 33;	//destination address of server
		packet[18] = (byte)131;	
		packet[19] = 16;
		
		short csValue = checksum(packet, packet.length);	//calculate checksum
		packet[10] = (byte)(csValue >> 8);	//first byte of checksum
		packet[11] = (byte)csValue;	//second byte of checksum
		packet[22] = data[0];
		packet[23] = data[1];
		int udpsize = 8 + data.length;
		packet[24] = (byte)(udpsize >>> 8);
		packet[25] = (byte)udpsize;
		byte[] headUDP = makeHeader(packet, udpsize + 12);
		csValue = checksum(headUDP, udpsize + 12);
		packet[26] = (byte)(csValue >> 8);
		packet[27] = (byte)csValue;
		return packet;
	}
	
	private static byte[] makeHeader(byte[] packet, int size) {
		byte[] headUDP = new byte[size];
		headUDP[0] = 0;
		headUDP[1] = 17;
		for(int i = 2; i < 6; i++){
			headUDP[i] = 0;
		}
		headUDP[6] = 52;
		headUDP[7] = 33;
		headUDP[8] = (byte)131;
		headUDP[9] = 16;
		headUDP[10] = packet[24];
		headUDP[11] = packet[25];
		for(int i = 12; i < size; i++){
			headUDP[i] = packet[i+8]; 
		}
		return headUDP;
	}

	public static short checksum(byte[] array, int count){
		short sumU = 0;
		int index = 0;
		while(count-- != 0){
			sumU += array[index++];
			if((sumU & 0xFFFF0000) != 0){
				sumU &= 0xFFFF;
				sumU++;
			}
		}
		return (short) (sumU & 0xFFFF);
	}
}
