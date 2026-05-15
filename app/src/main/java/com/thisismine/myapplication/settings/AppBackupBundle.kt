package com.thisismine.myapplication.settings

import com.thisismine.myapplication.data.repository.AppBackupSnapshot

data class AppBackupBundle(
    val snapshot: AppBackupSnapshot,
    val settings: AppSettings? = null
)


