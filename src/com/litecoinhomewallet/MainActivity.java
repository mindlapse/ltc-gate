package com.litecoinhomewallet;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import java.net.URL;
import javax.net.ssl.*;
import android.text.*;
import java.net.*;
import java.io.*;
import java.security.*;

// Test
public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
		//TextView homeIp = (TextView)findViewById(R.id.homeIp);
		setContentView(R.layout.main);
		
		
    }
	
	public void testServer(final View button) {

		final String ip = parseHomeIp((EditText)findViewById(R.id.homeIP));
		if (ip == null) return;
		final Integer port = parseHomePort((EditText)findViewById(R.id.homePort));
		if (port == null) return;
		
		new CheckServerTask().execute(new String[] {ip, port.toString()});
	}
	
	private String read(InputStream is) throws IOException {
		InputStream bis = new BufferedInputStream(is);
		StringBuilder sb = new StringBuilder();
		int c = 0;
		while ((c = bis.read()) >= 0) {
			sb.append(c);
		}
		bis.close();
		return sb.toString();
	}
	
	private String parseHomeIp(EditText ip) {
		if (TextUtils.isEmpty(ip.getText())) {
			toast(R.string.errIpReq);
			return null;
		} else {
			return ip.getText().toString();
		}
	}
	
	private Integer parseHomePort(EditText port) {
		if (TextUtils.isEmpty(port.getText())) {
			toast(R.string.errPortReq);
			return null;
		} else {
			return Integer.parseInt(port.getText().toString());
		}
	}
	
	private void toast(int resId) {
		Context c = getApplicationContext();
		Toast t = Toast.makeText(c, resId, Toast.LENGTH_LONG);
		t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		t.show();
	}
	
	private String getExText(Exception e) {
		return e.getClass().getName() + ": " + e.getMessage();
	}
	
	private void toast(String msg) {
		Context c = getApplicationContext();
		Toast t = Toast.makeText(c, msg, Toast.LENGTH_LONG);
		t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		t.show();
	}
	
	private class CheckServerTask extends AsyncTask<String, Void, String>
	{

		public CheckServerTask() {
			
		}
		
		@Override
		protected String doInBackground(String[] ipPort)
		{
			String result = null;
			HttpsURLConnection conn = null;
			try {
				URL url = new URL("https://" + ipPort[0] + ":" + ipPort[1]);
				conn = (HttpsURLConnection)url.openConnection();
				result = read(conn.getInputStream());

			} catch (MalformedURLException mu) {
				return getExText(mu);
			} catch (IOException io) {
				return getExText(io);
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result)
		{
			if (result != null) {
				toast(result);
			}
		}
		
		private String initTrusts() throws NoSuchAlgorithmException {
			String result = null;
			String alg = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory.getInstance(alg);
			return result;
		}
		
	}
}
