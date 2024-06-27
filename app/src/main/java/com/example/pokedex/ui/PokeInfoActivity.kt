package com.example.pokedex.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.pokedex.R
import com.example.pokedex.databinding.ActivityPokeInfoBinding
import com.example.pokedex.helpers.DialogUtils
import com.example.pokedex.helpers.ErrorType
import com.example.pokedex.model.PokeInfoViewModel

class PokeInfoActivity : AppCompatActivity() {

    private lateinit var viewModel: PokeInfoViewModel
    private lateinit var binding: ActivityPokeInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPokeInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(PokeInfoViewModel::class.java)

        initUI()
        observeConnectivity()
        DialogUtils.refreshInternetConnection(this)
    }

    private fun initUI() {
        val id = intent.getIntExtra("id", 0)

        binding.loadingIndicator.visibility = View.VISIBLE

        viewModel.getPokemonInfo(id)

        viewModel.pokemonInfo.observe(this, Observer { pokemon ->
            binding.loadingIndicator.visibility = View.GONE
            pokemon?.let {
                val typeNames = it.types.map { type -> type.type.name }
                binding.nameTextView.text = it.name
                binding.heightText.text = "Altura: ${it.height / 10.0}m"
                binding.weightText.text = "Peso: ${it.weight / 10.0}kg"
                binding.typeText.text = "Tipo: ${typeNames.joinToString(", ")}"
                binding.expBaseText.text = "Exp.Base: ${it.baseExperience}"

                val highResImageUrl = it.sprites.other.officialArtwork.frontDefault
                Glide.with(this)
                    .load(highResImageUrl ?: R.drawable.sorry1)
                    .into(binding.imageView)
            }
        })

        viewModel.getPokemonDescription(id)

        viewModel.pokemonDescription.observe(this, Observer { description ->
            binding.loadingIndicator.visibility = View.GONE
            description?.let {
                val spanishEntries =
                    it.flavorTextEntries.filter { entry -> entry.language.name == "es" }
                val spanishText = spanishEntries.firstOrNull()?.flavorText
                binding.descriptionText.text = spanishText ?: ""
            }
        })

        binding.regresar.setOnClickListener {
            finish()
        }
    }

    private fun observeConnectivity() {
        DialogUtils.observeInternetConnection(this).observe(this, Observer { isConnected ->
            if (!isConnected) {
                DialogUtils.showErrorDialog(this, ErrorType.NETWORK)
            } else {
                val id = intent.getIntExtra("id", 0)
                viewModel.getPokemonInfo(id)
                viewModel.getPokemonDescription(id)
            }
        })
    }
}
