package com.example.plannerapp.creator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plannerapp.data.PlannerDao
import com.example.plannerapp.data.UserDao

class CreatorStudioViewModelFactory(
    private val creatorRepository: CreatorRepository,
    private val userDao: UserDao,
    private val plannerDao: PlannerDao,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatorStudioViewModel::class.java)) {
            return CreatorStudioViewModel(
                creatorRepository = creatorRepository,
                userDao = userDao,
                plannerDao = plannerDao,
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
