import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.pokedex.DAO.PokemonDao
import com.example.pokedex.Entity.PokemonEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.*
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PokeListViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var pokemonDao: PokemonDao

    private lateinit var viewModel: PokeListViewModel

    @Before
    fun setUp() {
        viewModel = PokeListViewModel(pokemonDao)
    }

    @Test
    fun insertPokemons_success() = runTest {
        val pokemons = listOf(
            PokemonEntity(1, "Pikachu", 1, 1, 1, "", ""),
            PokemonEntity(2, "Charmander", 1, 1, 1, "", "")
        )
        val response = listOf(1L, 2L)

        // Mocking the DAO response
        Mockito.`when`(pokemonDao.insertAll(pokemons)).thenReturn(response)

        // Observers
        val statusObserver: Observer<Boolean?> = mock()
        viewModel.statusInsert.observeForever(statusObserver)

        // Execute the method
        viewModel.insertPokemons(pokemons)

        // Verify
        verify(pokemonDao).insertAll(pokemons)
        verify(statusObserver).onChanged(true)
    }

    @Test
    fun insertPokemons_failure() = runTest {
        val pokemons = listOf(
            PokemonEntity(1, "Pikachu", 1, 1, 1, "", ""),
            PokemonEntity(2, "Charmander", 1, 1, 1, "", "")
        )
        val response = listOf(1L, -1L)

        Mockito.`when`(pokemonDao.insertAll(pokemons)).thenReturn(response)

        val statusObserver: Observer<Boolean?> = mock()
        val failedObserver: Observer<Int> = mock()
        viewModel.statusInsert.observeForever(statusObserver)
        viewModel.pokemonsFailed.observeForever(failedObserver)

        viewModel.insertPokemons(pokemons)

        verify(pokemonDao).insertAll(pokemons)
        verify(failedObserver).onChanged(1)
        verify(statusObserver).onChanged(false)
    }
}
