package ex.dev.tool.wifidirectsample.module;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

import ex.dev.tool.wifidirectsample.R;
import ex.dev.tool.wifidirectsample.entity.QRDataEntity;
import ex.dev.tool.wifidirectsample.entity.WifiDirectEntity;
import ex.dev.tool.wifidirectsample.utils.Const;

public class Client {
    private final String TAG = getClass().getSimpleName();

    private Context context;
    private Socket socket;
    private BufferedWriter networkWriter;
    private BufferedReader networkReader;
    private InputStream inputStream;
    private OutputStream outputStream;

    private WifiManager wifiManager;
    private WifiConfiguration wifiConfiguration;
    private boolean exWifiEnabled;
    private List<ScanResult> wifiList;
    private int networkId;

    private Status mStatus = Status.READY_TO_CONNECT_WIFI;

    protected enum Status {
        READY_TO_CONNECT_WIFI, CONNECTING_TO_WIFI, CONNECTION_SUCCESS_TO_WIFI, CONNECTION_FAIL_TO_WIFI,
        READY_TO_CONNECT_SERVER, CONNECTING_TO_SERVER, CONNECTION_SUCCESS_TO_SERVER, CONNECTION_FAIL_TO_SERVER,
        READY_TO_DOWNLOAD, DOWNLOAD_PROCEEDING, DOWNLOAD_SUCCESS, DOWNLOAD_FAILED
    }

    public Client(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
    }

    // Server 단말에서 tethering 한 AP에 연결
    public boolean connectToWifi(String ssid) {
        try {
            Log.d(TAG, "Connecting Wi-Fi...");
            setStatus(Status.CONNECTING_TO_WIFI);

            if (ssid == null) {
                Log.d(TAG, "SSID is null");
                return false;
            }

            boolean result;
            result = changeWiFiStatus(true);

            if (!result)
                return false;

            result = changeWiFiStatus(false);

            if (!result)
                return false;

            // 연결 가능한 wifi 들을 scanning 하여 server 의 SSID 를 찾아 연결을 시도함
            Log.d(TAG, "Server ssid : " + ssid);
            ScanResult pmServer = startScanWifi(ssid);

            if (pmServer != null) {
                Log.d(TAG, "Try to connect to PM_SERVER");
                wifiConfiguration = new WifiConfiguration();
                wifiConfiguration.SSID = "\"".concat(pmServer.SSID).concat("\"");
                wifiConfiguration.status = WifiConfiguration.Status.DISABLED;
                wifiConfiguration.priority = 40;

                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfiguration.allowedAuthAlgorithms.clear();
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                networkId = -1;
                networkId = wifiManager.addNetwork(wifiConfiguration);

                if (networkId != -1) {
                    result = wifiManager.enableNetwork(networkId, true);
                    Log.d(TAG, "enable network : " + result);
                }

                Thread.sleep(1000);
                // 연결된 SSID 가 서버 단말에서 tethering 한 SSID 와 동일한지 확인
                if (wifiManager.getConnectionInfo().getSSID().contains(pmServer.SSID))
                    return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private boolean changeWiFiStatus(boolean isEnabled) {
        try {
            if (isEnabled) {
                // Client 단말의 wifi 가 enable 되어있는 경우 disable 시킴
                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED ||
                        wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                    // Wi-Fi가 enable 되어 있는 경우 download 완료 후 configuration roll-back 을 위해 저장해둠
                    exWifiEnabled = true;
                    wifiManager.setWifiEnabled(false);
                } else {
                    exWifiEnabled = false;
                    return true;
                }
                // Wi-Fi가 disable 될 때 까지 기다림
                while (true) {
                    int count = 0;
                    if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                        return true;
                    } else {
                        Log.d(TAG, "Try to disable  Wi-Fi....");
                        count++;
                        Thread.sleep(1000);
                    }

                    if (count > 3)
                        return false;
                }
            } else {
                // Client 단말의 wifi 가 disable 되어있는 경우 enable 시킴
                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED ||
                        wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
                    wifiManager.setWifiEnabled(true);
                }

                // Wi-Fi가 enable 될 때 까지 기다림
                while (true) {
                    int count = 0;
                    if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                        return true;
                    } else {
                        Log.d(TAG, "Try to enable Wi-Fi...");
                        count++;
                        Thread.sleep(1000);
                    }

                    if (count > 3)
                        return false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 주변에 있는 AP 들을 scanning 하여 Server 와 동일한 SSID 를 찾음
    private ScanResult startScanWifi(String ssid) {
        ScanResult pmServer = null;
        int count = 0;
        while (true) {
            try {
                Log.d(TAG, "Searching sever...");
                wifiManager.startScan();
                wifiList = wifiManager.getScanResults();

                if (wifiList != null) {
                    for (ScanResult result : wifiList) {
                        if (result.SSID.contains(ssid)) {
                            pmServer = result;
                            return pmServer;
                        }
                    }
                }
                count++;
                Thread.sleep(2000);

                if (count > 6)
                    break;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return pmServer;
    }

    // 서버에 연결
    public void connectToServer(final WifiDirectEntity wifiDirectEntity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                    Log.d(TAG, "Connecting to server...");
                    setStatus(Status.CONNECTING_TO_SERVER);
                    socket = new Socket(wifiDirectEntity.getIp(), wifiDirectEntity.getPort());
                    networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    if (socket != null) {
                        Log.d(TAG, "connected");
                        setStatus(Status.CONNECTION_SUCCESS_TO_SERVER);
                        writeDataToServer(Const.REQUEST_FILE_TRANSFER); // 서버에 파일전송 요청
                        receiveFileFromServer(wifiDirectEntity.getFileName(), wifiDirectEntity.getFileSize());
                    } else {
                        Log.e(TAG, "connection fail");
                        setStatus(Status.CONNECTION_FAIL_TO_SERVER);
                        socket.close();
                    }
                } catch (IOException | InterruptedException e) {
                    setStatus(Status.CONNECTION_FAIL_TO_SERVER);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 서버에 데이터 write
    private void writeDataToServer(final int cmd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "write cmd : " + cmd);
                    networkWriter.write(cmd);
                    networkWriter.flush();
                } catch (IOException e) {
                    setStatus(Status.DOWNLOAD_FAILED);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 서버로 부터 파일을 전달 받음 ( 파일 생성 -> server 에서 전달 받은 데이터를 생성한 파일에 write )
    private void receiveFileFromServer(final String fileName, final long fileSize) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "receiving file thread started");
                setStatus(Status.READY_TO_DOWNLOAD);
                try {
                    // 서버로 부터 받아온 파일 데이터를 작성할 파일 생성.
                    File file = new File("sdcard/Download/Scan2Set/" + fileName);
                    if (file.getParent() != null) {
                        File dir = new File(file.getParent());
                        if (!dir.exists())
                            dir.mkdirs();
                    }
                    file.createNewFile();
                    Log.d(TAG, "== File Info ==");
                    Log.d(TAG, "file name : " + file.getName());
                    Log.d(TAG, "file size : " + fileSize);

                    boolean result = receiveFile(file, socket, fileSize);
                    Log.d(TAG, "receive file result : " + (result ? "true" : "false"));
                    if (result) {
                        writeDataToServer(Const.CLIENT_FINISHED_RECEIVING_FILE);
                        Thread.sleep(2000);
                        inputStream.close();
                        outputStream.close();
                        rollbackWiFiConfig();
                        setStatus(Status.DOWNLOAD_SUCCESS);
                    } else {
                        setStatus(Status.DOWNLOAD_FAILED);
                    }
                } catch (IOException | InterruptedException e) {
                    setStatus(Status.DOWNLOAD_FAILED);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean receiveFile(File file, Socket socket, long fileSize) {
        Log.d(TAG, "start receiving file");
        byte buf[] = new byte[1024];
        int len;
        int percent = 0;
        int receivedData = 0;
        long size = fileSize;

        try {
            inputStream = socket.getInputStream();
            outputStream = new FileOutputStream(file);

            while ((len = inputStream.read(buf)) != -1) {
                Log.d(TAG, "receive file len : " + len);
                outputStream.write(buf, 0, len);
                outputStream.flush();
                receivedData += len;

                Log.d(TAG, "received data : " + receivedData);
                if (receivedData <= size) {
                    percent = (int) ((receivedData * 100) / size);
                    setStatus(Status.DOWNLOAD_PROCEEDING, file.getName(), percent);
                    if (receivedData == size)
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

            if (receivedData == size)
                return true;

            return false;
        }
        return true;
    }

    private void rollbackWiFiConfig() {
        if (!exWifiEnabled)
            wifiManager.setWifiEnabled(false);
    }

    private void setStatus(final Status status) {
        Log.d(TAG, "setStatus : " + status.toString());
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mStatus = status;
//                mProgressBar.setMessage(getStatusMessage(status));
//            }
//        });
    }

    private void setStatus(final Status status, final String fileName, final int percent) {
        Log.d(TAG, " setStatus : " + status.toString() + "file name : " + fileName + "percent : " + percent);
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mStatus = status;
//                if (mStatus == Status.DOWNLOAD_PROCEEDING)
//                    mProgressBar.setMessage(getStatusMessage(status) + fileName + "\n" + percent + "%");
//                else
//                    mProgressBar.setMessage(getStatusMessage(status));
//            }
//        });
    }

    private String getStatusMessage(final Status status) {
        String message;
        switch (status) {
            case READY_TO_CONNECT_WIFI:
                message = context.getString(R.string.ready_to_connect_wifi);
                break;
            case CONNECTING_TO_WIFI:
                message = context.getString(R.string.connecting_to_wifi);
                break;
            case CONNECTION_SUCCESS_TO_WIFI:
                message = context.getString(R.string.connection_success_to_wifi);
                break;
            case CONNECTION_FAIL_TO_WIFI:
                message = context.getString(R.string.connection_failed_to_wifi);
                break;
            case READY_TO_CONNECT_SERVER:
                message = context.getString(R.string.ready_to_connect_server);
                break;
            case CONNECTING_TO_SERVER:
                message = context.getString(R.string.connecting_to_server);
                break;
            case CONNECTION_SUCCESS_TO_SERVER:
                message = context.getString(R.string.connection_success_to_server);
                break;
            case CONNECTION_FAIL_TO_SERVER:
                message = context.getString(R.string.connection_failed_to_server);
                break;
            case READY_TO_DOWNLOAD:
                message = context.getString(R.string.ready_to_download);
                break;
            case DOWNLOAD_PROCEEDING:
                message = context.getString(R.string.download_proceeding);
                break;
            case DOWNLOAD_SUCCESS:
                message = context.getString(R.string.download_success);
                break;
            case DOWNLOAD_FAILED:
                message = context.getString(R.string.download_failed);
                break;
            default:
                message = "";
        }
        return message;
    }

    public void closeClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
