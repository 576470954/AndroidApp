package cn.latlaser.coordinate.data.database

import androidx.room.*
import cn.latlaser.coordinate.data.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY CAST(serialNumber AS INTEGER) ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id IN (:ids)")
    suspend fun deleteProjectsByIds(ids: List<Long>)

    @Query("SELECT serialNumber FROM projects")
    suspend fun getAllSerialNumbers(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM projects WHERE name = :name)")
    suspend fun isNameExists(name: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM projects WHERE name = :name AND id != :excludeId)")
    suspend fun isNameExistsExcludingId(name: String, excludeId: Long): Boolean
}
