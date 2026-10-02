package it.darkhelmet67.tado.network;

import com.android.volley.VolleyError;

import org.json.JSONObject;

// CALLBACK from ASYNC VOLLEY REQUEST
public interface VolleyCallback {

    void onSuccess(String result);

    void onResponse(JSONObject response);

    void onError(VolleyError error);
}
/*
{
  "errors": [
    {
      "code": "unauthorized",
      "title": "Bad credentials"
    }
  ]
}

 */