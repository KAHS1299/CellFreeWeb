<?php

use App\Http\Controllers\Api\AppBlockController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\CalendarController;
use App\Http\Controllers\Api\MobileSyncController;
use App\Http\Controllers\Api\StudentController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Routes loaded by the RouteServiceProvider with the "api" middleware group.
|
*/

Route::prefix('auth')->group(function () {
    Route::post('register', [AuthController::class, 'register']);
    Route::post('login', [AuthController::class, 'login']);
});

Route::prefix('v1')
    ->middleware(['auth:sanctum', 'student.only'])
    ->group(function () {
        Route::post('devices', [MobileSyncController::class, 'registerDevice']);
        Route::post('applications/sync', [MobileSyncController::class, 'syncApps']);
        Route::get('license', [MobileSyncController::class, 'license']);
    });

Route::middleware('auth:sanctum')->group(function () {
    Route::prefix('auth')->group(function () {
        Route::get('user', [AuthController::class, 'user']);
        Route::post('logout', [AuthController::class, 'logout']);
        Route::post('logout-all', [AuthController::class, 'logoutAll']);
        Route::put('update-profile', [AuthController::class, 'updateProfile']);
        Route::delete('delete-account', [AuthController::class, 'deleteAccount']);
    });

    Route::middleware('student.only')->group(function () {
        Route::prefix('auth/student')->group(function () {
            Route::get('institution', [StudentController::class, 'institution']);
            Route::get('applications', [StudentController::class, 'applications']);
            Route::get('productivity', [StudentController::class, 'productivity']);
            Route::get('restrictions/history', [StudentController::class, 'restrictionHistory']);

            Route::get('goals', [StudentController::class, 'goals']);
            Route::post('goals', [StudentController::class, 'createGoal']);
            Route::put('goals/{id}', [StudentController::class, 'updateGoal']);
            Route::delete('goals/{id}', [StudentController::class, 'deleteGoal']);

            Route::get('rewards', [StudentController::class, 'rewards']);
            Route::get('groups', [StudentController::class, 'groups']);
            Route::post('groups/join', [StudentController::class, 'joinGroup']);
            Route::get('groups/{groupId}/members', [StudentController::class, 'members']);
            Route::get('challenges', [StudentController::class, 'challenges']);
            Route::post('challenges/{challengeId}/join', [StudentController::class, 'joinChallenge']);
        });

        Route::prefix('app-blocks')->group(function () {
            Route::get('/', [AppBlockController::class, 'index']);
            Route::post('/', [AppBlockController::class, 'store']);
            Route::get('{id}', [AppBlockController::class, 'show']);
            Route::put('{id}', [AppBlockController::class, 'update']);
            Route::delete('{id}', [AppBlockController::class, 'destroy']);
            Route::patch('{id}/toggle-status', [AppBlockController::class, 'toggleStatus']);
            Route::get('active/list', [AppBlockController::class, 'getActiveApps']);
            Route::get('stats/summary', [AppBlockController::class, 'getStats']);
        });

        Route::prefix('reminders')->group(function () {
            Route::get('/', [CalendarController::class, 'index']);
            Route::post('/', [CalendarController::class, 'store']);
            Route::get('{id}', [CalendarController::class, 'show']);
            Route::put('{id}', [CalendarController::class, 'update']);
            Route::delete('{id}', [CalendarController::class, 'destroy']);
            Route::patch('{id}/toggle-complete', [CalendarController::class, 'toggleComplete']);
            Route::get('today/list', [CalendarController::class, 'getToday']);
            Route::get('upcoming/list', [CalendarController::class, 'getUpcoming']);
            Route::get('overdue/list', [CalendarController::class, 'getOverdue']);
            Route::get('calendar/month', [CalendarController::class, 'getByMonth']);
            Route::get('stats/summary', [CalendarController::class, 'getStats']);
        });

        Route::prefix('calendar-sync')->group(function () {
            Route::get('/', [CalendarController::class, 'listarSincronizaciones']);
            Route::post('/', [CalendarController::class, 'crearSincronizacion']);
            Route::put('{id}', [CalendarController::class, 'actualizarSincronizacion']);
            Route::delete('{id}', [CalendarController::class, 'eliminarSincronizacion']);
            Route::post('{id}/sync', [CalendarController::class, 'ejecutarSincronizacion']);
            Route::patch('{id}/toggle', [CalendarController::class, 'toggleSincronizacion']);
        });
    });

    // Compatibilidad con clientes antiguos; devuelve únicamente la cuenta autenticada.
    Route::get('user', function (Request $request) {
        return $request->user();
    });
});
