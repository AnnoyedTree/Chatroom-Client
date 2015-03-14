package webchat_client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JFrame;

public class Client implements Runnable 
{
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	private Socket socket;
	private Thread thread;
	private String username;
	
	private ChatGUI gui;
	
	public Client( Socket s, String name, ChatGUI frame ) throws IOException
	{
		socket = s;
		
		in = new ObjectInputStream( socket.getInputStream() );
		out = new ObjectOutputStream( socket.getOutputStream() );
		
		username = name;
		gui = frame;
		
		thread = new Thread( this );
		thread.start();
		
		sendMessage( "+" + name );
	}
	
	public Socket getSocket()
	{
		return socket;
	}

	public String getUsername()
	{
		return username;
	}
	
	public void sendMessage( Object object )
	{
		try
		{
			out.flush();
			out.writeObject( object );
		} catch ( IOException e ) {
			//dothings
		}
	}
	
	public void addMessage( String line )
	{
		gui.addToChat( line );
	}
	
	public void disconnect() throws IOException
	{
		if ( !socket.isConnected() )
			return;
		
		out.write(1);
		out.close();
		in.close();
		
		socket.close();
		//thread.interrupt();
		//System.out.println( "Connection terminated by client" );
	}
	
	@Override
	public void run()
	{
		try
		{
			Object object;
			
			while ( true ) 
			{
				if ( (object = in.readObject()) != null )
				{
					if ( object instanceof String )
						addMessage( (String)object );
					else if ( object instanceof ArrayList )
						gui.setUserList( (ArrayList<String>)object );
					
				}
			}
		} catch ( Exception e ) {
			//dothings
		}
	}
}
