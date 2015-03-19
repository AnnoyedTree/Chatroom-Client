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
	
	//Initialize our GUI. This is what our "Main/Login page" will look like
	private void init()
	{
		//Add a menu bar so we can have our connect and disconnect buttons
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu( "File" );
		
		JMenuItem connect = new JMenuItem( "Connect" );
		JMenuItem disconnect = new JMenuItem( "Disconnect" );
		
		connect.addActionListener( actionLogin() ); //Show login menu when the user hits "Connect"
		disconnect.addActionListener( actionDisconnect() ); //Disconnect user if he is valid and still logged in
		
		file.add( connect );
		file.add( disconnect );
		bar.add( file );
		setJMenuBar( bar );
		
		//JTabbedPane will allow for us to setup multiple tabs through private chat
		tabPanel = new JTabbedPane();
		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1,1) );
		
		messageText = new JTextField( "Enter a message here..." );
		messageText.addActionListener( actionEnter(messageText) );
		
		chatArea = new JTextArea();
		userList = new JList();
		userList.addMouseListener( listenSelect() );
		
		chatPane = new JScrollPane( chatArea );
		JScrollPane listPane = new JScrollPane( userList );
		
		chatArea.setEditable( false );
		
		panel.add( chatPane );
		panel.add( listPane );
		add( messageText, BorderLayout.SOUTH );
		
		tabPanel.addTab( "Open Chat", panel );
		add( tabPanel );
		addWindowListener( listenQuit() );
	}
	
	//ActionListener Login:
	//This will make our loginFrame popup, similar to JOptionPane only more Inputs
	//Inputs: Username Field, IP Address Field, Login button, Cancel button
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
	
	//ActionListener Connect:
	//After the user accepts his inputs (Username, IPAddress) he will begin to connect
	private ActionListener actionConnect( final JTextField name, final JTextField ip, final ChatGUI frame )
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				String[] address = ip.getText().split( ":" );
				String username = name.getText();

				//Attempt to connect to our Server
				try
				{
					Socket s = new Socket( address[0], Integer.parseInt(address[1]) );
					client = new Client( s, username, frame );
				} catch ( IOException ex ) {
					//Alert the user that no connection has been made
					JOptionPane.showMessageDialog( loginFrame, "Connection to server was not found", "Error", JOptionPane.ERROR_MESSAGE );
				} finally {
					//After login is successful or not, hide the login frame from the user
					loginFrame.setVisible( false );
				}
			}
		});
	}
	
	//ActionListener Cancel:
	//This is for when a user changes his mind while logging in
	//He is allowed to perform a cancel to HIDE to loginFrame
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
	
	//ActionListener Enter:
	//This is what happens when a user hits the "ENTER" key while typing a message
	private ActionListener actionEnter( final JTextField msg )
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				//If there is no valid connection or there is no message (SPAM PROTECTION)
				if ( client.getSocket() == null || !client.isConnected() || msg.getText().length() <= 0 )
					return;
				
				//The user is on the indexed tab of 0 (This is the "Open Chat" tab since its the first tab we created)
				if ( tabPanel.getSelectedIndex() <= 0 )
					client.sendMessage( client.getUsername() + ": " + msg.getText() );
				//If the user is on any other tab greater than 0, they will be private messages
				else
				{
					//This gets the name of the Tab (Tab names will display the current user in the Private Messaging)
					//We also setup our string to hold the user he is currently messaging
					String user = tabPanel.getTitleAt( tabPanel.getSelectedIndex() );
					client.sendMessage( "@" + client.getUsername() + ": " + user + " " + msg.getText() );
					
					//This will return the TAB the current user has selected, so when he presses ENTER
					//Send a private message to the user whos tab is currently selected
					JPanel pane = (JPanel)tabPanel.getSelectedComponent(); 
					addTextToTab( pane, client.getUsername() + ": " + msg.getText() );
				}
				
				msg.setText( "" );
			}
		});
	}
	
	//ActionListener: Disconnect
	//To properly log the user out of the chat room and close all connections
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
	
	//WindowListener: Quit
	//This performs when the X in the top right hand corner of the program is clicked
	//We want to close all connections before the program is exited
	private WindowAdapter listenQuit()
	{
		return ( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e )
			{
				try
				{
					if ( loginFrame != null )
						loginFrame.removeAll();
					
					if ( client != null && client.isConnected() )
						client.disconnect();
					
				} catch ( IOException ex ) {
					//dothings
				}
			}
		});
	}
	
	//MouseListener: Select
	//This is for the user clicking on another user in the "Open Chat" tab
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
				{
					//If there is not tab for the User then create one
					//And select the tab right after it is created
					createNewTab( name, "", false );
					for ( int i = 0; i < tabPanel.getTabCount(); i++ )
					{
						if ( tabPanel.getTitleAt(i).equals(name) )
						{
							tabPanel.setSelectedIndex(i);
							return;
						}
					}
				}
				else
					//If the tab already exists then just select it
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
	//This will display Username & IP-Address
	private void loginFrame()
	{
		if ( loginFrame != null && !loginFrame.isVisible() && !client.isConnected() )
		{
			loginFrame.setVisible( true );
			return;
		}
		
		loginFrame = new JFrame( "Login" );
		loginFrame.setSize( 400, 200 );
		loginFrame.setLocationRelativeTo( this );
		loginFrame.setDefaultCloseOperation( EXIT_ON_CLOSE );
		
		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(3,2) );
		
		JTextField txtusername = new JTextField( "Anonymous" );
		JTextField txtipaddress = new JTextField( "localhost:9020" );
		JLabel lblusername = new JLabel( "Username:" );
		JLabel lblipaddress = new JLabel( "IP Address:" );
		JButton connect = new JButton( "Login" );
		JButton cancel = new JButton( "Cancel" );
		
		connect.addActionListener( actionConnect(txtusername, txtipaddress, this) );
		cancel.addActionListener( actionCancel() );
		
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
	//We want to display all the users connected to the Chatroom
	public void setUserList( ArrayList<String> list )
	{
		list.remove( client.getUsername() );
		userList.setListData( list.toArray() );
	}
	
	//Adds a line to a private chat tab
	public void addToPrivateChat( String line )
	{
		//Split the line at ":" (Ex. Brian: Hello)
		//This will return separate strings (Ex. "Brian" ":" "Hello")
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
	private void createNewTab( String name, String line, boolean write )
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
	}
	
	//Forward Method
	private void addChatToTab( JPanel panel, String name, String line )
	{
		addTextToTab( panel, name + ": " + line );
	}
	
	//Add a line to the text area in the tab.
	private void addTextToTab( JPanel panel, String line )
	{
		//Fail-safe
		if ( panel == null )
			return;
		
		//Loop through all of the components within the Panel of the Tab
		for ( Component c : panel.getComponents() )
		{
			//If we find the ScrollPane the add to its viewport
			if ( c instanceof JScrollPane )
			{
				JViewport port = ((JScrollPane)c).getViewport();
				JTextArea chat = (JTextArea)port.getView();
				
				chat.append( line + "\n" );
				//Set the scrollbar to the bottom so user can see the most recent message
				((JScrollPane)c).getVerticalScrollBar().setValue( ((JScrollPane)c).getVerticalScrollBar().getMaximum() );
				//break;
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
