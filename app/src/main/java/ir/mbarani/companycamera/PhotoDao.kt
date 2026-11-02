package ir.mbarani.companycamera
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface PhotoDao {
 @Insert suspend fun insert(photo: Photo): Long
 @Query("SELECT * FROM photos ORDER BY createdAt DESC") fun observeAll(): Flow<List<Photo>>
 @Query("SELECT * FROM photos ORDER BY createdAt DESC") suspend fun all(): List<Photo>
 @Query("DELETE FROM photos WHERE id = :id") suspend fun delete(id: Long)
 @Query("DELETE FROM photos") suspend fun clear()
}
