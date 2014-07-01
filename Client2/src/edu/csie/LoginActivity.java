package edu.csie;

import com.example.client2.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity {
	private Context context;
	private EditText mNameEditText;
	private EditText mPhoneEditText;
	private Button mLoginButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		context = this;
		
		mNameEditText = (EditText)findViewById(R.id.nameEditText);
		mPhoneEditText = (EditText)findViewById(R.id.phoneEditText);
		mLoginButton = (Button)findViewById(R.id.loginButton);
		mLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Bundle bundle = new Bundle();
				bundle.putString("name", mNameEditText.getText().toString());
				bundle.putString("phone", mPhoneEditText.getText().toString());
				
				Intent intent = new Intent();
				intent.setClass(context, ClientActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);
				
			}			
		});
	}
}
