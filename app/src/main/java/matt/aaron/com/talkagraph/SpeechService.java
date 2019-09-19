package matt.aaron.com.talkagraph;

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechSettings;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


// https://github.com/GoogleCloudPlatform/android-docs-samples/blob/master/speech/SpeechRecognitionClient/app/src/main/java/com/google/cloud/examples/speechrecognition/MainActivity.kt
// get rid of speech grpc by using that
public class SpeechService extends Service {

  private static final String TAG = "SpeechService";
  private final SpeechBinder mBinder = new SpeechBinder();
  private final ArrayList<Listener> mListeners = new ArrayList<>();
  StreamingRecognizeRequest request;
  private ClientStream<StreamingRecognizeRequest> clientStream;
  private SpeechClient speechClient;
  private AtomicBoolean stoppedRecording = new AtomicBoolean(false);

  public static SpeechService from(IBinder binder) {
    return ((SpeechBinder) binder).getService();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    InputStream stream = this.getResources().openRawResource(R.raw.credentials);
    try {
      GoogleCredentials credential = GoogleCredentials.fromStream(stream);
      CredentialsProvider credentialsProvider = () -> credential;
      speechClient = SpeechClient
          .create(SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    speechClient.shutdown();
  }

  private String getDefaultLanguageCode() {
    final Locale locale = Locale.getDefault();
    final StringBuilder language = new StringBuilder(locale.getLanguage());
    final String country = locale.getCountry();
    if (!TextUtils.isEmpty(country)) {
      language.append("-");
      language.append(country);
    }
    return language.toString();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public void addListener(@NonNull Listener listener) {
    mListeners.add(listener);
  }

  public void removeListener(@NonNull Listener listener) {
    mListeners.remove(listener);
  }

  /**
   * Starts recognizing speech audio.
   *
   * @param sampleRate The sample rate of the audio.
   */
  public void startRecognizing(int sampleRate) {
    request = StreamingRecognizeRequest.newBuilder()
        .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
            .setConfig(RecognitionConfig.newBuilder()
                .setLanguageCode(getDefaultLanguageCode())
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(sampleRate)
                .setDiarizationSpeakerCount(4) // estimated count?
                .setEnableSpeakerDiarization(true)
                .build())
            .setInterimResults(true)
            .setSingleUtterance(false)
            .build())
        .build();

    ResponseObserver<StreamingRecognizeResponse> observer =
        new ResponseObserver<StreamingRecognizeResponse>() {
          @Override
          public void onStart(StreamController controller) {
            // no-op
          }

          @Override
          public void onResponse(StreamingRecognizeResponse response) {
            Log.d(TAG, response.toString());
            for (StreamingRecognitionResult result : response.getResultsList()) {
              if (result.getIsFinal()) {
                Optional<SpeechRecognitionAlternative> alternative = result.getAlternativesList()
                    .stream().findFirst();
                alternative.ifPresent(alt -> {
                  for (Listener listener : mListeners) {
                    listener.onSpeechRecognized(alt, true);
                  }
                });
              }
            }
          }

          @Override
          public void onComplete() {
          }

          @Override
          public void onError(Throwable t) {
            Log.i(TAG, "API completed.");
          }
        };
    clientStream = speechClient.streamingRecognizeCallable().splitCall(observer);
    clientStream.send(request);
  }

  /**
   * Recognizes the speech audio. This method should be called every time a chunk of byte buffer is
   * ready.
   *
   * @param data The audio data.
   * @param size The number of elements that are actually relevant in the {@code data}.
   */
  public void recognize(byte[] data, int size) {
    Log.d(TAG, ByteString.copyFrom(data).toString());
    clientStream.send(StreamingRecognizeRequest.newBuilder()
        .setAudioContent(ByteString.copyFrom(data, 0, size))
        .build());
  }

  /**
   * Finishes recognizing speech audio.
   */
  public void finishRecognizing() {
    stoppedRecording.set(true);
    // Can we send another request indicating speech is over?
    clientStream.closeSend();
  }

  public interface Listener {

    /**
     * Called when a new piece of text was recognized by the Speech API.
     *
     * @param text The text.
     * @param isFinal {@code true} when the API finished processing audio.
     */
    void onSpeechRecognized(SpeechRecognitionAlternative text, boolean isFinal);
  }

  private class SpeechBinder extends Binder {

    SpeechService getService() {
      return SpeechService.this;
    }
  }
}
