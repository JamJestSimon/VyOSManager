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
import pl.lapko.vyosmanager.data.VyOSResults
import kotlin.coroutines.resumeWithException

object VyOSConnection {
    private var VyOSAddress = ""
    private var VyOSPassword = ""
    private val mapper = jacksonObjectMapper()
    private var requestQueue : RequestQueue? = null

    fun createRequestQueue(context: Context){
        requestQueue = Volley.newRequestQueue(context)
    }

    fun getVyOSAddress(): String {
        return VyOSAddress
    }

    fun setupVyOSConnection(address: String, password: String){
        VyOSAddress = address
        VyOSPassword = password
    }

    fun verifyVyOSConnection(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try{
                val result = vyOSRequest("retrieve","showConfig",listOf("")).await()
                if(result == null){
                    onError(Exception("Unknown internal VyOS error"))
                } else if(!result.success){
                    onError(Exception("Internal VyOS error: ${result.error}"))
                } else {
                    onSuccess()
                }
            } catch(e: Exception) {
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.message.toString())
                onError(e)
            }
        }
    }

    fun getVyOSData(
        path: String,
        onSuccess: (VyOSResults) -> Unit,
        onError: (Exception) -> Unit
    ){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val result = vyOSRequest("retrieve","showConfig", listOf(path)).await()
                if(result == null){
                    onError(Exception("Unknown internal VyOS error"))
                } else if(!result.success){
                    onError(Exception("Internal VyOS error: ${result.error}"))
                } else {
                    onSuccess(result)
                }
            } catch (e: Exception){
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.toString())
                onError(e)
            }
        }
    }

    fun setVyOSData(
        paths: List<String>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val result = vyOSRequest("configure","set", paths).await()
                if(result == null){
                    onError(Exception("Unknown internal VyOS error"))
                } else if(!result.success){
                    onError(Exception("Internal VyOS error: ${result.error}"))
                } else {
                    onSuccess()
                }
            } catch (e: Exception){
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.toString())
                onError(e)
            }
        }
    }

    fun deleteVyOSData(
        path: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val result = vyOSRequest("configure","delete", listOf(path)).await()
                if(result == null){
                    onError(Exception("Unknown internal VyOS error"))
                } else if(!result.success){
                    onError(Exception("Internal VyOS error: ${result.error}"))
                } else {
                    onSuccess()
                }
            } catch (e: Exception){
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.toString())
                onError(e)
            }
        }
    }

    fun showVyOSData(
        path: String,
        onSuccess: (VyOSResults) -> Unit,
        onError: (Exception) -> Unit
    ){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val result = vyOSRequest("show","show", listOf(path)).await()
                if(result == null){
                    onError(Exception("Unknown internal VyOS error"))
                } else if(!result.success){
                    onError(Exception("Internal VyOS error: ${result.error}"))
                } else {
                    onSuccess(result)
                }
            } catch (e: Exception){
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.toString())
                onError(e)
            }
        }
    }

    fun saveVyOSData(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val result = vyOSRequest("config-file", "save", listOf("")).await()
                if(result == null){
                    onError(Exception("Unknown internal VyOS error"))
                } else if(!result.success){
                    onError(Exception("Internal VyOS error: ${result.error}"))
                } else {
                    onSuccess()
                }
            } catch (e: Exception){
                Log.println(Log.ERROR, "VOLLEY_DEBUG", e.toString())
                onError(e)
            }
        }
    }

    /*
    * list of supported endpoints with operations
    *
    * retrieve
    *   showConfig
    *
    * configure
    *   set
    *   delete
    *
    * config-file
    *   save
    *
    * */
    private suspend fun vyOSRequest(endpoint: String, op: String, path: List<String>): Deferred<VyOSResults?> {
        return CoroutineScope(Dispatchers.IO).async {
            Log.println(Log.INFO, "INFO", "Starting VyOS Request")
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
                        if(op == "save") {
                            params["data"] = "{\"op\": \"$op\"}"
                        } else {
                            if (path.size < 2) {
                                params["data"] = "{\"op\": \"$op\", \"path\": [${path.first()}]}"
                            } else {
                                params["data"] = "["
                                path.forEach {
                                    params["data"] += "{\"op\": \"$op\", \"path\": [$it]}, "
                                }
                                params["data"] = params["data"]!!.removeSuffix(", ")
                                params["data"] += "]"
                                path.forEach {
                                    Log.println(
                                        Log.INFO,
                                        "VOLLEY_DEBUG",
                                        "{\"op\": \"$op\", \"path\": [$it]}"
                                    )
                                }
                            }
                        }
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