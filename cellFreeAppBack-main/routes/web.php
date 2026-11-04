<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\AdminController;
use Illuminate\Support\Facades\Auth;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| contains the "web" middleware group. Now create something great!
|
*/

Route::get('/', function () { return Auth::check() ? redirect()->route('admin.dashboard') : redirect()->route('admin.login'); });

// Ruta de login para evitar el error RouteNotFoundException
Route::get('/login', fn () => redirect()->route('admin.login'))->name('login');
Route::get('/admin/login', fn () => view('admin.login'))->name('admin.login');
Route::post('/admin/login', [AdminController::class, 'login'])->name('admin.login.submit')->middleware('throttle:10,1');
Route::middleware(['auth', 'admin.only'])->prefix('admin')->name('admin.')->group(function () {
    Route::get('/', [AdminController::class, 'dashboard'])->name('dashboard');
    Route::post('/logout', function () { Auth::logout(); request()->session()->invalidate(); request()->session()->regenerateToken(); return redirect()->route('admin.login'); })->name('logout');
    Route::get('/usuarios', [AdminController::class, 'users'])->name('users');
    Route::post('/usuarios', [AdminController::class, 'createUser'])->name('users.create');
    Route::put('/usuarios/{user}', [AdminController::class, 'updateUser'])->name('users.update');
    Route::delete('/usuarios/{user}', [AdminController::class, 'deleteUser'])->name('users.delete');
    Route::put('/administradores/{user}/permisos', [AdminController::class, 'updateAdminPermissions'])->name('admins.permissions');
    Route::patch('/usuarios/{user}/estado', [AdminController::class, 'toggleUser'])->name('users.toggle');
    Route::get('/bloqueos', [AdminController::class, 'blocks'])->name('blocks');
    Route::get('/dispositivos', [AdminController::class, 'devices'])->name('devices');
    Route::get('/reportes', [AdminController::class, 'reports'])->name('reports');
    Route::get('/reportes/csv', [AdminController::class, 'exportReport'])->name('reports.export');
    Route::get('/auditoria', [AdminController::class, 'auditLog'])->name('audit');
    Route::post('/bloqueos', [AdminController::class, 'createBlock'])->name('blocks.create');
    Route::post('/bloqueos/importar', [AdminController::class, 'importBlocks'])->name('blocks.import');
    Route::post('/bloqueos/confirmar-csv', [AdminController::class, 'confirmImportBlocks'])->name('blocks.confirm');
    Route::patch('/bloqueos/{block}/estado', [AdminController::class, 'toggleBlock'])->name('blocks.toggle');
    Route::get('/licencias', [AdminController::class, 'plans'])->name('plans');
    Route::post('/planes', [AdminController::class, 'createPlan'])->name('plans.create');
    Route::put('/planes/{plan}', [AdminController::class, 'updatePlan'])->name('plans.update');
    Route::post('/licencias', [AdminController::class, 'createLicense'])->name('licenses.create');
    Route::patch('/licencias/{license}', [AdminController::class, 'licenseAction'])->name('licenses.action');
    Route::get('/instituciones', [AdminController::class, 'institutions'])->name('institutions');
    Route::post('/instituciones', [AdminController::class, 'createInstitution'])->name('institutions.create');
    Route::put('/instituciones/{id}', [AdminController::class, 'updateInstitution'])->name('institutions.update');
});
