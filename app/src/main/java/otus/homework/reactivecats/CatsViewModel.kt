package otus.homework.reactivecats

import android.content.Context

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class CatsViewModel(
    private val catsService: CatsService,
    private val localCatFactsGenerator: LocalCatFactsGenerator,
    context: Context
) : ViewModel() {

    private val _catsLiveData = MutableLiveData<Result>()
    val catsLiveData: LiveData<Result> = _catsLiveData

    private val disposable = CompositeDisposable()

    init {
        disposable.add(
            catsService.getCatFact()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { fact: Fact -> _catsLiveData.value = Success(fact) },
                    { t: Throwable -> _catsLiveData.value = ServerError }
                ))
    }

    fun getFacts() {
        disposable.add(
            catsService.getCatFact()
                .repeatWhen { it.delay(2000, TimeUnit.MILLISECONDS) }
                .onExceptionResumeNext { localCatFactsGenerator.generateCatFact() }
                .subscribe(
                    { },
                    { }
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}

class CatsViewModelFactory(
    private val catsRepository: CatsService,
    private val localCatFactsGenerator: LocalCatFactsGenerator,
    private val context: Context
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatsViewModel(catsRepository, localCatFactsGenerator, context) as T
}

sealed class Result
data class Success(val fact: Fact) : Result()
data class Error(val message: String) : Result()
object ServerError : Result()


