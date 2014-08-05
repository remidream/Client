package edu.csie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.database.Cursor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.client2.R;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class ClientActivity extends Activity {
	private Context mContext = this;
	public static Handler mHandler = new Handler();
	private NameListAdapter mAdapter;
	private TextView TextView01; // 用來顯示文字訊息
	private EditText EditText02; // 文字方塊
	private ListView listView;
	private String msg; // 暫存文字訊息

	private final static int STATE_SEND_USER_INFO = 0;
	private final static int STATE_WAIT = 1;

	private int state = STATE_SEND_USER_INFO;
	private static HashMap<String, String> getNameByPhone = new HashMap<String, String>();

	private static Multimap<String, String> getPhoneByName = ArrayListMultimap
			.<String, String> create();

	private Socket serverSocket; // 客戶端socket
	private BufferedReader br;
	private BufferedWriter bw;

	private String userName;
	private String userPhone;
	private String searchName;
	private String showNameOrPhone;

	private ArrayList<HashMap<String, String>> mTextList = new ArrayList<HashMap<String, String>>();
	private ProgressDialog mProgressDialog;

	private String getPhone() {
		StringBuilder string = new StringBuilder("");

		// 得到ContentResolver对象
		ContentResolver cr = getContentResolver();
		// 取得电话本中开始一项的光标
		Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);
		// 向下移动光标
		while (cursor.moveToNext()) {
			// 取得联系人名字
			int nameFieldColumnIndex = cursor
					.getColumnIndex(PhoneLookup.DISPLAY_NAME);
			String contact = cursor.getString(nameFieldColumnIndex);
			// 取得电话号码
			String ContactId = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts._ID));
			Cursor phone = cr.query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "="
							+ ContactId, null, null);

			while (phone.moveToNext()) {
				String PhoneNumber = phone
						.getString(phone
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				PhoneNumber = PhoneNumber.replaceAll("[\\s-]+", "");
				getNameByPhone.put(PhoneNumber, contact);
				getPhoneByName.put(contact, PhoneNumber);
				string.append(contact + ":" + PhoneNumber + "\n");
			}
			phone.close();
		}

		// 通訊錄結尾字串
		string.append("2147483648\n");
		/*
		 * can't use TelephonyManager telManager = (TelephonyManager)
		 * getSystemService(Context.TELEPHONY_SERVICE); String lineNumber =
		 * telManager.getLine1Number(); string.append(lineNumber + "\n");
		 */
		cursor.close();

		return string.toString();
	}

	//private ServiceConnection test;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TextView01 = (TextView) findViewById(R.id.TextView01);
		EditText02 = (EditText) findViewById(R.id.EditText02);
		listView = (ListView) findViewById(R.id.listView);

		mAdapter = new NameListAdapter();
		listView.setAdapter(mAdapter);

		userName = getIntent().getExtras().getString("name");
		userPhone = getIntent().getExtras().getString("phone");
		
		mProgressDialog = ProgressDialog.show(this, "登入中", "請稍後...");

		Thread t1 = new Thread(readData);
		t1.start();

		Button button1 = (Button) findViewById(R.id.Button01);
		button1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if("".equals(EditText02.getText().toString())){
					new AlertDialog.Builder(mContext)  
                    .setMessage("請正確輸入姓名")  
                    .setPositiveButton("Ok", null)  
                    .show(); 
					
                    return;
				}
				if (state == STATE_WAIT) {					
						new AlertDialog.Builder(ClientActivity.this)
						.setTitle("確認視窗")
						.setMessage(
								"確定要送出查詢嗎？")
						.setCancelable(false)
						.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									return;
								}
							})
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {

									try {
										bw.write(EditText02.getText().toString() + "\n");
										bw.flush();
										searchName = EditText02.getText().toString();
										EditText02.setText("");
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
								}
							}).show();
						
					
					

					// 送出欲查詢的人名後, 將文字方塊清空
					//searchName = EditText02.getText().toString();
					//EditText02.setText("");
				}
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(mContext)
					.setCancelable(false)
					.setTitle("Warning")
					.setMessage("確定要離開嗎？")
					.setPositiveButton("是",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).setNegativeButton("否", null).show();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Runnable testConnection = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while (true) {
					serverSocket.sendUrgentData(0xFF);
					Thread.sleep(5000);
				}
			} catch (Exception ex) {
				mHandler.post(showNetworkError);
			}
		}
	};

	private Runnable showNetworkError = new Runnable() {
		@SuppressLint("NewApi")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (ClientActivity.this.isFinishing()) {
				return;
			}
			if (!ClientActivity.this.isDestroyed()) {
				new AlertDialog.Builder(ClientActivity.this)
						.setTitle("Error")
						.setMessage("與伺服器中斷連線，請檢查是否連上網路。")
						.setCancelable(false)
						.setPositiveButton("確定",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										finish();
									}
								}).show();
			}
		}
	};

	private Runnable error = new Runnable() {
		public void run() {
			new AlertDialog.Builder(ClientActivity.this)
					.setTitle("登入失敗")
					.setMessage("此手機號碼已有人使用，請檢查是否輸入正確。")
					.setCancelable(false)
					.setPositiveButton("確定",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
									
								}
							}).show();
		}
	};

	// 顯示更新訊息
	private Runnable updateText = new Runnable() {
		public void run() {
			String[] s = msg.split("\t");
			final int sourceId = Integer.parseInt(s[0]);
			String name = s[1];
			final String phoneNumber = s[2];
			final String sourcePhone = s[3];
			if (!getPhoneByName.containsKey(name)) {
				return;
			}

			if (getNameByPhone.containsKey(sourcePhone)) {
				showNameOrPhone = getNameByPhone.get(sourcePhone);
			} else {
				showNameOrPhone = sourcePhone;
			}

			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Intent intent = new Intent(mContext, ClientActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
					0, intent, 0);
			Notification notification = new Notification.Builder(mContext)
					.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle("Client2")
					.setAutoCancel(true)
					.setContentText(
							"使用者" + showNameOrPhone + "向您要求: " + name
									+ " 的電話號碼, 請問給予嗎?")
					.setContentIntent(pendingIntent).build();
			notificationManager.notify(R.drawable.ic_launcher, notification);

			new AlertDialog.Builder(ClientActivity.this)
					.setTitle("新訊息")
					.setMessage(
							"使用者" + showNameOrPhone + "向您要求: " + name
									+ " 的電話號碼, 請問給予嗎?")
					.setCancelable(false)
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									try {
										bw.write(sourceId + "\tNO\n");
										bw.flush();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							})
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									try {
										bw.write(sourceId + "\tYES\t"
												+ phoneNumber + "\t"
												+ sourcePhone + "\t"
												+ userPhone + "\n");
										bw.flush();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}).show();
		}
	};

	private Runnable register = new Runnable() {
		private final static String MY_MESSAGE = "someone.sendmessage";

		public void run() {
			registerReceiver(mBroadcast, new IntentFilter(MY_MESSAGE));
			Intent intent = new Intent();
			intent.setAction(MY_MESSAGE);
			sendBroadcast(intent);
		}
	};

	private Runnable displayPhoneNumber = new Runnable() {
		@Override
		public void run() {
			// 電話號碼
			Log.d("msg", msg);
			String[] s = msg.split(":");
			// s[0]: phone number
			// s[2]: source phone number

			HashMap<String, String> item = new HashMap<String, String>();
			String sourceName;
			if (getNameByPhone.containsKey(s[3])) {
				sourceName = getNameByPhone.get(s[3]);
			} else {
				sourceName = s[3];
			}
			item.put("source name", sourceName);
			item.put("goal name", searchName);
			item.put("phone number", s[0]);

			mTextList.add(item);
		}
	};

	private class NameListAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mTextList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			MessageListElement messageListElement = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.adapter, null);
				messageListElement = new MessageListElement(convertView);
				convertView.setTag(messageListElement);
			} else {
				messageListElement = (MessageListElement) convertView.getTag();
			}

			String sourceName = mTextList.get(position).get("source name");
			String goalName = mTextList.get(position).get("goal name");
			String phoneNumber = mTextList.get(position).get("phone number");
			messageListElement.setTextView(sourceName, goalName, phoneNumber);

			return convertView;
		}
	}

	// 取得網路資料
	private Runnable readData = new Runnable() {
		@Override
		public void run() {
			InetAddress serverIp;

			try {
				serverIp = InetAddress.getByName("140.123.230.32");
				int serverPort = 5050;

				//FriendlyFindApplication application = (FriendlyFindApplication) ((Activity) mContext)
				//		.getApplication();
				serverSocket = // application.getServerSocket();
				new Socket(serverIp, serverPort);

				Thread t2 = new Thread(testConnection);
				t2.start();

				br = new BufferedReader(new InputStreamReader(
						serverSocket.getInputStream()));
				bw = new BufferedWriter(new OutputStreamWriter(
						serverSocket.getOutputStream()));
				if (state == STATE_SEND_USER_INFO) {
				//	bw = new BufferedWriter(new OutputStreamWriter(
				//		serverSocket.getOutputStream()));
					bw.write(userName + "\t" + userPhone + "\n");
					bw.write(getPhone()); // warning: 最多寫入 8192 字元
					bw.flush();

					state = STATE_WAIT;
				}

				mProgressDialog.dismiss();
				
				while ((msg = br.readLine()) != null) {
					if (msg.matches("[\\d\\s]+(:123456789:)[\\d\\s]+:[\\d\\s]+")) {
						mHandler.post(displayPhoneNumber);
					} else if (msg.matches("Error: [\\w\\s]+")) {
						mHandler.post(error);
					} else {
						mHandler.post(register);
						// mHandler.post(updateText);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private BroadcastReceiver mBroadcast = new BroadcastReceiver() {
		private final static String MY_MESSAGE = "someone.sendmessage";

		@Override
		public void onReceive(Context context, Intent mintent) {
			if (MY_MESSAGE.equals(mintent.getAction())) {
				mHandler.post(updateText);
			}
			unregisterReceiver(mBroadcast);
		}
	};
}
