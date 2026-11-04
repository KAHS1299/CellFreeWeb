<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AplicacionBloqueada;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Validator;

class AppBlockController extends Controller
{
    /**
     * Obtener todas las aplicaciones bloqueadas del usuario
     */
    public function index(Request $request): JsonResponse
    {
        try {
            $query = AplicacionBloqueada::where('usuario_id', $request->user()->id);

            // Filtros opcionales
            if ($request->has('categoria') && $request->categoria) {
                $query->categoria($request->categoria);
            }

            if ($request->has('estado')) {
                $query->where('estado', $request->boolean('estado'));
            }

            // Filtro para solo aplicaciones activas en el periodo actual
            if ($request->has('activo_ahora') && $request->boolean('activo_ahora')) {
                $query->activoEnPeriodo();
            }

            $aplicaciones = $query->orderBy('created_at', 'desc')->get();

            return response()->json([
                'status' => 'success',
                'data' => [
                    'aplicaciones_bloqueadas' => $aplicaciones->map(function ($app) {
                        $data = $app->toApiArray();
                        // Agregar información adicional sobre el estado actual
                        $data['activo_ahora'] = $app->estaActivoAhora();
                        $data['tiempo_restante'] = $app->tiempoRestante();
                        return $data;
                    })
                ]
            ], 200);

        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Crear una nueva aplicación bloqueada
     */
    public function store(Request $request): JsonResponse
    {
        try {
            $validator = Validator::make($request->all(), [
                'nombre_app' => 'required|string|max:100',
                'categoria' => 'nullable|string|max:50',
                'tiempo_bloqueo' => 'nullable|integer|min:1',
                'estado' => 'boolean',
                'nombre_paquete' => 'nullable|string|max:100',
                'fecha_inicio' => 'nullable|date',
                'fecha_fin' => 'nullable|date|after:fecha_inicio',
            ]);

            if ($validator->fails()) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Datos de validación incorrectos',
                    'errors' => $validator->errors()
                ], 422);
            }

            // Verificar si la aplicación ya está bloqueada para este usuario
            $existeApp = AplicacionBloqueada::where('usuario_id', $request->user()->id)
                ->where('nombre_app', $request->nombre_app)
                ->first();

            if ($existeApp) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Esta aplicación ya está en tu lista de bloqueo'
                ], 409);
            }

            $aplicacion = AplicacionBloqueada::create([
                'usuario_id' => $request->user()->id,
                'nombre_app' => $request->nombre_app,
                'categoria' => $request->categoria,
                'tiempo_bloqueo' => $request->tiempo_bloqueo,
                'estado' => $request->boolean('estado', true),
                'fecha_inicio' => $request->fecha_inicio,
                'fecha_fin' => $request->fecha_fin,
                'nombre_paquete' => $request->nombre_paquete,
            ]);

            return response()->json([
                'status' => 'success',
                'message' => 'Aplicación bloqueada agregada exitosamente',
                'data' => [
                    'aplicacion_bloqueada' => array_merge($aplicacion->toApiArray(), [
                        'activo_ahora' => $aplicacion->estaActivoAhora(),
                        'tiempo_restante' => $aplicacion->tiempoRestante(),
                    ])
                ]
            ], 201);

        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Mostrar una aplicación bloqueada específica
     */
    public function show(Request $request, $id): JsonResponse
    {
        try {
            $aplicacion = AplicacionBloqueada::where('usuario_id', $request->user()->id)
                ->whereNull('administrador_id')
                ->findOrFail($id);

            return response()->json([
                'status' => 'success',
                'data' => [
                    'aplicacion_bloqueada' => $aplicacion->toApiArray()
                ]
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Aplicación bloqueada no encontrada'
            ], 404);
        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Actualizar una aplicación bloqueada
     */
    public function update(Request $request, $id): JsonResponse
    {
        try {
            $validator = Validator::make($request->all(), [
                'nombre_app' => 'sometimes|required|string|max:100',
                'categoria' => 'sometimes|nullable|string|max:50',
                'tiempo_bloqueo' => 'sometimes|nullable|integer|min:1',
                'estado' => 'sometimes|boolean',
                'fecha_inicio' => 'sometimes|nullable|date',
                'fecha_fin' => 'sometimes|nullable|date|after:fecha_inicio',
                'nombre_paquete' => 'sometimes|nullable|string|max:100',
            ]);

            if ($validator->fails()) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Datos de validación incorrectos',
                    'errors' => $validator->errors()
                ], 422);
            }

            $aplicacion = AplicacionBloqueada::where('usuario_id', $request->user()->id)
                ->whereNull('administrador_id')
                ->findOrFail($id);

            // Verificar duplicados si se cambia el nombre
            if ($request->has('nombre_app') && $request->nombre_app !== $aplicacion->nombre_app) {
                $existeApp = AplicacionBloqueada::where('usuario_id', $request->user()->id)
                    ->where('nombre_app', $request->nombre_app)
                    ->where('id', '!=', $id)
                    ->first();

                if ($existeApp) {
                    return response()->json([
                        'status' => 'error',
                        'message' => 'Ya existe una aplicación con ese nombre en tu lista'
                    ], 409);
                }
            }

            $aplicacion->update($request->only([
                'nombre_app',
                'categoria',
                'tiempo_bloqueo',
                'estado',
                'fecha_inicio',
                'fecha_fin',
                'nombre_paquete'
            ]));

            return response()->json([
                'status' => 'success',
                'message' => 'Aplicación bloqueada actualizada exitosamente',
                'data' => [
                    'aplicacion_bloqueada' => array_merge($aplicacion->toApiArray(), [
                        'activo_ahora' => $aplicacion->estaActivoAhora(),
                        'tiempo_restante' => $aplicacion->tiempoRestante(),
                    ])
                ]
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Aplicación bloqueada no encontrada'
            ], 404);
        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Eliminar una aplicación bloqueada
     */
    public function destroy(Request $request, $id): JsonResponse
    {
        try {
            $aplicacion = AplicacionBloqueada::where('usuario_id', $request->user()->id)
                ->whereNull('administrador_id')
                ->findOrFail($id);

            $aplicacion->delete();

            return response()->json([
                'status' => 'success',
                'message' => 'Aplicación bloqueada eliminada exitosamente'
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Aplicación bloqueada no encontrada'
            ], 404);
        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Activar o desactivar una aplicación bloqueada
     */
    public function toggleStatus(Request $request, $id): JsonResponse
    {
        try {
            $aplicacion = AplicacionBloqueada::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            $aplicacion->estado = !$aplicacion->estado;
            $aplicacion->save();

            $mensaje = $aplicacion->estado ? 'Aplicación activada exitosamente' : 'Aplicación desactivada exitosamente';

            return response()->json([
                'status' => 'success',
                'message' => $mensaje,
                'data' => [
                    'aplicacion_bloqueada' => $aplicacion->toApiArray()
                ]
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Aplicación bloqueada no encontrada'
            ], 404);
        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Obtener aplicaciones activas para bloqueo
     */
    public function getActiveApps(Request $request): JsonResponse
    {
        try {
            $aplicaciones = AplicacionBloqueada::where('usuario_id', $request->user()->id)
                ->activoEnPeriodo()
                ->orderBy('nombre_app')
                ->get();

            return response()->json([
                'status' => 'success',
                'data' => [
                    'aplicaciones_activas' => $aplicaciones->map(function ($app) {
                        return array_merge($app->toApiArray(), [
                            'activo_ahora' => $app->estaActivoAhora(),
                            'tiempo_restante' => $app->tiempoRestante(),
                        ]);
                    })
                ]
            ], 200);

        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Obtener estadísticas de aplicaciones bloqueadas
     */
    public function getStats(Request $request): JsonResponse
    {
        try {
            $userId = $request->user()->id;

            $stats = [
                'total_aplicaciones' => AplicacionBloqueada::where('usuario_id', $userId)->count(),
                'aplicaciones_activas' => AplicacionBloqueada::where('usuario_id', $userId)->activo()->count(),
                'aplicaciones_inactivas' => AplicacionBloqueada::where('usuario_id', $userId)->where('estado', false)->count(),
                'categorias' => AplicacionBloqueada::where('usuario_id', $userId)
                    ->whereNotNull('categoria')
                    ->groupBy('categoria')
                    ->selectRaw('categoria, count(*) as total')
                    ->pluck('total', 'categoria'),
            ];

            return response()->json([
                'status' => 'success',
                'data' => [
                    'estadisticas' => $stats
                ]
            ], 200);

        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }
}
