package edu.csie;
import android.app.Activity;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.database.Cursor;
import android.widget.TextView;
import android.content.ContentResolver;

public class GetPhone extends Activity
{
    public void onCreate(Bundle savedInstanceState) 
    {
        TextView tv = new TextView(this);
        String string = "";
        super.onCreate(savedInstanceState);
        
        //得到ContentResolver对象
        ContentResolver cr = getContentResolver();
        //取得电话本中开始一项的光标
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        //向下移动光标
        while(cursor.moveToNext())
        {
            //取得联系人名字
            int nameFieldColumnIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
            String contact = cursor.getString(nameFieldColumnIndex);
            //取得电话号码
            String ContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor phone = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + ContactId, null, null);
            
            while(phone.moveToNext())
            {
                String PhoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                string += (contact + ":" + PhoneNumber + "\n");
            }
        }
        cursor.close();

    }
}