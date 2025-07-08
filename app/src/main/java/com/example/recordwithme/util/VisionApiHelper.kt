package com.example.recordwithme.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object VisionApiHelper {
    suspend fun getLabelsFromVisionApi(base64Image: String, apiKey: String): List<String> {
        val url = "https://vision.googleapis.com/v1/images:annotate?key=$apiKey"
        val requestBody = """
            {
              "requests": [
                {
                  "image": { "content": "$base64Image" },
                  "features": [{ "type": "LABEL_DETECTION", "maxResults": 5 }]
                }
              ]
            }
        """.trimIndent()

        val client = OkHttpClient()
        val body = requestBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: ""
        android.util.Log.d("VisionAPI", "response: $responseString")
        val labels = mutableListOf<String>()
        val json = JSONObject(responseString)
        if (!json.has("responses")) {
            android.util.Log.e("VisionAPI", "No 'responses' in Vision API result: $responseString")
            return labels
        }
        val labelAnnotations = json
            .getJSONArray("responses")
            .getJSONObject(0)
            .optJSONArray("labelAnnotations") ?: return labels
        for (i in 0 until labelAnnotations.length()) {
            labels.add(labelAnnotations.getJSONObject(i).getString("description"))
        }
        return labels
    }
} 