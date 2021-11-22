package ex.dev.tool.wifidirectsample.module;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import ex.dev.tool.wifidirectsample.utils.Const;

public class Server {
    private final String TAG = getClass().getSimpleName();

    private Context context;

    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedWriter networkWriter;
    private BufferedReader networkReader;
    private InputStream inputStream;
    private OutputStream outputStream;
    private File file;

    private WifiManager wifiManager;
    private WifiConfiguration exWifiConfiguration;
    private List<ScanResult> wifiList;
    private String SSID;
    private ConnectivityManager connectivityManager;

    private int cmd = 0;
    private boolean result = false;

    public Server(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public String getSSID() {
        if (SSID == null)
            SSID = "";
        return SSID;
    }

    // server 에서 전달하고자 하는 file 을 set 해줌
    public void setFile(File file) {
        this.file = file;
    }

    // PM_SERVER 이름으로 SSID 변경 후 tethering 시작
    public void startTethering(ConnectivityManager.OnStartTetheringCallback callback) {
        // Wifi 가 연결되어 있지 않은 경우 wifi 를 enable
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING)
            wifiManager.setWifiEnabled(true);

        int count = 0;
        wifiManager.startScan();
        wifiList = wifiManager.getScanResults();
        for (ScanResult result : wifiList) {
            Log.d(TAG, "scan result : " + result.SSID);
            if (result.SSID.contains(Const.PM_SERVER))
                count++;
        }
        exWifiConfiguration = wifiManager.getWifiApConfiguration();
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = Const.PM_SERVER + "_" + count;
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        SSID = wifiConfiguration.SSID;
        Log.d(TAG, "ssid : " + SSID);
        wifiManager.setWifiApConfiguration(wifiConfiguration);
        connectivityManager.startTethering(0, true, callback);
    }

    public void stopTethering() {
        connectivityManager.stopTethering(ConnectivityManager.TETHERING_WIFI);
        wifiManager.setWifiApConfiguration(exWifiConfiguration);
    }

    // 현재 단말의 IP 주소를 return 함
    public String getLocalIpAddress() throws UnknownHostException {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array()).getHostAddress();
    }

    public void setServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(Const.PORT);
                    Log.d(TAG, "open server");
                    socket = serverSocket.accept();
                    Log.d(TAG, "Connected to client");
                    networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    readDataFromClient();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void readDataFromClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Ready to read");
                    cmd = networkReader.read();
                    Log.d(TAG, "cmd : " + cmd);

                    if (cmd == Const.REQUEST_FILE_TRANSFER) { //파일 전송

                        if (file != null) {
                            Log.d(TAG, "\n-- File info --");
                            Log.d(TAG, "file name : " + file.getName());
                            Log.d(TAG, "file size : " + file.length());
                            result = sendFile(file, socket);
                            Log.d(TAG, "send file result : " + (result ? "true" : "false"));
                        } else {
                            Log.e(TAG, "file is not exist");
                        }

                        if (result) {  //파일을 성공적으로 보낸 경우 client 에서도 파일을 정상적으로 받았는지 여부 확인을 위해 read() 호출
                            readDataFromClient();
                        } else { // 파일 보내기를 실패한 경우는 inputStream/outputStream 을 close 해줌
                            inputStream.close();
                            outputStream.close();
                        }
                    } else if (cmd == Const.CLIENT_FINISHED_RECEIVING_FILE) { //Client 에서 파일을 정상적으로 받았으므로 inputStream/outputStream 을 close
                        Log.d(TAG, "Client finished receiving file");
                        inputStream.close();
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean sendFile(File file, Socket socket) {
        Log.d(TAG, "start sending data");
        byte buf[] = new byte[1024];
        int len;
        int percent = 0;
        int sendData = 0;
        long size = file.length();

        try {
            inputStream = new FileInputStream(file);
            outputStream = socket.getOutputStream();

            while ((len = inputStream.read(buf)) != -1) {
                Log.d(TAG, "send file len : " + len);
                outputStream.write(buf, 0, len);
                outputStream.flush();
                sendData += len;

                if (sendData <= size) {
                    percent = (int) ((sendData * 100) / size);
                    Log.d(TAG, "percent : " + percent);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

            if (sendData == size)
                return true;

            return false;
        }
        return true;
    }

    public void closeServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "close server");
                    if (serverSocket != null && socket != null) {
                        serverSocket.close();
                        socket.close();
                    }
                    stopTethering();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
