package ex.dev.tool.wifidirectsample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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

import ex.dev.tool.wifidirectsample.entity.QRDataEntity;
import ex.dev.tool.wifidirectsample.entity.WifiDirectEntity;
import ex.dev.tool.wifidirectsample.module.Server;
import ex.dev.tool.wifidirectsample.utils.Const;

public class ServerActivity extends BaseActivity {
    private final String TAG = getClass().getSimpleName();

    private Server server;
    private File file;

    private TextView tvServer;
    private ScrollView svServer;
    private TextView tvFileName;

    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        tvServer = findViewById(R.id.tv_server);
        svServer = findViewById(R.id.sv_server);
        svServer.fullScroll(View.FOCUS_DOWN);
        setScrollView(svServer);

        tvFileName = findViewById(R.id.tv_file_name);
        findViewById(R.id.btn_select_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFileExplorer();
            }
        });

        server = new Server(this);
        server.startTethering(onStartTetheringCallback);
        server.setServer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(server != null) {
            server.closeServer();
        }
    }

    private void openFileExplorer() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        properties.error_dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        properties.offset = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        properties.show_hidden_files = false;
        FilePickerDialog dialog = new FilePickerDialog(ServerActivity.this, properties);
        dialog.setTitle(getString(R.string.select_file));
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                Log.d(TAG, "onSelectedFilePaths : " + files[0]);
                if(files[0] != null && !files[0].isEmpty()) {
                    server.setFile(new File(files[0]));
                    tvFileName.setText(files[0]);
                    generateQRCode(Const.MAGIC_NUMBER + encryptQRData(getQRData()));
                }
            }
        });
        dialog.show();
    }

    // QR에 담을 데이터를 json 형식으로 리턴함 ( Wifi 정보, 서버 정보, 파일 정보가 담김 )
    private String getQRData() {
        try {
            String serverIp = server.getLocalIpAddress();
            String ssid = server.getSSID();
            appendText(tvServer, "ip : " + serverIp);
            appendText(tvServer, "ssid : " + ssid);

            String path = tvFileName.getText().toString();
            if(path != null && !path.isEmpty()) {
                file = new File(path);
                QRDataEntity qrDataEntity = new QRDataEntity(new WifiDirectEntity(ssid, serverIp, Const.PORT, file.getName(), file.length()));
                Gson gson = new Gson();
                return gson.toJson(qrDataEntity);
            } else {
                appendText(tvServer, "File not found to send");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            appendText(tvServer, e.getMessage());
        }
        return "";
    }

    // QR 코드를 생성
    private void generateQRCode(String contents) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            Bitmap bitmap = toBitmap(qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, 500, 500));
            ((ImageView) findViewById(R.id.iv_qr)).setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private Bitmap toBitmap(BitMatrix matrix) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }

    private final ConnectivityManager.OnStartTetheringCallback onStartTetheringCallback = new ConnectivityManager.OnStartTetheringCallback() {
        @Override
        public void onTetheringStarted() {
            super.onTetheringStarted();
            appendText(tvServer, "Tethering Started");
        }

        @Override
        public void onTetheringFailed() {
            super.onTetheringFailed();
            appendText(tvServer, "Tethering Failed");
        }
    };
}