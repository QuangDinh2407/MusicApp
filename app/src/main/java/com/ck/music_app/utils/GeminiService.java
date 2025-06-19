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
    private static final String API_KEY = "AIzaSyAoeRddVfm6laa-vX3SkyUfo-XgltlKy7M";
    
    private GenerativeModelFutures model;
    private Executor executor;
    
    //Khi gửi yêu cầu đến AI, chương trình không chờ kết quả mà tiếp tục chạy, có kết quả hiển thị ra sau
    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    //Tạo service làm việc với API
    public GeminiService() {
        // Khởi tạo Gemini model với model mới
        GenerativeModel gm = new GenerativeModel(
            "gemini-2.0-flash",  // Sử dụng model flash thay vì pro
            API_KEY
        );
        model = GenerativeModelFutures.from(gm);
        executor = Executors.newSingleThreadExecutor();
    }

    //Gửi request
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
                        Log.d(TAG, "Raw Gemini response: " + responseText);
                        
                        // Tìm và trích xuất phần JSON từ response
                        String jsonStr = extractJsonFromResponse(responseText);
                        Log.d(TAG, "Extracted JSON: " + jsonStr);
                        
                        callback.onSuccess(jsonStr);
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
    
    private String extractJsonFromResponse(String response) {
        // Tìm vị trí bắt đầu của JSON (ký tự '{')
        int startIndex = response.indexOf("{");
        // Tìm vị trí kết thúc của JSON (ký tự '}' cuối cùng)
        int endIndex = response.lastIndexOf("}") + 1;
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex);
        }
        
        throw new IllegalArgumentException("Không tìm thấy JSON hợp lệ trong response");
    }
    
    private String createTrendingSongsPrompt(String country) {
        return "Hãy liệt kê 8 bài hát đang thịnh hành nhất ở " + country + " hiện nay. " +
               "QUAN TRỌNG: CHỈ trả về JSON, không có text giải thích nào khác:\n" +
               "{\n" +
               "  \"songs\": [\n" +
               "    {\n" +
               "      \"title\": \"Tên bài hát\",\n" +
               "      \"artist\": \"Tên ca sĩ\",\n" +
               "      \"rank\": số thứ tự từ 1-8\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }
} 