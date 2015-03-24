package webchat_client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.*;

public class ChatGUI extends JFrame
{
	private Client client = null;
	private JFrame loginFrame;
	
	private JScrollPane chatPane;
	private JTextArea chatArea;
	
	private JTextField messageText;
	
	private JList userList;
	private JTabbedPane tabPanel;

	//Constructor
	public ChatGUI()
	{
		super( "Chatroom [No Connection]" );
		setSize( 600, 500 );
		setLocationRelativeTo( null );
		setDefaultCloseOperation( EXIT_ON_CLOSE );
		init();
	}
	
	//Create our main GUI screen
	private void init()
	{
		//Add a menu bar so we can have our connect and disconnect buttons
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu( "File" );
		
		JMenuItem connect = new JMenuItem( "Connect" );
		JMenuItem disconnect = new JMenuItem( "Disconnect" );
		
		connect.addActionListener( actionLogin() ); //Show login frame when the user hits "Connect"
		disconnect.addActionListener( actionDisconnect() ); //Disconnect user if he still logged in
		
		file.add( connect );
		file.add( disconnect );
		bar.add( file );
		setJMenuBar( bar );
		
		//Create a Tab Panel to create tabs to allow for private chatting 
		tabPanel = new JTabbedPane();
		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1,1) );
		
		//Create text field to allow user to input messages in to the chat room
		messageText = new JTextField( "Enter a message here..." );
		messageText.addActionListener( actionEnter(messageText) );
		
		//Create text area for chat messages
		chatArea = new JTextArea();
		
		//Create a list for users connected in the chat
		userList = new JList();
		userList.addMouseListener( listenSelect() );
		
		//Create a Scroll Pane to allow scrolling if the chatroom expands
		//greater then the chatArea
		chatPane = new JScrollPane( chatArea );
		JScrollPane listPane = new JScrollPane( userList );
		
		chatArea.setEditable( false );
		
		//Add all components we just created
		panel.add( chatPane );
		panel.add( listPane );
		add( messageText, BorderLayout.SOUTH );
		
		tabPanel.addTab( "Open Chat", panel );
		add( tabPanel );
		
		//Allow the player to disconnect if he clicks the [X] in the top right
		addWindowListener( listenQuit() );
	}
	
	//ActionListener for the "Connect" button
	//This will bring up the login frame for the user's input. (Username, IPAddress)
	private ActionListener actionLogin()
	{
		return ( new ActionListener() { 
			@Override
			public void actionPerformed( ActionEvent e ) 
			{
				loginFrame();
			}
		});
	}
	
	//ActionListener for when the "Login" button is pressed within the login frame
	//Get all the user's inputs and attempt to connect to the server
	private ActionListener actionConnect( final JTextField name, final JTextField ip, final ChatGUI frame )
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				//Split the string in to 2 parts. IP Address and Port
				String[] address = ip.getText().split( ":" );
				String username = name.getText();

				//Attempt to connect to our Server
				try
				{
					//Our socket (we can call this our connection)
					Socket s = new Socket( address[0], Integer.parseInt(address[1]) );
					
					//Connection successful, create a new client (this user)
					client = new Client( s, username, frame );
				} catch ( IOException ex ) {
					//Alert the user that no connection has been made with a popup
					JOptionPane.showMessageDialog( loginFrame, "Connection to server was not found", "Error", JOptionPane.ERROR_MESSAGE );
				} finally {
					//Hide the login frame if the connection was successful
					loginFrame.setVisible( false );
				}
			}
		});
	}
	
	//ActionListener for when the "Cancel" button is pressed within the login frame
	//Hide the login frame
	private ActionListener actionCancel()
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				loginFrame.setVisible( false );
			}
		});
	}
	
	//ActionListener for when the user presses "Enter" on the message text bar
	private ActionListener actionEnter( final JTextField msg )
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				//If there is no valid connection or there is no message (SPAM PROTECTION)
				if ( client.getSocket() == null || !client.isConnected() || msg.getText().length() <= 0 )
					return;
				
				//The user is on the tab 0. This tab will be the "Open Chat" tab, since it is the first one created
				if ( tabPanel.getSelectedIndex() <= 0 )
					client.sendMessage( client.getUsername() + ": " + msg.getText() );
				//If the user is on any other tab, it will be a private message
				else
				{
					//Get the title of the panel we are currently selected
					String user = tabPanel.getTitleAt( tabPanel.getSelectedIndex() );
					//Send a private message to the specified user
					client.sendMessage( "@" + client.getUsername() + ": " + user + " " + msg.getText() );
					
					//Get the panel currently located within the tab panel
					JPanel pane = (JPanel)tabPanel.getSelectedComponent();
					//Add the message to the chat in the tab panel
					addTextToTab( pane, client.getUsername() + ": " + msg.getText() );
				}
				
				//Clear text after "Enter" was hit
				msg.setText( "" );
			}
		});
	}
	
	//ActionListener for the "Disconnect" button in the menu bar
	private ActionListener actionDisconnect()
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					//Attempt to disconnect the client
					if ( client != null && client.isConnected() )
					{
						client.disconnect();
						JOptionPane.showMessageDialog( null, "You have disconnected from the server", "Connection Aborted", JOptionPane.INFORMATION_MESSAGE );
						setTitle( "Chatroom [No Connection]" );
					}
				} catch ( IOException ex ) {
					//do things
				}
			}
		});
	}
	
	//WindowsAdapter for the hit "X" is hit on the top right
	//We want to disconnect the user properly
	private WindowAdapter listenQuit()
	{
		return ( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e )
			{
				try
				{
					//Remove the login frame and all of it's components
					if ( loginFrame != null )
						loginFrame.removeAll();
					
					//Disconnect the user if he is still connected
					if ( client != null && client.isConnected() )
						client.disconnect();
					
				} catch ( IOException ex ) {
					//dothings
				}
			}
		});
	}
	
	//MouseListener for the user clicking on another user in the User List
	//The user list is only located in the "Open Chat" tab
	private MouseListener listenSelect()
	{
		return ( new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//Get the name of the currently selected user in the list
				String name = (String)userList.getSelectedValue();
				JPanel p = getPrivateTab( name );
				
				//If the name is blank don't do anything
				if ( name == null || name.equals("") )
					return;
				
				//Try to find a tab with the users name
				if ( p == null )
					//If there is not tab for the User then create one
					p = createNewTab( name, "", false );
				
				//Select our tab
				tabPanel.setSelectedComponent( p );
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	//This is the "Popup menu" for logging in to the server
	//This will display Username and IP Address
	private void loginFrame()
	{
		//If the frame then don't duplicate it
		if ( loginFrame != null && !loginFrame.isVisible() && !client.isConnected() )
		{
			loginFrame.setVisible( true );
			return;
		}
		
		//Create the login screen if not created
		loginFrame = new JFrame( "Login" );
		loginFrame.setSize( 400, 200 );
		loginFrame.setLocationRelativeTo( this );
		loginFrame.setDefaultCloseOperation( EXIT_ON_CLOSE );
		
		//Create the layout of the login frame
		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(3,2) );
		
		//Create username and ip address labels and text fields
		JTextField txtusername = new JTextField( "Anonymous" );
		JTextField txtipaddress = new JTextField( "localhost:9020" );
		JLabel lblusername = new JLabel( "Username:" );
		JLabel lblipaddress = new JLabel( "IP Address:" );
		
		//Buttons
		JButton connect = new JButton( "Login" );
		JButton cancel = new JButton( "Cancel" );
		
		connect.addActionListener( actionConnect(txtusername, txtipaddress, this) );
		cancel.addActionListener( actionCancel() );
		
		//Add all components to the panel
		panel.add( lblusername );
		panel.add( txtusername );
		panel.add( lblipaddress );
		panel.add( txtipaddress );
		panel.add( connect );
		panel.add( cancel );
		
		loginFrame.add( panel );
		loginFrame.setVisible( true );
	}
	
	//Adds a line to the "Open Chat" tab
	public void addToChat( String line )
	{
		chatArea.append( line + "\n" );
		chatPane.getVerticalScrollBar().setValue( chatPane.getVerticalScrollBar().getMaximum() );
	}
	
	//This updates every time a new user connects or leaves
	//Display all the users connected in the chatroom
	public void setUserList( ArrayList<String> list )
	{
		//Don't reveal the user to himself in the list or he can private message himself
		list.remove( client.getUsername() );
		//Add the user list obtained from the server to the gui
		userList.setListData( list.toArray() );
	}
	
	//Adds a line to a private chat tab
	public void addToPrivateChat( String line )
	{
		//Split the string in to an array at ":"
		String[] msg = line.split( ":", 3 );
		
		//Loop through all tabs and try to find the one the user is PMing
		JPanel p = getPrivateTab( msg[0] );
		if ( p == null )
			//No tab found, lets create a new tab
			createNewTab( msg[0], msg[1], true );
		else
			//We found the tab so all we need to do is add the line in to the chat
			addChatToTab( p, msg[0], msg[1] );
	}
	
	//Creates a new tab if one does not exist
	private JPanel createNewTab( String name, String line, boolean write )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1,1) );
		
		JTextArea chat = new JTextArea();
		JScrollPane scroll = new JScrollPane( chat );

		chat.setEditable( false );
		
		if ( write )
			chat.append( name + ":" + line + "\n" );
		
		panel.setName( name );
		panel.add( scroll );
		
		tabPanel.addTab( name, panel );
		
		return panel;
	}
	
	//Forward Method
	//this extra method is not necessary, at all...
	private void addChatToTab( JPanel panel, String name, String line )
	{
		addTextToTab( panel, name + ": " + line );
	}
	
	//Add a line to the text area in the tab.
	private void addTextToTab( JPanel panel, String line )
	{
		if ( panel == null )
			return;
		
		//Loop through all of the components within the Panel of the Tab
		for ( Component c : panel.getComponents() )
		{
			//The scroll pane that holds the chat text area component in the private message tab
			if ( c instanceof JScrollPane )
			{
				//Obtain the component of the chat text area
				JViewport port = ((JScrollPane)c).getViewport();
				JTextArea chat = (JTextArea)port.getView();
				
				chat.append( line + "\n" );
				
				//Set the scrollbar to the bottom so user can see the most recent message
				((JScrollPane)c).getVerticalScrollBar().setValue( ((JScrollPane)c).getVerticalScrollBar().getMaximum() );
			}
		}
	}
	
	//Loops through all the tabs titles and looks to match
	private JPanel getPrivateTab( String name )
	{
		for ( int i = 0; i < tabPanel.getTabCount(); i++ )
		{
			if ( tabPanel.getTitleAt(i).equals(name) )
				return (JPanel)tabPanel.getComponent(i);
		}
		return null;
	}
}
