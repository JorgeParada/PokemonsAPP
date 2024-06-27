package com.example.pokedex.model

import PokeListViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pokedex.DAO.PokemonDao

class ViewModelFactory(private val pokemonDao: PokemonDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PokeListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PokeListViewModel(pokemonDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}