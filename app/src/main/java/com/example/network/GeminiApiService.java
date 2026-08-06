package com.example.network;

import com.example.model.GeminiModels;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    Call<GeminiModels.GenerateContentResponse> generateContent(
        @Query("key") String apiKey,
        @Body GeminiModels.GenerateContentRequest request
    );
}
