package me.ztiany.lib.avbase.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
