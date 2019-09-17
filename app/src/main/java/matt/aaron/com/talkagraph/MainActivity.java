package matt.aaron.com.talkagraph;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
  private static final String TAG = "MainActivity";
  final Map<Integer, Integer> speakerMap = new HashMap<>();
  int totalWordCount = 0;
  private SpeechService speechService;
  private VoiceRecorder voiceRecorder;
  private final SpeechService.Listener speechServiceListener =
      new SpeechService.Listener() {
        @Override
        public void onSpeechRecognized(final SpeechRecognitionAlternative alternative,
            final boolean isFinal) {
          if (isFinal) {
            voiceRecorder.dismiss();
          }

          totalWordCount += alternative.getWordsCount();
          alternative.getWordsList().forEach(entry -> {
            int tag = entry.getSpeakerTag();
            if (speakerMap.containsKey(tag)) {
              speakerMap.put(tag, speakerMap.get(tag) + 1);
            } else {
              speakerMap.put(tag, 1);
            }
          });
          runOnUiThread(MainActivity.this::updatePercents);
        }
      };
  private final ServiceConnection serviceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
      speechService = SpeechService.from(binder);
      speechService.addListener(speechServiceListener);
      // mStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      speechService = null;
    }

  };
  private final VoiceRecorder.Callback voiceRecorderCallback = new VoiceRecorder.Callback() {

    @Override
    public void onVoiceStart() {
      // showStatus(true);
      if (speechService != null) {
        Log.e(TAG, "Start recognizing");
        speechService.startRecognizing(voiceRecorder.getSampleRate());
      }
    }

    @Override
    public void onVoice(byte[] data, int size) {
      if (speechService != null) {
        speechService.recognize(data, size);
      }
    }

    @Override
    public void onVoiceEnd() {
      // showStatus(false);
      if (speechService != null) {
        speechService.finishRecognizing();
      }
    }

  };

  private TextView speaker1;
  private TextView speaker2;
  private TextView speaker3;
  private TextView speaker4;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Record james' stupid voice
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 20);
    }

    speaker1 = findViewById(R.id.speaker1);
    speaker2 = findViewById(R.id.speaker2);
    speaker3 = findViewById(R.id.speaker3);
    speaker4 = findViewById(R.id.speaker4);

  }

  @Override
  protected void onStart() {
    super.onStart();

    // Prepare Cloud Speech API
    bindService(new Intent(this, SpeechService.class), serviceConnection, BIND_AUTO_CREATE);

    // Start listening to voices
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED) {
      //   startVoiceRecorder();
      // } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
      //     Manifest.permission.RECORD_AUDIO)) {
      //   showPermissionMessageDialog();
    } else {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
          REQUEST_RECORD_AUDIO_PERMISSION);
    }
  }

  @Override
  protected void onStop() {
    // Stop listening to voice
    if (voiceRecorder != null) {
      voiceRecorder.stop();
      voiceRecorder = null;
    }
    // Stop Cloud Speech API
    speechService.removeListener(speechServiceListener);
    unbindService(serviceConnection);
    speechService = null;

    super.onStop();
  }

  public void startRecording(View view) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      //  maybe show a toast here or something
      return;
    }
    if (voiceRecorder != null) {
      voiceRecorder.stop();
    }
    voiceRecorder = new VoiceRecorder(voiceRecorderCallback);
    voiceRecorder.start();
  }

  public void stopRecording(View view) {
    if (voiceRecorder != null) {
      voiceRecorder.stop();
      voiceRecorder = null;
    }
  }

  private void updatePercents() {
    // we're just doing word count for now we can do elapsed time later
    if (speakerMap.size() > 4) {
      Toast toast = Toast.makeText(getApplicationContext(),
          "More than 4 speakers detected",
          Toast.LENGTH_SHORT);
      toast.show();
    } else if (speakerMap.isEmpty()) {
      return;
    }
    // TODO make this more dynamic to support arbitrary speaker count
    speaker1.setText(String.format("Speaker %d spoke %f percent", 1,
        ((double) speakerMap.getOrDefault(1, 0) / totalWordCount) * 100));
    speaker2.setText(String.format("Speaker %d spoke %f percent", 2,
        ((double) speakerMap.getOrDefault(2, 0) / totalWordCount) * 100));
    speaker3.setText(String.format("Speaker %d spoke %f percent", 3,
        ((double) speakerMap.getOrDefault(3, 0) / totalWordCount) * 100));
    speaker4.setText(String.format("Speaker %d spoke %f percent", 4,
        ((double) speakerMap.getOrDefault(4, 0) / totalWordCount) * 100));
  }
}
