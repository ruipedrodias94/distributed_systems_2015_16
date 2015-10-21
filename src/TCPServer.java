
// TCPServer2.java: Multithreaded server
import java.net.*;
import java.io.*;
import java.util.*;

//Assim que se liga tenta fazer ping	

public class TCPServer {
	public static void main(String args[]) {
		int numero = 0;
		boolean isPrimary = false;
		Properties props = new Properties();
		InputStream inputConfigs = null;
		
		//Ler do Ficheiro das configs
		try {
			inputConfigs = new FileInputStream("clientConf.properties");
			props.load(inputConfigs);
				
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.getLocalizedMessage());
		}
		
		
		//Ligar Server
		try {
			int serverPort = Integer.parseInt(props.getProperty("portPrimario"));
			
			System.out.println("A Escuta no Porto : " + serverPort);
			ServerSocket listenSocket = new ServerSocket(serverPort);
			isPrimary = true;
			System.out.println("Servidor Primário à escuta!");
			Ping p = new Ping(isPrimary);
			p.ping();
			
			while (true) {
				Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
				new Connection(clientSocket, numero);
				numero++;
			}
		
		
		} catch (IOException e) {
			System.out.println("Impossivel ligar no porto primario:" + e.getMessage());
			System.out.println("Tentativa de ligar como porto secundario...");
		
		//não conseguiu ligar como primario vai tentar ligar como secundario	
		try{

			int serverPort2 = Integer.parseInt(props.getProperty("portSecundario"));
			
			System.out.println("A Escuta no Porto : " + serverPort2);
			ServerSocket listenSocket2 = new ServerSocket(serverPort2);
			isPrimary = false;
			System.out.println("Servidor Secundário à escuta!");
			Ping p2 = new Ping(isPrimary);
			p2.ping();
			}
			catch(IOException e2)
			{
				System.out.println("Impossível Ligar como secundario:"+e2.getMessage());
			}
			catch(NumberFormatException ex)
			{
				System.out.println("Erro a ler configuraçoes:"+ex.getMessage());
			}

		}
	}
}

// = Thread para tratar de cada canal de comunicacao com um cliente
class Connection extends Thread {
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	int thread_number;
	Shared_Clients sc = new Shared_Clients();

	public Connection(Socket aClientSocket, int numero) {
		thread_number = numero;
		sc.addClient(aClientSocket);
		try {
			clientSocket = aClientSocket;
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(aClientSocket.getOutputStream());
			out.writeUTF("Bem vinda Sua Puta Fraca");
			this.start();
		} catch (IOException e) {
			System.out.println("Connection:" + e.getMessage());
		}
	}

	// =============================
	public void run() {
		String resposta;
		try {
			while (true) {
				// an echo server
				String data = in.readUTF();
				System.out.println("T[" + thread_number + "] Recebeu: " + data);
				resposta = data;
				sc.send_clients(resposta, thread_number);

			}
		} catch (EOFException e) {
			System.out.println("EOF:" + e);
		} catch (IOException e) {
			System.out.println("IO:" + e);
		}
	}
}

class Shared_Clients {
	public static ArrayList<Socket> clientes = new ArrayList<Socket>();
	DataOutputStream out;

	synchronized void addClient(Socket c) {
		clientes.add(c);
		System.out.println("Cliente adicionado");
	}

	synchronized void send_clients(String msg, int clintNmr) {
		

		int i;
		for (i = 0; i < clientes.size(); i++) {

			try {

				if (i == 0) {
					msg = "de cliente " + clintNmr + " : " + msg;
				}
				if(i!=clintNmr){
				out = new DataOutputStream(clientes.get(i).getOutputStream());
				out.writeUTF(msg);
				System.out.println("Enviado de cliente " + clintNmr + " para " + i);
			}

			} catch (IOException e) {
				System.out.println("IO:" + e);
			}
		}
	}
}

//thread que vai estar continuamente a fazer ping entre servidores
class Ping extends Thread{


	Properties props = new Properties();
	InputStream inputConfigs = null;
	boolean isPrimary;
	String host, host2;
	int port,port2;


	Ping(boolean isPrimary)
	{
		this.isPrimary = isPrimary;

		try {
			this.inputConfigs = new FileInputStream("clientConf.properties");
			this.props.load(inputConfigs);
				
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.getLocalizedMessage());
		}
		this.host = props.getProperty("hostPrimario");
		this.port = Integer.parseInt(props.getProperty("portPrimario"));
		this.host2 = props.getProperty("hostSecundario");
		this.port2 = Integer.parseInt(props.getProperty("portSecundario"));
	}
		
		//Ler do Ficheiro das configs
		
		public void ping(){
		
		
		System.out.println("Iniciando ping...");
		this.start();
	}
		public void run()
		{
			//Se o servidor for primario
			if(this.isPrimary == true)
			{
				DatagramSocket aSocket = null;
				String s;
				try{
			aSocket = new DatagramSocket(port2);
			System.out.println("Socket Datagram a escuta no porto "+ host2);
			while(true){
				byte[] buffer = new byte[1000]; 			
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				s=new String(request.getData(), 0, request.getLength());	
				System.out.println("Server Recebeu: " + s);	

				DatagramPacket reply = new DatagramPacket(request.getData(), 
						request.getLength(), request.getAddress(), request.getPort());
				aSocket.send(reply);
			}}
			catch (SocketException e){System.out.println("Socket: " + e.getMessage());
		}catch (IOException e) {System.out.println("IO: " + e.getMessage());
		}finally {if(aSocket != null) aSocket.close();}

		}
		else
		{	
			DatagramSocket aSocket2 = null;
			String texto = "HEARTBEAT";
			try {
			aSocket2 = new DatagramSocket();
			while(true)
			{
				byte [] m = texto.getBytes();
				
				InetAddress aHost = InetAddress.getByName(host2);
				int serverPort = port2;		                                                
				DatagramPacket request = new DatagramPacket(m,m.length,aHost,serverPort);
				aSocket2.send(request);			                        
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
				aSocket2.receive(reply);
				System.out.println("Recebeu: " + new String(reply.getData(), 0, reply.getLength()));
			}
		}
		catch (SocketException e){System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){System.out.println("IO: " + e.getMessage());
		}finally {if(aSocket2 != null) aSocket2.close();}
		}
		}

}