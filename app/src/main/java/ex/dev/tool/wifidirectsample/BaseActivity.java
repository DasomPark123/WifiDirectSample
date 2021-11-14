package ex.dev.tool.wifidirectsample;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void appendText(TextView textView, String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (textView != null)
                    textView.append("\n" + string);
            }
        });
    }


}
