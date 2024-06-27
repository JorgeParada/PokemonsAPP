import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokedex.DAO.PokemonDao
import com.example.pokedex.Entity.PokemonEntity
import com.example.pokedex.api.ApiService
import com.example.pokedex.api.PokeApiResponse
import com.example.pokedex.api.PokeResult
import com.example.pokedex.mappers.toPokemonEntityList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PokeListViewModel(private val pokemonDao: PokemonDao) : ViewModel() {
    private val interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val retrofit = Retrofit.Builder().baseUrl("https://pokeapi.co/api/v2/")
        .client(OkHttpClient().newBuilder().addInterceptor(interceptor).build())
        .addConverterFactory(GsonConverterFactory.create()).build()

    private val service: ApiService = retrofit.create(ApiService::class.java)

    val pokemonList = MutableLiveData<List<PokeResult>>()
    val errorMessage = MutableLiveData<String>()
    private var currentOffset = 0
    private val initialLimit = 15
    private val batchLimit = 10

    init {
        getInitialPokemonList()
    }

    fun getInitialPokemonList() {
        val call = service.getPokemonList(initialLimit, 0)

        call.enqueue(object : Callback<PokeApiResponse> {
            override fun onResponse(
                call: Call<PokeApiResponse>, response: Response<PokeApiResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.results?.let { newList ->
                        pokemonList.postValue(newList)
                        insertPokemons(newList.toPokemonEntityList())
                        currentOffset = initialLimit
                    }
                } else {
                    errorMessage.postValue("Error: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<PokeApiResponse>, t: Throwable) {
                errorMessage.postValue(t.message)
                call.cancel()
            }
        })
    }

    private val _statusInsert: MutableLiveData<Boolean?> = MutableLiveData(null)
    val statusInsert: LiveData<Boolean?> get() = _statusInsert

    fun resetInsertStatus() {
        _statusInsert.value = null
    }

    val pokemonsFailed = MutableLiveData(0)

    fun insertPokemons(pokemons: List<PokemonEntity>) = viewModelScope.launch(Dispatchers.IO) {
        val response = pokemonDao.insertAll(pokemons)
        val errorResults = response.filter { it == -1L }
        if (errorResults.isEmpty()) {
            _statusInsert.postValue(true)
        } else {
            //TODO("Aplicar logica para manejo de pokemons que no lograron insertarse en la base de datos")
            pokemonsFailed.postValue(errorResults.size)
            _statusInsert.postValue(false)
        }
    }

    fun getNextBatch() {
        getPokemonList(currentOffset)
    }

    private fun getPokemonList(offset: Int) {
        val call = service.getPokemonList(batchLimit, offset)

        call.enqueue(object : Callback<PokeApiResponse> {
            override fun onResponse(
                call: Call<PokeApiResponse>, response: Response<PokeApiResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.results?.let { newList ->
                        val currentList = pokemonList.value?.toMutableList() ?: mutableListOf()
                        newList.forEach { newPokemon ->
                            if (!currentList.contains(newPokemon)) {
                                currentList.add(newPokemon)
                            }
                        }
                        pokemonList.postValue(currentList)
                        insertPokemons(newList.toPokemonEntityList())
                        currentOffset += batchLimit
                    }
                } else {
                    errorMessage.postValue("Error: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<PokeApiResponse>, t: Throwable) {
                errorMessage.postValue(t.message)
                call.cancel()
            }
        })
    }
}
