package asia.nana7mi.arirang.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import asia.nana7mi.arirang.R;
import asia.nana7mi.arirang.hook.IHookNotify;
import asia.nana7mi.arirang.service.HookNotifyService;

public class TestActivity extends AppCompatActivity {

    private IHookNotify hookNotify;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            hookNotify = IHookNotify.Stub.asInterface(service);
            try {
                hookNotify.onPermissionUsed(
                        "com.example.testapp",
                        "CLIPBOARD_READ"
                );
            } catch (Exception ignored) {}
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            hookNotify = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        Button btn = findViewById(R.id.btn_test_clipboard);
        btn.setOnClickListener(v -> {
            if (hookNotify != null) {
                try {
                    hookNotify.onPermissionUsed(
                            "com.example.testapp",
                            "CLIPBOARD_READ"
                    );
                } catch (Exception ignored) {}
            } else {
                Intent intent = new Intent(this, HookNotifyService.class);
                bindService(intent, conn, BIND_AUTO_CREATE);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unbindService(conn);
        } catch (Exception ignored) {}
    }
}
