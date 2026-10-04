package com.example.core.browser

import com.example.data.local.dao.BrowserProfileDao
import com.example.data.local.entity.BrowserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BrowserProfileManager(private val profileDao: BrowserProfileDao) {

    val allProfiles: Flow<List<BrowserProfileEntity>> = profileDao.getAllProfiles()

    private val _activeProfileId = MutableStateFlow<String>("profile_work")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    suspend fun createProfile(name: String, userAgent: String? = null, homeUrl: String = "https://www.google.com"): String {
        val id = "profile_${UUID.randomUUID().toString().take(8)}"
        val profile = BrowserProfileEntity(
            id = id,
            name = name,
            userAgent = userAgent,
            homeUrl = homeUrl
        )
        profileDao.insertProfile(profile)
        return id
    }

    fun selectActiveProfile(profileId: String) {
        _activeProfileId.value = profileId
    }

    suspend fun deleteProfile(profileId: String) {
        profileDao.deleteProfile(profileId)
        if (_activeProfileId.value == profileId) {
            _activeProfileId.value = "profile_work"
        }
    }
}
