//手机APP，包含WiFi通信，app界面等

package com.dz365.mcuwifi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import java.util.List;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

class public_info {
	static byte buf[] = null;
	static byte buf_length;
	static boolean need_send_cmd;
	static boolean recv_thread_exit;
	static boolean info_thread_exit;
}


class clientInfoThread extends Thread {
	Socket socket = null;
	Handler handler;
	String host = "10.10.100.254";  // wifi 模块默认：tcp server 10.10.100.254    端口8899
	int port = 8899;	
	boolean need_send_cmd = false;
	
	DataInputStream dis = null;
	DataOutputStream dos = null;
	
	public clientInfoThread(Handler handler) {				
		this.handler = handler;				
	}
	
	public void run() {
		try {		  
		  byte readBuffer[] = new byte[64];		  
		  
  	      try {
		    //socket = new Socket(host,port);
  	    	socket = new Socket();
  	    	
  	    	SocketAddress socketAddress = new InetSocketAddress(host,port);
  	    	socket.connect(socketAddress,2000);
  	    	
		    socket.setSoTimeout(300);
		    socket.setTcpNoDelay(true);		    
		  
		    dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		    dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		    
		    
		    
		    while (!Thread.interrupted()) {
		      //Thread.sleep(1000);	
		      if (public_info.need_send_cmd) {
		    	  public_info.need_send_cmd = false;
		    	  dos.write(public_info.buf, 0, public_info.buf_length);
			      dos.flush();
		      }
		      int count = 0;
			  try {
					count = dis.read(readBuffer);
			   } catch (IOException e) {
					continue;
			   }
			   if (count < 1)
					continue;
				Message msg = new Message();
				msg.what = 0x1234;
				msg.obj = readBuffer;			
				handler.sendMessage(msg);
		    }
		    
		    
		    }finally {
		      if (dos != null)	  
			    dos.close();
			  if (dis != null)
			    dis.close();
			  if (socket != null)
			    socket.close();
			  public_info.info_thread_exit = true;
		    }					  		 			  		  
		} catch (Exception e) {
			e.printStackTrace();
			
			Message msg = new Message();
			msg.what = 0x1235;
			msg.obj = e.getMessage();
			handler.sendMessage(msg);
		}
	}

}

public class MainActivity extends Activity {
	Button btnSearchWIFI;
	Button connectBtn;
	Button ledonBtn,ledoffBtn,ledon;
	Button jdqonBtn,jdqoffBtn,jdqoff;
	Button btnSend;
	
	EditText edtInfo;
	
	TextView tvInfo;
	WifiManager wifi;
	boolean changed_wifi;
	boolean connected_server;
	String old_ssid;
	Vibrator vib;                         //手机系统震动 对象  
	int old_id;
	String current_ssid;
	Handler netMessageHandle;
	clientInfoThread client_info_thread;
	        
	public enum WifiCipherType  
    {  
      WIFICIPHER_WEP,WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID  
    }  
	
	

	public void connect_server() {
		public_info.buf_length = 8;
		public_info.buf = new byte[public_info.buf_length];
		public_info.need_send_cmd = false;
		public_info.recv_thread_exit = false;
		public_info.info_thread_exit = false;
				
		// 判断  wifi 是否已经连接
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		
		while (true) {
		  NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);  
		  if (mWifi == null)
			  continue;
		  if (mWifi.isConnected()) {
			  break;
		  }
		}
		
		client_info_thread = new clientInfoThread(netMessageHandle);
		client_info_thread.start();
		
		set_btn_status(true);                                //允许界面按钮操作
	}
	
	public void disconnect_server() {
		client_info_thread.interrupt();		
	}
	
	public void set_btn_status(boolean status)
	{
		Button ledonBtn = (Button)findViewById(R.id.ledonBtn);
		ledonBtn.setEnabled(status);		
		Button ledoffBtn = (Button)findViewById(R.id.ledoffBtn);
		ledoffBtn.setEnabled(status);
		Button jdqonBtn = (Button)findViewById(R.id.jdqonBtn);
		jdqonBtn.setEnabled(status);
		Button jdqoffBtn = (Button)findViewById(R.id.jdqoffBtn);
		jdqoffBtn.setEnabled(status);
		Button jdqoff = (Button)findViewById(R.id.jdqoff);
		jdqoff.setEnabled(status);
		Button btnSend = (Button)findViewById(R.id.btnSend);
		btnSend.setEnabled(status);
	}
	
	protected void onDestroy() {
		super.onDestroy();		
		try {
			//unregisterReceiver(null);
		  disconnect_server();	
		  if (changed_wifi) {                //恢复之前的wifi网络
			  WifiConfiguration tempConfig = IsExists(old_ssid);
			  if (tempConfig != null) {
				 wifi.enableNetwork(tempConfig.networkId, true); 
			  }
		  }		  
		  Toast.makeText(getApplicationContext(), "Wi-Fi测试应用程序退出", Toast.LENGTH_LONG).show();  //提示信息
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	 private WifiConfiguration CreateWifiInfo(String SSID, String Password, WifiCipherType Type)   {  
	         WifiConfiguration config = new WifiConfiguration();    
	         config.allowedAuthAlgorithms.clear();  
	         config.allowedGroupCiphers.clear();  
	         config.allowedKeyManagement.clear();  
	         config.allowedPairwiseCiphers.clear();  
	         config.allowedProtocols.clear();  
	         
	         config.SSID = "\"" + SSID + "\"";  
	         //config.hiddenSSID = true;
	         //config.status = WifiConfiguration.Status.ENABLED;
	         
	         if(Type == WifiCipherType.WIFICIPHER_NOPASS)  
	         {  
	              config.wepKeys[0] = "\""+"\"";  
	              config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);  
	              config.wepTxKeyIndex = 0;  	              
	              
	        }  
	         else if(Type == WifiCipherType.WIFICIPHER_WEP)  
	        {  
	             config.preSharedKey = "\""+Password+"\"";   
	             config.hiddenSSID = true;    
	             config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);  
	             config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);  
	             config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);  
	             config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);  
	             config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);  
	             config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);  
	             config.wepTxKeyIndex = 0;  
	        }  
	         else if(Type == WifiCipherType.WIFICIPHER_WPA)  
	        {  
	             config.preSharedKey = "\""+Password+"\"";  
	             config.hiddenSSID = true;    
	             config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);    
	             config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);                          
	             config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);                          
	             config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);                     
	             config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);                       
	             //config.status = WifiConfiguration.Status.ENABLED;
	             config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
	             config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
	        }  
	      else  {  
	    	  return null;  
	      }  
	      return config;  
	}  
	 
	 private WifiConfiguration IsExists(String SSID) {      //查找 ssid 号的配置网络
	       List<WifiConfiguration> existingConfigs = wifi.getConfiguredNetworks();  //获取 配置的网络
	       for (WifiConfiguration existingConfig : existingConfigs)   {  
	          if (existingConfig.SSID.equals("\""+SSID+"\""))  
	          {  
	                  return existingConfig;  
	          }  
	       }  
	       return null;   
	 }  


	 public void show_result(byte[] buffer,int count) {
			StringBuffer msg = new StringBuffer();                                //创建缓冲区
			TextView tvInfo = (TextView)findViewById(R.id.tvInfo);   //创建 文本显示对象
			tvInfo.setText("");                                                   //清空对象内容 
			for (int i = 0; i < count; i++)
			  msg.append(String.format("0x%x ", buffer[i]));
			
			
			tvInfo.setText(msg);		                                           //显示到界面上
		}
	 
	 public void send_buffer_to_network(byte []msg,byte length) {
		 public_info.buf_length = length;
		 public_info.buf = msg;
		 public_info.need_send_cmd = true;
		 vib.vibrate(100);
	 }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		changed_wifi = false;
		connected_server = false;
		
		set_btn_status(false);                                //wifi设备未连接，设置 屏幕 一些 按钮 不能操作
		connectBtn = (Button)findViewById(R.id.connectBtn);   //创建 连接按钮对象
       	connectBtn.setEnabled(false);                                  //允许连接对象按钮操作
		
		vib = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);   //获取手机震动对象
		
		netMessageHandle = new Handler() {            //蓝牙消息 handler 对象
			public void handleMessage(Message msg) {
			  if (msg.what == 0x1234) {                             //如果消息是 0x1234,则是从 线程中 传输过来的数据  			 
				show_result((byte [])msg.obj,8);                    //将 缓冲区的数据显示到 UI
			  }
			  if (msg.what == 0x1235) {                             //如果消息是 0x1234,则是从 线程中 传输过来的数据  			 
				Toast.makeText(getApplicationContext(), "线程退出:" + msg.obj, Toast.LENGTH_LONG).show();
			  }
			}
		};
		
		registerReceiver(new BroadcastReceiver() {
			public void onReceive(Context context,Intent intent) {
			  List<ScanResult> results = wifi.getScanResults();
			  if (results.size() < 1) {
				  connectBtn.setEnabled(false);                                  //允许连接对象按钮操作
				  Toast.makeText(getApplicationContext(), "没有找到无线网络", Toast.LENGTH_LONG).show();
				  return;
			  }
			  Spinner spinner = (Spinner)findViewById(R.id.spinner1);       //获取 下拉框控件 对象
		      List<String>list = new ArrayList<String>();                   //创建列表，用于保存wifi设备ssid
		      for (ScanResult result: results) {
		       		list.add(result.SSID);                            //将wifi地址进入到列表
		      }
		      //创建数组适配器
		      ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_spinner_item,list);		       	
		      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);	//设置 下来显示方式	       	
		      spinner.setAdapter(adapter);                                  //将适配器中数据给下拉框对象  
		       			       			       	
		      connectBtn.setEnabled(true);                                  //允许连接对象按钮操作  
			}			
		},new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
		btnSearchWIFI = (Button)findViewById(R.id.btnSearchWIFI);
		btnSearchWIFI.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
				if (!wifi.isWifiEnabled())
					if (wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
						wifi.setWifiEnabled(true);        //打开 wifi功能
				wifi.startScan();                         //开始 搜索
				Toast.makeText(getApplicationContext(), "开始搜索无线网络", Toast.LENGTH_LONG).show();
				vib.vibrate(100);                        //震动
			}
		});
		
		
		
		connectBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				connected_server = false;
				
				Spinner spinner = (Spinner)findViewById(R.id.spinner1);    //获取 下拉框对象    
				String ssid = spinner.getSelectedItem().toString();            //从下拉框中选择项目，并获得它的地址
				current_ssid = ssid;
				
				WifiInfo info = wifi.getConnectionInfo();    //获取当前连接的wifi ssid,以便退出的时候恢复
				old_ssid = info.getSSID();
				
				WifiConfiguration wifiConfig = CreateWifiInfo(           //创建wifiConfiguration
						ssid,"",WifiCipherType.WIFICIPHER_NOPASS);       //  无加密
				
				WifiConfiguration tempConfig = IsExists(ssid);           //如果 ssid  存在，则删除
				if (tempConfig != null)
					wifi.removeNetwork(tempConfig.networkId);
				
				int id = wifi.addNetwork(wifiConfig);                    //加入到网络 
				if (id == -1) {
					Toast.makeText(getApplicationContext(), "加入到网络错误", Toast.LENGTH_LONG).show();
					return;
				}								
				
				boolean ret = wifi.enableNetwork(id, true);              //开启wifi网络
				if (ret) 
				  wifi.saveConfiguration();                              //  保持配置
				
				if (ret) {
					connect_server();                                    //连接服务器
					Toast.makeText(getApplicationContext(), String.format("加入wifi网络 %s 成功",ssid), Toast.LENGTH_SHORT).show();
					changed_wifi = true;															
				} else
					Toast.makeText(getApplicationContext(), String.format("加入wifi网络 %s 失败",ssid), Toast.LENGTH_SHORT).show();
				
				vib.vibrate(100);
			}   
			    
		});
		
		ledonBtn = (Button)findViewById(R.id.ledonBtn);
		ledonBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//开 LED 灯
				byte[] msg = new byte[8];
				msg[0] = (byte) 0x81;         //协议头
				msg[1] = (byte) 0x1;          //开灯命令
				msg[7] = (byte) (msg[0] + msg[1] +msg[2] +
						         msg[3] +msg[4] +msg[5] + msg[6]); //校验和
				
				send_buffer_to_network(msg,(byte) msg.length);
				
			}
		});
		
		ledoffBtn = (Button)findViewById(R.id.ledoffBtn);
		ledoffBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//关 LED 灯
				byte[] msg = new byte[8];
				msg[0] = (byte) 0x81;         //协议头
				msg[1] = (byte) 0x2;          //关灯命令
				msg[7] = (byte) (msg[0] + msg[1] +msg[2] +
						         msg[3] +msg[4] +msg[5] + msg[6]); //校验和
				
				send_buffer_to_network(msg,(byte) msg.length);
				
			}
		});
		
		
		jdqonBtn = (Button)findViewById(R.id.jdqonBtn);
		jdqonBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//开继电器 
				byte[] msg = new byte[8];
				msg[0] = (byte) 0x81;         //协议头
				msg[1] = (byte) 0x3;          //开继电器命令
				msg[7] = (byte) (msg[0] + msg[1] +msg[2] +
						         msg[3] +msg[4] +msg[5] + msg[6]); //校验和
				
				send_buffer_to_network(msg,(byte) msg.length);
				
			}
		});
		
		
		jdqoffBtn = (Button)findViewById(R.id.jdqoffBtn);
		jdqoffBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//关 继电器
				byte[] msg = new byte[8];
				msg[0] = (byte) 0x81;         //协议头
				msg[1] = (byte) 0x4;          //关继电器命令
				msg[7] = (byte) (msg[0] + msg[1] +msg[2] +
						         msg[3] +msg[4] +msg[5] + msg[6]); //校验和
				
				send_buffer_to_network(msg,(byte) msg.length);
				
			}
		});
		
		

		
		jdqoff = (Button)findViewById(R.id.jdqoff);
		jdqoff.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//开继电器 
				byte[] msg = new byte[8];
				msg[0] = (byte) 0x81;         //协议头
				msg[1] = (byte) 0x5;          //开继电器命令
				msg[7] = (byte) (msg[0] + msg[1] +msg[2] +
						         msg[3] +msg[4] +msg[5] + msg[6]); //校验和
				
				send_buffer_to_network(msg,(byte) msg.length);
				
			}
		});
		
		
		
		edtInfo = (EditText)findViewById(R.id.edtInfo);
		btnSend = (Button)findViewById(R.id.btnSend);
		btnSend.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View arg0) {
			  	String strMsg = edtInfo.getText().toString();
			  	if (strMsg.length() == 0) {
			  		Toast.makeText(getApplicationContext(), "文本框内容为空，请输入后操作", Toast.LENGTH_LONG).show();
			  		return;
			  	}
			  	send_buffer_to_network(strMsg.getBytes(),(byte)strMsg.length());
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
