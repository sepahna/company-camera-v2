package ir.mbarani.companycamera
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class CompanyCameraViewModel(app: Application) : AndroidViewModel(app) {
 private val db = Room.databaseBuilder(app, AppDatabase::class.java, "company-camera.db").build()
 private val repository = PhotoRepository(db.photoDao())
 val photos = repository.observePhotos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
 private val _category = MutableStateFlow("MACHINERY"); val category = _category.asStateFlow()
 private val _query = MutableStateFlow(""); val query = _query.asStateFlow()
 fun setCategory(value:String){_category.value=value}
 fun setQuery(value:String){_query.value=value}
 fun add(photo:Photo)=viewModelScope.launch { repository.add(photo) }
}
