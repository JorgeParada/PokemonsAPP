package com.example.pokedex.ui

import PokeListViewModel
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pokedex.Database.PokemonDatabase
import com.example.pokedex.api.PokeResult
import com.example.pokedex.databinding.ActivityMainBinding
import com.example.pokedex.helpers.DialogUtils
import com.example.pokedex.helpers.ErrorType
import com.example.pokedex.model.PokeListAdapter
import com.example.pokedex.model.ViewModelFactory
import com.example.pokedex.workers.PokeWorker
import java.util.concurrent.TimeUnit

class PokemonList : AppCompatActivity() {

    private lateinit var viewModel: PokeListViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var pokeListAdapter: PokeListAdapter
    private var currentSearchQuery: String = ""
    private var allPokemonList: MutableList<PokeResult> = mutableListOf()
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 30000L // 30 segundos
    private var showMessageNewPokemons = true
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_app_closed", false).apply()

        val pokemonDao = PokemonDatabase.getDatabase(application).pokemonDao()

        viewModel = ViewModelProvider(this, ViewModelFactory(pokemonDao)).get(PokeListViewModel::class.java)

        initUI()
        observeConnectivity()
        DialogUtils.refreshInternetConnection(this)
        setupPeriodicWork()

        if (!prefs.getBoolean("welcome_shown", false)) {
            showWelcomeMessageAndStartAutoUpdate()
        } else {
            startAutoUpdate()
        }
    }

    private fun setupPeriodicWork() {
        val workRequest: PeriodicWorkRequest = PeriodicWorkRequestBuilder<PokeWorker>(
            30, TimeUnit.SECONDS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "poke_worker",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun initUI() {
        binding.pokelistRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.encabezado.text = "POKEDEX"
        binding.indicacion.text = "Busca y filtra un Pokémon por su nombre"

        pokeListAdapter = PokeListAdapter { pokemonId ->
            val intent = Intent(this, PokeInfoActivity::class.java)
            intent.putExtra("id", pokemonId)
            startActivity(intent)
        }

        binding.pokelistRecyclerView.adapter = pokeListAdapter

        binding.clearButton.setOnClickListener {
            binding.editTextTextMultiLine.text.clear()
            binding.clearButton.visibility = View.GONE
        }

        binding.editTextTextMultiLine.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.clearButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                currentSearchQuery = s.toString()
                filterList(currentSearchQuery)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.pokemonList.observe(this, Observer { list ->
            list?.let {
                val newPokemonCount = it.size - allPokemonList.size
                if (newPokemonCount > 0) {
                    val newPokemons = it.takeLast(newPokemonCount)
                    allPokemonList.addAll(newPokemons)
                    filterList(currentSearchQuery)
                    if (!isAppInBackground() && showMessageNewPokemons) {
                        DialogUtils.showDialog(this,
                            "Nuevos Pokémon!",
                            "Se han agregado e insertado nuevos Pokémon a la lista y BD. ¡Desliza para verlos!"
                        )
                    }
                }
            }
        })

        viewModel.statusInsert.observe(this) { status ->
            status?.let {
                if (it) {
                    DialogUtils.showDialog(
                        this,
                        "Nuevos Pokémon!",
                        "Se han agregado e insertado nuevos Pokémon a la lista y BD. ¡Desliza para verlos!"
                    )
                } else {
                    viewModel.pokemonsFailed.observeOnce(this) { pokemonsFailed ->
                        DialogUtils.showDialog(
                            this,
                            "Fallo al insertar pokemons",
                            "$pokemonsFailed pokemons fallaron al insertarse en la base local"
                        )
                    }
                }
                viewModel.resetInsertStatus()
            }
        }
    }

    private fun observeConnectivity() {
        DialogUtils.observeInternetConnection(this).observe(this, Observer { isConnected ->
            if (!isConnected) {
                DialogUtils.showErrorDialog(this, ErrorType.NETWORK)
            } else {
                if (viewModel.pokemonList.value.isNullOrEmpty()) {
                    viewModel.getInitialPokemonList()
                } else {
                    viewModel.pokemonList.value?.let {
                        allPokemonList.clear()
                        allPokemonList.addAll(it)
                        filterList(currentSearchQuery)
                    }
                }
            }
        })
    }

    private fun filterList(query: String) {
        currentSearchQuery = query
        val filteredList = if (query.isEmpty()) {
            allPokemonList
        } else {
            allPokemonList.filter { pokemon ->
                pokemon.name.contains(query, ignoreCase = true) ||
                        pokemon.types?.any { type -> type.type.name.contains(query, ignoreCase = true) } == true
            }
        }
        pokeListAdapter.setData(filteredList)
    }

    private fun showWelcomeMessageAndStartAutoUpdate() {
        // Mostrar mensaje de bienvenida durante 4 segundos
        DialogUtils.showWelcomeMessage(this)
        showMessageNewPokemons = false // Desactivar el mensaje de nuevos Pokémon durante el mensaje de bienvenida

        prefs.edit().putBoolean("welcome_shown", true).apply()
        startAutoUpdate()

        // Reactivar el mensaje de nuevos Pokémon después de 4 segundos
        handler.postDelayed({
            showMessageNewPokemons = true
        }, 4000)
    }

    private fun startAutoUpdate() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                viewModel.getNextBatch()
                handler.postDelayed(this, updateInterval)
            }
        }, updateInterval)
    }

    private fun isAppInBackground(): Boolean {
        return prefs.getBoolean("is_app_in_background", false)
    }

    override fun onResume() {
        super.onResume()
        prefs.edit().putBoolean("is_app_in_background", false).apply()
    }

    override fun onPause() {
        super.onPause()
        prefs.edit().putBoolean("is_app_in_background", true).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        WorkManager.getInstance(this).cancelUniqueWork("poke_worker")
        prefs.edit().putBoolean("is_app_closed", true).apply()
        prefs.edit().putBoolean("welcome_shown", false).apply() // Resetear el estado al cerrar la aplicación
    }
}
