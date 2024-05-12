package pl.lapko.vyosmanager

import android.content.Context
import android.util.Log
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlin.coroutines.resumeWithException
import kotlin.reflect.typeOf

object VyOSConnection {
    private var VyOSAddress = ""
    private var VyOSPassword = ""
    private val mapper = jacksonObjectMapper()
    private var requestQueue : RequestQueue? = null

    fun createRequestQueue(context: Context){
        requestQueue = Volley.newRequestQueue(context)
    }

    fun setupVyOSConnection(address: String, password: String){
        VyOSAddress = address
        VyOSPassword = password
    }

    fun verifyVyOSConnection(
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try{
                val result = vyOSRequest("retrieve","showConfig","").await()
                onSuccess(result?.success ?: false)
            } catch(e: Exception) {
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.message.toString())
                onError(e)
            }
        }
    }

    fun getVyOSData(
        path: String,
        onSuccess: (VyOSResults?) -> Unit,
        onError: (Exception) -> Unit
    ){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val result = vyOSRequest("retrieve","showConfig",path).await()
                onSuccess(result)
            } catch (e: Exception){
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.toString())
                onError(e)
            }
        }
    }

    /*
    * list of endpoints with operations
    *
    * retrieve
    *   showConfig
    *   returnValues
    *   exists
    *
    * reset
    *   reset
    *
    * reboot
    *   reboot
    *
    * show
    *   show
    *
    * configure
    *   set
    *   delete
    *   comment
    *
    * config-file
    *   save
    *   load
    *
    * */
    private suspend fun vyOSRequest(endpoint: String, op: String, path: String): Deferred<VyOSResults?> {
        return CoroutineScope(Dispatchers.IO).async {
            Log.println(Log.INFO, "INFO", "Starting configuration retrieval")
            val url = "https://$VyOSAddress/$endpoint"
            val response = suspendCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST, url,
                    Response.Listener { response ->
                        Log.println(Log.INFO, "INFO", response)
                        continuation.resume(response)
                    },
                    Response.ErrorListener { error ->
                        Log.println(Log.WARN, "VOLLEY_DEBUG", error.toString())
                        if(error is TimeoutError || error is NoConnectionError){
                            continuation.resumeWithException(Exception("Timeout - server unreachable"))
                        } else {
                            Log.println(
                                Log.ERROR,
                                "VOLLEY_DEBUG",
                                error.networkResponse.statusCode.toString()
                            )
                            continuation.resumeWithException(Exception("Network response code: ${error.networkResponse.statusCode}"))
                        }
                    }) {
                    override fun getParams(): Map<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        params["data"] = "{\"op\": \"$op\", \"path\": [$path]}"
                        params["key"] = VyOSPassword
                        return params
                    }
                }
                requestQueue!!.add(stringRequest)
            }
            return@async mapper.readValue<VyOSResults>(response)
        }
    }
}