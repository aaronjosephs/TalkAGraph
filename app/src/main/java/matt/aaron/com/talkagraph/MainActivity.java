package matt.aaron.com.talkagraph;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;

public class MainActivity extends AppCompatActivity {

  private final ServiceConnection serviceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
      speechService = SpeechService.from(binder);
      speechService.addListener(speechServiceListener);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      speechService = null;
    }

  };
  private final VoiceRecorder.Callback voiceRecorderCallback = new VoiceRecorder.Callback() {

    @Override
    public void onVoiceStart() {
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
      if (speechService != null) {
        speechService.finishRecognizing();
      }
    }

  };
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
  private static final String TAG = "MainActivity";
  private RecyclerView speakerRecyclerView;
  private SpeechService speechService;
  private VoiceRecorder voiceRecorder;
  private Switch recordingSwitch;
  private SpeakerListAdapter speakerListAdapter;
  private final SpeechService.Listener speechServiceListener =
      new SpeechService.Listener() {
        @Override
        public void onSpeechRecognized(final SpeechRecognitionAlternative alternative,
            final boolean isFinal) {
          runOnUiThread(() -> {
            // TODO I don't think we should run this on the UI thread since the adapter handles the actual UI changes
            speakerListAdapter.resetSpeakers();
            alternative.getWordsList().forEach(entry -> speakerListAdapter
                .addOrIncrementSpeaker(entry.getSpeakerTag(), 1, entry.getWord()));
            // we can probably notify per entry here
            speakerListAdapter.notifyDataSetChanged();
          });
        }
      };
  private RecyclerView.LayoutManager speakerListLayoutManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 20);
    }

    recordingSwitch = findViewById(R.id.recordingSwitch);
    recordingSwitch.setOnClickListener(v -> {
      if (recordingSwitch.isChecked()) {
        startRecording(v);
      } else {
        stopRecording(v);
      }
    });

    speakerRecyclerView = findViewById(R.id.speakerList);
    speakerRecyclerView.setHasFixedSize(false);

    speakerListLayoutManager = new LinearLayoutManager(this);
    speakerRecyclerView.setLayoutManager(speakerListLayoutManager);

    speakerListAdapter = new SpeakerListAdapter();
    speakerRecyclerView.setAdapter(speakerListAdapter);
  }

  @Override
  protected void onStart() {
    super.onStart();

    // Prepare Cloud Speech API
    bindService(new Intent(this, SpeechService.class), serviceConnection, BIND_AUTO_CREATE);

    // Start listening to voices
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
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

    // Stop
    if (speechService != null) {
      speechService.removeListener(speechServiceListener);
      unbindService(serviceConnection);
      speechService = null;
    }

    super.onStop();
  }

  public void startRecording(View view) {
    AsyncTask.execute(() -> {
      if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
          != PackageManager.PERMISSION_GRANTED) {
        //  maybe show a toast here or something
        return;
      }
      if (voiceRecorder != null) {
        voiceRecorder.stop();
      }
      voiceRecorder = new VoiceRecorder(voiceRecorderCallback);
      voiceRecorder.start();
    });
  }

  public void stopRecording(View view) {
    AsyncTask.execute(() -> {
      if (voiceRecorder != null) {
        voiceRecorder.stop();
        voiceRecorder = null;
      }
    });
  }
}
