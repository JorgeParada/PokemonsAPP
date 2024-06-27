package com.example.pokedex.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon_table")
data class PokemonEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val height: Int,
    val weight: Int,
    val baseExperience: Int,
    val imageUrl: String?,
    val description: String?
)
