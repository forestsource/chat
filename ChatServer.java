// 練習問題 チャットサーバプログラム
package SD13;
import java.net.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer {
	static ArrayList<String> memberName = new ArrayList<String>();
	static ArrayList<Long> LoginTime = new ArrayList<Long>();
	static ArrayList<Integer> ChatNum = new ArrayList<Integer>();

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("起動方法: java ChatServer ポート番号");
			System.out.println("例: java ChatServer 50002");
			System.exit(1);
		}
		int port = Integer.valueOf(args[0]).intValue();
		ServerSocket serverS = null;
		boolean end = true;
		try {
			System.out.println(InetAddress.getLocalHost().getHostAddress());
			serverS = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("ポートにアクセスできません。");
			System.exit(-1);
		}
		while (end) {
			new ChatMThread(serverS.accept()).start();
		}
		serverS.close();
	}
}

class ChatMThread extends Thread {
	HashMap<String, String> userTable = new HashMap<String, String>();
	Socket socket;
	PrintWriter out;
	BufferedReader in;
	static List<ChatMThread> member;

	ChatMThread(Socket s) {
		super("ChatMThread");
		socket = s;
		if (member == null) {
			member = new ArrayList<ChatMThread>();
		}
		member.add(this);
		//
		// ユーザー登録
		//
		userTable.put("HMT", "hmt");
		userTable.put("Tommy", "heat");
		userTable.put("Guest", "guest");
		userTable.put("Test", "test");
		userTable.put("Guest1", "guest1");
		userTable.put("Guest2", "guest2");
	}

	public void run() {
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			// /////////////////////////////
			/* ユーザ名とパスワードを取得 */
			// /////////////////////////////
			String getUserPassword = in.readLine();
			String[] UserPass = getUserPassword.split(" ");
			// ///////////////////////////////
			/* HashMap参照 */
			// ///////////////////////////////
			if (userTable.containsKey(UserPass[1])){//ユーザ名が存在するか
				if (userTable.get(UserPass[1]).equals(UserPass[3])) {//ユーザ名とパスワードが一致するか
					if (ChatServer.memberName.contains(UserPass[1])) {//マルチログインでないか
						out.println("101 multiple login");
						return;
					} else {						// ログイン成功
						Login(UserPass[1]);
					}
				}else{
					out.println("100 password invalid");
					return;
				}
			} else {//パスワードが不正
				out.println("100 password invalid");
				return;
			}
			String fromClient;
			while ((fromClient = in.readLine()) != null) {
				System.out.println("runのwhileでスレッドの名前を取得:"+Thread.currentThread().getName());
				String ClientName = Thread.currentThread().getName();
				if (fromClient.equals("logout")) break;							//logout
				System.out.println("chat でwriteToAllを呼び出す");//debug
				String[] chatString = fromClient.split(" ");
				if(chatString.length==1)
					System.out.println("chat でsplitを判断:"+chatString.length);//debug

				if(chatString.length==1) {
					writeToAll("chat", ClientName, " ");
					DataBaseInsert(ClientName, 1, fromClient+" ");// chat内容をDBへ書き込み
				}else{
					for(int i=2;i<chatString.length;i++){//チャット内容でスペースがあった場合結合をしなければならない。
						chatString[1] +=" "+chatString[i];
					}
					writeToAll("chat", ClientName, chatString[1]);
					DataBaseInsert(ClientName, 1, fromClient);// chat内容をDBへ書き込み
					System.out.println("chatをDBへ書き込み成功");//debug
				}
			}
			Logout(Thread.currentThread().getName());
		} catch (IOException e) {
			System.out.println("run:" + e);
			System.exit(0);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		end();
	}

	/*
	 * 
	 * Login(ユーザ名)
	 * 
	 */
	public void Login(String user){
		Thread.currentThread().setName(user);// スレッド番号,ユーザ名 						 ログイン成功
		ChatServer.memberName.add(user);
		out.println("0 login succeed");					//ログイン成功通知
		System.out.println("ログイン成功通知");//debug
		String LastLogin = DataBaseSelect(user,0,"login");// 前回のログインを取得
		System.out.println("ラストログイン時刻取得");//debug
		writeToAll("login", user, LastLogin);
		System.out.println("chatログ取得開始");//debug
		DataBaseSelect(user,1," ");
		//out.println("oldchat "+user+" "+);
		System.out.println("ログイン書き込み開始");//debug
		DataBaseInsert(user, 0, "login");// loginをDBへ書き込み
		System.out.println("ログインID:"+ChatServer.memberName.indexOf(user));//debug
		System.out.println("ログイン完了");//debug
		System.out.println("");//debug
	}

	/*
	 * 
	 * Logout(ユーザ名)
	 * 
	 */
	public void Logout(String ClientName){
		System.out.println("logout でwriteToAllを呼び出す");//debug
		writeToAll("logout", ClientName, ClientName);
		System.out.println("logout でDataBaseInsertを呼び出す");//debug
		DataBaseInsert(ClientName, 0, "logout");// logoutをDBへ書き込み
		ChatServer.memberName.remove(Thread.currentThread().getName());
	}

	/*
	 * 
	 * writeToAll(コマンド名, ユーザ名, 通知内容)
	 * 
	 */
	public void writeToAll(String command, String user, String notice) {
		long nowTime = new Date().getTime();
		int memberId = ChatServer.memberName.indexOf(user);
		DateFormat df = DateFormat.getDateTimeInstance();
		String inTime,noticeString="";
		System.out.println("writeToAll:変数準備OK");//debug
		/*
		 * 配列登録
		 */
		if(command.equals("login")) {
			ChatServer.LoginTime.add(nowTime); //ログイン時間を追加
			ChatServer.ChatNum.add(0);
		}
		if(command.equals("chat"))  {//発言回数を１追加
			System.out.println("chat ID:"+ChatServer.ChatNum.get(memberId));//debug
			ChatServer.ChatNum.set(memberId,ChatServer.ChatNum.get(memberId)+1);
		}
		/*
		 * 実際に通知を行う
		 */
		if (command.equals("login")){
			noticeString=("login user " + user + " " + notice);
		}
		if (command.equals("chat"))
			noticeString=("chat " + user + " " + notice);
		if (command.equals("logout")){
			ChatServer.memberName.remove(user);
			System.out.println("memberId:"+memberId);//debug
			inTime = df.format(ChatServer.LoginTime.get(memberId));
			System.out.println("(writeToAll)inTime:"+inTime);//debug
			System.out.println(user+"のid:"+memberId);//debug
			System.out.println("chat回数:"+ChatServer.ChatNum.get(memberId));//debug
			noticeString=("logout user " + user +" "+inTime+" "+ChatServer.ChatNum.get(memberId));
		}
		Iterator<ChatMThread> it = member.iterator();
		while (it.hasNext()) {
			ChatMThread client = it.next();
			client.out.println(noticeString);
		}

	}

	/*
	 * 
	 * DataBaseInsert(ユーザ名, 種別, 内容)
	 * 
	 */
	public void DataBaseInsert(String user, int classification, String chat) {
		DateFormat df = DateFormat.getDateTimeInstance();
		String nowTime = Long.toString(new Date().getTime());
		try {
			Connection cn = null;
			Class.forName("org.sqlite.JDBC");
			String dbFileName = "chat_tb";
			cn = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
			Statement stmt = null;
			// ////////sqlite3　書き込み/////////////////
			stmt = cn.createStatement();
			System.out.println("書き込み直前");//debug
			int rs = stmt.executeUpdate("insert into chat_tb values ('"+ nowTime + "','" + user + "',"+ classification + ",'" + chat + "');");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("正常にDBへ追加");//debug
	}
	/*
	 * 
	 * DataBaseSELECT(ユーザ名, 種別, 内容)
	 * 
	 */
	public String DataBaseSelect(String user, int classification, String chat) {
		DateFormat df = DateFormat.getDateTimeInstance();
		try {
			Connection cn = null;
			Class.forName("org.sqlite.JDBC");
			String dbFileName = "chat_tb";
			cn = DriverManager.getConnection("jdbc:sqlite:"+dbFileName);
			cn.setAutoCommit(false);
			Statement stmt = null;
			// ////////sqlite3　書き込み/////////////////
			stmt = cn.createStatement();
			if(classification == 0) {
				//				System.out.println("SELECT id FROM chat_tb WHERE user ='"+user+"' AND chat='login' ORDER BY id desc;");//debug
				ResultSet rs = stmt.executeQuery("SELECT id FROM chat_tb WHERE user ='"+user+"' AND chat='login' ORDER BY id desc;" );
				if(rs.next()){
					String inTime =df.format(Long.parseLong(rs.getString("id")));
					rs.close();
					stmt.close();
					cn.close();
					return inTime;
				}
				else{
					System.out.println("初ログインです。");//debug
					rs.close();
					stmt.close();
					cn.close();
					return df.format(new Date().getTime());//現在の時刻を返す。
				}
			}
			if(classification==1){
				String[] oldChats = new String[11];
				//ResultSet rs = stmt.executeQuery("SELECT chat FROM chat_tb WHERE class=1 AND user='"+user+"' LIMIT 10 ORDER BY id desc;" );//そのユーザの過去10件分
				ResultSet rs = stmt.executeQuery("SELECT user,id,chat FROM chat_tb WHERE class=1 ORDER BY id desc LIMIT 10;" );//全ユーザの過去10件分
				int count = 0;//過去何番目かをカウント
				while(rs.next()){
					count++;
					String inTime = df.format(Long.parseLong(rs.getString("id")));
					System.out.println(inTime);
					String users = rs.getString("user");
					String[] chats = rs.getString("chat").split(" ");
					if(chats.length==1){
						oldChats[count]=("oldchat "+ users + " " + count + " " + inTime + " " + " ");
					}
					else{
						for(int i=2;i<chats.length;i++){//チャット内容でスペースがあった場合結合をしなければならない。
							chats[1] += " "+chats[i];
						}
						System.out.println("oldchat "+ users + " " + count + " " + inTime + " " + chats[1]);
						oldChats[count]=("oldchat "+ users + " " + count + " " + inTime + " " + chats[1]);
					}
				}
				for(int i=10;i>0;i--){
					out.println(oldChats[i]);
				}
				rs.close();
				stmt.close();
				cn.close();
				return "";
			}
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("正常にDBへ問い合わせ");//debug
		return "";
	}
	public void end() {
		try {
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("end:" + e);
		}
		member.remove(this);
	}
}
