package webchat_client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

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
		
		gui = frame;
		
		thread = new Thread( this );
		thread.start();
		
		setUsername( name );
		gui.setTitle( "Chatroom [" + socket.getLocalAddress().getHostAddress() + "@" + socket.getPort() + "] User: " + getUsername() );
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
	
	public void addPrivateMessage( String line )
	{
		line = line.replace( "-", "" );
		gui.addToPrivateChat( line );
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
	
	private void setUsername( String name )
	{
		StringBuilder newname = new StringBuilder();
		for ( char c : name.toCharArray() )
		{
			if ( c != '@' && c != '+' && c != '-' )
				newname.append(c);
		}
		
		username = newname.toString();
		sendMessage( "+" + newname.toString() );
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
					{
						String line = object.toString();
						
						if ( line.startsWith("-") )
							addPrivateMessage( line );
						else
							addMessage( line );
					}
					else if ( object instanceof ArrayList )
						gui.setUserList( (ArrayList<String>)object );
					
				}
			}
		} catch ( Exception e ) {
			//dothings
		}
	}
}
