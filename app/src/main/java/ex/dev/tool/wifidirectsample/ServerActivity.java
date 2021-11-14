package ex.dev.tool.wifidirectsample;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import ex.dev.tool.wifidirectsample.utils.Const;

public class ServerActivity extends BaseActivity {
    private final String TAG = getClass().getSimpleName();
    private WifiManager wifiManager;
    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedWriter networkWriter;
    private BufferedReader networkReader;
    private InputStream inputStream;
    private OutputStream outputStream;

    private TextView tvServer;

    private int cmd = 0;
    private boolean result = false;

    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        tvServer = findViewById(R.id.tv_server);

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        //Wi-fi가 disabled 혹은 disabling 중인 경우 Wi-fi룰 킴
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            //wifiManager.setWifiEnabled(true);
            Log.d(TAG, "Wifi not connected");
            appendText(tvServer, "Wifi not connected");
        } else {
            setServer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeServer();
    }

    private void setServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(Const.PORT);

                    appendText(tvServer, "open server");
                    socket = serverSocket.accept();
                    appendText(tvServer, "Connected to client");
                    networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    readDataFromClient();

                } catch (IOException e) {
                    e.printStackTrace();
                    appendText(tvServer, e.getMessage());
                }
            }
        }).start();
    }

    private void readDataFromClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        appendText(tvServer, "Ready to read");
                        cmd = networkReader.read();
                        appendText(tvServer, "cmd : " + cmd);
                        break;
                    }

                    if (cmd == Const.REQUEST_FILE_TRANSFER) { //파일 전송
                        File file = new File("sdcard/Download/RF750_95.00B1_20210907.bin");
                        appendText(tvServer, "start sending file");
                        appendText(tvServer, "file name : " + file.getName());
                        appendText(tvServer, "file size : " + file.length());
                        result = sendFile(file, socket, tvServer);
                        appendText(tvServer, "send file result : " + (result ? "true" : "false"));

                        //파일을 성공적으로 보낸 경우 client 에서도 파일을 정상적으로 받았는지 여부 확인을 위해 read() 호출
                        if (result) {
                            readDataFromClient();
                        }
                        // 파일 보내기를 실패한 경우는 inputStream/outputStream 을 close 해줌
                        else {
                            inputStream.close();
                            outputStream.close();
                        }
                    } else if (cmd == Const.FINISHED_RECEIVING_FILE) { //Client 에서 파일을 정상적으로 받았으므로 inputStream/outputStream 을 close
                        appendText(tvServer, "Finished receiving file");
                        inputStream.close();
                        outputStream.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    appendText(tvServer, e.getMessage());
                }
            }
        }).start();
    }

    private void closeServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "close server");
                    if (serverSocket != null && socket != null) {
                        serverSocket.close();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean sendFile(File file, Socket socket, TextView textView) {
        appendText(textView, "send data");
        byte buf[] = new byte[1024];
        int len;
        int percent = 0;
        int receivedData = 0;
        long size = file.length();

        try {
            inputStream = new FileInputStream(file);
            outputStream = socket.getOutputStream();

            while (true) {
                if ((len = inputStream.read(buf)) == -1) {
                    appendText(textView, "send file len : " + len);
                    break;
                }
                appendText(textView, "send file len : " + len);
                appendText(textView, "writing file data");
                outputStream.write(buf, 0, len);
                outputStream.flush();
                //temp = (int) ((file.length() * 100) / size);
                receivedData += len;

                if (receivedData <= size) {
                    // handler.sendMessage(new Message().obtain(handler, FileReceiveStart, temp));
                    percent = (int) ((receivedData * 100) / size);
                    appendText(textView, "percent : " + percent);
                }
            }
            //appendText(textView, "close outputStream ");
            //outputStream.close();
            //appendText(textView, "close inputStream");
            //inputStream.close();
        } catch (IOException e) {
            appendText(textView, e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }
}