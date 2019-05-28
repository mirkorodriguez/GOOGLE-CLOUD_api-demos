package asynchronous;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
// Imports the Google Cloud client library
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;

import synchronous.LocalAudioFileTranscriberClient;

import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;

import util.Util;
import static util.Constants.*;

import java.util.List;

// ~480 Minutes | Audio longer than ~1 minute must use the uri field to reference an audio file in Google Cloud Storage.
public class CloudAudioFileTranscriberClient {

	/**
	 * Demonstrates using the Speech API to transcribe an audio file.
	 */
	public static void main(String... args) throws Exception {

		//Classloader
		ClassLoader loader = LocalAudioFileTranscriberClient.class.getClassLoader();

		// The path to the google key json file
		String keyJsonFileName = KEY_JSON_FILE_NAME;
		
		// Setting ENV variable GOOGLE_APPLICATION_CREDENTIALS
		Util.injectEnvironmentVariable(KEY,loader.getResource(keyJsonFileName).getPath());

		//Audio file on Google Storage:
		String gcsUri = AUDIO_FILE_NAME_GS;

		try (SpeechClient speech = SpeechClient.create()) {

		    // Configure remote file request for Linear16
		    RecognitionConfig config = RecognitionConfig.newBuilder()
														.setEncoding(AudioEncoding.LINEAR16)
														.setSampleRateHertz(AUDIO_HERTZ_8000)
														.setLanguageCode(LANG_ES_CO)
														.build();
		    RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

		    // Use non-blocking call for getting file transcription
		    OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
		        speech.longRunningRecognizeAsync(config, audio);
		    while (!response.isDone()) {
		      System.out.println("Waiting for response...");
		      Thread.sleep(10000);
		    }

		    List<SpeechRecognitionResult> results = response.get().getResultsList();

		    for (SpeechRecognitionResult result : results) {
		      // There can be several alternative transcripts for a given chunk of speech. Just use the
		      // first (most likely) one here.
		      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
		      System.out.printf("Transcription: %s\n", alternative.getTranscript());
		      System.out.printf("Confidence: %f\n", alternative.getConfidence());
		    }
		  }

	}
}
