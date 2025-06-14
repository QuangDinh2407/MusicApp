package com.ck.music_app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceSearchHelper {

    private static final String TAG = "VoiceSearchHelper";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;

    private Context context;
    private SpeechRecognizer speechRecognizer;
    private VoiceSearchListener listener;
    private boolean isListening = false;

    public interface VoiceSearchListener {
        void onVoiceSearchStart();
        
        void onVoiceSearchListening(); // Thông báo đã bắt đầu lắng nghe
        
        void onVoiceDetected(); // Thông báo đã nhận được giọng nói

        void onVoiceSearchResult(String result);

        void onVoiceSearchError(String error);

        void onVoiceSearchEnd();
    }

    public VoiceSearchHelper(Context context, VoiceSearchListener listener) {
        this.context = context;
        this.listener = listener;
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                    isListening = true;
                    if (listener != null) {
                        listener.onVoiceSearchStart();
                        listener.onVoiceSearchListening(); // Thông báo đã sẵn sàng lắng nghe
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech");
                    if (listener != null) {
                        listener.onVoiceDetected(); // Thông báo đã nhận được giọng nói
                    }
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Volume level changed
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Audio buffer received
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "End of speech");
                    isListening = false;
                }

                @Override
                public void onError(int error) {
                    Log.e(TAG, "Speech recognition error: " + error);
                    isListening = false;

                    String errorMessage = getErrorMessage(error);
                    if (listener != null) {
                        listener.onVoiceSearchError(errorMessage);
                        listener.onVoiceSearchEnd();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    Log.d(TAG, "Speech recognition results received");
                    isListening = false;

                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String result = matches.get(0);
                        Log.d(TAG, "Recognition result: " + result);

                        if (listener != null) {
                            listener.onVoiceSearchResult(result);
                            listener.onVoiceSearchEnd();
                        }
                    } else {
                        if (listener != null) {
                            listener.onVoiceSearchError("Không nhận diện được giọng nói");
                            listener.onVoiceSearchEnd();
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // Partial results (optional)
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Speech recognition events
                }
            });
        } else {
            Log.e(TAG, "Speech recognition not available on this device");
        }
    }

    public boolean checkPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.RECORD_AUDIO },
                PERMISSION_REQUEST_RECORD_AUDIO);
    }

    public void startListening() {
        if (!checkPermission()) {
            if (listener != null) {
                listener.onVoiceSearchError("Cần cấp quyền ghi âm để sử dụng tính năng này");
            }
            return;
        }

        if (speechRecognizer == null) {
            if (listener != null) {
                listener.onVoiceSearchError("Thiết bị không hỗ trợ nhận diện giọng nói");
            }
            return;
        }

        if (isListening) {
            Log.d(TAG, "Already listening");
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN"); // Vietnamese
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói tên bài hát hoặc nghệ sĩ...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        try {
            speechRecognizer.startListening(intent);
            Log.d(TAG, "Started listening");
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition", e);
            if (listener != null) {
                listener.onVoiceSearchError("Lỗi khi bắt đầu nhận diện giọng nói");
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "Stopped listening");
        }
    }

    public boolean isListening() {
        return isListening;
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        isListening = false;
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Lỗi âm thanh";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Lỗi client";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Không có quyền ghi âm";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Lỗi mạng";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Hết thời gian kết nối mạng";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Không nhận diện được giọng nói";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Nhận diện giọng nói đang bận";
            case SpeechRecognizer.ERROR_SERVER:
                return "Lỗi server";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Không nghe thấy giọng nói";
            default:
                return "Lỗi không xác định";
        }
    }

    public static boolean isVoiceSearchAvailable(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }
}