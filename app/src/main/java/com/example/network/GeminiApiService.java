package com.example.network;

import com.example.model.GeminiModels;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    @POST("v1/chat/completions")
    Call<GeminiModels.GenerateContentResponse> generateContent(
        @Body GeminiModels.GenerateContentRequest request
    );
}
