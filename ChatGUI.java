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
	
	public ChatGUI()
	{
		super( "Chatroom [No Connection]" );
		setSize( 600, 500 );
		setLocationRelativeTo( null );
		setDefaultCloseOperation( EXIT_ON_CLOSE );
		
		init();
	}
	
	private void init()
	{
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu( "File" );
		
		JMenuItem connect = new JMenuItem( "Connect" );
		JMenuItem disconnect = new JMenuItem( "Disconnect" );
		
		connect.addActionListener( actionLogin() );
		disconnect.addActionListener( actionDisconnect() );
		
		file.add( connect );
		file.add( disconnect );
		bar.add( file );
		setJMenuBar( bar );
		
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
		//panel.add( userList );
		//panel.add( messageText );
		add( messageText, BorderLayout.SOUTH );
		add( panel );
		
		addWindowListener( listenQuit() );
	}
	
	//gui things to make neater
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
	
	private ActionListener actionConnect( final JTextField name, final JTextField ip, final ChatGUI frame )
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				String[] address = ip.getText().split( ":" );
				String username = name.getText();
				
				try
				{
					Socket s = new Socket( address[0], Integer.parseInt(address[1]) );
					client = new Client( s, username, frame );
					setTitle( "Chatroom [" + address[0] + " @ " + address[1] + "] (" + username + ")" );
				} catch ( IOException ex ) {
					JOptionPane.showMessageDialog( loginFrame, "Connection to server was not found", "Error", JOptionPane.ERROR_MESSAGE );
				} finally {
					loginFrame.setVisible( false );
				}
			}
		});
	}
	
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
	
	private ActionListener actionEnter( final JTextField msg )
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( client.getSocket() == null || !client.getSocket().isConnected() )
					return;
				
				if ( msg.getText().startsWith("@") ) //handle private message
					client.sendMessage( "@" + client.getUsername() + ": " + msg.getText() );
				else
					client.sendMessage( client.getUsername() + ": " + msg.getText() );
				
				msg.setText( "" );
			}
		});
	}
	
	private ActionListener actionDisconnect()
	{
		return ( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					if ( client != null && client.getSocket().isConnected() )
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
					
					if ( client != null && client.getSocket().isConnected() )
						client.disconnect();
					
				} catch ( IOException ex ) {
					//dothings
				}
			}
		});
	}
	
	private MouseListener listenSelect()
	{
		return ( new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				messageText.setText( "@" + userList.getSelectedValue() );
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
	
	private void loginFrame()
	{
		if ( loginFrame != null && !loginFrame.isVisible() )
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
		JTextField txtipaddress = new JTextField( "localhost:9090" );
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
	
	public void addToChat( String line )
	{
		chatArea.append( line + "\n" );
		chatPane.getVerticalScrollBar().setValue( chatPane.getVerticalScrollBar().getMaximum() );
	}
	
	public void setUserList( ArrayList<String> list )
	{
		userList.setListData( list.toArray() );
	}
}
