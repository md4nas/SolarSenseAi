package com.example.solarsenseapp.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.solarsenseapp.MainActivity;
import com.example.solarsenseapp.utils.Constants;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceController {
    private static final String TAG = "VoiceController";

    private final MainActivity activity;
    private final Button btnVoice;
    private final ProgressBar voiceProgressBar;
    private final TextView txtVoiceCommands;
    private final Button btnShowCommands;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private VoiceCommandCallback voiceCommandCallback;

    public interface VoiceCommandCallback {
        void onVoiceCommand(String command);
    }

    public VoiceController(MainActivity activity, Button btnVoice, ProgressBar voiceProgressBar,
                           TextView txtVoiceCommands, Button btnShowCommands) {
        this.activity = activity;
        this.btnVoice = btnVoice;
        this.voiceProgressBar = voiceProgressBar;
        this.txtVoiceCommands = txtVoiceCommands;
        this.btnShowCommands = btnShowCommands;

        setupVoiceRecognition();
        setupUI();
    }

    public void setVoiceCommandCallback(VoiceCommandCallback callback) {
        this.voiceCommandCallback = callback;
    }

    private void setupVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            btnVoice.setEnabled(false);
            activity.showToast("Voice recognition not available on this device");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new VoiceRecognitionListener());

        btnVoice.setOnClickListener(v -> {
            if (!activity.isAutoMode()) {
                startListening();
            }
        });
    }

    private void setupUI() {
        txtVoiceCommands.setText(Constants.VOICE_COMMANDS_HELP);
        txtVoiceCommands.setVisibility(View.GONE);

        btnShowCommands.setOnClickListener(v -> {
            boolean isVisible = txtVoiceCommands.getVisibility() == View.VISIBLE;
            txtVoiceCommands.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            btnShowCommands.setText(isVisible ? "❓ Voice Commands" : "⬆️ Hide Voice Commands");
        });
    }

    private void startListening() {
        voiceProgressBar.setVisibility(View.VISIBLE);
        btnVoice.setText("Listening...");
        btnVoice.setBackgroundColor(activity.getColor(android.R.color.holo_red_dark));
        speechRecognizer.startListening(speechIntent);
    }

    private void stopListening() {
        voiceProgressBar.setVisibility(View.GONE);
        btnVoice.setText("🎤 Voice Command");
        btnVoice.setBackgroundColor(activity.getColor(android.R.color.holo_purple));
    }

    private class VoiceRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {}

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}

        @Override
        public void onError(int error) {
            activity.runOnUiThread(() -> {
                stopListening();
                activity.showToast("Voice error: " + getErrorText(error));
            });
        }

        @Override
        public void onResults(Bundle results) {
            activity.runOnUiThread(() -> {
                stopListening();

                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && voiceCommandCallback != null) {
                    voiceCommandCallback.onVoiceCommand(matches.get(0).toLowerCase());
                }
            });
        }
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "RecognitionService busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }

    public void cleanup() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}