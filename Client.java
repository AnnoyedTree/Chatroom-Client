package webchat_client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

//We implement runnable since we don't want to create a new Thread class
public class Client implements Runnable 
{
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	private Socket socket = null;
	private Thread thread;
	private String username;
	
	private ChatGUI gui;

	//Constructor
	public Client( Socket s, String name, ChatGUI frame ) throws IOException
	{
		//Our socket. This is more or less our connection to the server
		socket = s;
		
		//Add an Input/Output stream to allow 'communication' to the server, from us (the client).
		in = new ObjectInputStream( socket.getInputStream() );
		out = new ObjectOutputStream( socket.getOutputStream() );
		
		//Assign GUI to the client
		gui = frame;
		
		//Create a new thread for the client
		thread = new Thread( this );
		thread.start();
		
		//Set the user's name
		setUsername( name );
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
			//Flush what was in the output stream before we write data
			out.flush();
			//Write to object to the server. The server will handle the data from here
			out.writeObject( object );
		} catch ( IOException e ) {
			//dothings
		}
	}
	
	//This adds a message to the "Open Chat" tab 
	public void addMessage( String line )
	{
		gui.addToChat( line );
	}
	
	//This will handle all private messages.
	public void addPrivateMessage( String line )
	{
		line = line.replace( "-", "" );
		gui.addToPrivateChat( line );
	}
	
	//When the user disconnects we want to end all connections
	public void disconnect() throws IOException
	{
		//If he is already not connected we don't need to run this
		if ( !isConnected() )
			return;
		
		//We write a message to the server, 
		//telling that this client no longer wishes to have a connection established
		out.write(1);
		
		//Now we can close our connections
		out.close();
		in.close();
		
		socket.close();
		//thread.interrupt();
	}
	
	//Client2Server messages hold a special character at the beginning of the message to help the client OR server
	//distinguish what type of message it is receiving.
	//Don't allow the characters @, +, or - since we use these and don't want users starting with these characters
	//in their names.
	private void setUsername( String name ) throws IOException
	{
		StringBuilder newname = new StringBuilder();
		//Loop through all characters within the String
		for ( char c : name.toCharArray() )
		{
			//If the character is not @, +, or - then it is a valid character
			if ( c != '@' && c != '+' && c != '-' )
				newname.append(c);
		}
		
		//After we give his "new" username. It could be blank or very short.
		//We want the user to have more than a couple of characters within their names
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
				//If our input we received from the server is a valid object
				if ( (object = in.readObject()) != null )
				{
					//If its a message
					if ( object instanceof String )
					{
						String line = object.toString();
						
						if ( line.startsWith("-") ) //the - is to tell our program that the message we received
							addPrivateMessage( line ); //is a private message
						else
							addMessage( line ); //This is a message for everybody to see, since it contains no
												//special characters
					}
					else if ( object instanceof ArrayList ) //If we receive the userList from the server
						gui.setUserList( (ArrayList<String>)object ); //This will update from the server everytime somebody connects or disconnects
					
				}
			}
		} catch ( Exception e ) {
			//dothings
		}
	}
}
