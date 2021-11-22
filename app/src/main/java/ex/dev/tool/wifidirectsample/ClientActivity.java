package ex.dev.tool.wifidirectsample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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

import ex.dev.tool.wifidirectsample.entity.QRDataEntity;
import ex.dev.tool.wifidirectsample.module.Client;
import ex.dev.tool.wifidirectsample.utils.Const;
import ex.dev.tool.wifidirectsample.utils.Utils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ClientActivity extends BaseActivity {
    private final String TAG = getClass().getSimpleName();
    private TextView tvClient;
    private ScrollView svScroll;

    private Client client;

    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        tvClient = findViewById(R.id.tv_client);
        svScroll = findViewById(R.id.sv_client);
        svScroll.fullScroll(View.FOCUS_DOWN);
        setScrollView(svScroll);

        client = new Client(this);

        moveToQRScanner();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        client.closeClient();
    }

    private void moveToQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setPrompt(getString(R.string.point_qr_to_download_file));
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // QRScanner 로 부터 돌와왔을 때 동작 부분
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                appendText(tvClient, "QR Scan canceled");
            } else {
                String scannedQRData = decryptQRData(intentResult.getContents().substring(Const.MAGIC_NUMBER.length()));
                appendText(tvClient, scannedQRData);
                Gson gson = new Gson();
                QRDataEntity qrDataEntity = gson.fromJson(scannedQRData, QRDataEntity.class);
                startFileDownloadTask(qrDataEntity);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startFileDownloadTask(QRDataEntity qrDataEntity) {
        Single.create(subscriber -> {
            boolean result = client.connectToWifi(qrDataEntity.getWifiDirectEntity().getSsid());
            subscriber.onSuccess(result);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if ((boolean) result) {
                        appendText(tvClient, "Successfully connected with " + qrDataEntity.getWifiDirectEntity().getSsid());
                        client.connectToServer(qrDataEntity.getWifiDirectEntity());
                    } else {
                        appendText(tvClient, "Failed connection");
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    appendText(tvClient, throwable.getMessage());
                });
    }
}