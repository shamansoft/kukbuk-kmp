package net.shamansoft.kukbuk.di

import org.koin.core.module.Module

/**
 * iOS actual implementation - uses common modules
 */
actual fun getPlatformProductionModules(): List<Module> = getCommonProductionModules()

actual fun getPlatformLocalDevModules(): List<Module> = getCommonLocalDevModules()
