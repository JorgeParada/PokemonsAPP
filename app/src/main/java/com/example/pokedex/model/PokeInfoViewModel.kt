package com.example.pokedex.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokedex.DAO.PokemonDao
import com.example.pokedex.Database.PokemonDatabase
import com.example.pokedex.Entity.PokemonEntity
import com.example.pokedex.api.ApiService
import com.example.pokedex.api.Pokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PokeInfoViewModel : ViewModel() {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service: ApiService = retrofit.create(ApiService::class.java)

    val pokemonInfo = MutableLiveData<Pokemon>()
    val pokemonDescription = MutableLiveData<Pokemon>()
    val errorMessage = MutableLiveData<String>()

    fun getPokemonInfo(id: Int) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { service.getPokemonInfo(id).execute() }
                if (response.isSuccessful) {
                    response.body()?.let { pokemon ->
                        Log.d("PokeListAdapter", "Pokemon: , Types: ${pokemon.types}")
                        pokemonInfo.postValue(pokemon)
                    }
                } else {
                    errorMessage.postValue("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                errorMessage.postValue(e.message)
            }
        }
    }

    fun getPokemonDescription(id: Int) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { service.getPokemonSpecies(id).execute() }
                if (response.isSuccessful) {
                    response.body()?.let { pokemon ->
                        pokemonDescription.postValue(pokemon)
                    }
                } else {
                    errorMessage.postValue("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                errorMessage.postValue(e.message)
            }
        }
    }
}
