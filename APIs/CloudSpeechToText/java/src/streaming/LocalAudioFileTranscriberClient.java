package streaming;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.common.util.concurrent.SettableFuture;
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

		Path path = Paths.get(loader.getResource(localFileName).getPath());
		byte[] data = Files.readAllBytes(path);

		try (SpeechClient speech = SpeechClient.create()) {

			// Configure request with local raw PCM audio
			RecognitionConfig recConfig = RecognitionConfig.newBuilder()
															.setEncoding(AudioEncoding.LINEAR16)
															.setSampleRateHertz(AUDIO_HERTZ_8000)
															.setLanguageCode(LANG_ES_CO)
															.setModel("default")
															.build();

			StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder().setConfig(recConfig).build();

			class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
				private final SettableFuture<List<T>> future = SettableFuture.create();
				private final List<T> messages = new java.util.ArrayList<T>();

				@Override
				public void onNext(T message) {
					messages.add(message);
				}

				@Override
				public void onError(Throwable t) {
					future.setException(t);
				}

				@Override
				public void onCompleted() {
					future.set(messages);
				}

				// Returns the SettableFuture object to get received messages / exceptions.
				public SettableFuture<List<T>> future() {
					return future;
				}
			}

			ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver = new ResponseApiStreamingObserver<>();

			BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speech
					.streamingRecognizeCallable();

			ApiStreamObserver<StreamingRecognizeRequest> requestObserver = callable.bidiStreamingCall(responseObserver);

			// The first request must **only** contain the audio configuration:
			requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(config).build());

			// Subsequent requests must **only** contain the audio data.
			requestObserver
					.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data)).build());

			// Mark transmission as completed after sending the data.
			requestObserver.onCompleted();

			List<StreamingRecognizeResponse> responses = responseObserver.future().get();

			for (StreamingRecognizeResponse response : responses) {
				// For streaming recognize, the results list has one is_final result (if
				// available) followed
				// by a number of in-progress results (if iterim_results is true) for subsequent
				// utterances.
				// Just print the first result here.
				StreamingRecognitionResult result = response.getResultsList().get(0);
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcript : %s\n", alternative.getTranscript());
				System.out.printf("Confidence : %f\n", alternative.getConfidence());
			}
		}

	}

}
