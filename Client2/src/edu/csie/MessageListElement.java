package edu.csie;

import com.example.client2.R;

import android.view.View;
import android.widget.TextView;

public class MessageListElement {
	private TextView sourceNameTextView;
	private TextView goalNameTextView;
	private TextView hintMessageTextView1;
	private TextView hintMessageTextView2;
	private TextView phoneNumberTextView;

	public MessageListElement(View view) {
		// TODO Auto-generated constructor stub
		sourceNameTextView = (TextView) view.findViewById(R.id.sourceNameTextView);
		goalNameTextView = (TextView) view.findViewById(R.id.goalNameTextView);
		hintMessageTextView1 = (TextView) view.findViewById(R.id.hintTextView1);
		hintMessageTextView2 = (TextView) view.findViewById(R.id.hintTextView2);
		phoneNumberTextView = (TextView) view.findViewById(R.id.phoneNumberTextView);
	}

	public void setTextView(String sourceName, String goalName, String phoneNumber) {
		sourceNameTextView.setText(sourceName);
		goalNameTextView.setText(goalName);
		phoneNumberTextView.setText(phoneNumber);
	}
}
