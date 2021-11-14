package ex.dev.tool.wifidirectsample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    private final int REQUEST_PERMISSION = 0x1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ServerActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                startActivity(intent);
            }
        });

        checkPermissions();
    }

    // Read/Write permission 확인 후  없을 경우 permission 요청
    private void checkPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
       if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
           permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
       else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
               permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
           }
        if (!permissions.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions.toArray(new String[]{}), REQUEST_PERMISSION);
        }
    }

    // 요청한 permission 을 사용자가 동의하였는지 여부 확인
    private boolean verifyPermission(int[] grantResults) {
        if(grantResults.length < 1) {
            return false;
        }
        for( int result : grantResults) {
            if(result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //permission 거절 시 앱 종료
        if(requestCode == REQUEST_PERMISSION) {
            if(!verifyPermission(grantResults))
                finish();
        }
    }
}