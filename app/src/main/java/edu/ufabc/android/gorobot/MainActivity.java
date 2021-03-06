package edu.ufabc.android.gorobot;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    private static final int ENABLE_BLUETOOTH = 1;
    private static final int SELECT_PAIRED_DEVICE = 2;
    private static final int SELECT_DISCOVERED_DEVICE = 3;
    private static final int GET_COORDINATES = 4;
    private static final int GET_COORDINATES_FROM_LIST = 5;
    private static final int STATUS_ACTIVITY = 6;
    private static final String MyPREFERENCES = "MyPrefs" ;
    private static final String LAT = "latKey";
    private static final String LNG = "lngKey";
    private static final String TAG = "MAIN_ACTIVITY";
    private static TextView statusMessage;
    private static TextView textSpace;
    private ConnectionThread connect;

    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            byte[] data = bundle.getByteArray("data");
            String dataString= new String(data);

            if(dataString.equals("---N"))
                statusMessage.setText("Ocorreu um erro durante a conexão");
            else if(dataString.equals("---S"))
                statusMessage.setText("Conectado");


            else {



                textSpace.setText(new String(data));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusMessage = (TextView) findViewById(R.id.statusMessage);
        textSpace = (TextView) findViewById(R.id.textSpace);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            statusMessage.setText("Não foi encontrado Hardware Bluetooth ou não é funcional");
        }
        else {
            statusMessage.setText("Hardware Bluetooth funcional");
            if(!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH);
                statusMessage.setText("Solicitando ativação do Bluetooth...");
            }
            else {
                statusMessage.setText("Bluetooth já ativado");
            }
        }

        Log.d(TAG,"OnCreate");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case ENABLE_BLUETOOTH:
                if(resultCode == RESULT_OK) {
                    statusMessage.setText("Bluetooth ativado");
                }
                else {
                    statusMessage.setText("Bluetooth não ativado");
                }
                break;
            case SELECT_PAIRED_DEVICE:
            case SELECT_DISCOVERED_DEVICE:
                if(resultCode == RESULT_OK) {
                    statusMessage.setText("Você selecionou " + data.getStringExtra("btDevName") + "\n"
                            + data.getStringExtra("btDevAddress"));

                    connect = new ConnectionThread(data.getStringExtra("btDevAddress"));
                    connect.start();
                }
                else {
                    statusMessage.setText("Nenhum dispositivo selecionado");
                }
                break;
            case GET_COORDINATES_FROM_LIST:
            case GET_COORDINATES:
                if(resultCode == RESULT_OK){
                    String latitude = data.getStringExtra("LATITUDE");
                    String longitude = data.getStringExtra("LONGITUDE");
                    EditText messageBox = (EditText) findViewById(R.id.editText_MessageBox);
                    EditText messageBox2 = (EditText) findViewById(R.id.editText_MessageBox2);
                    messageBox.setText(latitude);
                    messageBox2.setText(longitude);
                }
        }
    }

    public void searchPairedDevices(View view) {
        Intent searchPairedDevicesIntent = new Intent(this, PairedDevices.class);
        startActivityForResult(searchPairedDevicesIntent, SELECT_PAIRED_DEVICE);
    }

    public void discoverDevices(View view) {
        Intent discoverDevicesIntent = new Intent(this, DiscoveredDevices.class);
        startActivityForResult(discoverDevicesIntent, SELECT_DISCOVERED_DEVICE);
    }

    public void sendMessage(View view) {

                    EditText messageBox = (EditText) findViewById(R.id.editText_MessageBox);
                    String messageBoxString = messageBox.getText().toString();
                    byte[] data = messageBoxString.getBytes();
                    EditText messageBox2 = (EditText) findViewById(R.id.editText_MessageBox2);
                    String messageBoxString2 = messageBox2.getText().toString();
                    final byte[] data2 = messageBoxString2.getBytes();


                    try {
                        connect.write(data);

                        //delay to make enough time to Arduino receive the strings
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                connect.write(data2);
                            }
                        }, 2000);
                        saveInSharedPreferences(messageBoxString, messageBoxString2);



                    }
                    catch (Exception e){
                        Toast.makeText(MainActivity.this, "Erro: conecte o robô via bluetooth primeiro.",
                                Toast.LENGTH_LONG).show();
                        Log.d(TAG, e.getMessage());
                    }
    }

    public void saveInSharedPreferences(String lat, String lng){
        SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefsEditor = sharedpreferences.edit();
        int inputCount = sharedpreferences.getInt("inputCount", 0);

        sharedPrefsEditor.putString(LAT+inputCount, lat);
        sharedPrefsEditor.putString(LNG+inputCount, lng);
        inputCount++;
        sharedPrefsEditor.putInt("inputCount", inputCount);
        sharedPrefsEditor.apply();
    }

    //reset command to stop the robot
    public void reset(View view){
        try {
        String resetCommand = "r";
        byte[] data =  resetCommand.getBytes();
        connect.write(data);
        }
        catch (Exception e){
            Toast.makeText(MainActivity.this, "Erro: conecte o robô via bluetooth primeiro.",
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, e.getMessage());
        }
    }

    public void openMap(View view){
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivityForResult(intent, GET_COORDINATES);
    }

    public void openListOfLastLocations(View view){
        Intent intent = new Intent(MainActivity.this, ListaActivity.class);
        startActivityForResult(intent, GET_COORDINATES_FROM_LIST);
    }
}