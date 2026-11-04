package com.example.cellfreeapp.data.api

import com.example.cellfreeapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface BlockedAppsApiService {
    
    @GET("app-blocks")
    suspend fun getBlockedApps(
        @Query("categoria") categoria: String? = null,
        @Query("estado") estado: Boolean? = null,
        @Query("activo_ahora") activoAhora: Boolean? = null,
        @Header("Authorization") authorization: String
    ): Response<BlockedAppsResponse>
    
    @POST("app-blocks")
    suspend fun createBlockedApp(
        @Body request: BlockedAppRequest,
        @Header("Authorization") authorization: String
    ): Response<BlockedAppsResponse>
    
    @GET("app-blocks/{id}")
    suspend fun getBlockedApp(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<BlockedAppsResponse>
    
    @PUT("app-blocks/{id}")
    suspend fun updateBlockedApp(
        @Path("id") id: Int,
        @Body request: BlockedAppRequest,
        @Header("Authorization") authorization: String
    ): Response<BlockedAppsResponse>
    
    @DELETE("app-blocks/{id}")
    suspend fun deleteBlockedApp(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<BlockedAppsResponse>
    
    @PATCH("app-blocks/{id}/toggle-status")
    suspend fun toggleAppStatus(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<BlockedAppsResponse>
    
    @GET("app-blocks/active/list")
    suspend fun getActiveBlockedApps(
        @Header("Authorization") authorization: String
    ): Response<BlockedAppsResponse>
    
    @GET("app-blocks/stats/summary")
    suspend fun getBlockStats(
        @Header("Authorization") authorization: String
    ): Response<BlockStatsResponse>
}