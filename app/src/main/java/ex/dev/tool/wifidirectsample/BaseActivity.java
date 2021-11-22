package ex.dev.tool.wifidirectsample;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ex.dev.tool.wifidirectsample.utils.cryption.AESCrypt;

public class BaseActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();

    private AESCrypt aesCrypt = new AESCrypt();
    private ScrollView scrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setScrollView(ScrollView scrollView) {
        this.scrollView = scrollView;
    }

    protected void appendText(TextView textView, String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (textView != null)
                    textView.append("\n" + string);
                if (scrollView != null)
                    scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    protected String encryptQRData(String data) {
        return aesCrypt.aesEncrypt(data);
    }

    protected String decryptQRData(String encryptedData) {
        return aesCrypt.aesDecrypt(encryptedData);
    }
}
