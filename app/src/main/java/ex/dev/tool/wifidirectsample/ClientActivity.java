package ex.dev.tool.wifidirectsample;

import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ex.dev.tool.wifidirectsample.utils.Const;

public class ClientActivity extends BaseActivity {
    private final String TAG = getClass().getSimpleName();
    private WifiManager wifiManager;
    private Socket socket;
    private BufferedWriter networkWriter;
    private BufferedReader networkReader;

    private boolean isFileTransfering = false;

    private TextView tvClient;

    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        tvClient = findViewById(R.id.tv_client);

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            Log.d(TAG, "wifi is not connected");
            appendText(tvClient, "wifi is not connected");
        } else {
            connectToServer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array()).getHostAddress();
    }

    private void connectToServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appendText(tvClient, "clientThread is started");
                    //String serverIp = ipToString(wifiManager.getDhcpInfo().serverAddress);
                    String serverIp = getLocalIpAddress();
                    appendText(tvClient, "ip : " + serverIp);
                    socket = new Socket("192.168.35.159", Const.PORT);
                    appendText(tvClient, "create socket");
                    networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    if (socket != null) {
                        appendText(tvClient, "connected");
                        writeDataToServer(Const.REQUEST_FILE_TRANSFER);
                        receiveFileFromServer();
                    } else {
                        appendText(tvClient, "failed connection");
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    appendText(tvClient, e.getMessage());
                }
            }
        }).start();
    }

    private void writeDataToServer(int cmd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    appendText(tvClient, "write cmd : " + cmd);
                    networkWriter.write(cmd);
                    networkWriter.flush();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    appendText(tvClient, e.getMessage());
                }
            }
        }).start();
    }

    private void receiveFileFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                appendText(tvClient, "clientReadThread started");
                try {
                    File file = new File("sdcard/Download/RF750_95.00B1_20210907.bin");
                    File dir = new File(file.getParent());
                    if(!dir.exists())
                        dir.mkdirs();
                    file.createNewFile();
                    appendText(tvClient, "start receiving file");
                    boolean result = receiveFile(file, socket, tvClient);
                    appendText(tvClient, "result : " + (result ? "true" : "false"));
                    if(result) {
                        isFileTransfering = false;
                        writeDataToServer(Const.FINISHED_RECEIVING_FILE);
                    }
                } catch (IOException e) {
                    appendText(tvClient, e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean receiveFile(File file, Socket socket, TextView textView) {
        appendText(textView, "receive data");
        byte buf[] = new byte[1024];
        int len;
        int percent = 0;
        int receivedData = 0;
        long size = 154616;

        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = new FileOutputStream(file);

            while (true) {
                if ((len = inputStream.read(buf)) == -1) {
                    appendText(textView, "receive file len : " + len);
                    break;
                }
                appendText(textView, "receive file len : " + len);
                isFileTransfering = true;
                outputStream.write(buf, 0, len);
                outputStream.flush();
                receivedData += len;

                if (receivedData <= size) {
                    percent = (int) ((receivedData * 100) / size);
                    appendText(textView, "percent : " + percent);
                    if (receivedData == size)
                        break;
                }
            }
            appendText(textView, "close outputStream ");
            outputStream.close();
            appendText(textView, "close inputStream ");
            inputStream.close();
        } catch (IOException e) {
            appendText(textView, e.getMessage());
            e.printStackTrace();

            if (receivedData == size)
                return true;

            return false;
        }
        return true;
    }

    private String ipToString(int num) {
        return ((num) & 0xFF) + "." +
                ((num >> 8) & 0xFF) + "." +
                ((num >> 16) & 0xFF) + "." +
                ((num >> 24) & 0xFF);
    }
}