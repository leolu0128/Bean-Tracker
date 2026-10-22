package com.leolu.beantracker

import android.app.Application
import com.leolu.beantracker.data.AppDatabase
import com.leolu.beantracker.data.BeanRepository
import com.leolu.beantracker.data.CoverStore
import com.leolu.beantracker.data.Prefs

class BeanTrackerApp : Application() {
    // Everything is lazy so nothing runs before the first frame needs it.
    val repository by lazy { BeanRepository(AppDatabase.create(this), CoverStore(this)) }
    val prefs by lazy { Prefs(this) }
}
