package com.example.pokedex.mappers

import com.example.pokedex.Entity.PokemonEntity
import com.example.pokedex.api.PokeResult

fun List<PokeResult>.toPokemonEntityList() = map(PokeResult::toPokemonEntity)

fun PokeResult.toPokemonEntity() = PokemonEntity(
    extractPokemonIdFromUrl(url), name, height, weight, baseExperience, url, ""
)

private fun extractPokemonIdFromUrl(url: String): Long {
    val idPattern = "(\\d+)/$".toRegex()
    val matchResult = idPattern.find(url)
    return matchResult?.groupValues?.get(1)?.toLongOrNull() ?: 0
}