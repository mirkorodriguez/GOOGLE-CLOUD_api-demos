package streaming;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.TargetDataLine;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import util.Util;

import static util.Constants.*;

//~1 Minute Audio Length
public class StreamAudioFileTranscriberClient {

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

		//Streaming from local Microphone
		streamingMicRecognize();
	}

	/** Performs microphone streaming speech recognition with a duration of 1 minute. */
	public static void streamingMicRecognize() throws Exception {

	  ResponseObserver<StreamingRecognizeResponse> responseObserver = null;
	  try (SpeechClient client = SpeechClient.create()) {

	    responseObserver =
	        new ResponseObserver<StreamingRecognizeResponse>() {
	          ArrayList<StreamingRecognizeResponse> responses = new ArrayList<>();

	          public void onStart(StreamController controller) {}

	          public void onResponse(StreamingRecognizeResponse response) {
	            responses.add(response);
	          }

	          public void onComplete() {
	            for (StreamingRecognizeResponse response : responses) {
	              StreamingRecognitionResult result = response.getResultsList().get(0);
	              SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
	              System.out.printf("Transcript : %s\n", alternative.getTranscript());
	              System.out.printf("Confidence : %f\n", alternative.getConfidence());
	            }
	          }

	          public void onError(Throwable t) {
	            System.out.println(t);
	          }
	        };

	    ClientStream<StreamingRecognizeRequest> clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);

	    RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
													            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
													            .setLanguageCode(LANG_ES_CO)
													            .setSampleRateHertz(AUDIO_HERTZ_16000)
													            .build();
	    StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
	    																					.setConfig(recognitionConfig)
	    																					.build();

	    StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
															    		.setStreamingConfig(streamingRecognitionConfig)
															            .build(); // The first request in a streaming call has to be a config

	    clientStream.send(request);

	    // SampleRate:16000Hz, SampleSizeInBits: 16, Number of channels: 1, Signed: true, bigEndian: false
	    AudioFormat audioFormat = new AudioFormat(AUDIO_HERTZ_16000, AUDIO_SIZE_IN_BITS_16, 1, true, false);
	    DataLine.Info targetInfo = new Info(TargetDataLine.class, audioFormat); // Set the system information to read from the microphone audio stream

	    if (!AudioSystem.isLineSupported(targetInfo)) {
	      System.out.println("Microphone not supported");
	      System.exit(0);
	    }
	    
	    // Target data line captures the audio stream the microphone produces.
	    TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
	    targetDataLine.open(audioFormat);
	    targetDataLine.start();
	    System.out.println("Start speaking ...");
	    long startTime = System.currentTimeMillis();
	    
	    // Audio Input Stream
	    AudioInputStream audio = new AudioInputStream(targetDataLine);
	    while (true) {
	      long estimatedTime = System.currentTimeMillis() - startTime;
	      byte[] data = new byte[6400];
	      audio.read(data);
	      if (estimatedTime > WAITING_FOR_SPEAKING_60SEC) { // 60 seconds
	        System.out.println("Stop speaking and wait ... ");
	        targetDataLine.stop();
	        targetDataLine.close();
	        break;
	      }
	      request = StreamingRecognizeRequest.newBuilder()
								             .setAudioContent(ByteString.copyFrom(data))
								             .build();
	      clientStream.send(request);
	    }
	  } catch (Exception e) {
	    System.out.println(e);
	  }
	  responseObserver.onComplete();
	}

	
}
