package matt.aaron.com.talkagraph;

import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.api.gax.rpc.BidiStream;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.WordInfo;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

  MediaRecorder mediaRecorder;
  String fileName = "./resources/audio.raw";

  private TextView speaker1;
  private TextView speaker2;
  private TextView speaker3;
  private TextView speaker4;

  private static RecognitionConfig buildDiarizationConfig() {
    return RecognitionConfig.newBuilder()
        // TODO the only encodings they have in common
        .setEncoding(AudioEncoding.AMR_WB) // LINEAR_16
        .setSampleRateHertz(16000)
        .setLanguageCode("en-US")
        // This is just the esimated number of speakers in the convo
        .setDiarizationSpeakerCount(4)
        .setEnableSpeakerDiarization(true)
        // Some newer api that doesn't seem to be finished yet
        // .setDiarizationConfig(SpeakerDiarizationConfig.newBuilder().setEnableSpeakerDiarization(true).setMaxSpeakerCount()build())
        .build();
  }

  private static StreamingRecognizeRequest buildStreamingRequest(ByteString audioBytes) {
    StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
        .setConfig(buildDiarizationConfig()).build();
    return StreamingRecognizeRequest.newBuilder().setAudioContent(audioBytes)
        .setStreamingConfig(streamingConfig).build();
  }

  private static RecognizeResponse getRecognizeResponse(RecognitionConfig config,
      ByteString audio) {
    try (SpeechClient speechClient = SpeechClient.create()) {
      // Get the contents of the local audio file
      RecognitionAudio recognitionAudio =
          RecognitionAudio.newBuilder().setContent(audio).build();

      // Perform the transcription request
      return speechClient.recognize(config, recognitionAudio);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  //TODO streaming stuff
  // get normal working first
  private static void unused() {
    // Instantiates a client
    try (SpeechClient speechClient = SpeechClient.create()) {
      // Reads the audio file into memory
      Path path = Paths.get("");
      byte[] data = Files.readAllBytes(path);
      ByteString audioBytes = ByteString.copyFrom(data);

      StreamingRecognizeRequest streamingRequest = buildStreamingRequest(audioBytes);

      // Builds the sync recognize request

      // StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder().setConfig(config).build();
      //
      // StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build();

      // RecognitionAudio audio = RecognitionAudio.newBuilder()
      //     .setContent(audioBytes)
      //     .build();

      // Performs speech recognition on the audio file
      // RecognizeResponse response = speechClient.recognize(config, audio);
      // List<SpeechRecognitionResult> results = response.getResultsList();

      BidiStream<StreamingRecognizeRequest, StreamingRecognizeResponse> observer = speechClient
          .streamingRecognizeCallable().call();

      observer.send(streamingRequest);

      for (StreamingRecognizeResponse response : observer) {
        for (StreamingRecognitionResult result : response.getResultsList()) {
          for (WordInfo wordInfo : result.getAlternatives(0).getWordsList()) {

            wordInfo.getSpeakerTag(); // Assign the int to a name later
          }
        }
      }

      // for (SpeechRecognitionResult result : results) {
      //   // There can be several alternative transcripts for a given chunk of speech. Just use the
      //   // first (most likely) one here.
      //   SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      //   System.out.printf("Transcription: %s%n", alternative.getTranscript());
      //   alternative.getWords(1).getSpeakerTag();
      // }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Record jame's stupid voice
    mediaRecorder = new MediaRecorder();
    mediaRecorder.setAudioSource(AudioSource.MIC);
    // Is this the right format for cloud to speech?
    mediaRecorder.setOutputFormat(OutputFormat.MPEG_2_TS);
    mediaRecorder.setOutputFile(new File(fileName));
    // TODO what encoder to use????
    mediaRecorder.setAudioEncoder(AudioEncoder.AMR_WB);
    try {
      mediaRecorder.prepare();
    } catch (IOException e) {
      // TODO handle exception better
      e.printStackTrace();
    }

    setContentView(R.layout.activity_main);

    speaker1 = findViewById(R.id.speaker1);
    speaker2 = findViewById(R.id.speaker2);
    speaker3 = findViewById(R.id.speaker3);
    speaker4 = findViewById(R.id.speaker4);
  }

  public void startRecording(View view) {
    mediaRecorder.start();
  }

  public void stopRecording(View view) {
    mediaRecorder.stop();
    Path path = Paths.get("");
    byte[] data;
    try {
      data = Files.readAllBytes(path);

      ByteString audioBytes = ByteString.copyFrom(data);
      RecognizeResponse response = getRecognizeResponse(buildDiarizationConfig(), audioBytes);

      updatePercents(response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private void updatePercents(RecognizeResponse response) {
    // we're just doing word count for now we can do elapsed time later
    final Map<Integer, Integer> speakerMap = new HashMap<>();
    int totalWordCount = 0;
    for (SpeechRecognitionResult result : response.getResultsList()) {
      // There can be several alternative transcripts for a given chunk of speech. Just
      // use the first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternatives(0);
      // The words array contains the entire transcript up until that point.
      // Referencing the last spoken word to get the associated Speaker tag

      totalWordCount += alternative.getWordsCount();
      alternative.getWordsList().forEach(entry -> {
        int tag = entry.getSpeakerTag();
        if (speakerMap.containsKey(tag)) {
          speakerMap.put(tag, speakerMap.get(tag) + 1);
        } else {
          speakerMap.put(tag, 1);
        }
      });
    }
    if (speakerMap.size() > 4) {
      Toast toast = Toast.makeText(getApplicationContext(),
          "More than 4 speakers detected",
          Toast.LENGTH_SHORT);
      toast.show();
    }
    // TODO make this more dynamic to support arbitrary speaker count
    speaker1.setText(String.format("Speaker %d spoke %f percent", 1,
        ((double) speakerMap.get(1) / totalWordCount) * 100));
    speaker2.setText(String.format("Speaker %d spoke %f percent", 2,
        ((double) speakerMap.get(2) / totalWordCount) * 100));
    speaker3.setText(String.format("Speaker %d spoke %f percent", 3,
        ((double) speakerMap.get(3) / totalWordCount) * 100));
    speaker4.setText(String.format("Speaker %d spoke %f percent", 4,
        ((double) speakerMap.get(4) / totalWordCount) * 100));
  }
}
