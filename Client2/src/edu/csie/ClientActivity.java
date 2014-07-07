package edu.csie;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.database.Cursor;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ContentResolver;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

import com.example.client2.R;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

// 
public class ClientActivity extends Activity {
	private Context mContext = this;
	public static Handler mHandler = new Handler();
	private TextView TextView01; // 用來顯示文字訊息
	private EditText EditText02; // 文字方塊
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TextView01 = (TextView) findViewById(R.id.TextView01);
		EditText02 = (EditText) findViewById(R.id.EditText02);

		userName = this.getIntent().getExtras().getString("name");
		userPhone = this.getIntent().getExtras().getString("phone");

		TextView01.append(userName + "\n");
		TextView01.append(userPhone + "\n");

		Thread t = new Thread(readData);
		t.start();

		Button button1 = (Button) findViewById(R.id.Button01);
		button1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (serverSocket.isConnected()) {
					if (state == STATE_WAIT) {
						try {
							BufferedWriter bw;
							bw = new BufferedWriter(new OutputStreamWriter(
									serverSocket.getOutputStream()));
							bw.write(EditText02.getText() + "\n");
							bw.flush();

							TextView01.append(EditText02.getText() + "\n");
						} catch (Exception e) {
							e.printStackTrace();
						}

						// 送出欲查詢的人名後, 將文字方塊清空
						EditText02.setText("");
					}
				} else {
					TextView01.append("not connected\n");
				}
			}
		});
	}

	// 顯示更新訊息
	private Runnable updateText = new Runnable() {
		public void run() {
			String[] s = msg.split("\t");
			final int sourceId = Integer.parseInt(s[0]);
			String name = s[1];
			String showNameOrPhone;
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
			NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			Intent intent = new Intent(mContext, ClientActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		     PendingIntent pendingIntent 
		      = PendingIntent.getActivity(mContext, 0, intent, 0);		      
			Notification notification = new Notification.Builder(mContext).setSmallIcon(R.drawable.ic_launcher)
				     .setContentTitle("Client2")
				     .setContentText("使用者" + showNameOrPhone + "向您要求: " + name
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
												+ sourcePhone + "\n");
										bw.flush();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}).show();
		}
	};

	private Runnable displayPhoneNumber = new Runnable() {
		@Override
		public void run() {
			// 電話號碼
			TextView01.append(msg + "\n");
		}
	};

	// 取得網路資料
	private Runnable readData = new Runnable() {
		@Override
		public void run() {
			InetAddress serverIp;

			try {
				serverIp = InetAddress.getByName("140.123.230.32");
				int serverPort = 5050;
				serverSocket = new Socket(serverIp, serverPort);
				br = new BufferedReader(new InputStreamReader(
						serverSocket.getInputStream()));
				bw = new BufferedWriter(new OutputStreamWriter(
						serverSocket.getOutputStream()));

				if (state == STATE_SEND_USER_INFO) {
					bw = new BufferedWriter(new OutputStreamWriter(
							serverSocket.getOutputStream()));
					bw.write(userName + "\t" + userPhone + "\n");
					bw.write(getPhone()); // warning: 最多寫入 8192 字元
					bw.flush();

					state = STATE_WAIT;
				}

				while ((msg = br.readLine()) != null) {
					if (msg.matches("[\\d\\s]+")) {
						mHandler.post(displayPhoneNumber);
					} else {
						mHandler.post(updateText);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
}