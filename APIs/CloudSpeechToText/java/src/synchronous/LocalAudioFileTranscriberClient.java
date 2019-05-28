package synchronous;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

// Imports the Google Cloud client library
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import util.Util;
import static util.Constants.*;

//~1 Minute Audio Length
public class LocalAudioFileTranscriberClient {

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

		// The path to the audio file to transcribe
		String localFileName = AUDIO_FILE_NAME_LOCAL;

		// Instantiates a client
		try (SpeechClient speechClient = SpeechClient.create()) {
			
			// Reads the audio file into memory
			Path path = Paths.get(loader.getResource(localFileName).getPath());
			byte[] data = Files.readAllBytes(path);
			ByteString audioBytes = ByteString.copyFrom(data);

			// Builds the sync recognize request
			RecognitionConfig config = RecognitionConfig.newBuilder()
														.setEncoding(AudioEncoding.LINEAR16)
														.setSampleRateHertz(AUDIO_HERTZ_8000)
														.setLanguageCode(LANG_ES_CO)
														.build();
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

			// Performs speech recognition on the audio file
			RecognizeResponse response = speechClient.recognize(config, audio);
			List<SpeechRecognitionResult> results = response.getResultsList();

			for (SpeechRecognitionResult result : results) {
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcription: %s%n", alternative.getTranscript());
				System.out.printf("Confidence: %f%n", alternative.getConfidence());
			}
		}
	}
}
