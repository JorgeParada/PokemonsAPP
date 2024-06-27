package com.example.pokedex.model

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.pokedex.R
import com.example.pokedex.api.PokeResult
import com.example.pokedex.databinding.PokeListBinding

class PokeListAdapter(private val pokemonClick: (Int) -> Unit) : RecyclerView.Adapter<PokeListAdapter.SearchViewHolder>() {
    private var pokemonList: List<PokeResult> = emptyList()
    private var filteredList: List<PokeResult> = emptyList()

    fun setData(list: List<PokeResult>) {
        val diffResult = DiffUtil.calculateDiff(PokemonDiffCallback(pokemonList, list))
        pokemonList = list
        filterList("")
        diffResult.dispatchUpdatesTo(this)
    }

    fun filterList(query: String) {
        filteredList = if (query.isEmpty()) {
            pokemonList
        } else {
            pokemonList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.types.any { type -> type.type.name.contains(query, ignoreCase = true) }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = PokeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (filteredList.isEmpty()) {
            1 // Mostrar un elemento especial cuando no hay resultados
        } else {
            filteredList.size
        }
    }

    private fun extractPokemonIdFromUrl(url: String): Int {
        val idPattern = "(\\d+)/$".toRegex()
        val matchResult = idPattern.find(url)
        return matchResult?.groupValues?.get(1)?.toInt() ?: 0
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val binding = holder.binding

        if (filteredList.isEmpty()) {
            binding.pokemonText.text = "No hay resultados"
            Glide.with(binding.imageView.context)
                .load(R.drawable.sorry1)
                .into(binding.imageView)
            binding.position.text = ""
            holder.itemView.setOnClickListener(null)  // Desactivar clics
        } else {
            val pokemon = filteredList[position]
            val capitalizedPokemonName = pokemon.name.replaceFirstChar { it.uppercase() }
            val pokemonId = pokemon.url?.let { extractPokemonIdFromUrl(it) }
            val imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${pokemonId}.png"

            binding.loadingIndicator.visibility = View.VISIBLE
            Glide.with(binding.imageView.context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loadingIndicator.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loadingIndicator.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.imageView)

            binding.pokemonText.text = capitalizedPokemonName
            binding.position.text = pokemonId.toString()

            holder.itemView.setOnClickListener { pokemonId?.let { it1 -> pokemonClick(it1) } }
        }
    }

    class SearchViewHolder(val binding: PokeListBinding) : RecyclerView.ViewHolder(binding.root)

    class PokemonDiffCallback(private val oldList: List<PokeResult>, private val newList: List<PokeResult>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
