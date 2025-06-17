package com.ck.music_app.utils;

import android.content.Context;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiService {
    private static final String TAG = "GeminiService";
    private static final String API_KEY = "AIzaSyDEgWFtoagGB-F5lwAPpw751IRFlaEoljU";
    
    private GenerativeModelFutures model;
    private Executor executor;
    
    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public GeminiService() {
        // Khởi tạo Gemini model
        GenerativeModel gm = new GenerativeModel(
            "gemini-pro", 
            API_KEY
        );
        model = GenerativeModelFutures.from(gm);
        executor = Executors.newSingleThreadExecutor();
    }
    
    public void getTrendingSongs(String country, GeminiCallback callback) {
        try {
            Log.d(TAG, "Starting Gemini request for country: " + country);
            String prompt = createTrendingSongsPrompt(country);
            Log.d(TAG, "Prompt: " + prompt);
            
            Content content = new Content.Builder()
                .addText(prompt)
                .build();
                
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String responseText = result.getText();
                        Log.d(TAG, "Gemini response received: " + responseText);
                        callback.onSuccess(responseText);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response: " + e.getMessage());
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
                
                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Error from Gemini API: " + t.getMessage());
                    t.printStackTrace();
                    callback.onError("API Error: " + t.getMessage());
                }
            }, executor);
            
        } catch (Exception e) {
            Log.e(TAG, "Exception in getTrendingSongs: " + e.getMessage());
            e.printStackTrace();
            callback.onError("Service Error: " + e.getMessage());
        }
    }
    
    private String createTrendingSongsPrompt(String country) {
        return "Hãy đưa ra danh sách 8 bài hát thịnh hành nhất hiện tại của " + country + 
               ". Vui lòng trả về theo định dạng JSON với cấu trúc sau:\n" +
               "{\n" +
               "  \"songs\": [\n" +
               "    {\n" +
               "      \"title\": \"Tên bài hát\",\n" +
               "      \"artist\": \"Tên ca sĩ\",\n" +
               "      \"rank\": 1\n" +
               "    }\n" +
               "  ]\n" +
               "}\n" +
               "Chỉ trả về JSON, không cần giải thích thêm.";
    }
} 