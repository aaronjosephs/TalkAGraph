package matt.aaron.com.talkagraph;

public class Speaker {

  private int speakerTag;
  private int speakerWordCount;
  private StringBuilder speakerSample;

  Speaker(int speakerTag, String firstWord) {
    this.speakerTag = speakerTag;
    this.speakerSample = new StringBuilder(firstWord);
    this.speakerWordCount = 0;
  }

  public int getSpeakerTag() {
    return speakerTag;
  }

  public double getSpeakerPercent(int totalWordCount) {
    return 100.0 * ((float) speakerWordCount / totalWordCount);
  }

  public String getSpeakerSample() {
    return speakerSample.toString();
  }

  public void addWords(int wordCount, String word) {
    speakerWordCount += wordCount;
    if (word != null) {
      speakerSample.append(" ").append(word);
    }
  }
}
