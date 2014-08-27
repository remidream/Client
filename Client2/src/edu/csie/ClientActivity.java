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
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.security.Key;   
import java.security.KeyFactory;   
import java.security.KeyPair;   
import java.security.KeyPairGenerator;   
import java.security.PrivateKey;   
import java.security.PublicKey;   
import java.security.Signature;   
import java.security.interfaces.RSAPrivateKey;   
import java.security.interfaces.RSAPublicKey;   
import java.security.spec.PKCS8EncodedKeySpec;   
import java.security.spec.X509EncodedKeySpec;   

import javax.crypto.Cipher;  

import java.util.HashMap;   
import java.util.Map;   

import com.example.client2.R;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class ClientActivity extends Activity {
	private Context mContext = this;
	public static Handler mHandler = new Handler();
	private NameListAdapter mAdapter;
	private TextView TextView01; // 用來顯示文字訊息
	private EditText EditText02; // 文字方塊
	private Switch transferSwitch;
	private ListView listView;
	private String msg; // 暫存文字訊息
	private String publicKey;   
    private String privateKey;   
    private String xxx2;
	private final static int STATE_SEND_USER_INFO = 0;
	private final static int STATE_WAIT = 1;
	private int SEND_YES_OR_NO = 0;
	private int transfer = 0;
	private int state = STATE_SEND_USER_INFO;
	private int index ;
	private String sign;
	private boolean initializeAddressbook = true;
	
	private static HashMap<String, String> getNameByPhone = new HashMap<String, String>();
	private static Multimap<String, String> getPhoneByName = ArrayListMultimap
			.<String, String> create();
	private static StringMultimap getPhoneByNameTest = new StringMultimap();

	private ArrayList<String> contactList = new ArrayList<String>();
	private ArrayList<String> phoneList = new ArrayList<String>();

	private Socket serverSocket; // 客戶端socket
	private BufferedReader br;
	private BufferedWriter bw;

	private String userName;
	private String userPhone;
	private String CName;
	private String BeginPhone;
	private String BeginPublicKey;
    private byte[] encodedData2;
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

				if (initializeAddressbook) {
					getNameByPhone.put(PhoneNumber, contact);
					getPhoneByName.put(contact, PhoneNumber);
					getPhoneByNameTest.put(contact, PhoneNumber);
					contactList.add(contact);
					phoneList.add(PhoneNumber);
				}
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

	// private ServiceConnection test;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TextView01 = (TextView) findViewById(R.id.TextView01);
		EditText02 = (EditText) findViewById(R.id.EditText02);
		transferSwitch = (Switch) findViewById(R.id.transferSwitch);
		transferSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {  
          @Override  
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {  
              if(isChecked) {  
            	  SEND_YES_OR_NO = 1;
              } else {
            	  SEND_YES_OR_NO = 0;
              }
          }  
        });
		listView = (ListView) findViewById(R.id.listView);

		mAdapter = new NameListAdapter();
		listView.setAdapter(mAdapter);

		userName = getIntent().getExtras().getString("name");
		userPhone = getIntent().getExtras().getString("phone");
		getPhone();
		initializeAddressbook = false;

		mProgressDialog = ProgressDialog.show(this, "登入中", "請稍後...");

		Thread t1 = new Thread(readData);
		t1.start();

		Button button1 = (Button) findViewById(R.id.Button01);
		button1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if ("".equals(EditText02.getText().toString())) {
					new AlertDialog.Builder(mContext).setMessage("請正確輸入姓名")
							.setPositiveButton("Ok", null).show();

					return;
				}
				if (state == STATE_WAIT) {
					new AlertDialog.Builder(ClientActivity.this)
							.setTitle("確認視窗")
							.setMessage("確定要送出查詢嗎？")
							.setCancelable(false)
							.setNegativeButton("No",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											return;
										}
									})
							.setPositiveButton("Yes",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {

											try {
												//Log.d("qqqqqq","");
												bw.write(getPhone());
												bw.write(EditText02.getText()
														.toString() + "\n");
												bw.flush();
												CName = EditText02.getText()
														.toString();
												EditText02.setText("");
												//Log.d("rrrrrrr","");
											} catch (IOException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
											}

										}
									}).show();

					// 送出欲查詢的人名後, 將文字方塊清空
					// searchName = EditText02.getText().toString();
					// EditText02.setText("");
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
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
			Log.d("publickey \t Aphone \t Cname", msg);
			
			String[] s = msg.split("\t");
			final String publickey = s[0];
			final String APhone = s[1];
			final String Cname = s[2];
			if (s.length == 5) {
				transfer = 1;
				BeginPhone = s[3];
				BeginPublicKey = s[4];
			}

			String AshowNameOrPhone;
			if (getNameByPhone.containsKey(APhone)) {
				AshowNameOrPhone = getNameByPhone.get(APhone);
			} else {
				AshowNameOrPhone = APhone;
			}
			Log.d("AshowNameOrPhone", AshowNameOrPhone);

			if (getPhoneByName.containsKey(Cname)) {
				/*
				final Collection<String> collection = getPhoneByName.get(Cname);
				final Object[] CphoneNumber = collection.toArray();
				*/
				final ArrayList<String> CPhoneNumber = getPhoneByNameTest.get(Cname);
				
				NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				Intent intent = new Intent(mContext, ClientActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						mContext, 0, intent, 0);
				Notification notification = new Notification.Builder(mContext)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle("Client2")
						.setAutoCancel(true)
						.setContentText(
								"使用者" + AshowNameOrPhone + "向您要求: " + Cname
										+ " 的電話號碼, 請問給予嗎?")
						.setContentIntent(pendingIntent).build();
				notificationManager
						.notify(R.drawable.ic_launcher, notification);

				new AlertDialog.Builder(ClientActivity.this)
						.setTitle("新訊息")
						.setMessage(
								"使用者" + AshowNameOrPhone + "向您要求: " + Cname
										+ " 的電話號碼, 請問給予嗎?")
						.setCancelable(false)
						.setNegativeButton("否",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										try {
											bw.write("NO\n");
											bw.flush();
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								})
						.setPositiveButton("是",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										try {
								
											//String encodedString = new String(encodedData);
										//	Log.d("encode", encodedString);
											Log.d("CPhoneNumber.size()", "" + CPhoneNumber.size());
											if (CPhoneNumber.size() == 1) {
												if (transfer == 0) {
													byte[] data = CPhoneNumber.get(0).getBytes();
													byte[] encodedData = RSACoder.encryptByPublicKey(data, publickey);
													String xxx = new String(Coder.encryptBASE64(encodedData));
													bw.write("YES" 
															+ "\t" + APhone + "\t"
															+ userPhone + "\t"
															+ xxx + "\t" 
															+ sign  + "\t" 
															+ publicKey + "\t"
															+ xxx2 + "\t"
															+ "\n");
													bw.flush();
												} else if (transfer == 1) {
													byte[] data = CPhoneNumber.get(0).getBytes();
													byte[] encodedData = RSACoder.encryptByPublicKey(data, BeginPublicKey);
													String xxx = new String(Coder.encryptBASE64(encodedData));
													bw.write("YES"
															+ "\t" + BeginPhone
															+ "\t" + userPhone 
															+ "\t" + xxx 
															+ "\t" + sign
															+ "\t" + publicKey
															+ "\t" + xxx2
															+ "\n");
													bw.flush();
													transfer = 0;
												}
												return;
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
										
										final CharSequence[] phoneNumbers = CPhoneNumber.toArray(new String[CPhoneNumber.size()]);
										Log.d("phoneNumbers", "" + phoneNumbers.length);
										for (int i = 0; i < phoneNumbers.length; ++i) {
											Log.d("phoneNumber:", "" + phoneNumbers[i]);
										}
										//final String[] test = {"1", "2"};
                                                   
										new AlertDialog.Builder(ClientActivity.this)
						                    .setTitle("您的通訊錄裡有多個符合此人名,請選擇一組您將要傳送的電話號碼")
						           
						                    .setSingleChoiceItems(phoneNumbers, 0, new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int which) {
													// TODO Auto-generated method stub
									                   index = which;
												}
						                    })
						                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
						                        @Override
						                        public void onClick(DialogInterface dialog, int which) {
						                            try {
						                       
						                            	if (transfer == 0) {
						                            		byte[] data = CPhoneNumber.get(index).getBytes();
															byte[] encodedData = RSACoder.encryptByPublicKey(data,publickey);
															String xxx = new String(Coder.encryptBASE64(encodedData));
															bw.write("YES"
																	+ "\t" + APhone 
																	+ "\t" + userPhone
																	+ "\t" + xxx 
																	+ "\t" + sign
																	+ "\t" + publicKey
																	+ "\t" + xxx2
																	+ "\n");
															bw.flush();
														} else if (transfer == 1) {
															byte[] data = CPhoneNumber.get(index).getBytes();
															byte[] encodedData = RSACoder.encryptByPublicKey(data,BeginPublicKey);
															String xxx = new String(Coder.encryptBASE64(encodedData));
															bw.write("YES"
																	+ "\t" + BeginPhone
																	+ "\t" + userPhone
																	+ "\t" + xxx 
																	+ "\t" + sign
																	+ "\t" + publicKey
																	+ "\t" + xxx2
																	+ "\n");
															bw.flush();
															transfer = 0;
														}
													} catch (Exception e) {
														e.printStackTrace();
													}
						                        }
						                    })
						                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
						                        @Override
						                        public void onClick(DialogInterface dialog, int which) {
						                        	dialog.dismiss();
						                        }
						                    })
						                    .show();
											
											
									}
									}).show();
						
				
			} else if (SEND_YES_OR_NO == 1) {
				NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				Intent intent = new Intent(mContext, ClientActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						mContext, 0, intent, 0);
				Notification notification = new Notification.Builder(mContext)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle("Client2")
						.setAutoCancel(true)
						.setContentText(
								"使用者" + AshowNameOrPhone + "向您要求: " + Cname
										+ " 的電話號碼但您沒有, 請問要將" + AshowNameOrPhone
										+ "的要求再次傳送嗎？")
						.setContentIntent(pendingIntent).build();
				notificationManager
						.notify(R.drawable.ic_launcher, notification);

				new AlertDialog.Builder(ClientActivity.this)
						.setTitle("新訊息")
						.setMessage(
								"使用者" + AshowNameOrPhone + "向您要求: " + Cname
										+ " 的電話號碼但您沒有, 請問要將" + AshowNameOrPhone
										+ "的要求再次傳送嗎？")
						.setCancelable(false)
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										try {
											bw.write("NO\n");
											bw.flush();
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								})
						.setNeutralButton ("輸入",
								new DialogInterface.OnClickListener() {
								@Override
									public void onClick(DialogInterface dialog,
												int which) {
										LayoutInflater inflater = LayoutInflater.from(ClientActivity.this);
					                    final View v = inflater.inflate(R.layout.edit_text, null);
					                    final String[] contacts = contactList.toArray(new String[contactList.size()]);
					                    final String[] phones = phoneList.toArray(new String[phoneList.size()]);
	
					                    new AlertDialog.Builder(ClientActivity.this)
					                    .setTitle("請選擇或自行手動輸入" + Cname + " 的電話號碼")
					                    .setView(v)
					                    .setSingleChoiceItems(contacts, -1, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												// TODO Auto-generated method stub
												EditText editText = (EditText) (v.findViewById(R.id.edittext));
												editText.setText(phones[which]);
											}
					                    })
					                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
					                        @Override
					                        public void onClick(DialogInterface dialog, int which) {
					                            
					                            EditText editText = (EditText) (v.findViewById(R.id.edittext));
					                            try {
					                       
													if(transfer==0){
														byte[] data = editText.getText().toString().getBytes();
														byte[] encodedData = RSACoder.encryptByPublicKey(data, publickey);
														String xxx = new String(Coder.encryptBASE64(encodedData));
														bw.write("YES" 
																+ "\t" + APhone 
																+ "\t" + userPhone
																+ "\t" + xxx 
																+ "\t" + sign
																+ "\t" + publicKey
																+ "\t" + xxx2
																+ "\n");
														
														bw.flush();
													}
													else if(transfer==1){
														byte[] data = editText.getText().toString().getBytes();
														byte[] encodedData = RSACoder.encryptByPublicKey(data, BeginPublicKey);
														String xxx = new String(Coder.encryptBASE64(encodedData));
														bw.write("YES" 
																+ "\t" + BeginPhone 
																+ "\t" + userPhone
																+ "\t" + xxx 
																+ "\t" + sign
																+ "\t" + publicKey
																+ "\t" + xxx2
																+ "\n");
														bw.flush();
														transfer=0;
													}
												} catch (Exception e) {
													e.printStackTrace();
												}
					                        }
					                    })
					                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
					                        @Override
					                        public void onClick(DialogInterface dialog, int which) {
					                        	dialog.dismiss();
					                        }
					                    })
					                    .show();
									}
							   })
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										try {
											if (transfer == 0) {
												bw.write(getPhone());
												bw.write( Cname + "\t" + APhone + "\t" +  publickey
														+ "\n");
												bw.flush();
											} else if (transfer == 1) {
												bw.write(getPhone());
												bw.write( Cname + "\t" + BeginPhone + "\t" 
														+ BeginPublicKey  
														+ "\n");
												bw.flush();
												
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}).show();

			} else if (SEND_YES_OR_NO == 0) {
				return;
			}
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
			String[] s = msg.split("\t");
			String xxx3 = s[4];
			String publickey2 = s[3];
			String sign2 = s[2];
			String CPhone = s[1];
//			String CPhone = msg.substring(msg.indexOf('\t') + 1);
			String BPhone = s[0];
			
		//	Log.d("CPhone", CPhone);
		//	Log.d("test2", new String(CPhone.getBytes()));
			try {
				byte[] yyy2 = (Coder.decryptBASE64(xxx3));
				boolean status = RSACoder.verify(yyy2, publickey2 , sign2);
				if(!status){
					return;
				}
				Log.d("status","" + status);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				byte[] yyy = (Coder.decryptBASE64(CPhone));
			    byte[] decodedData = RSACoder.decryptByPrivateKey(yyy,privateKey);
				CPhone = new String(decodedData,"UTF-8");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			HashMap<String, String> item = new HashMap<String, String>();
			String BName;
			if (getNameByPhone.containsKey(BPhone)) {
				BName = getNameByPhone.get(BPhone);
			} else {
				BName = BPhone;
			}

			item.put("source name", BName);
			item.put("goal name", CName);
			item.put("phone number", CPhone);

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
				int serverPort = 3708;

				// FriendlyFindApplication application =
				// (FriendlyFindApplication) ((Activity) mContext)
				// .getApplication();
				serverSocket = // application.getServerSocket();
				new Socket(serverIp, serverPort);

				Thread t2 = new Thread(testConnection);
				t2.start();
				
				Map<String, Object> keyMap = RSACoder.initKey(); 
				publicKey = RSACoder.getPublicKey(keyMap);   
			    privateKey = RSACoder.getPrivateKey(keyMap); 
		
		

			//    String inputStr = "09 63-0278-57"; 
		       
			   // byte[] data = inputStr.getBytes("UTF8");
		  			    
		    //    byte[] encodedData = RSACoder.encryptByPublicKey(data, publicKey);
		        
		        String inputStr = "sign";     
		        byte[] data = inputStr.getBytes("UTF8");     
		  
		        encodedData2 = RSACoder.encryptByPrivateKey(data, privateKey); 
		        xxx2 = new String(Coder.encryptBASE64(encodedData2));
		        sign = RSACoder.sign(encodedData2, privateKey);
		        
	//	        String xxx = new String(Coder.encryptBASE64(encodedData));
	//	        byte[] yyy = (Coder.decryptBASE64(xxx));
	//	        String encodedStr = new String(encodedData,"UTF8");
		      		        
	//	        byte[] encodedByte = encodedStr.getBytes("UTF8");
		      
		        
		  //      byte[] decodedData = RSACoder.decryptByPrivateKey(yyy,privateKey ); 
		  //     String outputStr = new String(decodedData);
		  //      Log.d("outputStr",outputStr);
		        
			    Log.d("publicKey",publicKey);
			   
			    
				br = new BufferedReader(new InputStreamReader(
						serverSocket.getInputStream()));
				bw = new BufferedWriter(new OutputStreamWriter(
						serverSocket.getOutputStream()));
				if (state == STATE_SEND_USER_INFO) {
					// bw = new BufferedWriter(new OutputStreamWriter(
					// serverSocket.getOutputStream()));
					bw.write(userName + "\t" + userPhone + "\t" + publicKey + "\n");
					bw.flush();

					state = STATE_WAIT;
				}

				mProgressDialog.dismiss();

				while ((msg = br.readLine()) != null) {
				//	Log.d("br", br.toString());
					Log.d("msg", msg);
					if (msg.matches("[\\d\\s]+\t[^\n^\t]+\t[^\n^\t]+\t[^\n^\t]+\t[^\n]+")) {
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


abstract class RSACoder extends Coder {   
    public static final String KEY_ALGORITHM = "RSA";   
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";   

    private static final String PUBLIC_KEY = "RSAPublicKey";   
    private static final String PRIVATE_KEY = "RSAPrivateKey";   

    /** *//** 
     * 用私钥对信息生成数字签名 
     *   
     * @param data 
     *            加密数据 
     * @param privateKey 
     *            私钥 
     *   
     * @return 
     * @throws Exception 
     */ 
    public static String sign(byte[] data, String privateKey) throws Exception {   
        // 解密由base64编码的私钥   
        byte[] keyBytes = decryptBASE64(privateKey);   

        // 构造PKCS8EncodedKeySpec对象   
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);   

        // KEY_ALGORITHM 指定的加密算法   
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   

        // 取私钥匙对象   
        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);   

        // 用私钥对信息生成数字签名   
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);   
        signature.initSign(priKey);   
        signature.update(data);   

        return encryptBASE64(signature.sign());   
    }   

    /** *//** 
     * 校验数字签名 
     *   
     * @param data 
     *            加密数据 
     * @param publicKey 
     *            公钥 
     * @param sign 
     *            数字签名 
     *   
     * @return 校验成功返回true 失败返回false 
     * @throws Exception 
     *   
     */ 
    public static boolean verify(byte[] data, String publicKey, String sign)   
            throws Exception {   

        // 解密由base64编码的公钥   
        byte[] keyBytes = decryptBASE64(publicKey);   

        // 构造X509EncodedKeySpec对象   
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);   

        // KEY_ALGORITHM 指定的加密算法   
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   

        // 取公钥匙对象   
        PublicKey pubKey = keyFactory.generatePublic(keySpec);   

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);   
        signature.initVerify(pubKey);   
        signature.update(data);   

        // 验证签名是否正常   
        return signature.verify(decryptBASE64(sign));   
    }   

    /** *//** 
     * 解密<br> 
* 用私钥解密 http://www.5a520.cn http://www.feng123.com
     *   
     * @param data 
     * @param key 
     * @return 
     * @throws Exception 
     */ 
    public static byte[] decryptByPrivateKey(byte[] data, String key)   
            throws Exception {   
        // 对密钥解密   
        byte[] keyBytes = decryptBASE64(key);   

        // 取得私钥   
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);   
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   
        Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);   

        // 对数据解密   
        
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");   
        cipher.init(Cipher.DECRYPT_MODE, privateKey);   

        return cipher.doFinal(data);   
    }   

    /** *//** 
     * 解密<br> 
     * 用私钥解密 
     *   
     * @param data 
     * @param key 
     * @return 
     * @throws Exception 
     */ 
    public static byte[] decryptByPublicKey(byte[] data, String key)   
            throws Exception {   
        // 对密钥解密   
        byte[] keyBytes = decryptBASE64(key);   

        // 取得公钥   
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);   
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   
        Key publicKey = keyFactory.generatePublic(x509KeySpec);   

        // 对数据解密   
        
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");   
        cipher.init(Cipher.DECRYPT_MODE, publicKey);   

        return cipher.doFinal(data);   
    }   

    /** *//** 
     * 加密<br> 
     * 用公钥加密 
     *   
     * @param data 
     * @param key 
     * @return 
     * @throws Exception 
     */ 
    public static byte[] encryptByPublicKey(byte[] data, String key)   
            throws Exception {   
        // 对公钥解密   
        byte[] keyBytes = decryptBASE64(key);   

        // 取得公钥   
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);   
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   
        Key publicKey = keyFactory.generatePublic(x509KeySpec);   

        // 对数据加密   
        
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");   
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);   

        return cipher.doFinal(data);   
    }   

    /** *//** 
     * 加密<br> 
     * 用私钥加密 
     *   
     * @param data 
     * @param key 
     * @return 
     * @throws Exception 
     */ 
    public static byte[] encryptByPrivateKey(byte[] data, String key)   
            throws Exception {   
        // 对密钥解密   
        byte[] keyBytes = decryptBASE64(key);   

        // 取得私钥   
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);   
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   
        Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);   

        // 对数据加密   
      
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");   
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);   

        return cipher.doFinal(data);   
    }   

    /** *//** 
     * 取得私钥 
     *   
     * @param keyMap 
     * @return 
     * @throws Exception 
     */ 
    public static String getPrivateKey(Map<String, Object> keyMap)   
            throws Exception {   
        Key key = (Key) keyMap.get(PRIVATE_KEY);   

        return encryptBASE64(key.getEncoded());   
    }   

    /** *//** 
     * 取得公钥 
     *   
     * @param keyMap 
     * @return 
     * @throws Exception 
     */ 
    public static String getPublicKey(Map<String, Object> keyMap)   
            throws Exception {   
        Key key = (Key) keyMap.get(PUBLIC_KEY);   

        return encryptBASE64(key.getEncoded());   
    }   

    /** *//** 
     * 初始化密钥 
     *   
     * @return 
     * @throws Exception 
     */ 
    public static Map<String, Object> initKey() throws Exception {   
        KeyPairGenerator keyPairGen = KeyPairGenerator   
                .getInstance(KEY_ALGORITHM);   
        keyPairGen.initialize(1024);   

        KeyPair keyPair = keyPairGen.generateKeyPair();   

        // 公钥   
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();   

        // 私钥   
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();   

        Map<String, Object> keyMap = new HashMap<String, Object>(2);   

        keyMap.put(PUBLIC_KEY, publicKey);   
        keyMap.put(PRIVATE_KEY, privateKey);   
        return keyMap;   
    }   
} 
class Coder {

    /**
     * Base64解码
     * @param key
     * @return
     */
    public static byte[] decryptBASE64(String key){
        return Base64.decode(key,android.util.Base64.NO_WRAP);
    }
    
    /**
     * Base64编码
     * @param sign
     * @return
     */
    public static String encryptBASE64(byte[] sign){
        return Base64.encodeToString(sign, android.util.Base64.NO_WRAP);
    }
}