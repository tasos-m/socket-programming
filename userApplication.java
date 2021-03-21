package userApplication;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import javax.sound.sampled.*;


public class userApplication {
	
	private static  String ServerIP = "155.207.18.208";
	//listening ports
	private static int serverListeningPort = 38015;
	private static int clientListeningPort = 48015;
	
	public static void main(String[] args) throws IOException {
		
		//request codes
		String echoWithoutDelayCode= "E0000";
		String echoRequestCode= "E2803";
		String imageRequestCode= "M6658";
		String audioRequestCode= "A9228";
		String obdRequestCode = "V0175";	
		
		//echo test
		int durationOfEcho = 50; //duration of echoTest in seconds
		String delays;
		 
		//with delay
		delays= echo(echoRequestCode,durationOfEcho);
		createFile(delays,"echowithdelay.csv"); 
	
		//without delay 
		delays= echo(echoWithoutDelayCode,durationOfEcho);
    	createFile(delays,"echowithdelaywithoutDelay.csv"); 
		
		// Temperature
		temperature(echoWithoutDelayCode);
		
		// Image
		image("E1.jpg",imageRequestCode,"",1024);
		image("E2.jpg",imageRequestCode,"PTZ",1024);	
			 
		//Audio
		audioDPCM(audioRequestCode,"F","",500,128,8,"differences.csv","audio.csv");
		
		//Audio 
		audioAQDPCM(audioRequestCode,"F","",500,132,16,"differences.csv","audio.csv","mean.csv","beta.csv");

		


		//TCP
		//http request response
		byte[] addressArray = {(byte)155, (byte)207,18,(byte)208};
		InetAddress inetAddress = InetAddress.getByAddress(addressArray);
		int portNumber= 80;
		Socket sWeb = new Socket(inetAddress,portNumber); //TCP socket
		
		InputStream inWeb = sWeb.getInputStream();
		OutputStream outWeb = sWeb.getOutputStream();
		
		String httpRequest ="GET /index.html HTTP/1.0\r\n\r\n";
		byte[] httpBuffer= new byte[5000];
		
		outWeb.write(httpRequest.getBytes());
		inWeb.read(httpBuffer);
		
		String httpResponse =new String(httpBuffer, "UTF-8");
		System.out.println(httpResponse);
		
		sWeb.close();
		
		//ithakicopter
	
		//send tcp packet as request
		portNumber= 38048;
		Socket sCopter = new Socket(inetAddress,portNumber);
		InputStream inCopter = sCopter.getInputStream();
		OutputStream outCopter = sCopter.getOutputStream();
			
		String copterRequest ="AUTO FLIGHTLEVEL=100 LMOTOR=200 RMOTOR=200 PILOT \r\n";
		System.out.println(copterRequest);
		outCopter.write(copterRequest.getBytes());
		
		//get response with udp packets
		telemetry(60,"ithakicopter2_2.txt");				
		sCopter.close();


		vehicle(obdRequestCode,60);
	}
	
	public static String echo(String code,int duration) throws IOException {
			
		// Create send socket and packet to transmit
		DatagramSocket s = new DatagramSocket();
		
		byte[] txbuffer = code.getBytes();
		InetAddress hostAddress = InetAddress.getByName(ServerIP);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverListeningPort);
		
		// Create receive socket and packet to receive
		DatagramSocket r = new DatagramSocket(clientListeningPort);
		r.setSoTimeout(4000);

		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		

		
		long startTime,endTime; //variables to calculate echo duration
		long t1,t2,packetDelay = 0;		//time points for packet delay calculation
		int sumPacket = 0;	//number of packets received
		String delays = new String();
		
		startTime = System.currentTimeMillis();
		do {
			  try {
				  
				  t1= System.currentTimeMillis();
				  s.send(p);
				  r.receive(q);
				  t2= System.currentTimeMillis();
				  
				  sumPacket++;
				  packetDelay = t2-t1;
				  
				  String message = new String(q.getData(),0,q.getLength());
				  System.out.println(message);
				  System.out.println(packetDelay);
				
			  } catch (Exception x) {
				  System.out.println(x);
			  }
			  
			  endTime= System.currentTimeMillis();
			  
			  //add delays to a string
			  delays+= packetDelay + "\t";
			  
			  
		}
		while(endTime-startTime<= duration*1000);
		
		System.out.println("Number of packets:"+sumPacket);
		
		// Sockets termination.
		s.close();
		r.close();
		
		return delays;
	}
	public static void temperature(String code) throws IOException {
		
		// Create send socket and packet to transmit
		DatagramSocket s = new DatagramSocket();
		
		String packetInfo = code + "T00";
		byte[] txbuffer = packetInfo.getBytes();
		InetAddress hostAddress = InetAddress.getByName(ServerIP);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverListeningPort);
		
		
		// Create receive socket and packet to receive
		DatagramSocket r = new DatagramSocket(clientListeningPort);

		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		p.setData(packetInfo.getBytes());
		p.setLength(packetInfo.getBytes().length);
		
		try {
			  
			  s.send(p);
			  r.receive(q);
			
			  String temp = new String(q.getData(),0,q.getLength());
			  System.out.println(temp);
			  
			  createFile(temp,"temperature.txt");
			  
		  } catch (Exception x) {
			  System.out.println(x);
		  }		
		// Sockets termination.
		s.close();
		r.close();		
		
	}
	public static void image(String filename,String code,String camera,int L) throws IOException{
		// Creating send socket and packet format.
		DatagramSocket s = new DatagramSocket();
		String packetInfo = code + camera + "UDP=" + L;
		InetAddress hostAddress = InetAddress.getByName(ServerIP);
		DatagramPacket p = new DatagramPacket(packetInfo.getBytes(), packetInfo.length(), hostAddress, serverListeningPort);
		
		// Creating receive socket and packet format.
		DatagramSocket r = new DatagramSocket(clientListeningPort);
		r.setSoTimeout(4000);
		byte[] rxbuffer = new byte[L];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);		
		
		try {
			
			
			FileOutputStream f = new FileOutputStream(filename);    
            
			s.send(p);
           
			for(int j=0;j<50000;j++) {
				r.receive(q);
        		for(int i=0;i<q.getLength();i++) {
        			f.write(q.getData()[i]); 
        		}
        		System.out.println("image packet:"+j);
        		if(q.getLength()!=L) {
     				  break; //last packet has different length 
     			}
			}
           
			f.close();    
			  
		} catch (Exception x) {
			  System.out.println(x);
		}
		// Sockets termination.
		s.close();
		r.close();		
		
	}
	public static void audioDPCM(String code, String source,String track, int numberOfPackets, int packetSize,int Q, String diffFileName, String audioFileName) throws IOException {
		
		DatagramSocket s = new DatagramSocket();
		String packetInfo =  code + "L" + track + source + numberOfPackets;
		InetAddress hostAddress = InetAddress.getByName(ServerIP);
		DatagramPacket p = new DatagramPacket(packetInfo.getBytes(), packetInfo.length(), hostAddress, serverListeningPort);
		
		// Creating receive socket and packet format.
		DatagramSocket r = new DatagramSocket(clientListeningPort);
		r.setSoTimeout(4000);
		byte[] rxbuffer = new byte[128];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		byte[][] buffer =new byte[numberOfPackets][packetSize]; //every line is a packet
		int packetsReceived=0;
		
		try {
			s.send(p);
           
			for(int i=0;i<numberOfPackets;i++){
				r.receive(q);
				packetsReceived++;
				System.out.println("audio packet:"+packetsReceived);
				//store packet to buffer
        		for(int j=0;j<packetSize;j++) {
        			buffer[i][j]= q.getData()[j];
        		}
        		
			}	 
			  
		} catch (Exception x) {
			  System.out.println(x);
		}
		// Sockets termination.
		s.close();
		r.close();	
		
		int[] diffBuffer= new int[buffer.length*buffer[0].length*2];
		int sum=0;
		
		//each byte received represents 2 bytes
		// store the nibble bytes and transform them from 0 to 15
		for(int i=0; i<buffer.length;i++) {
			
			for(int j=0; j<buffer[0].length; j++) {
			
				diffBuffer[sum]= ( ((int)buffer[i][j] & 0xF0 )>> 4 ) - 8; //first nibble 
				sum++;
				
				diffBuffer[sum]= ( (int)buffer[i][j] & 0xF ) - 8;	//second nibble
				sum++;
			}
			
		}		
		
		byte[] audioBufferOut = new byte[buffer.length*buffer[0].length*2+1];
		audioBufferOut[0]=0;
		for(int i=1; i<audioBufferOut.length; i++) {
			
			if((audioBufferOut[i-1]+diffBuffer[i-1])<-128) {
				audioBufferOut[i]= -128;
			}
			else if((audioBufferOut[i-1]+diffBuffer[i-1])>127) {
				audioBufferOut[i]= 127;
			}
			else {
				audioBufferOut[i]= (byte) (audioBufferOut[i-1]+ diffBuffer[i-1]);
			}
			
			
		}
		
		// Save the differences to a file 
		try {
			FileOutputStream fOut=new FileOutputStream(diffFileName);
			System.out.println("Writing bytes to file...");
			
			for(int i=0; i<buffer.length;i++) {
				fOut.write(Integer.toString(diffBuffer[i]).getBytes());
				fOut.write("\t".getBytes());
			}
			fOut.close();
		} catch ( IOException e) {
			e.printStackTrace();
		} 
		
		// Save the audio to a file
		try {
			FileOutputStream fOut=new FileOutputStream(audioFileName);
			System.out.println("Writing bytes to file...");
			
			for(int i=0; i<buffer.length;i++) {
				fOut.write(Byte.toString(audioBufferOut[i]).getBytes());
				fOut.write("\t".getBytes());
			}
			fOut.close();
		} catch ( IOException e) {
			e.printStackTrace();
		}
		
		//Play the audio
		
		try {
			
			
			AudioFormat linearPCM = new AudioFormat(8000,Q,1,true,false);
			SourceDataLine lineOut = AudioSystem.getSourceDataLine(linearPCM);
			lineOut.open(linearPCM,32000);
			System.out.print("Playing audio...");
			lineOut.start();
			lineOut.write(audioBufferOut, 0, audioBufferOut.length);
			lineOut.stop();
			System.out.print("...audio has stopped\n");
			lineOut.close();
			
			
		} catch (LineUnavailableException e) {
			
			e.printStackTrace();
		}
	}
	public static void audioAQDPCM(String code, String source, String track, int numberOfPackets, int packetSize,int Q, String diffFileName, String audioFileName, String meanFileName, String betaFileName) throws IOException {
		
		DatagramSocket s = new DatagramSocket();
		String packetInfo = code + "AQ" + "L" + track + source + numberOfPackets;
		InetAddress hostAddress = InetAddress.getByName(ServerIP);
		DatagramPacket p = new DatagramPacket(packetInfo.getBytes(), packetInfo.length(), hostAddress, serverListeningPort);
		
		// Creating receive socket and packet format.
		DatagramSocket r = new DatagramSocket(clientListeningPort);
		r.setSoTimeout(4000);
		byte[] rxbuffer = new byte[132];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		byte[][] buffer =new byte[numberOfPackets][packetSize]; //every line is a packet
		int packetsReceived=0;
		
		try {
			s.send(p);
           
			for(int i=0;i<numberOfPackets;i++){
				r.receive(q);
				packetsReceived++;
				System.out.println("audio packet:"+packetsReceived);
				//store packet to buffer
        		for(int j=0;j<packetSize;j++) {
        			buffer[i][j]= q.getData()[j];
        		}
        		
			}	 
			  
		} catch (Exception x) {
			  System.out.println(x);
		}
		// Sockets termination.
		s.close();
		r.close();	
		
		
		int[] mean= new int[buffer.length];
		int[] beta= new int[buffer.length];
		int meanL,meanM,betaL,betaM;
		
		//get mean and beta from the header of every packet 
		for(int i=0; i<buffer.length;i++) {
			meanL=(int)buffer[i][0];
			meanM=(int)buffer[i][1];
			betaL=(int)buffer[i][2];
			betaM=(int)buffer[i][3];
			
			mean[i]=(meanM<<8)|(meanL & 0xFF);
			beta[i]= (betaM<<8)|(betaL & 0xFF);
			
		}
		
		int[] diffBuffer= new int[buffer.length*(buffer[0].length-4)*2];
		int sum = 0;
		
		for(int i=0; i<buffer.length;i++) {
			
			for(int j=4; j<buffer[0].length; j++) {
			
				diffBuffer[sum]= ((int)buffer[i][j] & 0xF0 )>> 4; //first nibble 
				sum++;
				
				diffBuffer[sum]= (int)buffer[i][j] & 0xF;	//second nibble
				sum++;
			}
			
		}	
		
		int [] diffBufferOut = new int[buffer.length*(buffer[0].length-4)*2];
		int packet;
		
		//recreating each sample as the difference*step plus the mean
		for(int i=0; i<diffBuffer.length;i++) {
			
			packet= i/256; // a packet has 256 nibbles
			
			diffBuffer[i] = diffBuffer[i] - 8; //transform 0 to 15 values to -8 to 7

			diffBuffer[i] = (diffBuffer[i] * beta[packet]);
			
			diffBufferOut[i] = diffBuffer[i]; //keep differences for graph
			
			diffBuffer[i] = diffBuffer[i] +mean[packet];
		
		}
		
		byte[] audioBufferOut= new byte[buffer.length*(buffer[0].length-4)*2*2];
		for(int i=0; i<audioBufferOut.length;i++) {
			int j=i/2;
			
			//storing 16bit number to 2 bytes
			audioBufferOut[i]=(byte) (diffBuffer[j]&0xFF);
			audioBufferOut[i+1]= (byte)((diffBuffer[j]>>8)&0xFF);
			i++;
		}
		
		// Save the differences to a file 
		try {
			FileOutputStream fOut=new FileOutputStream(diffFileName);
			System.out.println("Writing bytes to file...");
			
			for(int i=0; i<buffer.length;i++) {
				fOut.write(Integer.toString(diffBufferOut[i]).getBytes());
				fOut.write("\t".getBytes());
			}
			fOut.close();
		} catch ( IOException e) {
			e.printStackTrace();
		} 
		// Save the means to a file 
		try {
			FileOutputStream fOut=new FileOutputStream(meanFileName);
			System.out.println("Writing bytes to file...");
			
			for(int i=0; i<buffer.length;i++) {
				fOut.write(Integer.toString(mean[i]).getBytes());
				fOut.write("\t".getBytes());
			}
			fOut.close();
		} catch ( IOException e) {
			e.printStackTrace();
		} 
		// Save the beta to a file 
		try {
			FileOutputStream fOut=new FileOutputStream(betaFileName);
			System.out.println("Writing bytes to file...");
			
			for(int i=0; i<buffer.length;i++) {
				fOut.write(Integer.toString(beta[i]).getBytes());
				fOut.write("\t".getBytes());
			}
			fOut.close();
		} catch ( IOException e) {
			e.printStackTrace();
		} 
		// Save the audio to a file
		try {
			FileOutputStream fOut=new FileOutputStream(audioFileName);
			System.out.println("Writing bytes to file...");
			
			for(int i=0; i<buffer.length;i++) {
				fOut.write(Byte.toString(audioBufferOut[i]).getBytes());
				fOut.write("\t".getBytes());
			}
			fOut.close();
		} catch ( IOException e) {
			e.printStackTrace();
		}
		//Play the audio
		
		try {
			
			
			AudioFormat linearPCM = new AudioFormat(8000,Q,1,true,false);
			SourceDataLine lineOut = AudioSystem.getSourceDataLine(linearPCM);
			lineOut.open(linearPCM,32000);
			System.out.print("Playing audio...");
			lineOut.start();
			lineOut.write(audioBufferOut, 0, audioBufferOut.length);
			lineOut.stop();
			System.out.print("...audio has stopped\n");
			lineOut.close();
			
			
		} catch (LineUnavailableException e) {
			
			e.printStackTrace();
		}
	}		
	//gets udp response packets from ithakicopter for a specific duration 
	public static void telemetry(int duration,String filename) throws IOException {
		byte[] copterBuffer= new byte[2048];
		DatagramSocket udpCopter = new DatagramSocket(48078);
		udpCopter.setSoTimeout(2000);
		DatagramPacket pCopter = new DatagramPacket(copterBuffer, copterBuffer.length);
		long time1,time2;
		String data="";
		
		time1= System.currentTimeMillis();
		do {
			try {
				udpCopter.receive(pCopter);
				  String message = new String(pCopter.getData(),0,pCopter.getLength());
				  System.out.println(message);
				  
				  data= data+message+"\r\n";
				  
			  } catch (Exception x) {
				  System.out.println(x);
			}
			
			time2= System.currentTimeMillis();
		}
		while(time2-time1<= duration*1000);
		
		createFile(data,filename); //save data to a file
	}
	
	
	//gets packets of every OBD parameter for a desired engine run time
	public static void vehicle(String code,int duration) throws IOException{
		
		String request = "OBD=01 05" ;
		// Create send socket and packet to transmit
		DatagramSocket s = new DatagramSocket();
		String packetInfo = code + request;

		byte[] txbuffer = packetInfo.getBytes();
		InetAddress hostAddress = InetAddress.getByName(ServerIP);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverListeningPort);
		
		// Create receive socket and packet to receive
		DatagramSocket r = new DatagramSocket(clientListeningPort);
		r.setSoTimeout(4000);

		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		p.setData(packetInfo.getBytes());
		p.setLength(packetInfo.getBytes().length);
		
		long time1,time2; 	
		String engTime= "OBD=01 1F";
		String airTemp= "OBD=01 0F";
		String throttle= "OBD=01 11";
		String rpm= "OBD=01 0C";
		String speed= "OBD=01 0D";
		String coolTemp= "OBD=01 05";
		String data=""; //data of OBD will be stored in a string, to create a txt file
		
		time1= engTime(s,r,p,q,code,engTime);
		time2=time1;
		
		// Air temperature
		do {
			data = data + Long.toString(time2)+ "\t";
			//preparing transmitted packet
			packetInfo = code+airTemp;
			p.setData(packetInfo.getBytes());
			p.setLength(packetInfo.getBytes().length);
			
			String message=new String();
			String byte1,result;
			int XX, value;
			
			//receiving OBD-II response
			try {
				  
				  s.send(p);
				  r.receive(q);
			
				  message = new String(q.getData(),0,q.getLength());
				  System.out.println("Air temperature message: "+message);
				    
			  } catch (Exception x) {
				  System.out.println(x);
			 }
			
			//storing parameter bytes
			if(message.length()>0) {
				byte1 = message.substring(6).substring(0,2);
			}
			else {
				byte1="FF";
			}
			
			XX= Integer.parseInt(byte1,16);
			
			//using formula
			value= XX-40;
			
			result= Integer.toString(value);
			
			data = data + result + "\t";
			
			
			//Throttle
			packetInfo = code + throttle;
			p.setData(packetInfo.getBytes());
			p.setLength(packetInfo.getBytes().length);
			
			message=new String();

			
			//receiving OBD-II response
			try {
				  
				  s.send(p);
				  r.receive(q);
			
				  message = new String(q.getData(),0,q.getLength());
				  System.out.println("Throttle message: "+message);
				    
			  } catch (Exception x) {
				  System.out.println(x);
			 }
			
			//storing parameter bytes
			if(message.length()>0) {
				byte1 = message.substring(6).substring(0,2);
			}
			else {
				byte1="FF";
			}
			
			XX= Integer.parseInt(byte1,16);
			
			//using formula
			value= XX*100/255;
			
			result= Integer.toString(value);
			
			
			data = data + result +"\t";
			
			
			// Rpm
			
			packetInfo = code + rpm;
			p.setData(packetInfo.getBytes());
			p.setLength(packetInfo.getBytes().length);
			
			message=new String();
			String byte2;
			int  YY;
			
			//receiving OBD-II response
			try {
				  
				  s.send(p);
				  r.receive(q);
			
				  message = new String(q.getData(),0,q.getLength());
				  System.out.println("RPM message: "+message);
				    
			  } catch (Exception x) {
				  System.out.println(x);
			 }
			
			//storing parameter bytes
			if(message.length()>0) {
				byte1 = message.substring(6).substring(0,2);
				byte2= message.substring(6).substring(3);
			}
			else {
				byte1="FF";
				byte2="FF";		
			}

			XX= Integer.parseInt(byte1,16);
			YY= Integer.parseInt(byte2,16);
			
			//using formula
			value= ((XX*256)+YY)/4;
			result= Integer.toString(value);
						
			data = data + result +"\t";
			
			//speed
			
			packetInfo = code + speed;
			p.setData(packetInfo.getBytes());
			p.setLength(packetInfo.getBytes().length);
			
			message = new String();

			
			//receiving OBD-II response
			try {
				  
				  s.send(p);
				  r.receive(q);
			
				  message = new String(q.getData(),0,q.getLength());
				  System.out.println("Speed message: "+message);
				    
			  } catch (Exception x) {
				  System.out.println(x);
			 }
			
			//storing parameter bytes
			if(message.length()>0) {
				byte1 = message.substring(6).substring(0,2);
			}
			else {
				byte1="FF";
			}
			
			XX= Integer.parseInt(byte1,16);
			
			//using formula
			value=XX;
			result= Integer.toString(value);

			data = data + result +"\t";
			
			//coolant temperature
			packetInfo = code + coolTemp;
			p.setData(packetInfo.getBytes());
			p.setLength(packetInfo.getBytes().length);
			
			message=new String();

			
			//receiving OBD-II response
			try {
				  
				  s.send(p);
				  r.receive(q);
			
				  message = new String(q.getData(),0,q.getLength());
				  System.out.println("Coolant temperature message: "+message);
				    
			  } catch (Exception x) {
				  System.out.println(x);
			 }
			
			//storing parameter bytes
			if(message.length()>0) {
				byte1 = message.substring(6).substring(0,2);
			}
			else {
				byte1="FF";
			}
			
			XX= Integer.parseInt(byte1,16);
			
			//using formula
			value=XX-40;
			result= Integer.toString(value);
			System.out.println("Coolant temperature (C): "+result);
			data = data + result +"\t";
			
			data= data+ "\r\n";
			
			time2= engTime(s,r,p,q,code,engTime);
		}
		while(time2-time1<= duration); //get data for a specific engine runtime in seconds
		
		createFile(data,"obd.txt");
	}
	//returns OBD engine runtime in seconds
		public static int engTime(DatagramSocket s, DatagramSocket r, DatagramPacket p, DatagramPacket q,String obdCode, String engTime) {
			
			//preparing transmitted packet
			String packetInfo = obdCode+engTime;
			p.setData(packetInfo.getBytes());
			p.setLength(packetInfo.getBytes().length);
			
			String message=new String();
			String byte1,byte2;
			int XX, YY , value;
			
			//receiving OBD-II response
			try {
				  s.send(p);
				  r.receive(q);
		 
				  message = new String(q.getData(),0,q.getLength());
				  System.out.println("Engine run time message: "+message);
				  
			  } catch (Exception x) {
				  System.out.println(x);
			  }
			
			//storing parameter bytes
			if(message.length()>0) {
				byte1 = message.substring(6).substring(0,2);
				byte2= message.substring(6).substring(3);
			}
			else {
				byte1="00";
				byte2="00";
			}

			XX= Integer.parseInt(byte1,16);
			YY= Integer.parseInt(byte2,16);
		
			//using formula
			value = 256*XX+YY;
			System.out.println("Runtime seconds: "+value);
			
			return value;
		}
	
	//saves a string to a file
	public static void createFile(String data, String name) {
		
		File file =new File(name);
		try {
			Files.write(file.toPath(), data.getBytes());
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	

	
}
