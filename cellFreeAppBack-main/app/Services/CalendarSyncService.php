<?php

namespace App\Services;

use App\Models\CalendarSync;
use App\Models\Recordatorio;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use Kigkonsult\Icalcreator\Vcalendar;
use Carbon\Carbon;

class CalendarSyncService
{
    /**
     * Sincronizar calendario desde URL externa
     */
    public function sincronizarCalendario(CalendarSync $sync): array
    {
        try {
            // Descargar el contenido del calendario
            $response = Http::timeout(30)
                ->withOptions([
                    'verify' => false, // Desactivar verificación SSL para desarrollo
                    'curl' => [
                        CURLOPT_SSL_VERIFYPEER => false,
                        CURLOPT_SSL_VERIFYHOST => false,
                    ]
                ])
                ->get($sync->url_calendario);

            if (!$response->successful()) {
                throw new \Exception("Error al descargar calendario: HTTP {$response->status()}");
            }

            $icalContent = $response->body();

            // Parsear el contenido iCal
            $eventos = $this->parsearIcal($icalContent);

            // Importar eventos como recordatorios
            $importados = $this->importarEventos($sync, $eventos);

            // Marcar sincronización como exitosa
            $sync->marcarSincronizada();

            Log::info('Sincronización de calendario completada', [
                'sync_id' => $sync->id,
                'eventos_importados' => $importados,
                'total_eventos' => count($eventos)
            ]);

            return [
                'success' => true,
                'eventos_importados' => $importados,
                'total_eventos' => count($eventos)
            ];

        } catch (\Exception $e) {
            $sync->marcarError($e->getMessage());
            Log::error('Error en sincronización de calendario', [
                'sync_id' => $sync->id,
                'error' => $e->getMessage()
            ]);

            return [
                'success' => false,
                'error' => $e->getMessage()
            ];
        }
    }

    /**
     * Parsear contenido iCal y extraer eventos
     */
    private function parsearIcal(string $icalContent): array
    {
        $eventos = [];

        try {
            $vcalendar = Vcalendar::factory()->parse($icalContent);

            foreach ($vcalendar->getComponents() as $component) {
                if (strtoupper($component->getCompType()) === 'VEVENT') {
                    $evento = $this->extraerDatosEvento($component);
                    if ($evento) {
                        $eventos[] = $evento;
                    }
                }
            }

        } catch (\Exception $e) {
            Log::error('Error parseando iCal', [
                'error' => $e->getMessage()
            ]);
            throw new \Exception('Error al parsear el calendario: ' . $e->getMessage());
        }

        return $eventos;
    }

    /**
     * Extraer datos de un evento individual
     */
    private function extraerDatosEvento($component): ?array
    {
        try {
            $uid = $component->getUid();
            $summary = $component->getSummary();
            $description = $component->getDescription();
            $dtstart = $component->getDtstart();

            if (!$uid || !$summary || !$dtstart) {
                return null;
            }

            // Convertir fecha/hora
            $fechaHora = $this->convertirFecha($dtstart);

            return [
                'uid_externo' => is_string($uid) ? $uid : (method_exists($uid, 'getValue') ? $uid->getValue() : $uid),
                'titulo' => is_string($summary) ? $summary : (method_exists($summary, 'getValue') ? $summary->getValue() : $summary),
                'descripcion' => $description ? 
                    (is_string($description) ? $description : (method_exists($description, 'getValue') ? $description->getValue() : $description)) : '',
                'fecha_hora' => $fechaHora,
                'tipo' => $this->determinarTipo(
                    is_string($summary) ? $summary : (method_exists($summary, 'getValue') ? $summary->getValue() : $summary)
                ),
            ];

        } catch (\Exception $e) {
            Log::warning('Error extrayendo evento', ['error' => $e->getMessage()]);
            return null;
        }
    }

    /**
     * Convertir fecha del formato iCal a Carbon
     */
    private function convertirFecha($dtstart): Carbon
    {
        if (is_object($dtstart)) {
            // Si es DateTime object
            return Carbon::parse($dtstart->format('Y-m-d H:i:s'));
        } elseif (is_string($dtstart)) {
            // Si es string en formato iCal
            return Carbon::createFromFormat('Ymd\THis\Z', $dtstart) ?: 
                   Carbon::createFromFormat('Ymd', $dtstart) ?:
                   Carbon::parse($dtstart);
        }
        
        return Carbon::parse($dtstart);
    }

    /**
     * Determinar tipo de recordatorio basado en el título
     */
    private function determinarTipo(string $titulo): string
    {
        $titulo = strtolower($titulo);

        if (str_contains($titulo, 'examen') || str_contains($titulo, 'prueba') || str_contains($titulo, 'test')) {
            return 'examen';
        } elseif (str_contains($titulo, 'tarea') || str_contains($titulo, 'entrega') || str_contains($titulo, 'homework')) {
            return 'tarea';
        } else {
            return 'estudio';
        }
    }

    /**
     * Importar eventos como recordatorios
     */
    private function importarEventos(CalendarSync $sync, array $eventos): int
    {
        $importados = 0;

        foreach ($eventos as $evento) {
            try {
                // Verificar si el evento ya existe (evitar duplicados)
                $existe = Recordatorio::where('usuario_id', $sync->usuario_id)
                    ->where('uid_externo', $evento['uid_externo'])
                    ->exists();

                if (!$existe) {
                    Recordatorio::create([
                        'usuario_id' => $sync->usuario_id,
                        'calendario_sync_id' => $sync->id,
                        'uid_externo' => $evento['uid_externo'],
                        'titulo' => $evento['titulo'],
                        'descripcion' => $evento['descripcion'],
                        'fecha_hora' => $evento['fecha_hora'],
                        'tipo' => $evento['tipo'],
                        'completado' => false,
                        'es_sincronizado' => true,
                    ]);

                    $importados++;
                }

            } catch (\Exception $e) {
                Log::warning('Error importando evento', [
                    'evento' => $evento,
                    'error' => $e->getMessage()
                ]);
            }
        }

        return $importados;
    }

    /**
     * Sincronizar todas las sincronizaciones activas
     */
    public function sincronizarTodas(): array
    {
        $syncs = CalendarSync::paraSincronizar()->get();
        $resultados = [];

        foreach ($syncs as $sync) {
            $resultado = $this->sincronizarCalendario($sync);
            $resultados[] = [
                'sync_id' => $sync->id,
                'nombre' => $sync->nombre,
                'resultado' => $resultado
            ];
        }

        return $resultados;
    }

    /**
     * Eliminar eventos sincronizados de un calendario específico
     */
    public function eliminarEventosSincronizados(CalendarSync $sync): int
    {
        return Recordatorio::where('usuario_id', $sync->usuario_id)
            ->where('calendario_sync_id', $sync->id)
            ->delete();
    }
}