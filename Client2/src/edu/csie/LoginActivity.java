package edu.csie;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import com.example.client2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private SharedPreferences mPreferences;
	private Context mContext;
	private EditText mNameEditText;
	private EditText mPhoneEditText;
	private CheckBox mCheckBox;
	private Button mLoginButton;
	int correct;
	Bundle bundle = new Bundle();
	private static Handler mHandler = new Handler();
	private Runnable iferror = new Runnable() {

		@Override
		public void run() {
			if (mPhoneEditText.getText().length() != 10) {
				// TODO Auto-generated method stub
				Toast.makeText(mContext, "請輸入正確的手機號碼！", Toast.LENGTH_SHORT)
						.show();
				mPhoneEditText.setText("");
				correct = 0;
			}
			if (correct == 1) {
				Intent intent = new Intent();
				intent.setClass(mContext, ClientActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);
			}
		}
	};

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		mContext = this;
		mPreferences = getPreferences(MODE_PRIVATE);

		mNameEditText = (EditText) findViewById(R.id.nameEditText);
		mNameEditText.setText(mPreferences.getString("NAME", ""));
		mPhoneEditText = (EditText) findViewById(R.id.phoneEditText);
		mPhoneEditText.setText(mPreferences.getString("PHONE", ""));
		mCheckBox = (CheckBox) findViewById(R.id.rememberCheckBox);
		mCheckBox.setChecked(mPreferences.getBoolean("REMEMBER", false));
		mLoginButton = (Button) findViewById(R.id.loginButton);

		mLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				correct = 1;
				if (isOnline()) {
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							Socket serverSocket = null;
							try {
								InetAddress serverIp = InetAddress
										.getByName("140.123.230.32");
								final int serverPort = 5050;

								serverSocket = new Socket(serverIp, serverPort);
								serverSocket.close();
								//FriendlyFindApplication application = (FriendlyFindApplication) ((Activity) mContext)
								//		.getApplication();
								//application.setServerSocket(serverSocket);
								
								// Bundle bundle = new Bundle();
								bundle.putString("name", mNameEditText
										.getText().toString());
								bundle.putString("phone", mPhoneEditText
										.getText().toString());
								
								SharedPreferences.Editor editor = mPreferences.edit();
								if (mCheckBox.isChecked()) {
									editor.putString("NAME", mNameEditText.getText().toString());
									editor.putString("PHONE", mPhoneEditText.getText().toString());
									editor.putBoolean("REMEMBER", true);
								} else {
									editor.clear();
								}
								editor.commit();
								
								mHandler.post(iferror);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();

								if (serverSocket == null) {
									mHandler.post(new Runnable() {
										@Override
										public void run() {
											// TODO Auto-generated method stub
											Toast.makeText(mContext, "網路異常",
													Toast.LENGTH_SHORT).show();
										}
									});
								}
							}
						}
					});
					thread.start();
				} else {
					AlertDialog.Builder alertDialog = new AlertDialog.Builder(
							mContext);
					alertDialog.setTitle("Error");
					alertDialog.setMessage("請檢查是否有連上網路。");
					alertDialog.setPositiveButton("確定", null);
					alertDialog.setCancelable(false);
					alertDialog.show();
				}
			}
		});
	}
}
