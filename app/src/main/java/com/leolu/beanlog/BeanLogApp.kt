package com.leolu.beanlog

import android.app.Application
import com.leolu.beanlog.data.AppDatabase
import com.leolu.beanlog.data.BeanRepository
import com.leolu.beanlog.data.CoverStore
import com.leolu.beanlog.data.Prefs

class BeanLogApp : Application() {
    // Everything is lazy so nothing runs before the first frame needs it.
    val repository by lazy { BeanRepository(AppDatabase.create(this), CoverStore(this)) }
    val prefs by lazy { Prefs(this) }
}
