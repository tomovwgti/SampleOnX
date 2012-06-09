
package com.tomovwgti.onx;

import net.arnx.jsonic.JSON;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tomovwgti.json.Light;
import com.tomovwgti.json.Msg;

import de.roderick.weberknecht.WebSocketEventHandler;
import de.roderick.weberknecht.WebSocketMessage;

public class SampleOnXActivity extends Activity {
    static final String TAG = SampleOnXActivity.class.getSimpleName();
    private static String WS_URI = "ws://192.168.110.110:8001/";

    private AlertDialog mAlertDialog;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        final Handler handler = new Handler();

        if (pref.getString("IPADDRESS", "").equals("")) {
            // IPアドレス確認ダイアログ
            mAlertDialog = showAlertDialog();
            mAlertDialog.show();
        }
        WS_URI = "ws://" + pref.getString("IPADDRESS", "") + ":8001/";

        int status = (int) getIntent().getDoubleExtra("status", 0.0);
        Log.i(TAG, "status: " + status);

        new Thread(new Runnable() {
            @Override
            public void run() {
                connectWebSocket();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SampleOnXActivity.this, "AirCon ON!", Toast.LENGTH_SHORT)
                                .show();
                        finish();
                    }
                });
            }
        }).start();
    }

    private void connectWebSocket() {
        // WebSocket通信開始
        WebSocketManager.connect(WS_URI, new WebSocketEventHandler() {
            @Override
            public void onOpen() {
                Log.d(TAG, "websocket connect open");
                Msg msg = new Msg();
                Light light = new Light();
                msg.setCommand("");
                msg.setSender("android");
                msg.setCommand("light");
                light.setRed(255);
                light.setGreen(255);
                light.setBlue(255);
                msg.setLight(light);
                String message = JSON.encode(msg);
                WebSocketManager.send(message);
            }

            @Override
            public void onMessage(WebSocketMessage message) {
                Log.d(TAG, "websocket onMessage");
            }

            @Override
            public void onClose() {
                Log.d(TAG, "websocket connect close");
            }
        });
    }

    private AlertDialog showAlertDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View entryView = factory.inflate(R.layout.dialog_entry, null);
        final EditText edit = (EditText) entryView.findViewById(R.id.username_edit);

        edit.setHint("***.***.***.***");
        // キーハンドリング
        edit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // Enterキーハンドリング
                if (KeyEvent.KEYCODE_ENTER == keyCode) {
                    // 押したときに改行を挿入防止処理
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        return true;
                    }
                    // 離したときにダイアログ上の[OK]処理を実行
                    else if (KeyEvent.ACTION_UP == event.getAction()) {
                        if (edit != null && edit.length() != 0) {
                            // ここで[OK]が押されたときと同じ処理をさせます
                            String editStr = edit.getText().toString();
                            editor.putString("IPADDRESS", editStr);
                            editor.commit();
                            // AlertDialogを閉じます
                            mAlertDialog.dismiss();
                            finish();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // AlertDialog作成
        return new AlertDialog.Builder(this).setTitle("Server IP Address").setView(entryView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String editStr = edit.getText().toString();
                        // OKボタン押下時のハンドリング
                        editor.putString("IPADDRESS", editStr);
                        editor.commit();
                        finish();
                    }
                }).create();
    }
}
