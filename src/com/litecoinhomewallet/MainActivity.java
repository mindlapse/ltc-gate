package com.litecoinhomewallet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

// Test
public class MainActivity extends Activity
{
	
	public static final String TAG = "ltc";
	
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
			sb.append((char)c);
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
				initTrusts();
				URL url = new URL("https://" + ipPort[0] + ":" + ipPort[1]);
				conn = (HttpsURLConnection)url.openConnection();
				/*
				Certificate[] scs = conn.getServerCertificates();
				if (scs == null || scs.length == 0) {
					return "No server certificates";
				} else {
					return "scs.length: " + scs.length;
				}
				*/
				//toast("" + conn.getServerCertificates().length);
				result = read(conn.getInputStream());

			} catch (MalformedURLException mu) {
				return getExText(mu);
			} catch (IOException io) {
				return getExText(io);
			} catch (Exception e) {
				return getExText(e);
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
		
		private void initTrusts() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
			
			String alg = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(alg);
			tmf.init((KeyStore)null);
			final X509TrustManager systemTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
					
			TrustManager[] tms = new TrustManager[] {
					new LitecoinTrustManager(systemTrustManager) 
			};
			SSLContext sslCtx = SSLContext.getInstance("TLS");
			sslCtx.init(null, tms, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslCtx.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new LitecoinHostnameVerifier());
		}
	}
	
	private static class LitecoinHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	private static class LitecoinTrustManager implements X509TrustManager {
		
		private X509TrustManager systemTrustManager;
		
		public LitecoinTrustManager(X509TrustManager systemTrustManager) {
			this.systemTrustManager = systemTrustManager;
		}
		@Override
		public void checkClientTrusted(X509Certificate[] chain,
				String authType) throws CertificateException {
			systemTrustManager.checkClientTrusted(chain, authType);
		}
		@Override
		public void checkServerTrusted(X509Certificate[] chain,
				String authType) throws CertificateException {
			
			Log.w(TAG, "received chain with " + chain.length + " certs");
			
			try {
				systemTrustManager.checkServerTrusted(chain, authType);
				Log.w(TAG, "Server certificate passed android system trust check");
			} catch (CertificateException e) {
				if (chain.length == 1) {
					X509Certificate untrusted = chain[0];
					
					Log.w(TAG, "Type: " + untrusted.getType());
					Log.w(TAG, "Subject: " + untrusted.getSubjectDN() == null ? null : untrusted.getSubjectDN().getName());
					Log.w(TAG, "Issuer: " + untrusted.getIssuerDN() == null ? null : untrusted.getIssuerDN().getName());
					Log.w(TAG, "Algo: " + untrusted.getPublicKey().getAlgorithm());
					
					// TODO ask the user if they want to accept it.
					
				} else {
					Log.w(TAG, "More than one in the chain, not a self signed certificate");
					throw e;
				}
			}
		}
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return systemTrustManager.getAcceptedIssuers();
		}
	};

}
