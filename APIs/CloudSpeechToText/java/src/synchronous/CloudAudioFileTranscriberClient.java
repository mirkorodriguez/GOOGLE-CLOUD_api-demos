package synchronous;

// Imports the Google Cloud client library
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;

import util.Util;
import static util.Constants.*;

import java.util.List;

// ~1 Minute Audio Length
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
				
		// Instantiates a client with
		try (SpeechClient speech = SpeechClient.create()) {
			// Builds the request for remote FLAC file
			RecognitionConfig config = RecognitionConfig.newBuilder()
														.setEncoding(AudioEncoding.LINEAR16)
														.setSampleRateHertz(AUDIO_HERTZ_8000)
														.setLanguageCode(LANG_ES_CO)
														.build();
			RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

			// Use blocking call for getting audio transcript
			RecognizeResponse response = speech.recognize(config, audio);
			List<SpeechRecognitionResult> results = response.getResultsList();

			for (SpeechRecognitionResult result : results) {
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcription: %s%n", alternative.getTranscript());
				System.out.printf("Confidence: %f%n", alternative.getConfidence());
			}
		}

	}
}
