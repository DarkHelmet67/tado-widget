package it.darkhelmet67.tado.network;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import it.darkhelmet67.tado.utils.DLog;
/*
Classe REQUEST che fornsice nella RESPONSE  il CODICE di ERRORE HTTP
*/
public class CustomRequest extends Request<JSONObject> {
    private int mMethod;
    private String mUrl;
    private Map<String, String> mParams;
    private Listener<JSONObject> mListener;

    public CustomRequest(int method, String url, Map<String, String> params,
                         Listener<JSONObject> reponseListener, ErrorListener errorListener) {
        super(method, url, errorListener);
        DLog.d(this.getClass().getSimpleName(), "init @CustomRequest - url=" + url + ", params=" + params.toString());
        this.mMethod = method;
        this.mUrl = url;
        this.mParams = params;
        this.mListener = reponseListener;
    }

    @Override
    public String getUrl() {
        DLog.d(this.getClass().getSimpleName(), "getUrl @CustomRequest");
        if(mMethod == Request.Method.GET) {
            StringBuilder stringBuilder = new StringBuilder(mUrl);
            Iterator<Map.Entry<String, String>> iterator = mParams.entrySet().iterator();
            int i = 1;
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if(i == 1) {
                    stringBuilder.append("?" + entry.getKey() + "=" + entry.getValue());
                } else {
                    stringBuilder.append("&" + entry.getKey() + "=" + entry.getValue());
                }
                iterator.remove(); // avoids a ConcurrentModificationException
                i++;
            }
            mUrl = stringBuilder.toString();
        }
        return mUrl;
    }

    @Override
    protected Map<String, String> getParams()
            throws com.android.volley.AuthFailureError {
        DLog.d(this.getClass().getSimpleName(), "getParams @CustomRequest - params=" + mParams.toString());
        return mParams;
    };

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        DLog.d(this.getClass().getSimpleName(), "parseNetworkResponse @CustomRequest - response=" + response.toString());
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        // TODO Auto-generated method stub
        mListener.onResponse(response);
    }
}
