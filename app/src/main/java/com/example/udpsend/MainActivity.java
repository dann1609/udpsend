package com.example.udpsend;


import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;




public class MainActivity extends Activity {

	EditText mensaje;
	TextView label;
	Button boton;
	LocationManager myLocationManager;
	String PROVIDER = LocationManager.GPS_PROVIDER;
	int i=0,rpm=0;
	//String host = "luciaserver.ddns.net";
	String host = "192.168.1.4";
    String port = "61919";
    String uriString = "udp://" + host + ":" + port + "/";
    Uri uri;
    Intent intent;
    String udpmsg,norte,oeste,gsec,gday,gweek,mspeed,angle;
    int uxgps;
    
    private static final String TAG = "OBDII Terminal";
	private ListView mConversationView;
	
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private static StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	static BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private static BluetoothChatService mChatService = null;
	
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	
	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	  
	  
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	EditText command_line;
	Button send_command;
	TextView msgWindow;
	
	 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		//Se relaciona el cuadro que dice mensaje con la variable EditText mensaje
        mensaje=(EditText)findViewById(R.id.mensaje);
		//Se relaciona el boton que dice enviar con la variable Button boton
        boton=(Button)findViewById(R.id.boton);
		//se relaciona el cuadro que pregunta si enviar paquete con la variable TextView label
        label=(TextView)findViewById(R.id.label);
		//Se declara el location manager
        myLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//Se declara el listener
        LocationListener milocListener = new MiLocationListener();
		//Se activa el listener para que registre cada 3000 mS
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,0, milocListener);
        //Se relaciona el cuadro negro con la varible TextView msgWindows
        msgWindow = (TextView) findViewById(R.id.msgWindow);
		final ActionBar actionBar = getActionBar();
		//  actionBar.setDisplayOptions(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Log.d(TAG, "Adapter: " + mBluetoothAdapter);
		
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
        
     // Se declara el Location Manager y se verifica los servicios de locacion del GPS o la Network
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
          // Se construlle el dialogo
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setTitle("Servicios de Localizacion no activos");
          builder.setMessage("Por favor activa los servicios de localizacion y el GPS");
          builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialogInterface, int i) {
            // Se construye el camino a las configuraciones del gps si el usuario acepta
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            }
          });
			//se crea el dialogo
          Dialog alertDialog = builder.create();
          alertDialog.setCanceledOnTouchOutside(false);
			// se visualiza el dialogo
          alertDialog.show();
        }
        //Intent intent2 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        //startActivity(intent2);

		//Se crea un  listener a la espera de que pulsen el boton
        boton.setOnClickListener(new OnClickListener() {
	                	
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				
				int c;
		        c=0;
		        
		        
		       
		        
		        
		        //get last known location, if available		        
		        Location location = myLocationManager.getLastKnownLocation(PROVIDER); 
		        
		        
		        //Se comprueba si la locacion es nulla
		        if(location == null){
					//si lo es label muestra no location
		        	   label.setText("No Location!");
		        	  }else{
		        	  //si no se obtienen los valores de velocidad angulo se le da formato 000
		        		  mspeed=String.format("%03d", (Math.round(location.getSpeed()*2.23)));
		        		  angle=String.format("%03d", (Math.round(location.getBearing())));
					//tiempo del gps y se transforma a tiempo en segundos dias y semanas se les da formato a uxgps 00000 a gday 0 a gweek 0000
		        		uxgps=(int)((location.getTime()-315964800000L)/1000);
		  		        gsec=String.format("%05d",(uxgps%86400));
		  		        gday=String.format("%01d",(uxgps/86400)%7);
		  		        gweek=String.format("%04d",(uxgps/604800));
		        		//Se obtiene los valores de Latitud y longitud y se les da formato 00000000  y 000000000
		        			norte=String.format("%+08d", (Math.round(location.getLatitude() * 100000)));
		        	        oeste=String.format("%+09d",(Math.round(location.getLongitude()* 100000)));
					//Se integran todas en un texto que se pone en el cuadro label.
		        	        label.setText("Latitude: " + norte+" Longitude: " +  oeste+" "+location+gweek+gday+gsec+"vel"+mspeed+"angle"+angle);
		        	     // Log.d("hola","hola");
		        	        //se agrrega el codigo que pido rpm al msgwindows
		        	        msgWindow.append("\n" + "Command: " + "01 0C");
					//La barra de texto se pone vacia
		    				command_line.setText("");
					//Se manda el mensaje al obd
		    				sendMessage("01 0C" + "\r");
		    				//Se crea el texto que se va a enviar por udp
		    		        udpmsg=">rev01"+gweek+gday+gsec+norte+oeste+mspeed+angle+String.format("%04d",rpm)+";id=lucia19<";
					//se le asigna a label el texto que se va a enviar
		    		        label.setText(udpmsg);
					//Se genera un logcon lo mismo
		    		        Log.d(udpmsg,udpmsg);
		    		        //Todos este codigo es para enviar el mensaje udp
		    		        uriString = "udp://" + host + ":" + port + "/";
		    		        uriString += Uri.encode(udpmsg);
		    		        uri = Uri.parse(uriString);
		    		        intent = new Intent(Intent.ACTION_SENDTO, uri);
		    		        intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		    		        intent.addCategory(Intent.CATEGORY_DEFAULT);

		    		        startActivity(intent);
		    		        //Ya se envio el udp
		    		        }
		        
		     
		        
		        
			}
		});//lo que acabamos de ver esta edentro del listener del boton es decir si se unde el boton se ejecuta todos lo que esta entre el listener y este texto
    }
    
    //aca esta detalla las propiedades del LocationListener ese mismo que se declara antes del boton y se pone a que sirva cada 3 segundos.
    public class MiLocationListener implements LocationListener
    {
		//lo que esta aca dentro se ejecuta cuan o el listener del gps detecta una posicion diferente, en este caso seria cada tres segundos.
    public void onLocationChanged(Location loc)
    {
		//Se obtiene la locacion
    	Location location = myLocationManager.getLastKnownLocation(PROVIDER);
		//si no se obtienen los valores de velocidad angulo se le da formato 000
    	  mspeed=String.format("%03d", (Math.round(location.getSpeed()*2.23)));
    	  angle=String.format("%03d", (Math.round(location.getBearing())));
		//tiempo del gps y se transforma a tiempo en segundos dias y semanas se les da formato a uxgps 00000 a gday 0 a gweek 0000
		uxgps=(int)((location.getTime()-315964800000L)/1000);
    	  gsec=String.format("%05d",(uxgps%86400));
	      gday=String.format("%01d",(uxgps/86400)%7);
	      gweek=String.format("%04d",(uxgps/604800));
		//Se aumenta el contador i para saber cuantas muestras hay
    	i=i+1;
    	
        if(location == null){
        	   label.setText("No Location!");
        	  }else{
			//Se obtiene los valores de Latitud y longitud y se les da formato 00000000  y 000000000
			norte=String.format("%+08d", (Math.round(location.getLatitude() * 100000)));
        oeste=String.format("%+09d",(Math.round(location.getLongitude()* 100000)));
			//se le asigna a label el texto que se va a enviar
        label.setText("Latitude: " + norte+" Longitude: " +  oeste+" "+location+gweek+gday+gsec+"vel"+mspeed+"angle"+angle+"No "+i);
        	  }
		//se agrrega el codigo que pido rpm al msgwindows
        msgWindow.append("\n" + "Command: " + "01 0C");
		command_line.setText("");
		//Se envia el mensaje al obd
		sendMessage("01 0C" + "\r");

		//Se inicializa el mensaje a enviar
        udpmsg=">rev01"+gweek+gday+gsec+norte+oeste+mspeed+angle+String.format("%04d",rpm)+";id=lucia19<";
        uriString = "udp://" + host + ":" + port + "/";
        uriString += Uri.encode(udpmsg);
        uri = Uri.parse(uriString);
        intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        startActivity(intent);
		//se envia el mensaje udp
    }

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		Intent serverIntent = null;
		
		switch (item.getItemId()) {
			case R.id.secure_connect_scan:
				// Launch the DeviceListActivity to see devices and do scan
				serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
				return true;
				
				
			default:
				return super.onOptionsItemSelected(item);
		}		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		//Se inicializa el listener del botton y otras cosas cuando la app entra a la app

		command_line = (EditText) findViewById(R.id.command_line);
		command_line.setInputType(InputType.TYPE_CLASS_TEXT);
		command_line.setSingleLine();
		addListenerOnButton();   

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		// Otherwise, setup the chat session
		} else {
			if (mChatService == null) setupChat();
		}
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}	

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
		Log.d("BT Handler SETUP ", "" +  mChatService.BTmsgHandler);
		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");   
	}	
	
	public void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);
			LogWriter.write_info("\n" + "Cmd: " + message);
			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			//mOutEditText.setText(mOutStringBuffer);
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.d("Terminal", "onActivityResult...");
		
		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)connectDevice(data, true);
				break;
			case REQUEST_CONNECT_DEVICE_INSECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)connectDevice(data, false);
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a chat session
					setupChat();
				} else {
					// User did not enable Bluetooth or an error occurred
					Log.d(TAG, "BT not enabled");
					Toast.makeText(this, "BT NOT ENABLED", Toast.LENGTH_SHORT).show();
					finish();
				}
		}
	}  
	
	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras()
				.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}
	
	private final Handler mHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case MESSAGE_STATE_CHANGE:
					
					switch (msg.arg1) {
						case BluetoothChatService.STATE_CONNECTED:
							setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
							mConversationArrayAdapter.clear();
							onConnect();
							break;
						case BluetoothChatService.STATE_CONNECTING:
							setStatus(R.string.title_connecting);
							break;
						case BluetoothChatService.STATE_LISTEN:
						case BluetoothChatService.STATE_NONE:
							setStatus(R.string.title_not_connected);
							break;
					}
					break;
				case MESSAGE_WRITE:
					break;
				case MESSAGE_READ:
					//StringBuilder res = new StringBuilder();
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buffer               
					String readMessage = new String(readBuf, 0, msg.arg1);
					readMessage=readMessage.trim();
					msgWindow.append("\n" + "Responsel: " + readMessage.length() + " ");
					LogWriter.write_info(readMessage);
					if(readMessage.length()>=10)
					{ 
					readMessage=readMessage+" ";
					msgWindow.append("\n" + "Response: " + readMessage.trim() + " ");
					LogWriter.write_info(readMessage); 
					readMessage = readMessage.substring(5, 11);
					msgWindow.append("\n" + "Response: " + readMessage.trim() + " ");
					LogWriter.write_info(readMessage);
					readMessage=readMessage.replaceAll("\\s","");
					msgWindow.append("\n" + "Response: " + readMessage.trim() + " ");
					LogWriter.write_info(readMessage);
					rpm=Integer.decode("0x"+readMessage)/4;
					readMessage =Integer.toString(Integer.decode("0x"+readMessage));
					msgWindow.append("\n" + "Response: " + readMessage.trim() + " ");
					LogWriter.write_info(readMessage);	
					readMessage =String.format("%04d",rpm);
					msgWindow.append("\n" + "Response: " + readMessage.trim() + " ");
					LogWriter.write_info(readMessage);	
					}

					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(), "Connected to "
							+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
							Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};		  

	public void addListenerOnButton() {
		send_command = (Button) findViewById(R.id.send_command);
		send_command.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String command_txt = command_line.getText().toString();
				msgWindow.append("\n" + "Command: " + command_txt);
				command_line.setText("");
				sendMessage(command_txt + "\r");
			}
		});
	}
	public void onConnect(){		
		//sendMessage("ATDP");
		sendMessage("ATE0");			
	}

}
