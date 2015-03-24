package webchat_client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

//We implement runnable since we don't want to create a new Thread class
//Personally, it would just be one more file to work with when its such
//a small program
public class Client implements Runnable 
{
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	private Socket socket = null;
	private Thread thread;
	private String username;
	
	private ChatGUI gui;

	//Constructor called from the Login button
	public Client( Socket s, String name, ChatGUI frame ) throws IOException
	{
		//Our socket. This is the connection to the server
		socket = s;
		
		//Add an input stream from the connection (to receive messages)
		in = new ObjectInputStream( socket.getInputStream() );
		//Add an output stream from the connection (to send messages)
		out = new ObjectOutputStream( socket.getOutputStream() );
		
		//Assign a GUI frame to the client
		gui = frame;
		
		//Create a new thread for the client
		//Thread is needed for input/output stream
		thread = new Thread( this );
		thread.start();
		
		//Set the user's name under my conditions
		setUsername( name );
		//Rename the gui frame title, since there is a connection
		gui.setTitle( "Chatroom [" + socket.getLocalAddress().getHostAddress() + "@" + socket.getPort() + "] User: " + getUsername() );
	}
	
	//This is our connection
	public Socket getSocket()
	{
		return socket;
	}

	//Client's name
	public String getUsername()
	{
		return username;
	}
	
	//This method will allow us to send output messages to the server
	//so the server is able to receive it as input
	public void sendMessage( Object object )
	{
		try
		{
			//Flush the output stream
			out.flush();
			//Send the message to the server
			out.writeObject( object );
		} catch ( IOException e ) {
			//dothings
		}
	}
	
	//Add a message to the "Open Chat" tab
	public void addMessage( String line )
	{
		gui.addToChat( line );
	}
	
	//Add a message to the Private Messaging tabs
	public void addPrivateMessage( String line )
	{
		line = line.replace( "-", "" );
		gui.addToPrivateChat( line );
	}
	
	//Client requests a disconnect, send request to the server and close connections
	public void disconnect() throws IOException
	{
		//If he is already not connected we don't need to run this
		if ( !isConnected() )
			return;
		
		//Message to server so the server closes the connections to this client
		out.write(1);
		
		//Now we can close our connections
		out.close();
		in.close();
		
		socket.close();
		//thread.interrupt();
	}
	
	//Set the user's name under my conditions
	//Won't allow special characters of "@,+,-" and spaces
	//It could possibly mess up how messages are handled by the server
	private void setUsername( String name ) throws IOException
	{
		StringBuilder newname = new StringBuilder();
		//Loop through all characters within the String
		for ( char c : name.toCharArray() )
		{
			//If the character is not @, +, -, or an empty space then it is a valid character
			if ( c != '@' && c != '+' && c != '-' && c != ' ' )
				newname.append(c);
		}
		
		//After getting the new username, check if the user's name is not nothing
		if ( newname.toString().equals("") || newname.toString().length() <= 2 )
			throw new IOException();
		
		username = newname.toString();
		
		//Send a message to the server, telling the server a new client has successfully connected
		sendMessage( "+" + newname.toString() );
	}
	
	//If the client is connected
	public boolean isConnected()
	{
		return (boolean)getSocket().isConnected();
	}
	
	//This runnable is what our Thread runs (We made one in the constructor)
	//We want our thread to have thinking capabilities 
	@Override
	public void run()
	{
		try
		{
			Object object;
			
			//While this thread is running
			while ( true ) 
			{
				//Input is received from the server
				if ( (object = in.readObject()) != null )
				{
					//If its a string (message)
					if ( object instanceof String )
					{
						String line = object.toString();
						
						if ( line.startsWith("-") ) //the "-" character for receiving a private message
							addPrivateMessage( line );
						else
							addMessage( line ); //This is a message for everybody to see, since it contains no
												//special characters
					}
					//User list
					else if ( object instanceof ArrayList )
						gui.setUserList( (ArrayList<String>)object ); //This will update from the server everytime somebody connects or disconnects
					
				}
			}
		} catch ( Exception e ) {
			//dothings
		}
	}
}
