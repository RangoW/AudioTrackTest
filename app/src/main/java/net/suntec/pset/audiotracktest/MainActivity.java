package net.suntec.pset.audiotracktest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

//import static net.suntec.pset.audiotracktest.AudioTrackManager.TAG;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private AudioTrackManager mATM;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: 1");
        mATM = AudioTrackManager.getInstance();
        mATM.start("/sdcard/test.pcm");
        Log.d(TAG, "onCreate: 2");

        Button btnPlay = (Button) findViewById(R.id.buttonPlay);
        Button btnPause = (Button) findViewById(R.id.buttonPause);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mATM.play()) {
                    Toast.makeText(getApplicationContext(),
                            "already Playing！", Toast.LENGTH_SHORT)
                        .show();
                }
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mATM.pause()) {
                    Toast.makeText(getApplicationContext(),
                            "already Paused！", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mATM.stopPlay();
    }
}