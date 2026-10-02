package it.darkhelmet67.tado.network;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import it.darkhelmet67.tado.R;
import it.darkhelmet67.tado.utils.DLog;
import it.darkhelmet67.tado.utils.UI;

// import com.android.volley.Request;

/**
 * @author Luca Ceccatelli
 */

public class VolleyFunctions {

    private static final String TAG = VolleyFunctions.class.getSimpleName();
    private Context context;

    public VolleyFunctions(Context context) {
        DLog.i(TAG, "init @VolleyFunctions - context=" + context.toString());
        this.context = context;
    }

    public void getDevices(final String username, final String password, final VolleyCallback callback) {
        DLog.d(TAG, "getDevices @VolleyFunctions");

        // URL
        String url = context.getString(R.string.url_server) + context.getString(R.string.url_getAppUsers);

        // PARAMS
        url = url.replace("%USERNAME", username)
                .replace("%PASSWORD", password);

        // LC - CREATE VOLLEY QUEUE/REQUEST
        DLog.d(TAG, "getDevices @VolleyFunctions - URL=" + url + ", callback=" + callback.toString());
        getJsonRequest(url, callback, true);
    }

    public void claimAppUser(final String username, final String password,
                             final String nickname, final String deviceModel,
                             final String deviceOsVer, final VolleyCallback callback) {
        DLog.d(TAG, "claimAppUser @VolleyFunctions");

        // URL
        String url = context.getString(R.string.url_server) + context.getString(R.string.url_claimAppUser);

        // PARAMS
        try {
            url = url.replace("%USERNAME", username)
                    .replace("%PASSWORD", password)
                    .replace("%NICKNAME", URLEncoder.encode(nickname, "utf-8"))
                    .replace("%DEVICE_MODEL", URLEncoder.encode(deviceModel, "utf-8"))
                    .replace("%DEVICE_OSVER", URLEncoder.encode(deviceOsVer, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            DLog.e(TAG, "claimAppUser @VolleyFunctions - UnsupportedEncodingException=" + e.getMessage());
            e.printStackTrace();
        }

        // LC - CREATE VOLLEY QUEUE/REQUEST
        DLog.d(TAG, "claimAppUser @VolleyFunctions - URL=" + url + ", callback=" + callback.toString());
        getJsonRequest(url, callback, true);
    }

    // FIXME LC 2016-01-01
    public void createAppUser(final String username, final String password,
                              final String nickname, final String deviceModel,
                              final String deviceOsVer, final VolleyCallback callback) {
        DLog.d(TAG, "createAppUser @VolleyFunctions");

        // URL
        String url = context.getString(R.string.url_server) + context.getString(R.string.url_createAppUser);

        // PARAMS
        try {
            url = url.replace("%USERNAME", username)
                    .replace("%PASSWORD", password)
                    .replace("%NICKNAME", URLEncoder.encode(nickname, "utf-8"))
                    .replace("%DEVICE_MODEL", URLEncoder.encode(deviceModel, "utf-8"))
                    .replace("%DEVICE_OSVER", URLEncoder.encode(deviceOsVer, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            DLog.e(TAG, "createAppUser @VolleyFunctions - UnsupportedEncodingException=" + e.getMessage());
            e.printStackTrace();
        }

        // LC - CREATE VOLLEY QUEUE/REQUEST
        DLog.d(TAG, "createAppUser @VolleyFunctions - URL=" + url + ", callback=" + callback.toString());
        getJsonRequest(url, callback, true);
    }

    public void getCurrentState(final String username, final String password, final VolleyCallback callback) {
        DLog.d(TAG, "getCurrentState @VolleyFunctions");

        // URL
        String url = context.getString(R.string.url_server) + context.getString(R.string.url_getCurrentState);

        // PARAMS
        url = url.replace("%USERNAME", username)
                .replace("%PASSWORD", password);

        // LC - CREATE VOLLEY QUEUE/REQUEST
        DLog.d(TAG, "getCurrentState @VolleyFunctions - URL=" + url + ", callback=" + callback.toString());
        getJsonRequest(url, callback, false);
    }

    private void getJsonRequest(String url, final VolleyCallback callback, final boolean showDialog) {
        DLog.d(TAG, "getJsonRequest @VolleyFunctions - url=" + url + ", callback=" + callback.toString());

        // LC - CREATE VOLLEY QUEUE/REQUEST
        RequestQueue queue = Volley.newRequestQueue(context);
//        JsonObjectRequestStatus request = new JsonObjectRequestStatus(Request.Method.POST, url, null,
        JsonObjectRequestStatus request = new JsonObjectRequestStatus(CustomRequest.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // LC - RETURNS RESPONSE to CALLING FUNCTION using CALLBACK INTERFACE
                        DLog.d(TAG, "getJsonRequest @VolleyFunctions - onResponse=" + response.toString());
                        callback.onResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // LC - RETURNS RESPONSE to CALLING FUNCTION using CALLBACK INTERFACE
                        DLog.e(TAG, "getJsonRequest @VolleyFunctions - onErrorResponse=" + error.toString());
                        error.printStackTrace();
                        // FIXME LC 2016-01-29 - niente CALLBACK, visualizzo un POPUP con l'ERRORE!
                        String statusString = "HTTP error";
                        String errorString = error.toString();
                        String responseString = "";
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse == null) {
                            // NON so come gestire l'errore...
                            if(errorString.contains("NoConnectionError") || errorString.contains("UnknownHostException"))
                                statusString = "No Internet connection";
                        } else {
                            try {
                                int statusCode = networkResponse.statusCode;
                                switch (statusCode) {
                                    case 400:
                                        statusString = "HTTP error 400 - Bad Request";
                                        break;
                                    case 401:
                                        statusString = "HTTP error 401 - Unauthorized";
                                        break;
                                    case 403:
                                        statusString = "HTTP error 403 - Forbidden";
                                        break;
                                    case 404:
                                        statusString = "HTTP error 404 - Not Found";
                                        break;
                                    case 407:
                                        statusString = "HTTP error 407 - Proxy Authentication Required";
                                        break;
                                    case 408:
                                        statusString = "HTTP error 408 - Request Timeout";
                                        break;
                                    case 419:
                                        statusString = "HTTP error 419 - Authentication Timeout";
                                        break;
                                    case 500:
                                        statusString = "HTTP error 500 - Internal Server Error";
                                        break;
                                    case 502:
                                        statusString = "HTTP error 502 - Bad Gateway";
                                        break;
                                    case 503:
                                        statusString = "HTTP error 503 - Service Unavailable";
                                        break;
                                    case 511:
                                        statusString = "HTTP error 511 - Network Authentication Required";
                                        break;
                                    default:
                                        statusString = "HTTP error " + statusCode;
                                        break;
                                }
                                // errorString += "\n" + statusString;
                                responseString = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                                // errorString += "\n" + responseString;
                            } catch (UnsupportedEncodingException e1) {
                                e1.printStackTrace();
                            }
                        }
                        if(showDialog) {
                            UI.showDialog(context, UI.AlertType.OK, statusString,
                                    errorString + "\n" + responseString, null, null);
                        } else {
                            Toast.makeText(context, errorString, Toast.LENGTH_SHORT).show();
                        }
                        callback.onError(error);
                    }
                });
        queue.add(request);
    }
}
