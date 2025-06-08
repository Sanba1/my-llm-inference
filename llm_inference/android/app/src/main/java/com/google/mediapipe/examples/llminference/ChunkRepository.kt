package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data class for PDF chunks.
 * This is the model used throughout your app.
 */
data class ChunkData(
    val chunkId: String,
    val text: String,
    val embedding: FloatArray
)

/**
 * Room Entity for persisting a chunk.
 * The embedding is stored as a comma-separated String.
 */
@Entity(tableName = "chunks")
data class ChunkEntity(
    @PrimaryKey val chunkId: String,
    val text: String,
    val embeddingJson: String
)

/**
 * DAO for accessing chunk data.
 */
@Dao
interface ChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: ChunkEntity)

    @Query("SELECT * FROM chunks")
    suspend fun getAllChunks(): List<ChunkEntity>

    @Query("DELETE FROM chunks")
    suspend fun clearAllChunks()
}

/**
 * Room Database for storing chunks.
 */
@Database(entities = [ChunkEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chunkDao(): ChunkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chunk_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Repository to manage ChunkData.
 * Converts between ChunkData and ChunkEntity.
 * Call [initialize] before using the repository.
 */
object ChunkRepository {
    // Must be initialized before using any methods.
    lateinit var db: AppDatabase

    /**
     * Initializes the repository with a Room database.
     * Call this (e.g., in Application.onCreate) with a valid Context.
     */
    fun initialize(context: Context) {
        db = AppDatabase.getDatabase(context)
    }

    /**
     * Inserts a chunk into persistent storage.
     */
    suspend fun addChunk(chunk: ChunkData) {
        val entity = ChunkEntity(
            chunkId = chunk.chunkId,
            text = chunk.text,
            embeddingJson = convertEmbeddingToJson(chunk.embedding)
        )
        db.chunkDao().insertChunk(entity)
    }

    /**
     * Retrieves all stored chunks, converting them back to ChunkData.
     */
    suspend fun getAllChunks(): List<ChunkData> {
        return db.chunkDao().getAllChunks().map { entity ->
            ChunkData(
                chunkId = entity.chunkId,
                text = entity.text,
                embedding = convertJsonToEmbedding(entity.embeddingJson)
            )
        }
    }

    /**
     * Clears all stored chunks.
     */
    suspend fun clearAll() {
        db.chunkDao().clearAllChunks()
    }

    /**
     * Helper: Converts a FloatArray to a comma-separated String.
     */
    private fun convertEmbeddingToJson(embedding: FloatArray): String {
        return embedding.joinToString(separator = ",")
    }

    /**
     * Helper: Converts a comma-separated String back to a FloatArray.
     */
    private fun convertJsonToEmbedding(json: String): FloatArray {
        return json.split(",").map { it.toFloat() }.toFloatArray()
    }
}
