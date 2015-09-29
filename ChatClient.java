
package SD13;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class ChatClient extends JFrame implements ActionListener ,KeyListener{
	static ChatClient chat ;
	InputPanel inP;  //入力域とボタンのパネル
	JTextArea talkArea;
	JMenuBar mbar;
	JMenu menu;
	JMenu menu2;
	JMenuItem saveM, exitM,logoutM;
	Socket chatS = null;
	BufferedReader in = null;
	PrintStream out = null;
	String userName;
	String typekey;
	String presskey;
	int KeyCode;

	public void init(){
		typekey = "";
		presskey = "";
		addKeyListener(this);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		//キーコード取得
		KeyCode = e.getKeyCode();
		//画面に表示
		System.out.println("「" + KeyCode + "」が押されました。");
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		int keycode = e.getKeyCode();
		if (keycode == KeyEvent.VK_ENTER){
			System.out.println("enterキーが押された");
		}

	}

	public ChatClient(String title) {
		super(title);
		mbar = new JMenuBar();  //set menu
		setJMenuBar(mbar);
		menu = new JMenu("終了");
		menu2 = new JMenu("ログアウト");
		exitM = new JMenuItem("終了");
		logoutM = new JMenuItem("ログアウト");
		exitM.addActionListener(this);
		logoutM.addActionListener(this);
		menu.add(exitM);
		menu2.add(logoutM);
		mbar.add(menu);
		mbar.add(menu2);

		inP = new InputPanel();
		talkArea = new JTextArea(10,40);
		talkArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(talkArea);
		Container c = getContentPane();
		c.add(scrollPane, BorderLayout.CENTER);
		c.add(inP, BorderLayout.SOUTH);
	}

	public void actionPerformed(ActionEvent e){
		Object obj = e.getSource();
		if(obj == exitM){
			//sendMessage(" 接続を切りました。");
			out.println("logout");
			System.exit(0);
		}
		if(obj == logoutM){
			//sendMessage(" ログアウトしました。");
			out.println("logout");
			System.exit(0);
		}
	} 

	class InputPanel extends JPanel implements ActionListener{
		JTextField field;
		JButton goB;
		InputPanel(){
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			field = new JTextField(40);
			goB = new JButton("発言");
			goB.addActionListener(this);
			add(field);
			add(goB);
		}
		public void actionPerformed(ActionEvent e) {
			//sendMessage(field.getText());
			String judge =field.getText(); 
			out.println("chat "+field.getText());
			field.setText("");
			goB.requestFocus(); 
		}
		public void keyPressed(KeyEvent e){
			int keycode = e.getKeyCode();
			if (keycode == KeyEvent.VK_ENTER){
				System.out.println("enterキーが押された");
				out.println("chat "+field.getText());
				field.setText("");
				goB.requestFocus();
			}
		}
	}

	public void startChat() {
		//		sendMessage(" 接続しました。");
		String fromServer;
		/*
		 * サーバーからの通知　判別
		 */
		/*正規表現方式
		String loginS = "^login\\s\\w+";
		String ChatS = "^chat\\s\\w+\\s\\w+";
		Pattern loginP = Pattern.compile(loginS);
		Pattern ChatP = Pattern.compile(ChatS);*/
		try{
			while ((fromServer = in.readLine()) != null) {
				System.out.println(fromServer);
				//				System.out.println(fromServer);//デバッグ用
				/*Matcher l = loginP.matcher(fromServer);
				Matcher c = ChatP.matcher(fromServer);*/
				String judge =fromServer; 
				if(fromServer.equals("0 login succeed")){ 
					chat.setVisible(true) ;
				}else if(fromServer.equals("100 password invalid")){ 
					System.out.println("ユーザ名かパスワードが間違っています。");
					System.exit(1);
				}else if(fromServer.equals("101 multiple login")){
					System.out.println("多重ログインです。");
					System.exit(1);
					/*}else if(l.find()){//Login通知の時
					String[] login = fromServer.split(" ");
					out.println(login[2]+"さんがログインしました");
				}else if(c.find()){//Chat通知の時
					String[] Chat = fromServer.split(" ");
					for(int i=3;i<Chat.length-2;i++){//チャット内容でスペースがあった場合結合をしなければならない。
						Chat[2] += " "+Chat[i];
					}*/
				}else{
					System.out.println(fromServer); 										//デバッグよう
					String[] command = fromServer.split(" ");
					if(command[0].equals("login")) talkArea.append(command[2]+"さんがログインしました。前回のログインは"+command[3]+" "+command[4]+"です。\n");
					else if(command[0].equals("logout")) talkArea.append(command[2]+"さんがログアウトしました。\n"+command[3]+" "+command[4]+"にログインして以来、"+command[5]+"個の発言をしました。\n");
					else if(command[0].equals("oldchat")) {
						if(command.length==5) {
							talkArea.append(command[1]+":過去"+command[2]+"番目,"+command[3]+" "+command[4]+" >\n");
						}
						else{
							for(int i=6;i<command.length;i++){//チャット内容でスペースがあった場合結合をしなければならない。
								command[5] += " "+command[i];
							}
							talkArea.append(command[1]+":過去"+command[2]+"番目,"+command[3]+" "+command[4]+" >"+command[5]+"\n");
						}
					}
					else if(command[0].equals("chat")) {
						if(command.length==2) {
							talkArea.append(command[1]+">\n");
						}else{
							for(int i=3;i<command.length;i++){//チャット内容でスペースがあった場合結合をしなければならない。
								command[2] += " "+command[i];
							}
							talkArea.append(command[1]+">"+command[2]+"\n");
						}
					}else{
						talkArea.append(fromServer+"\n"); 
					}
				}
			}
			end();
		}catch(SocketException e){
			System.exit(1);
		}catch (IOException e){ 
			System.out.println("チャット中に問題が起こりました。");
			System.exit(1);
		}
	}

	public void sendMessage(String msg) {
		String s = userName + ":" + msg;
		out.println(s);
	}


	//network setup
	public void initNet(String serverName, int port, String uName , String Password) {
		userName = uName;
		// create Socket
		try {
			chatS = new Socket(InetAddress.getByName(serverName), port);
			// ローカルホストでテストの場合は上の代わりに下の行を有効にする
			//chatS = new Socket(InetAddress.getLocalHost(), port);
			in = new BufferedReader(
					new InputStreamReader(chatS.getInputStream()));
			out = new PrintStream(chatS.getOutputStream());
			out.println("user "+uName+" pass "+Password);
		} catch (UnknownHostException e) {
			System.out.println("ホストに接続できません。");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("IOコネクションを得られません。");
			System.exit(1);
		}
	}

	public void end() {
		try {
			out.close();
			in.close();
			chatS.close();
		} catch (IOException e) { System.out.println("end:" + e); }
	}

	/**
	 * java ChatClient サーバのIPアドレス ポート番号 ユーザ名
	 * args[0] = serveraddress
	 * args[1] = portnumber
	 * args[2] = username
	 */
	public static void main(String[] args) {
		String sName ="";
		int portN = 0;
		String uName = "";
		String Password = "";
		if (args.length != 3 && args.length !=4 ) {
			System.out.println(args.length);
			System.out.println(
					"Usage: java ChatClient サーバのIPアドレス ポート番号 ユーザ名");
			System.out.println("例: java ChatClient 210.0.0.1 50002 ariga");
			System.exit(0);
		}
		// Getting argument.
		else if(args.length == 4){
			sName = args[0];
			portN = Integer.valueOf(args[1]).intValue();
			uName = args[2];
			Password = args[3];
		}
		else if(args.length == 3){
			try {
				sName = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				System.out.println("ホストアドレスが取得できません");
			}
			portN = Integer.valueOf(args[0]).intValue();
			uName = args[1];
			Password = args[2];
		}
		System.out.println("serverName = " + sName);
		System.out.println("portNumber = " + portN);
		System.out.println("userName = " + uName);
		System.out.println("Password = " + Password);
		// Setup and start.
		JFrame.setDefaultLookAndFeelDecorated(true);
		chat = new ChatClient(uName + " -> " + sName);
		chat.initNet(sName, portN, uName , Password);
		chat.pack();
		chat.startChat();

	}

}
