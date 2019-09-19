package matt.aaron.com.talkagraph;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import matt.aaron.com.talkagraph.SpeakerListAdapter.SpeakerViewHolder;

public class SpeakerListAdapter extends RecyclerView.Adapter<SpeakerViewHolder> {

  private final List<Speaker> speakers;
  private int totalWordCount;

  public SpeakerListAdapter() {
    this.speakers = new ArrayList<>();
    this.totalWordCount = 0;
  }

  public void addOrIncrementSpeaker(int tag, int wordCount, String word) {
    totalWordCount += wordCount;

    for (Speaker speaker : speakers) {
      if (speaker.getSpeakerTag() == tag) {
        speaker.addWords(wordCount, word);
        return;
      }
    }
    Speaker speaker = new Speaker(tag, word);
    speaker.addWords(wordCount, null);
    speakers.add(speaker);
  }

  public void resetSpeakers() {
    speakers.clear();
  }

  @NonNull
  @Override
  public SpeakerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
    View speakerCard = LayoutInflater.from(viewGroup.getContext())
        .inflate(R.layout.speaker_view, viewGroup, false);
    return new SpeakerViewHolder(speakerCard);
  }

  @Override
  public void onBindViewHolder(@NonNull SpeakerViewHolder speakerViewHolder, int i) {
    Speaker speaker = speakers.get(i);

    speakerViewHolder.speakerTagView.setText(String.format("Speaker %d", speaker.getSpeakerTag()));
    speakerViewHolder.speakerPercentTextView
        .setText(String.format("Spoke %.2f percent", speaker.getSpeakerPercent(totalWordCount)));
    speakerViewHolder.sampleSpeechTextView.setText(speaker.getSpeakerSample());
  }

  @Override
  public int getItemCount() {
    return speakers.size();
  }

  static class SpeakerViewHolder extends RecyclerView.ViewHolder {

    TextView speakerTagView;
    TextView speakerPercentTextView;
    TextView sampleSpeechTextView;

    SpeakerViewHolder(@NonNull View itemView) {
      super(itemView);

      speakerTagView = itemView.findViewById(R.id.speakerTag);
      speakerPercentTextView = itemView.findViewById(R.id.speakerPercent);
      sampleSpeechTextView = itemView.findViewById(R.id.speakerSampleSpeech);
    }
  }
}
