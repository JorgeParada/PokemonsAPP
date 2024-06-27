package com.example.pokedex.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.pokedex.Database.PokemonDatabase
import com.example.pokedex.Entity.PokemonEntity
import com.example.pokedex.R
import com.example.pokedex.api.ApiService
import com.example.pokedex.api.PokeApiResponse
import com.example.pokedex.mappers.toPokemonEntityList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class PokeWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service: ApiService = retrofit.create(ApiService::class.java)
    private val limit = 10
    private var currentOffset = 0

    override suspend fun doWork(): Result {
        if (isAppInBackground() && !isAppClosed()) {
            getPokemonList()
        }
        scheduleNext()
        return Result.success()
    }

    private suspend fun getPokemonList() {
        withContext(Dispatchers.IO) {
            try {
                val response = service.getPokemonList(limit, currentOffset).execute()
                if (response.isSuccessful) {
                    response.body()?.results?.let { newList ->
                        showNotification(newList.size)
                        insertPokemons(newList.toPokemonEntityList())
                        currentOffset += limit
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun insertPokemons(pokemons: List<PokemonEntity>) {
        val pokemonDao = PokemonDatabase.getDatabase(applicationContext).pokemonDao()
        withContext(Dispatchers.IO) {
            try {
                val response = pokemonDao.insertAll(pokemons)
                val errorResults = response.filter { it == -1L }
                if (errorResults.isEmpty()) {
                    //TODO("Manejar logica cuando los pokemons se insertaron correctamente")
                } else {
                    //TODO("Aplicar logica para manejo de pokemons que no lograron insertarse en la base de datos")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(newPokemonCount: Int) {
        createNotificationChannel()
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val notification = NotificationCompat.Builder(applicationContext, "poke_channel")
            .setContentTitle("Nuevos Pokémon")
            .setContentText("Se han agregado $newPokemonCount nuevos Pokémon a la lista. ¡Entra a ver!")
            .setSmallIcon(R.drawable.pokenoti)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Poke Channel"
            val descriptionText = "Channel for Poke Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("poke_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleNext() {
        val workRequest = OneTimeWorkRequestBuilder<PokeWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "poke_worker",
            ExistingWorkPolicy.REPLACE,
            workRequest)
    }

    private fun isAppInBackground(): Boolean {
        val prefs: SharedPreferences = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_app_in_background", false)
    }

    private fun isAppClosed(): Boolean {
        val prefs: SharedPreferences = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_app_closed", true)
    }
}
