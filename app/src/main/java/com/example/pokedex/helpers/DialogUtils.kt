package com.example.pokedex.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object DialogUtils {

    private val isConnected = MutableLiveData<Boolean>()
    private var isDialogVisible = false
    private var dialog: AlertDialog? = null

    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun observeInternetConnection(context: Context): LiveData<Boolean> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = android.net.NetworkRequest.Builder()
        connectivityManager.registerNetworkCallback(
            builder.build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    isConnected.postValue(true)
                }

                override fun onLost(network: android.net.Network) {
                    isConnected.postValue(false)
                }
            }
        )
        return isConnected
    }

    fun showErrorDialog(context: Context, errorType: ErrorType) {
        if (isDialogVisible) return
        when (errorType) {
            ErrorType.NETWORK -> showNoInternetDialog(context)
            ErrorType.SERVER -> showServerErrorDialog(context)
            ErrorType.DATABASE -> showDatabaseErrorDialog(context)
            ErrorType.UNKNOWN -> showUnknownErrorDialog(context)
        }
    }

    private fun showNoInternetDialog(context: Context) {
        isDialogVisible = true
        dialog = AlertDialog.Builder(context)
            .setTitle("No hay conexión a Internet")
            .setMessage("Por favor, verifica tu conexión a Internet.")
            .setCancelable(false)
            .show()
    }

    private fun showServerErrorDialog(context: Context) {
        isDialogVisible = true
        dialog = AlertDialog.Builder(context)
            .setTitle("Error de PokeAPI")
            .setMessage("Hubo un problema con el servidor. Por favor, inténtalo más tarde.")
            .setCancelable(false)
            .show()
    }

    private fun showDatabaseErrorDialog(context: Context) {
        isDialogVisible = true
        dialog = AlertDialog.Builder(context)
            .setTitle("Error de la Base de Datos")
            .setMessage("Hubo un problema con la base de datos. Por favor, reinicia la aplicación.")
            .setCancelable(false)
            .show()
    }

    private fun showUnknownErrorDialog(context: Context) {
        isDialogVisible = true
        dialog = AlertDialog.Builder(context)
            .setTitle("Error Desconocido")
            .setMessage("Hubo un problema desconocido. Por favor, intenta nuevamente.")
            .setCancelable(false)
            .show()
    }

    fun showWelcomeMessage(context: Context) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("¡Bienvenido!")
            .setMessage("Bienvenido a la Pokedex. ¡Disfruta explorando los Pokémon!")
            .setCancelable(false)
            .show()

        // Cerrar el diálogo automáticamente después de 4 segundos
        Handler(context.mainLooper).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 4000L)
    }

    fun showDialog(context: Context, title: String, description: String) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(description)
            .setCancelable(false)
            .show()

        // Cerrar el diálogo automáticamente después de 4 segundos
        Handler(context.mainLooper).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 4000L)
    }

    fun refreshInternetConnection(context: Context) {
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                val connected = isInternetConnected(context)
                isConnected.postValue(connected)
                if (connected && isDialogVisible) {
                    dialog?.dismiss()
                    isDialogVisible = false
                }
                handler.postDelayed(this, 5000) // Verifica la conexión cada 5 segundos
            }
        }
        handler.post(runnable)
    }
}
