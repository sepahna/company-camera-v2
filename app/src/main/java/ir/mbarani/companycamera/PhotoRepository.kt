package ir.mbarani.companycamera
import kotlinx.coroutines.flow.Flow
class PhotoRepository(private val dao: PhotoDao) {
 fun observePhotos(): Flow<List<Photo>> = dao.observeAll()
 suspend fun add(photo: Photo) = dao.insert(photo)
 suspend fun all() = dao.all()
}
