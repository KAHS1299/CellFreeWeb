<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Recordatorio;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Validator;
use Carbon\Carbon;

class CalendarController extends Controller
{
    /**
     * Obtener todos los recordatorios del usuario
     */
    public function index(Request $request): JsonResponse
    {
        try {
            $query = Recordatorio::where('usuario_id', $request->user()->id);

            // Filtros opcionales
            if ($request->has('tipo') && $request->tipo) {
                $query->tipo($request->tipo);
            }

            if ($request->has('completado')) {
                if ($request->boolean('completado')) {
                    $query->completado();
                } else {
                    $query->pendiente();
                }
            }

            if ($request->has('fecha_desde') && $request->has('fecha_hasta')) {
                $query->whereBetween('fecha_hora', [
                    $request->fecha_desde,
                    $request->fecha_hasta
                ]);
            }

            $recordatorios = $query->orderBy('fecha_hora', 'asc')->get();

            return response()->json([
                'status' => 'success',
                'data' => [
                    'recordatorios' => $recordatorios->map(function ($recordatorio) {
                        return $recordatorio->toApiArray();
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
     * Crear un nuevo recordatorio
     */
    public function store(Request $request): JsonResponse
    {
        try {
            // Obtener datos directamente  
            $data = $request->json()->all();
            $titulo = $data['titulo'] ?? null;
            $descripcion = $data['descripcion'] ?? null;
            $fecha_hora = $data['fecha_hora'] ?? null;
            $tipo = $data['tipo'] ?? null;
            $completado = $data['completado'] ?? false;

            // Validación manual básica
            if (!$titulo || !$fecha_hora || !$tipo) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Faltan campos requeridos: titulo, fecha_hora, tipo'
                ], 422);
            }

            if (!in_array($tipo, ['tarea', 'examen', 'estudio'])) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Tipo debe ser: tarea, examen o estudio'
                ], 422);
            }

            $recordatorio = Recordatorio::create([
                'usuario_id' => $request->user()->id,
                'titulo' => $titulo,
                'descripcion' => $descripcion,
                'fecha_hora' => $fecha_hora,
                'tipo' => $tipo,
                'completado' => $completado,
            ]);

            return response()->json([
                'status' => 'success',
                'message' => 'Recordatorio creado exitosamente',
                'data' => [
                    'recordatorio' => $recordatorio->toApiArray()
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
     * Mostrar un recordatorio específico
     */
    public function show(Request $request, $id): JsonResponse
    {
        try {
            $recordatorio = Recordatorio::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            return response()->json([
                'status' => 'success',
                'data' => [
                    'recordatorio' => $recordatorio->toApiArray()
                ]
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Recordatorio no encontrado'
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
     * Actualizar un recordatorio
     */
    public function update(Request $request, $id): JsonResponse
    {
        try {
            $validator = Validator::make($request->all(), [
                'titulo' => 'sometimes|required|string|max:100',
                'descripcion' => 'sometimes|nullable|string',
                'fecha_hora' => 'sometimes|required|date',
                'tipo' => 'sometimes|required|in:tarea,examen,estudio',
                'completado' => 'sometimes|boolean',
            ]);

            if ($validator->fails()) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Datos de validación incorrectos',
                    'errors' => $validator->errors()
                ], 422);
            }

            $recordatorio = Recordatorio::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            $recordatorio->update($request->only([
                'titulo', 'descripcion', 'fecha_hora', 'tipo', 'completado'
            ]));

            return response()->json([
                'status' => 'success',
                'message' => 'Recordatorio actualizado exitosamente',
                'data' => [
                    'recordatorio' => $recordatorio->toApiArray()
                ]
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Recordatorio no encontrado'
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
     * Eliminar un recordatorio
     */
    public function destroy(Request $request, $id): JsonResponse
    {
        try {
            $recordatorio = Recordatorio::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            $recordatorio->delete();

            return response()->json([
                'status' => 'success',
                'message' => 'Recordatorio eliminado exitosamente'
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Recordatorio no encontrado'
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
     * Marcar recordatorio como completado/pendiente
     */
    public function toggleComplete(Request $request, $id): JsonResponse
    {
        try {
            $recordatorio = Recordatorio::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            if ($recordatorio->completado) {
                $recordatorio->marcarPendiente();
                $mensaje = 'Recordatorio marcado como pendiente';
            } else {
                $recordatorio->marcarCompletado();
                $mensaje = 'Recordatorio marcado como completado';
            }

            return response()->json([
                'status' => 'success',
                'message' => $mensaje,
                'data' => [
                    'recordatorio' => $recordatorio->toApiArray()
                ]
            ], 200);

        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Recordatorio no encontrado'
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
     * Obtener recordatorios de hoy
     */
    public function getToday(Request $request): JsonResponse
    {
        try {
            $recordatorios = Recordatorio::where('usuario_id', $request->user()->id)
                ->hoy()
                ->orderBy('fecha_hora', 'asc')
                ->get();

            return response()->json([
                'status' => 'success',
                'data' => [
                    'recordatorios_hoy' => $recordatorios->map(function ($recordatorio) {
                        return $recordatorio->toApiArray();
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
     * Obtener próximos recordatorios (siguiente semana por defecto)
     */
    public function getUpcoming(Request $request): JsonResponse
    {
        try {
            $days = $request->get('days', 7);
            
            $recordatorios = Recordatorio::where('usuario_id', $request->user()->id)
                ->proximos($days)
                ->orderBy('fecha_hora', 'asc')
                ->get();

            return response()->json([
                'status' => 'success',
                'data' => [
                    'recordatorios_proximos' => $recordatorios->map(function ($recordatorio) {
                        return $recordatorio->toApiArray();
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
     * Obtener recordatorios vencidos
     */
    public function getOverdue(Request $request): JsonResponse
    {
        try {
            $recordatorios = Recordatorio::where('usuario_id', $request->user()->id)
                ->vencidos()
                ->orderBy('fecha_hora', 'desc')
                ->get();

            return response()->json([
                'status' => 'success',
                'data' => [
                    'recordatorios_vencidos' => $recordatorios->map(function ($recordatorio) {
                        return $recordatorio->toApiArray();
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
     * Obtener recordatorios por mes para vista calendario
     */
    public function getByMonth(Request $request): JsonResponse
    {
        try {
            $validator = Validator::make($request->all(), [
                'year' => 'required|integer|min:2020|max:2030',
                'month' => 'required|integer|min:1|max:12',
            ]);

            if ($validator->fails()) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Datos de validación incorrectos',
                    'errors' => $validator->errors()
                ], 422);
            }

            $startDate = Carbon::create($request->year, $request->month, 1)->startOfMonth();
            $endDate = $startDate->copy()->endOfMonth();

            $recordatorios = Recordatorio::where('usuario_id', $request->user()->id)
                ->whereBetween('fecha_hora', [$startDate, $endDate])
                ->orderBy('fecha_hora', 'asc')
                ->get();

            // Agrupar por fecha
            $recordatoriosPorFecha = $recordatorios->groupBy(function($recordatorio) {
                return $recordatorio->fecha_hora->format('Y-m-d');
            })->map(function($grupo) {
                return $grupo->map(function($recordatorio) {
                    return $recordatorio->toApiArray();
                });
            });

            return response()->json([
                'status' => 'success',
                'data' => [
                    'year' => $request->year,
                    'month' => $request->month,
                    'recordatorios_por_fecha' => $recordatoriosPorFecha
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
     * Obtener estadísticas de recordatorios
     */
    public function getStats(Request $request): JsonResponse
    {
        try {
            $userId = $request->user()->id;

            $stats = [
                'total_recordatorios' => Recordatorio::where('usuario_id', $userId)->count(),
                'recordatorios_completados' => Recordatorio::where('usuario_id', $userId)->completado()->count(),
                'recordatorios_pendientes' => Recordatorio::where('usuario_id', $userId)->pendiente()->count(),
                'recordatorios_vencidos' => Recordatorio::where('usuario_id', $userId)->vencidos()->count(),
                'recordatorios_hoy' => Recordatorio::where('usuario_id', $userId)->hoy()->count(),
                'por_tipo' => Recordatorio::where('usuario_id', $userId)
                    ->groupBy('tipo')
                    ->selectRaw('tipo, count(*) as total')
                    ->pluck('total', 'tipo'),
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

    // ===== MÉTODOS DE SINCRONIZACIÓN DE CALENDARIO =====

    /**
     * Listar sincronizaciones de calendario del usuario
     */
    public function listarSincronizaciones(Request $request): JsonResponse
    {
        try {
            $sincronizaciones = \App\Models\CalendarSync::where('usuario_id', $request->user()->id)
                ->orderBy('created_at', 'desc')
                ->get()
                ->map(fn($sync) => $sync->toApiArray());

            return response()->json([
                'status' => 'success',
                'data' => [
                    'sincronizaciones' => $sincronizaciones
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
     * Crear nueva sincronización de calendario
     */
    public function crearSincronizacion(Request $request): JsonResponse
    {
        try {
            // Validación manual básica
            $nombre = $request->input('nombre');
            $url = $request->input('url_calendario');
            $intervalo = $request->input('intervalo_sincronizacion', 60);

            if (empty($nombre) || empty($url)) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Faltan campos requeridos: nombre, url_calendario'
                ], 422);
            }

            // Validar URL
            if (!filter_var($url, FILTER_VALIDATE_URL)) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'URL de calendario inválida'
                ], 422);
            }

            // Validar intervalo
            if (!is_numeric($intervalo) || $intervalo < 15 || $intervalo > 1440) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Intervalo debe estar entre 15 y 1440 minutos'
                ], 422);
            }

            $sync = \App\Models\CalendarSync::create([
                'usuario_id' => $request->user()->id,
                'nombre' => $nombre,
                'url_calendario' => $url,
                'intervalo_sincronizacion' => $intervalo,
                'configuracion' => $request->input('configuracion', [])
            ]);

            return response()->json([
                'status' => 'success',
                'message' => 'Sincronización creada exitosamente',
                'data' => [
                    'sincronizacion' => $sync->toApiArray()
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
     * Ejecutar sincronización manual de un calendario
     */
    public function ejecutarSincronizacion(Request $request, $id): JsonResponse
    {
        try {
            $sync = \App\Models\CalendarSync::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            $syncService = new \App\Services\CalendarSyncService();
            $resultado = $syncService->sincronizarCalendario($sync);

            if ($resultado['success']) {
                return response()->json([
                    'status' => 'success',
                    'message' => 'Sincronización completada exitosamente',
                    'data' => [
                        'eventos_importados' => $resultado['eventos_importados'],
                        'total_eventos' => $resultado['total_eventos'],
                        'sincronizacion' => $sync->fresh()->toApiArray()
                    ]
                ], 200);
            } else {
                return response()->json([
                    'status' => 'error',
                    'message' => 'Error en la sincronización',
                    'error' => $resultado['error']
                ], 400);
            }

        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Actualizar sincronización de calendario
     */
    public function actualizarSincronizacion(Request $request, $id): JsonResponse
    {
        try {
            $sync = \App\Models\CalendarSync::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            // Validación básica si se proporcionan nuevos datos
            if ($request->has('nombre') && empty($request->input('nombre'))) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'El nombre no puede estar vacío'
                ], 422);
            }

            if ($request->has('url_calendario')) {
                $url = $request->input('url_calendario');
                if (!filter_var($url, FILTER_VALIDATE_URL)) {
                    return response()->json([
                        'status' => 'error',
                        'message' => 'URL de calendario inválida'
                    ], 422);
                }
            }

            // Actualizar campos
            $sync->update($request->only([
                'nombre', 
                'url_calendario', 
                'estado', 
                'intervalo_sincronizacion', 
                'configuracion'
            ]));

            return response()->json([
                'status' => 'success',
                'message' => 'Sincronización actualizada exitosamente',
                'data' => [
                    'sincronizacion' => $sync->fresh()->toApiArray()
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
     * Eliminar sincronización de calendario
     */
    public function eliminarSincronizacion(Request $request, $id): JsonResponse
    {
        try {
            $sync = \App\Models\CalendarSync::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            // Opcionalmente eliminar también los recordatorios sincronizados
            if ($request->boolean('eliminar_recordatorios', false)) {
                $syncService = new \App\Services\CalendarSyncService();
                $eliminados = $syncService->eliminarEventosSincronizados($sync);
                
                return response()->json([
                    'status' => 'success',
                    'message' => 'Sincronización y recordatorios eliminados exitosamente',
                    'data' => [
                        'recordatorios_eliminados' => $eliminados
                    ]
                ], 200);
            } else {
                $sync->delete();
                
                return response()->json([
                    'status' => 'success',
                    'message' => 'Sincronización eliminada exitosamente'
                ], 200);
            }

        } catch (\Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Error en el servidor',
                'error' => config('app.debug') ? $e->getMessage() : 'Error interno del servidor'
            ], 500);
        }
    }

    /**
     * Activar/Desactivar sincronización
     */
    public function toggleSincronizacion(Request $request, $id): JsonResponse
    {
        try {
            $sync = \App\Models\CalendarSync::where('usuario_id', $request->user()->id)
                ->findOrFail($id);

            $nuevoEstado = $sync->estado === 'activo' ? 'inactivo' : 'activo';
            $sync->update(['estado' => $nuevoEstado]);

            return response()->json([
                'status' => 'success',
                'message' => "Sincronización {$nuevoEstado}",
                'data' => [
                    'sincronizacion' => $sync->fresh()->toApiArray()
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
